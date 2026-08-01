package com.inscopelabs.abx.xtools.plugins.sdk.packaging

import java.io.File

/**
 * Helpers for exporting/importing project bundles — the format is a ZIP
 * with a fixed top-level `manifest.json` (not the plugin manifest, but
 * a transport-level envelope) so we can version the exchange format.
 */
object ExportHelper {

    private const val FORMAT_VERSION: Int = 1

    fun envelopeJson(projectName: String): String = """
        {
          "format": "xtools-project",
          "formatVersion": $FORMAT_VERSION,
          "name": ${q(projectName)},
          "exportedAt": ${q(java.time.Instant.now().toString())}
        }
    """.trimIndent()

    fun parseEnvelope(text: String): Envelope? = runCatching {
        // Light-touch parse — just enough to refuse the wrong format.
        val obj = org.json.JSONObject(text)
        if (obj.optString("format") != "xtools-project") return null
        Envelope(
            formatVersion = obj.optInt("formatVersion", 0),
            name = obj.optString("name"),
            exportedAt = obj.optString("exportedAt"),
        )
    }.getOrNull()

    data class Envelope(
        val formatVersion: Int,
        val name: String,
        val exportedAt: String,
    )

    private fun q(s: String): String = org.json.JSONObject.quote(s)
}
