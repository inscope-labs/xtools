package com.inscopelabs.abx.xtools.plugin

import org.json.JSONObject

data class PluginManifest(
    val id: String,
    val name: String,
    val version: String,
    val author: String,
    val description: String,
    val entry: String = "index.html",
    val permissions: List<String> = emptyList(),
    val checksum: String = ""
) {
    companion object {
        fun fromJson(jsonStr: String): PluginManifest {
            val obj = JSONObject(jsonStr)
            val permissionsList = mutableListOf<String>()
            val permArray = obj.optJSONArray("permissions")
            if (permArray != null) {
                for (i in 0 until permArray.length()) {
                    permissionsList.add(permArray.getString(i))
                }
            }
            return PluginManifest(
                id = obj.optString("id", "unknown_plugin"),
                name = obj.optString("name", "Unnamed Plugin"),
                version = obj.optString("version", "1.0.0"),
                author = obj.optString("author", "Unknown"),
                description = obj.optString("description", ""),
                entry = obj.optString("entry", "index.html"),
                permissions = permissionsList,
                checksum = obj.optString("checksum", "")
            )
        }
    }
}

data class Plugin(
    val manifest: PluginManifest,
    val localPath: String, // asset path e.g. "plugins/sample" or file path
    val isInstalled: Boolean = true,
    val isEnabled: Boolean = true,
    val isBuiltIn: Boolean = false,
    val installationTimestamp: Long = System.currentTimeMillis()
)
