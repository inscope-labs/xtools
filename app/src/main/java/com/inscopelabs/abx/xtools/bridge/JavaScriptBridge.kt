package com.inscopelabs.abx.xtools.bridge

import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.inscopelabs.abx.xtools.webview.DebugConsoleLogger
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * JavaScriptBridge handles bidirectional communication between
 * JavaScript and Kotlin through the WebView bridge.
 */
class JavaScriptBridge(private val webView: WebView) {

    private val gson = Gson()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val debugLogger = DebugConsoleLogger()

    private val pendingCallbacks = ConcurrentHashMap<String, (BridgeResponse) -> Unit>()

    interface BridgeCallback {
        fun onMessage(message: BridgeMessage): BridgeResponse?
        fun onError(error: String)
    }

    private var callback: BridgeCallback? = null

    fun setBridgeCallback(callback: BridgeCallback) {
        this.callback = callback
    }

    /**
     * Inject the AndroidBridge into the WebView JavaScript context.
     */
    fun inject() {
        val script = """
            (function() {
                window.AndroidBridge = {
                    _callbacks: {},
                    _eventListeners: {},

                    call: function(action, params, callback) {
                        var id = 'cb_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
                        var message = {
                            id: id,
                            action: action,
                            params: params || {},
                            timestamp: Date.now()
                        };

                        if (callback) {
                            this._callbacks[id] = callback;
                        }

                        window.prompt(JSON.stringify(message));
                        return id;
                    },

                    on: function(event, listener) {
                        if (!this._eventListeners[event]) {
                            this._eventListeners[event] = [];
                        }
                        this._eventListeners[event].push(listener);
                    },

                    off: function(event, listener) {
                        if (this._eventListeners[event]) {
                            this._eventListeners[event] = this._eventListeners[event].filter(
                                function(l) { return l !== listener; }
                            );
                        }
                    },

                    emit: function(event, data) {
                        if (this._eventListeners[event]) {
                            this._eventListeners[event].forEach(function(listener) {
                                try {
                                    listener(data);
                                } catch (e) {
                                    console.error('Event listener error:', e);
                                }
                            });
                        }
                    },

                    log: function(level, message) {
                        console.log('[JS->Native] [' + level + '] ' + message);
                    }
                };

                window.xtools = window.AndroidBridge;

                // Signal ready
                document.dispatchEvent(new CustomEvent('android-bridge-ready'));
            })();
        """.trimIndent()

        mainHandler.post {
            webView.evaluateJavascript(script) { result ->
                debugLogger.log("info", "AndroidBridge injected: $result")
            }
        }
    }

    /**
     * Handle messages from JavaScript (triggered by window.prompt).
     */
    fun handleMessage(message: String?): String? {
        if (message.isNullOrBlank()) return null

        return try {
            val bridgeMessage = gson.fromJson(message, BridgeMessage::class.java)
            handleBridgeMessage(bridgeMessage)
        } catch (e: JsonSyntaxException) {
            debugLogger.logError("Failed to parse bridge message: ${e.message}")
            createErrorResponse("invalid_message", "Failed to parse message")
        } catch (e: Exception) {
            debugLogger.logError("Bridge error: ${e.message}")
            createErrorResponse("bridge_error", e.message ?: "Unknown error")
        }
    }

    private fun handleBridgeMessage(message: BridgeMessage): String {
        return try {
            val response = callback?.onMessage(message)
            if (response != null) {
                gson.toJson(response)
            } else {
                // Process the action and return response
                val result = processAction(message)
                gson.toJson(result)
            }
        } catch (e: Exception) {
            debugLogger.logError("Action processing error: ${e.message}")
            gson.toJson(BridgeResponse.error(message.id, e.message ?: "Unknown error"))
        }
    }

    private fun processAction(message: BridgeMessage): BridgeResponse {
        return try {
            when (message.action) {
                BridgeContract.Actions.LOG -> {
                    val level = message.params?.get("level") as? String ?: "info"
                    val text = message.params?.get("message") as? String ?: ""
                    debugLogger.log(level, "[JS] $text")
                    BridgeResponse.success(message.id)
                }

                BridgeContract.Actions.GET_DEVICE_INFO -> {
                    // Device info is handled by the callback
                    callback?.onMessage(message)?.let { return it }
                    BridgeResponse.success(message.id, emptyMap<String, Any>())
                }

                else -> {
                    // Pass to callback for handling
                    callback?.onMessage(message)?.let { return it }
                    BridgeResponse.error(message.id, "Unknown action: ${message.action}")
                }
            }
        } catch (e: Exception) {
            BridgeResponse.error(message.id, e.message ?: "Action failed")
        }
    }

    /**
     * Send an event to JavaScript.
     */
    fun sendEvent(event: BridgeEvent) {
        mainHandler.post {
            val script = """
                (function() {
                    var event = ${gson.toJson(event)};
                    if (window.AndroidBridge && window.AndroidBridge.emit) {
                        window.AndroidBridge.emit(event.event, event.data);
                    }
                })();
            """.trimIndent()

            webView.evaluateJavascript(script) { result ->
                debugLogger.log("info", "Event sent: ${event.event}")
            }
        }
    }

    /**
     * Send a response to a specific callback.
     */
    fun sendResponse(response: BridgeResponse) {
        mainHandler.post {
            val script = """
                (function() {
                    var response = ${gson.toJson(response)};
                    var callback = window.AndroidBridge._callbacks[response.id];
                    if (callback) {
                        try {
                            callback(response);
                            delete window.AndroidBridge._callbacks[response.id];
                        } catch (e) {
                            console.error('Callback error:', e);
                        }
                    }
                })();
            """.trimIndent()

            webView.evaluateJavascript(script, null)
        }
    }

    /**
     * Clear all pending callbacks.
     */
    fun clearCallbacks() {
        pendingCallbacks.clear()
    }

    private fun createErrorResponse(errorCode: String, errorMessage: String): String {
        return gson.toJson(
            BridgeResponse(
                id = UUID.randomUUID().toString(),
                success = false,
                error = "$errorCode: $errorMessage"
            )
        )
    }
}
