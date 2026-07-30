package com.inscopelabs.abx.xtools.security

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue

enum class AuditEventType {
    PLUGIN_INSTALLED,
    PLUGIN_ACTIVATED,
    PERMISSION_GRANTED,
    PERMISSION_REVOKED,
    BRIDGE_CALL,
    SECURITY_VIOLATION,
    SYSTEM_ERROR
}

data class AuditEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: AuditEventType,
    val pluginId: String,
    val action: String? = null,
    val outcome: String, // e.g. SUCCESS, DENIED, ERROR
    val details: String? = null // High-level status text only. NO request/response payloads or file contents.
) {
    fun toJson(): String {
        return JSONObject().apply {
            put("id", id)
            put("timestamp", timestamp)
            put("eventType", eventType.name)
            put("pluginId", pluginId)
            if (action != null) put("action", action)
            put("outcome", outcome)
            if (details != null) put("details", details)
        }.toString()
    }

    companion object {
        fun fromJson(jsonStr: String): AuditEntry? {
            return try {
                val obj = JSONObject(jsonStr)
                AuditEntry(
                    id = obj.optString("id", UUID.randomUUID().toString()),
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                    eventType = AuditEventType.valueOf(obj.getString("eventType")),
                    pluginId = obj.getString("pluginId"),
                    action = if (obj.has("action")) obj.getString("action") else null,
                    outcome = obj.getString("outcome"),
                    details = if (obj.has("details")) obj.getString("details") else null
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

/**
 * Dedicated Audit Logger for security and bridge event recording.
 * Stores logs in dedicated local file storage without recording request/response payloads or file contents.
 */
class AuditLogger(private val context: Context? = null) {

    private val inMemoryLogs = ConcurrentLinkedQueue<AuditEntry>()
    private val maxInMemoryEntries = 500

    private fun getAuditFile(): File? {
        val ctx = context ?: return null
        val dir = File(ctx.filesDir, "audit_logs")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "security_audit.log")
    }

    @Synchronized
    fun log(entry: AuditEntry) {
        inMemoryLogs.add(entry)
        while (inMemoryLogs.size > maxInMemoryEntries) {
            inMemoryLogs.poll()
        }

        val auditFile = getAuditFile() ?: return
        try {
            FileOutputStream(auditFile, true).use { stream ->
                stream.write((entry.toJson() + "\n").toByteArray())
            }
        } catch (_: Exception) {
            // Silently fail if file IO unavailable
        }
    }

    fun logPluginInstall(pluginId: String, success: Boolean, reason: String? = null) {
        log(
            AuditEntry(
                eventType = AuditEventType.PLUGIN_INSTALLED,
                pluginId = pluginId,
                outcome = if (success) "SUCCESS" else "FAILED",
                details = reason
            )
        )
    }

    fun logPluginActivation(pluginId: String, activated: Boolean) {
        log(
            AuditEntry(
                eventType = AuditEventType.PLUGIN_ACTIVATED,
                pluginId = pluginId,
                outcome = if (activated) "ACTIVATED" else "DEACTIVATED"
            )
        )
    }

    fun logPermissionEvent(pluginId: String, permission: String, granted: Boolean) {
        log(
            AuditEntry(
                eventType = if (granted) AuditEventType.PERMISSION_GRANTED else AuditEventType.PERMISSION_REVOKED,
                pluginId = pluginId,
                action = permission,
                outcome = if (granted) "GRANTED" else "REVOKED"
            )
        )
    }

    fun logBridgeCall(pluginId: String, action: String, outcome: String, summaryText: String? = null) {
        // NOTE: Strictly excludes payloads or file contents
        log(
            AuditEntry(
                eventType = AuditEventType.BRIDGE_CALL,
                pluginId = pluginId,
                action = action,
                outcome = outcome,
                details = summaryText
            )
        )
    }

    fun logSecurityViolation(pluginId: String, violation: String) {
        log(
            AuditEntry(
                eventType = AuditEventType.SECURITY_VIOLATION,
                pluginId = pluginId,
                outcome = "VIOLATION_DETECTED",
                details = violation
            )
        )
    }

    fun logSystemError(pluginId: String, errorMsg: String) {
        log(
            AuditEntry(
                eventType = AuditEventType.SYSTEM_ERROR,
                pluginId = pluginId,
                outcome = "ERROR",
                details = errorMsg
            )
        )
    }

    fun getLogs(pluginIdFilter: String? = null, limit: Int = 100): List<AuditEntry> {
        val auditFile = getAuditFile()
        val entries = mutableListOf<AuditEntry>()

        if (auditFile != null && auditFile.exists()) {
            try {
                auditFile.forEachLine { line ->
                    if (line.isNotBlank()) {
                        AuditEntry.fromJson(line)?.let { entry ->
                            if (pluginIdFilter == null || entry.pluginId == pluginIdFilter) {
                                entries.add(entry)
                            }
                        }
                    }
                }
            } catch (_: Exception) {
            }
        } else {
            entries.addAll(inMemoryLogs.filter { pluginIdFilter == null || it.pluginId == pluginIdFilter })
        }

        return entries.takeLast(limit)
    }

    fun clearLogs() {
        inMemoryLogs.clear()
        getAuditFile()?.delete()
    }
}
