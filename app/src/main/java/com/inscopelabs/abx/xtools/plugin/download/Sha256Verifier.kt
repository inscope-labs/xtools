package com.inscopelabs.abx.xtools.plugin.download

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

/**
 * Verifies the SHA-256 hash of a downloaded plugin package.
 *
 * @see §4.2 Step 3.2.2
 */
object Sha256Verifier {

    @Throws(SecurityException::class)
    fun verify(file: File, expectedHash: String): Boolean {
        if (!file.exists()) throw SecurityException("File not found: ${file.absolutePath}")

        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }

        val computedHash = digest.digest().joinToString("") { "%02x".format(it) }
        val match = computedHash.equals(expectedHash, ignoreCase = true)

        if (!match) {
            throw SecurityException("SHA-256 mismatch: expected $expectedHash, got $computedHash")
        }
        return true
    }
}
