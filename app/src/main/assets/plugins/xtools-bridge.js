/**
 * xtools-bridge.js
 * JavaScript SDK for xtools Web Application Plugins
 */
(function(window) {
    'use strict';

    if (window.XTools) {
        console.warn('XTools bridge SDK already initialized.');
        return;
    }

    var callbacks = {};
    var eventListeners = {};
    var messageIdCounter = 0;

    function generateId() {
        return 'msg_' + Date.now() + '_' + (++messageIdCounter);
    }

    function sendToNative(action, payload) {
        return new Promise(function(resolve, reject) {
            var id = generateId();
            callbacks[id] = { resolve: resolve, reject: reject };

            var message = {
                id: id,
                action: action,
                payload: payload || {}
            };

            if (window.XToolsNativeBridge && typeof window.XToolsNativeBridge.postMessage === 'function') {
                window.XToolsNativeBridge.postMessage(JSON.stringify(message));
            } else {
                reject(new Error('Native bridge interface not available'));
            }
        });
    }

    // Called by Kotlin bridge upon response
    window.__xtools_on_native_response = function(responseJson) {
        try {
            var response = typeof responseJson === 'string' ? JSON.parse(responseJson) : responseJson;
            var callback = callbacks[response.id];
            if (callback) {
                if (response.error) {
                    callback.reject(new Error(response.error));
                } else {
                    callback.resolve(response.result);
                }
                delete callbacks[response.id];
            }
        } catch (e) {
            console.error('[XTools SDK] Failed to process native response:', e);
        }
    };

    // Called by Kotlin bridge to trigger native events
    window.__xtools_on_native_event = function(eventName, dataJson) {
        try {
            var data = typeof dataJson === 'string' ? JSON.parse(dataJson) : dataJson;
            var listeners = eventListeners[eventName];
            if (listeners && listeners.length) {
                listeners.forEach(function(listener) {
                    try {
                        listener(data);
                    } catch (err) {
                        console.error('[XTools SDK] Error in event listener for ' + eventName, err);
                    }
                });
            }
        } catch (e) {
            console.error('[XTools SDK] Failed to process native event:', e);
        }
    };

    var XTools = {
        version: '1.0.0',

        // Storage API (Encrypted SharedPreferences / Sandboxed storage)
        storage: {
            get: function(key) {
                return sendToNative('storage.get', { key: key });
            },
            set: function(key, value) {
                return sendToNative('storage.set', { key: key, value: String(value) });
            },
            remove: function(key) {
                return sendToNative('storage.remove', { key: key });
            },
            clear: function() {
                return sendToNative('storage.clear', {});
            }
        },

        // UI API (Native toasts, dialogs, vibration, theme)
        ui: {
            showToast: function(message) {
                return sendToNative('ui.toast', { message: message });
            },
            showDialog: function(title, message, confirmText) {
                return sendToNative('ui.dialog', { title: title, message: message, confirmText: confirmText });
            },
            vibrate: function(durationMs) {
                return sendToNative('ui.vibrate', { durationMs: durationMs || 100 });
            },
            getTheme: function() {
                return sendToNative('ui.getTheme', {});
            }
        },

        // System API (Device information, SHA-256 verification, uptime, battery)
        system: {
            getInfo: function() {
                return sendToNative('system.getInfo', {});
            },
            calculateHash: function(content) {
                return sendToNative('system.sha256', { content: content });
            },
            getAppId: function() {
                return sendToNative('system.getAppId', {});
            }
        },

        // HTTP / Network API (Proxied through Kotlin for CSP bypass / secure fetching)
        http: {
            fetch: function(url, options) {
                options = options || {};
                return sendToNative('http.fetch', {
                    url: url,
                    method: options.method || 'GET',
                    headers: options.headers || {},
                    body: options.body || ''
                });
            }
        },

        // Events API
        events: {
            on: function(eventName, callback) {
                if (!eventListeners[eventName]) {
                    eventListeners[eventName] = [];
                }
                eventListeners[eventName].push(callback);
            },
            off: function(eventName, callback) {
                if (eventListeners[eventName]) {
                    eventListeners[eventName] = eventListeners[eventName].filter(function(cb) {
                        return cb !== callback;
                    });
                }
            }
        }
    };

    window.XTools = XTools;
    console.log('[XTools SDK] JavaScript bridge SDK loaded v' + XTools.version);
})(window);
