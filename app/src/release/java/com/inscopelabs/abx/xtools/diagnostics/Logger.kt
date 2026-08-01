package com.inscopelabs.abx.xtools.diagnostics

import android.content.Context
import java.io.File

object Logger {
    fun initialize(context: Context) {}
    fun d(component: String, message: String) {}
    fun i(component: String, message: String) {}
    fun w(component: String, message: String, throwable: Throwable? = null) {}
    fun e(component: String, message: String, throwable: Throwable? = null) {}
    fun getLogFile(): File? = null
}
