package com.inscopelabs.abx.xtools.bridge

import android.content.Context
import android.database.Cursor
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import com.inscopelabs.abx.xtools.plugin.manager.SecurityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class BridgeHandler(
    private val context: Context,
    private val securityManager: SecurityManager
) {
    private val _consoleLogs = MutableStateFlow<List<ConsoleLogEntry>>(emptyList())
    val consoleLogs = _consoleLogs.asStateFlow()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    var activePluginId: String = "system"

    fun log(level: String, message: String) {
        val entry = ConsoleLogEntry(
            level = level,
            message = message,
            pluginId = activePluginId
        )
        _consoleLogs.value = listOf(entry) + _consoleLogs.value.take(199)
    }

    fun logError(message: String) {
        log("ERROR", message)
    }

    fun clearLogs() {
        _consoleLogs.value = emptyList()
    }

    suspend fun handleRequest(
        request: BridgeRequest,
        onResponse: (BridgeResponse) -> Unit
    ) {
        log("LOG", "Request [${request.action}] ID: ${request.id}")

        val requiredPermission = getRequiredPermission(request.action)
        if (requiredPermission != null && !securityManager.hasPermission(activePluginId, requiredPermission)) {
            log("ERROR", "Permission denied for ${request.action} on plugin $activePluginId")
            onResponse(BridgeResponse(request.id, error = "Permission '$requiredPermission' required"))
            return
        }

        try {
            when (request.action) {
                "storage.get", "getPreferences" -> {
                    val key = request.payload.optString("key")
                    val value = securityManager.getEncryptedStorage(activePluginId, key)
                    onResponse(BridgeResponse(request.id, result = value))
                }
                "storage.set", "setPreferences" -> {
                    val key = request.payload.optString("key")
                    val value = request.payload.optString("value")
                    securityManager.setEncryptedStorage(activePluginId, key, value)
                    onResponse(BridgeResponse(request.id, result = true))
                }
                "storage.remove" -> {
                    val key = request.payload.optString("key")
                    securityManager.removeEncryptedStorage(activePluginId, key)
                    onResponse(BridgeResponse(request.id, result = true))
                }
                "storage.clear" -> {
                    securityManager.clearEncryptedStorage(activePluginId)
                    onResponse(BridgeResponse(request.id, result = true))
                }

                "db.execute" -> {
                    val sql = request.payload.optString("sql")
                    if (sql.isBlank()) {
                        onResponse(BridgeResponse(request.id, error = "SQL query cannot be empty"))
                        return
                    }
                    val safePluginName = activePluginId.replace("[^a-zA-Z0-9_]".toRegex(), "_")
                    val db = context.openOrCreateDatabase("plugin_${safePluginName}.db", Context.MODE_PRIVATE, null)
                    db.execSQL(sql)
                    db.close()
                    onResponse(BridgeResponse(request.id, result = true))
                }
                "db.query" -> {
                    val sql = request.payload.optString("sql")
                    if (sql.isBlank()) {
                        onResponse(BridgeResponse(request.id, error = "SQL query cannot be empty"))
                        return
                    }
                    val safePluginName = activePluginId.replace("[^a-zA-Z0-9_]".toRegex(), "_")
                    val db = context.openOrCreateDatabase("plugin_${safePluginName}.db", Context.MODE_PRIVATE, null)
                    val cursor = db.rawQuery(sql, null)
                    val rows = JSONArray()
                    while (cursor.moveToNext()) {
                        val row = JSONObject()
                        for (i in 0 until cursor.columnCount) {
                            val colName = cursor.getColumnName(i)
                            when (cursor.getType(i)) {
                                Cursor.FIELD_TYPE_INTEGER -> row.put(colName, cursor.getLong(i))
                                Cursor.FIELD_TYPE_FLOAT -> row.put(colName, cursor.getDouble(i))
                                Cursor.FIELD_TYPE_STRING -> row.put(colName, cursor.getString(i))
                                Cursor.FIELD_TYPE_BLOB -> row.put(colName, cursor.getBlob(i).toString())
                                else -> row.put(colName, JSONObject.NULL)
                            }
                        }
                        rows.put(row)
                    }
                    cursor.close()
                    db.close()
                    onResponse(BridgeResponse(request.id, result = rows))
                }

                "ui.toast", "showToast" -> {
                    val message = request.payload.optString("message", "")
                    val durationStr = request.payload.optString("duration", "short")
                    val duration = if (durationStr.equals("long", ignoreCase = true)) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, message, duration).show()
                    }
                    onResponse(BridgeResponse(request.id, result = true))
                }
                "ui.vibrate" -> {
                    val durationMs = request.payload.optLong("durationMs", 100)
                    triggerVibration(durationMs)
                    onResponse(BridgeResponse(request.id, result = true))
                }
                "ui.dialog" -> {
                    val title = request.payload.optString("title", "Plugin Alert")
                    val message = request.payload.optString("message", "")
                    log("INFO", "Dialog triggered: $title - $message")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "[$title] $message", Toast.LENGTH_LONG).show()
                    }
                    onResponse(BridgeResponse(request.id, result = true))
                }
                "ui.getTheme" -> {
                    val themeObj = JSONObject()
                    themeObj.put("primary", "#0284C7")
                    themeObj.put("background", "#0F172A")
                    themeObj.put("isDark", true)
                    onResponse(BridgeResponse(request.id, result = themeObj))
                }

                "system.getInfo", "getDeviceInfo" -> {
                    val info = JSONObject()
                    info.put("platform", "android")
                    info.put("platformVersion", Build.VERSION.RELEASE)
                    info.put("manufacturer", Build.MANUFACTURER)
                    info.put("model", Build.MODEL)
                    info.put("release", Build.VERSION.RELEASE)
                    info.put("sdk", Build.VERSION.SDK_INT)
                    info.put("appId", context.packageName)
                    info.put("language", java.util.Locale.getDefault().language)
                    info.put("time", System.currentTimeMillis())
                    onResponse(BridgeResponse(request.id, result = info))
                }
                "getPluginInfo" -> {
                    val pluginInfo = JSONObject()
                    pluginInfo.put("id", activePluginId)
                    pluginInfo.put("name", activePluginId.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() })
                    pluginInfo.put("version", "1.0.0")
                    onResponse(BridgeResponse(request.id, result = pluginInfo))
                }
                "checkPermission", "requestPermission" -> {
                    val perm = request.payload.optString("permission", "")
                    val isGranted = if (perm.isNotBlank()) securityManager.hasPermission(activePluginId, perm) else true
                    onResponse(BridgeResponse(request.id, result = isGranted))
                }
                "openUrl", "navigate" -> {
                    val url = request.payload.optString("url", "")
                    log("INFO", "Navigation requested to $url")
                    onResponse(BridgeResponse(request.id, result = true))
                }
                "share" -> {
                    log("INFO", "Share triggered for plugin $activePluginId")
                    onResponse(BridgeResponse(request.id, result = true))
                }
                "close" -> {
                    log("INFO", "Close requested for plugin $activePluginId")
                    onResponse(BridgeResponse(request.id, result = true))
                }
                "pickFile" -> {
                    onResponse(BridgeResponse(request.id, result = null))
                }
                "system.sha256" -> {
                    val content = request.payload.optString("content", "")
                    val hash = calculateSha256(content)
                    onResponse(BridgeResponse(request.id, result = hash))
                }
                "system.getAppId" -> {
                    onResponse(BridgeResponse(request.id, result = context.packageName))
                }

                "http.fetch" -> {
                    val url = request.payload.optString("url")
                    val method = request.payload.optString("method", "GET")
                    val headersObj = request.payload.optJSONObject("headers")
                    val bodyStr = request.payload.optString("body", "")

                    if (url.isBlank()) {
                        onResponse(BridgeResponse(request.id, error = "URL cannot be empty"))
                        return
                    }

                    val reqBuilder = Request.Builder().url(url)
                    headersObj?.keys()?.forEach { hKey ->
                        reqBuilder.addHeader(hKey, headersObj.optString(hKey))
                    }

                    if (method.equals("POST", ignoreCase = true) || method.equals("PUT", ignoreCase = true)) {
                        reqBuilder.method(method.uppercase(), bodyStr.toRequestBody())
                    } else {
                        reqBuilder.method(method.uppercase(), null)
                    }

                    try {
                        val response = withContext(Dispatchers.IO) {
                            httpClient.newCall(reqBuilder.build()).execute()
                        }
                        val respBody = response.body?.string() ?: ""
                        val resultObj = JSONObject()
                        resultObj.put("status", response.code)
                        resultObj.put("isSuccessful", response.isSuccessful)
                        resultObj.put("data", respBody)
                        onResponse(BridgeResponse(request.id, result = resultObj))
                    } catch (e: Exception) {
                        onResponse(BridgeResponse(request.id, error = "Network Error: ${e.message}"))
                    }
                }

                else -> {
                    log("WARN", "Unknown action: ${request.action}")
                    onResponse(BridgeResponse(request.id, error = "Unsupported bridge action: ${request.action}"))
                }
            }
        } catch (e: Exception) {
            logError("Bridge Handler Exception on ${request.action}: ${e.message}")
            onResponse(BridgeResponse(request.id, error = e.message ?: "Bridge Execution Exception"))
        }
    }

    private fun getRequiredPermission(action: String): String? {
        return when {
            action.startsWith("storage.") -> "storage"
            action.startsWith("db.") -> "storage"
            action.startsWith("ui.") -> "ui"
            action.startsWith("system.") -> "system"
            action.startsWith("http.") -> "http"
            else -> null
        }
    }

    private fun calculateSha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun triggerVibration(durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val vibrator = vibratorManager.defaultVibrator
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(durationMs)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
