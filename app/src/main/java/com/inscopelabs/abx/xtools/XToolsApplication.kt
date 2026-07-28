package com.inscopelabs.abx.xtools

import android.app.Application
import java.io.File

class XToolsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ensureWebViewCacheDirs()
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
}
