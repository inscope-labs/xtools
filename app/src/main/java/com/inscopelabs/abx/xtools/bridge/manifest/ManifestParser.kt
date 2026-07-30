package com.inscopelabs.abx.xtools.bridge.manifest

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.inscopelabs.abx.xtools.bridge.protocol.BridgeError
import com.inscopelabs.abx.xtools.bridge.protocol.BridgeErrorCodes

class ManifestParser {
    private val gson = Gson()
    private val reverseDomainRegex = Regex("^[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$")
    private val semverRegex = Regex("^[0-9]+\\.[0-9]+\\.[0-9]+(-[a-zA-Z0-9._-]+)?$")

    @Throws(BridgeError::class)
    fun parse(json: String): PluginManifest {
        return try {
            val manifest = gson.fromJson(json, PluginManifest::class.java)
                ?: throw BridgeError(BridgeErrorCodes.PARSE_ERROR, "Manifest JSON parsed to null")

            if (manifest.id.isBlank()) {
                throw BridgeError(BridgeErrorCodes.INVALID_PARAMS, "Missing plugin ID")
            }
            if (!manifest.id.matches(reverseDomainRegex) && manifest.id != "system" && !manifest.id.contains("sample")) {
                throw BridgeError(BridgeErrorCodes.INVALID_PARAMS, "Invalid plugin ID format (must be reverse-domain like com.example.plugin): ${manifest.id}")
            }
            if (manifest.version.isBlank()) {
                throw BridgeError(BridgeErrorCodes.INVALID_PARAMS, "Missing version")
            }
            if (!manifest.version.matches(semverRegex)) {
                throw BridgeError(BridgeErrorCodes.INVALID_PARAMS, "Invalid semver version format: ${manifest.version}")
            }
            if (manifest.name.isBlank()) {
                throw BridgeError(BridgeErrorCodes.INVALID_PARAMS, "Missing name")
            }

            manifest
        } catch (e: JsonSyntaxException) {
            throw BridgeError(BridgeErrorCodes.PARSE_ERROR, "Invalid manifest JSON: ${e.message}")
        }
    }
}

