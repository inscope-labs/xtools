package com.inscopelabs.abx.xtools.bridge

import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.inscopelabs.abx.xtools.bridge.api.BridgeApiFacade
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class JsBridge(
    private val handler: BridgeHandler? = null,
    private val scope: CoroutineScope,
    private val facade: BridgeApiFacade? = null
) {
    private var webViewRef: WebView? = null
    private var activePluginId: String = "system"

    fun attachWebView(webView: WebView, pluginId: String = "system") {
        this.webViewRef = webView
        this.activePluginId = pluginId
    }

    fun detachWebView() {
        this.webViewRef = null
    }

    @JavascriptInterface
    fun postMessage(messageJson: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val request = BridgeRequest.fromJson(messageJson, activePluginId)
                if (facade != null) {
                    val response = facade.execute(request.pluginId.ifEmpty { activePluginId }, request)
                    sendResponseToJs(response)
                } else if (handler != null) {
                    handler.handleRequest(request) { response ->
                        sendResponseToJs(response)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val errResponse = BridgeResponse.error("unknown", e.message ?: "Bridge execution error")
                sendResponseToJs(errResponse)
            }
        }
    }

    private fun sendResponseToJs(response: BridgeResponse) {
        val jsCode = "window.__xtools_on_native_response(${JSONObjectEscape.escape(response.toJson())});"
        webViewRef?.post {
            webViewRef?.evaluateJavascript(jsCode, null)
        }
    }

    fun sendEventToJs(eventName: String, dataJson: String) {
        val jsCode = "window.__xtools_on_native_event('${eventName}', ${dataJson});"
        webViewRef?.post {
            webViewRef?.evaluateJavascript(jsCode, null)
        }
    }

    private object JSONObjectEscape {
        fun escape(str: String): String {
            return org.json.JSONObject.quote(str)
        }
    }
}
