package com.inscopelabs.abx.xtools

import android.app.Application
import com.inscopelabs.abx.xtools.diagnostics.DiagnosticsInitializer
import com.inscopelabs.abx.xtools.diagnostics.GlobalExceptionHandler
import java.io.File

/**
 * xtools Application class for global initialization.
 */
class XToolsApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        ensureWebViewCacheDirs()

        // Diagnostics must come up before the exception handler,
        // since GlobalExceptionHandler calls Logger + CrashReporterManager.
        DiagnosticsInitializer.initialize(this)
        Thread.setDefaultUncaughtExceptionHandler(GlobalExceptionHandler(this))
    }

    private fun ensureWebViewCacheDirs() {
        try {
            val codeCacheDir = File(cacheDir, "WebView/Default/HTTP Cache/Code Cache")
            File(codeCacheDir, "js").mkdirs()
            File(codeCacheDir, "wasm").mkdirs()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        lateinit var instance: XToolsApplication
            private set
    }
}
