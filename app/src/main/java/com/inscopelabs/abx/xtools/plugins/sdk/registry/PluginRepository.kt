package com.inscopelabs.abx.xtools.plugins.sdk.registry

import com.inscopelabs.abx.xtools.plugins.sdk.api.PluginId
import com.inscopelabs.abx.xtools.plugins.sdk.api.PluginManifest
import com.inscopelabs.abx.xtools.plugins.sdk.api.RegisteredPlugin
import com.inscopelabs.abx.xtools.plugins.sdk.bridge.ManifestCodec
import java.io.File

/**
 * Persistent half of [com.inscopelabs.abx.xtools.plugins.sdk.api.PluginRegistry].
 * Stores the manifest + install metadata in a single JSON file under
 * `<pluginsRoot>/.xtools/registry.json`. Cheap to read at boot, simple to
 * back up, easy to diff for debugging.
 */
class PluginRepository(private val file: File) {

    fun load(): List<RegisteredPlugin> {
        if (!file.exists()) return emptyList()
        val text = runCatching { file.readText() }.getOrNull() ?: return emptyList()
        val arr = runCatching { org.json.JSONArray(text) }.getOrNull() ?: return emptyList()
        val out = mutableListOf<RegisteredPlugin>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val manifestJson = o.optJSONObject("manifest")?.toString() ?: continue
            val manifest = runCatching {
                ManifestCodec.decode(manifestJson)
            }.getOrNull() ?: continue
            out += RegisteredPlugin(
                id = PluginId.of(o.optString("id")),
                manifest = manifest,
                installPath = o.optString("installPath"),
                signatureValid = o.optBoolean("signatureValid", false),
                installedAtMs = o.optLong("installedAtMs", 0L),
                version = o.optString("version", manifest.version),
            )
        }
        return out
    }

    fun save(plugins: List<RegisteredPlugin>) {
        file.parentFile?.mkdirs()
        val arr = org.json.JSONArray()
        for (p in plugins) {
            arr.put(
                org.json.JSONObject().apply {
                    put("id", p.id.value)
                    put("installPath", p.installPath)
                    put("signatureValid", p.signatureValid)
                    put("installedAtMs", p.installedAtMs)
                    put("version", p.version)
                    put("manifest", org.json.JSONObject(ManifestCodec.encode(p.manifest)))
                }
            )
        }
        file.writeText(arr.toString(2), Charsets.UTF_8)
    }
}
