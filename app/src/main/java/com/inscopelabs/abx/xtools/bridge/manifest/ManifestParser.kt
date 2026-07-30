package com.inscopelabs.abx.xtools.bridge.manifest

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.inscopelabs.abx.xtools.bridge.protocol.BridgeError
import com.inscopelabs.abx.xtools.bridge.protocol.BridgeErrorCodes

class ManifestParser {
    private val gson = Gson()

    @Throws(BridgeError::class)
    fun parse(json: String): PluginManifest {
        return try {
            gson.fromJson(json, PluginManifest::class.java).also {
                // Basic validation
                if (it.id.isBlank()) throw BridgeError(BridgeErrorCodes.INVALID_PARAMS, "Missing plugin ID")
                if (it.version.isBlank()) throw BridgeError(BridgeErrorCodes.INVALID_PARAMS, "Missing version")
                if (it.name.isBlank()) throw BridgeError(BridgeErrorCodes.INVALID_PARAMS, "Missing name")
            }
        } catch (e: JsonSyntaxException) {
            throw BridgeError(BridgeErrorCodes.PARSE_ERROR, "Invalid manifest JSON: ${e.message}")
        }
    }
}
