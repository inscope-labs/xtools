package com.inscopelabs.abx.xtools.bridge

import android.content.Context
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
                "storage.get" -> {
                    val key = request.payload.optString("key")
                    val value = securityManager.getEncryptedStorage(activePluginId, key)
                    onResponse(BridgeResponse(request.id, result = value))
                }
                "storage.set" -> {
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

                "ui.toast" -> {
                    val message = request.payload.optString("message", "")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
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

                "system.getInfo" -> {
                    val info = JSONObject()
                    info.put("manufacturer", Build.MANUFACTURER)
                    info.put("model", Build.MODEL)
                    info.put("release", Build.VERSION.RELEASE)
                    info.put("sdk", Build.VERSION.SDK_INT)
                    info.put("appId", context.packageName)
                    info.put("time", System.currentTimeMillis())
                    onResponse(BridgeResponse(request.id, result = info))
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
