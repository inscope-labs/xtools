package com.inscopelabs.abx.xtools.dispatcher

import java.security.MessageDigest

object ChatUtils {
    fun generateChecksum(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun sanitizeInput(input: String): String {
        return input.trim().replace("\u0000", "")
    }
}