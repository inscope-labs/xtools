package com.inscopelabs.abx.xtools.kernel.dispatcher.handlers

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import com.inscopelabs.abx.xtools.BuildConfig
import com.inscopelabs.abx.xtools.bridge.BridgeRequest
import com.inscopelabs.abx.xtools.bridge.BridgeResponse
import com.inscopelabs.abx.xtools.bridge.protocol.BridgeErrorCodes
import com.inscopelabs.abx.xtools.kernel.dispatcher.BridgeActionHandler
import com.inscopelabs.abx.xtools.kernel.dispatcher.BridgeDispatcher
import com.inscopelabs.abx.xtools.kernel.mode.ModeArbiter
import org.json.JSONObject

/**
 * Handler for system.getDeviceInfo action.
 * Returns real Android build specs, platform version, app version, and current kernel operating mode.
 */
class GetDeviceInfoHandler(
    private val context: Context,
    private val modeArbiter: ModeArbiter
) : BridgeActionHandler {
    override val actionName: String = "system.getDeviceInfo"
    override val requiredCapability: String = "system"

    override suspend fun handle(pluginId: String, request: BridgeRequest): BridgeResponse {
        val mode = modeArbiter.currentMode.value.name
        val info = JSONObject().apply {
            put("model", Build.MODEL)
            put("manufacturer", Build.MANUFACTURER)
            put("platform", "android")
            put("platformVersion", Build.VERSION.RELEASE ?: Build.VERSION.SDK_INT.toString())
            put("release", Build.VERSION.RELEASE ?: "")
            put("sdk", Build.VERSION.SDK_INT)
            put("appVersion", BuildConfig.VERSION_NAME)
            put("appId", BuildConfig.APPLICATION_ID)
            put("operatingMode", mode)
            put("language", context.resources.configuration.locales.get(0)?.language ?: "en")
        }
        return BridgeResponse.success(request.id, info)
    }
}

class ClipboardReadHandler(
    private val context: Context
) : BridgeActionHandler {
    override val actionName: String = "clipboard.read"
    override val requiredCapability: String = "clipboard"

    override suspend fun handle(pluginId: String, request: BridgeRequest): BridgeResponse {
        val clipboardManager = context.getSystemService(ClipboardManager::class.java)
        val clip = clipboardManager?.primaryClip
        val text = if (clip != null && clip.itemCount > 0) {
            clip.getItemAt(0).coerceToText(context)?.toString() ?: ""
        } else {
            ""
        }
        val result = JSONObject().apply { put("text", text) }
        return BridgeResponse.success(request.id, result)
    }
}

class ClipboardWriteHandler(
    private val context: Context
) : BridgeActionHandler {
    override val actionName: String = "clipboard.write"
    override val requiredCapability: String = "clipboard"

    override suspend fun handle(pluginId: String, request: BridgeRequest): BridgeResponse {
        val text = request.payload.optString("text", "")
        if (text.isBlank()) {
            return BridgeResponse.error(
                id = request.id,
                error = "INVALID_PARAMS: 'text' field is required and must not be blank.",
                code = BridgeErrorCodes.INVALID_PARAMS,
                contextData = mapOf("action" to actionName)
            )
        }
        val clipboardManager = context.getSystemService(ClipboardManager::class.java)
        val clipData = ClipData.newPlainText("xtools-plugin-clip", text)
        clipboardManager?.setPrimaryClip(clipData)
        return BridgeResponse.success(request.id, null)
    }
}

/**
 * Generic handler for Phase 4 bridge actions that are not yet implemented.
 * Returns a structured NOT_YET_IMPLEMENTED error response compliant with BridgeResponse schema.
 */
class NotYetImplementedActionHandler(
    override val actionName: String,
    override val requiredCapability: String? = null
) : BridgeActionHandler {
    override suspend fun handle(pluginId: String, request: BridgeRequest): BridgeResponse {
        return BridgeResponse.error(
            id = request.id,
            error = "NOT_YET_IMPLEMENTED: Bridge action '$actionName' is not available in Phase 1 (scheduled for Phase 4).",
            code = BridgeErrorCodes.METHOD_NOT_FOUND,
            contextData = mapOf("action" to actionName, "phase" to "Phase 4")
        )
    }
}

object DefaultHandlerRegistry {
    fun registerDefaultHandlers(
        dispatcher: BridgeDispatcher,
        context: Context,
        modeArbiter: ModeArbiter
    ) {
        // Real handlers
        dispatcher.registerHandler(GetDeviceInfoHandler(context, modeArbiter))
        dispatcher.registerHandler(ClipboardReadHandler(context))
        dispatcher.registerHandler(ClipboardWriteHandler(context))

        // System handlers (NOT_YET_IMPLEMENTED)
        dispatcher.registerHandler(NotYetImplementedActionHandler("system.showNotification", "system"))
        dispatcher.registerHandler(NotYetImplementedActionHandler("system.requestPermission", "system"))
        dispatcher.registerHandler(NotYetImplementedActionHandler("system.getPreference", "system"))

        // Storage handlers (NOT_YET_IMPLEMENTED - Phase 4)
        dispatcher.registerHandler(NotYetImplementedActionHandler("storage.read", "storage"))
        dispatcher.registerHandler(NotYetImplementedActionHandler("storage.write", "storage"))
        dispatcher.registerHandler(NotYetImplementedActionHandler("storage.list", "storage"))
        dispatcher.registerHandler(NotYetImplementedActionHandler("storage.createDirectory", "storage"))
        dispatcher.registerHandler(NotYetImplementedActionHandler("storage.deleteFile", "storage"))
        dispatcher.registerHandler(NotYetImplementedActionHandler("storage.deleteDirectory", "storage"))

        // Context handlers (NOT_YET_IMPLEMENTED - Phase 4)
        dispatcher.registerHandler(NotYetImplementedActionHandler("context.addEntry", "context"))
        dispatcher.registerHandler(NotYetImplementedActionHandler("context.getEntries", "context"))
        dispatcher.registerHandler(NotYetImplementedActionHandler("context.exportContext", "context"))
        dispatcher.registerHandler(NotYetImplementedActionHandler("context.clearContext", "context"))
        dispatcher.registerHandler(NotYetImplementedActionHandler("context.estimateSize", "context"))
    }
}
