package com.inscopelabs.abx.xtools.diagnostics

import java.util.UUID

object SessionManager {
    val sessionId: String = UUID.randomUUID().toString().take(8).uppercase()
    private var activated = false

    fun activateSession() {
        if (!activated) {
            activated = true
            android.util.Log.i("SessionManager", "Diagnostic session activated: $sessionId")
        }
    }
}
