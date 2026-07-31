package com.inscopelabs.abx.xtools.plugins.sdk.bridge

import com.inscopelabs.abx.xtools.plugins.sdk.api.PluginManifest
import com.inscopelabs.abx.xtools.plugins.sdk.api.PluginSdk
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Reads and writes the plugin manifest. The codec is intentionally
 * permissive on read (unknown keys are ignored — forward compat) and
 * strict on write (re-serialization is canonical).
 */
object ManifestCodec {

    val json: Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun decode(bytes: ByteArray): PluginManifest = try {
        json.decodeFromString(PluginManifest.serializer(), String(bytes, Charsets.UTF_8))
    } catch (e: SerializationException) {
        throw IllegalArgumentException("plugin-manifest.json is malformed: ${e.message}", e)
    } catch (e: IllegalArgumentException) {
        throw e
    }

    fun decode(text: String): PluginManifest = decode(text.toByteArray(Charsets.UTF_8))

    fun encode(manifest: PluginManifest): String = json.encodeToString(manifest)

    /**
     * Validate the schema-version field. Returns `null` if the manifest is
     * acceptable, otherwise a human-readable reason.
     */
    fun schemaCheck(manifest: PluginManifest): String? {
        if (manifest.schema < 1) return "schema must be >= 1"
        if (manifest.schema > PluginSdk.CURRENT_MANIFEST_SCHEMA) {
            return "manifest schema ${manifest.schema} is newer than SDK supports (${PluginSdk.CURRENT_MANIFEST_SCHEMA})"
        }
        return null
    }
}
