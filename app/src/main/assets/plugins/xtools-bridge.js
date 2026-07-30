/**
 * xtools JavaScript Bridge SDK
 *
 * This script provides the JavaScript interface for communicating
 * with the native Android host application.
 */

(function(global) {
    'use strict';

    // Auto-create AndroidBridge shim if XToolsNativeBridge is available
    if (!global.AndroidBridge && global.XToolsNativeBridge) {
        var callbacks = {};
        var eventListeners = {};
        var callbackIdCounter = 0;

        global.AndroidBridge = {
            call: function(action, params, callback) {
                var callbackId = 'cb_' + Date.now() + '_' + (++callbackIdCounter);
                if (typeof callback === 'function') {
                    callbacks[callbackId] = callback;
                }

                var message = {
                    id: callbackId,
                    action: action,
                    payload: params || {}
                };

                try {
                    global.XToolsNativeBridge.postMessage(JSON.stringify(message));
                } catch (e) {
                    console.error('xtools: Error posting message to native bridge', e);
                    if (callback) {
                        callback({ success: false, error: e.message });
                    }
                }
                return callbackId;
            },

            on: function(event, listener) {
                if (!eventListeners[event]) {
                    eventListeners[event] = [];
                }
                eventListeners[event].push(listener);
            },

            off: function(event, listener) {
                if (eventListeners[event]) {
                    eventListeners[event] = eventListeners[event].filter(function(l) {
                        return l !== listener;
                    });
                }
            }
        };

        // Hook native response handlers
        global.__xtools_on_native_response = function(responseJson) {
            try {
                var response = typeof responseJson === 'string' ? JSON.parse(responseJson) : responseJson;
                var cb = callbacks[response.id];
                if (cb) {
                    delete callbacks[response.id];
                    var isSuccess = !response.error;
                    cb({
                        success: isSuccess,
                        data: response.result !== undefined ? response.result : null,
                        error: response.error || null
                    });
                }
            } catch (e) {
                console.error('xtools: Failed to handle native response', e);
            }
        };

        global.__xtools_on_native_event = function(eventName, dataJson) {
            try {
                var data = typeof dataJson === 'string' ? JSON.parse(dataJson) : dataJson;
                var listeners = eventListeners[eventName];
                if (listeners && listeners.length) {
                    listeners.forEach(function(l) {
                        try { l(data); } catch (err) { console.error(err); }
                    });
                }
            } catch (e) {
                console.error('xtools: Failed to handle native event', e);
            }
        };
    }

    // Bridge instance
    var bridge = global.AndroidBridge || global.xtools;

    if (!bridge) {
        console.error('xtools: AndroidBridge not found. Please ensure the host app is loaded.');
        return;
    }

    /**
     * xtools SDK namespace
     */
    var xtools = {
        version: '1.0.0',

        /**
         * Device information
         */
        device: {
            platform: 'android',
            platformVersion: '',
            appVersion: '',
            language: navigator.language,
            timezone: Intl.DateTimeFormat().resolvedOptions().timeZone
        },

        /**
         * Call a native action
         * @param {string} action - Action name
         * @param {object} params - Action parameters
         * @param {function} callback - Response callback
         * @returns {string} Callback ID
         */
        call: function(action, params, callback) {
            return bridge.call(action, params, callback);
        },

        /**
         * Subscribe to native events
         * @param {string} event - Event name
         * @param {function} listener - Event listener
         */
        on: function(event, listener) {
            bridge.on(event, listener);
        },

        /**
         * Unsubscribe from native events
         * @param {string} event - Event name
         * @param {function} listener - Event listener to remove
         */
        off: function(event, listener) {
            bridge.off(event, listener);
        },

        /**
         * Get device information
         * @param {function} callback - Callback with device info
         */
        getDeviceInfo: function(callback) {
            bridge.call('system.getDeviceInfo', {}, function(response) {
                if (callback) {
                    callback(response.success ? response.data : null);
                }
            });
        },

        /**
         * Show a toast message
         * @param {string} message - Toast message
         * @param {string} duration - 'short' or 'long'
         */
        showToast: function(message, duration) {
            bridge.call('showToast', {
                message: message,
                duration: duration || 'short'
            });
        },

        /**
         * Get preference value
         * @param {string} key - Preference key
         * @param {function} callback - Callback with value
         */
        getPreference: function(key, callback) {
            bridge.call('getPreferences', { key: key }, function(response) {
                if (callback) {
                    callback(response.success ? response.data : null);
                }
            });
        },

        /**
         * Set preference value
         * @param {string} key - Preference key
         * @param {*} value - Preference value
         * @param {function} callback - Completion callback
         */
        setPreference: function(key, value, callback) {
            bridge.call('setPreferences', {
                key: key,
                value: value
            }, function(response) {
                if (callback) {
                    callback(response.success);
                }
            });
        },

        /**
         * Pick a file from the device
         * @param {object} options - File picker options
         * @param {function} callback - Callback with file data
         */
        pickFile: function(options, callback) {
            bridge.call('pickFile', options || {}, function(response) {
                if (callback) {
                    callback(response.success ? response.data : null);
                }
            });
        },

        /**
         * Navigate to a URL
         * @param {string} url - Target URL
         */
        navigate: function(url) {
            bridge.call('navigate', { url: url });
        },

        /**
         * Close the current plugin/activity
         */
        close: function() {
            bridge.call('close');
        },

        /**
         * Get plugin information
         * @param {function} callback - Callback with plugin info
         */
        getPluginInfo: function(callback) {
            bridge.call('getPluginInfo', {}, function(response) {
                if (callback) {
                    callback(response.success ? response.data : null);
                }
            });
        },

        /**
         * Request a permission
         * @param {string} permission - Permission name
         * @param {function} callback - Callback with result
         */
        requestPermission: function(permission, callback) {
            bridge.call('requestPermission', {
                permission: permission
            }, function(response) {
                if (callback) {
                    callback(response.success, response.data);
                }
            });
        },

        /**
         * Check if a permission is granted
         * @param {string} permission - Permission name
         * @param {function} callback - Callback with result
         */
        checkPermission: function(permission, callback) {
            bridge.call('checkPermission', {
                permission: permission
            }, function(response) {
                if (callback) {
                    callback(response.success, response.data);
                }
            });
        },

        /**
         * Open URL in external browser
         * @param {string} url - URL to open
         */
        openUrl: function(url) {
            bridge.call('openUrl', { url: url });
        },

        /**
         * Share content
         * @param {object} data - Share data
         */
        share: function(data) {
            bridge.call('share', data);
        },

        /**
         * Log message to native console
         * @param {string} level - Log level
         * @param {string} message - Message to log
         */
        log: function(level, message) {
            console.log('[' + level + '] ' + message);
        }
    };

    // Event aliases
    xtools.on('ready', function() {
        console.log('xtools: Host is ready');
    });

    xtools.on('resume', function() {
        console.log('xtools: App resumed');
    });

    xtools.on('pause', function() {
        console.log('xtools: App paused');
    });

    xtools.on('backButton', function() {
        console.log('xtools: Back button pressed');
    });

    xtools.on('themeChanged', function(data) {
        console.log('xtools: Theme changed to', data);
    });

    xtools.system = {
        getInfo: function() {
            return new Promise(function(resolve, reject) {
                bridge.call('system.getDeviceInfo', {}, function(response) {
                    if (response && response.success) {
                        resolve(response.data);
                    } else {
                        reject(new Error((response && response.error) || 'Failed to get device info'));
                    }
                });
            });
        },
        getDeviceInfo: function(callback) {
            xtools.getDeviceInfo(callback);
        }
    };

    // Export to global scope
    global.xtools = xtools;
    global.XTools = xtools;

    // Also expose as module if supported
    if (typeof module !== 'undefined' && module.exports) {
        module.exports = xtools;
    }

    // Dispatch ready event
    document.dispatchEvent(new CustomEvent('xtools-ready'));

})(typeof window !== 'undefined' ? window : this);
