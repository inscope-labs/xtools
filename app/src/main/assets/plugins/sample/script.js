document.addEventListener('DOMContentLoaded', function() {
    console.log('Sample plugin DOM loaded');

    // UI Actions
    document.getElementById('btnToast').addEventListener('click', function() {
        XTools.ui.showToast('Hello from XTools JavaScript Plugin!')
            .catch(function(err) { alert('Error: ' + err.message); });
    });

    document.getElementById('btnVibrate').addEventListener('click', function() {
        XTools.ui.vibrate(150);
        XTools.ui.showToast('Haptic feedback triggered');
    });

    document.getElementById('btnDialog').addEventListener('click', function() {
        XTools.ui.showDialog('Native Bridge Confirmation', 'This popup is spawned from JS through the Android Kotlin Bridge.', 'Got it!');
    });

    // Storage Actions
    document.getElementById('btnSaveStorage').addEventListener('click', function() {
        var key = document.getElementById('storageKey').value.trim();
        var val = document.getElementById('storageValue').value.trim();
        if (!key) return;

        XTools.storage.set(key, val)
            .then(function(res) {
                document.getElementById('storageOutput').innerText = '✅ Saved: ' + key + ' = ' + val;
            })
            .catch(function(err) {
                document.getElementById('storageOutput').innerText = '❌ Error: ' + err.message;
            });
    });

    document.getElementById('btnLoadStorage').addEventListener('click', function() {
        var key = document.getElementById('storageKey').value.trim();
        if (!key) return;

        XTools.storage.get(key)
            .then(function(res) {
                document.getElementById('storageOutput').innerText = '🔑 Value for [' + key + ']: ' + (res || '(null)');
            })
            .catch(function(err) {
                document.getElementById('storageOutput').innerText = '❌ Error: ' + err.message;
            });
    });

    // SHA-256 Calculation
    document.getElementById('btnCalcHash').addEventListener('click', function() {
        var input = document.getElementById('hashInput').value;
        XTools.system.calculateHash(input)
            .then(function(res) {
                document.getElementById('hashOutput').innerText = 'SHA-256:\n' + res;
            })
            .catch(function(err) {
                document.getElementById('hashOutput').innerText = '❌ Error: ' + err.message;
            });
    });

    // System Info
    document.getElementById('btnSysInfo').addEventListener('click', function() {
        XTools.system.getInfo()
            .then(function(res) {
                document.getElementById('sysInfoOutput').innerText = JSON.stringify(res, null, 2);
            })
            .catch(function(err) {
                document.getElementById('sysInfoOutput').innerText = '❌ Error: ' + err.message;
            });
    });
});
