package com.inscopelabs.abx.xtools.webview

import android.util.Log
import com.inscopelabs.abx.xtools.BuildConfig

/**
 * DebugConsoleLogger provides console logging capabilities
 * for WebView JavaScript execution.
 */
class DebugConsoleLogger {
    private val tag = "xtools:webview"

    /**
     * Get the JavaScript console override script.
     */
    fun getConsoleScript(): String {
        return """
            (function() {
                var originalConsole = {
                    log: console.log.bind(console),
                    info: console.info.bind(console),
                    warn: console.warn.bind(console),
                    error: console.error.bind(console),
                    debug: console.debug.bind(console)
                };

                function formatArgs(args) {
                    return Array.from(args).map(function(arg) {
                        if (typeof arg === 'object') {
                            try {
                                return JSON.stringify(arg);
                            } catch (e) {
                                return String(arg);
                            }
                        }
                        return String(arg);
                    }).join(' ');
                }

                console.log = function() {
                    AndroidBridge.log('info', formatArgs(arguments));
                    originalConsole.log.apply(console, arguments);
                };

                console.info = function() {
                    AndroidBridge.log('info', formatArgs(arguments));
                    originalConsole.info.apply(console, arguments);
                };

                console.warn = function() {
                    AndroidBridge.log('warning', formatArgs(arguments));
                    originalConsole.warn.apply(console, arguments);
                };

                console.error = function() {
                    AndroidBridge.log('error', formatArgs(arguments));
                    originalConsole.error.apply(console, arguments);
                };

                console.debug = function() {
                    AndroidBridge.log('debug', formatArgs(arguments));
                    originalConsole.debug.apply(console, arguments);
                };

                window.onerror = function(msg, url, line, col, error) {
                    var message = msg + ' at ' + url + ':' + line + ':' + col;
                    AndroidBridge.log('error', message);
                    return false;
                };

                window.onunhandledrejection = function(event) {
                    AndroidBridge.log('error', 'Unhandled Promise rejection: ' + event.reason);
                };
            })();
        """.trimIndent()
    }

    fun log(level: String, message: String) {
        when (level) {
            "debug" -> Log.d(tag, message)
            "info" -> Log.i(tag, message)
            "warning", "warn" -> Log.w(tag, message)
            "error" -> Log.e(tag, message)
            else -> Log.v(tag, message)
        }
    }

    fun logRequest(url: String) {
        Log.d(tag, "[REQUEST] $url")
    }

    fun logNavigation(url: String) {
        Log.d(tag, "[NAVIGATION] $url")
    }

    fun logPageFinished(url: String) {
        Log.d(tag, "[PAGE FINISHED] $url")
    }

    fun logError(message: String) {
        Log.e(tag, "[ERROR] $message")
    }
}
