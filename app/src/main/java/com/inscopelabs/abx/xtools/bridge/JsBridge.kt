package com.inscopelabs.abx.xtools.bridge

import android.webkit.JavascriptInterface
import android.webkit.WebView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class JsBridge(
    private val handler: BridgeHandler,
    private val scope: CoroutineScope
) {
    private var webViewRef: WebView? = null

    fun attachWebView(webView: WebView) {
        this.webViewRef = webView
    }

    fun detachWebView() {
        this.webViewRef = null
    }

    @JavascriptInterface
    fun postMessage(messageJson: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val request = BridgeRequest.fromJson(messageJson)
                handler.handleRequest(request) { response ->
                    sendResponseToJs(response)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                handler.logError("JS Bridge parsing error: ${e.message}")
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
