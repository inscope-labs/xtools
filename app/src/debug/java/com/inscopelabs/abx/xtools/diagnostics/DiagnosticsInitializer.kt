package com.inscopelabs.abx.xtools.diagnostics

import android.content.Context

object DiagnosticsInitializer {
    private var watchdog: AnrWatchdog? = null

    fun initialize(context: Context) {
        SessionManager.activateSession()
        StartupDiagnostics.recordEvent("Diagnostic session activated: ${SessionManager.sessionId}")

        Logger.initialize(context)
        StartupDiagnostics.recordEvent("Logger initialized")

        watchdog = AnrWatchdog().apply { start() }
        StartupDiagnostics.recordEvent("ANR Watchdog started")
    }

    fun shutdown() {
        watchdog?.stopMonitoring()
    }
}
