package com.inscopelabs.abx.xtools.security

import java.security.Signature
import java.security.cert.Certificate
import java.security.cert.X509Certificate

/**
 * Establishes and verifies plugin identity based on signing certificates and manifest contents.
 *
 * @see §2.4 Step 1.5.4
 */
class PluginIdentity {

    /**
     * Derives a stable, unique plugin ID from its signing certificate.
     */
    fun deriveId(certificate: X509Certificate): String {
        return certificate.publicKey.encoded.contentHash()
    }

    /**
     * Verifies that a plugin's manifest signature matches its certificate.
     * Fails closed: returns false on any verification failure, mismatch, or exception.
     */
    fun verifySignature(manifestBytes: ByteArray, signature: ByteArray, certificate: Certificate): Boolean {
        if (manifestBytes.isEmpty() || signature.isEmpty()) {
            return false
        }
        return try {
            val keyType = certificate.publicKey.algorithm
            val algorithm = when (keyType.uppercase()) {
                "EC", "ECDSA" -> "SHA256withECDSA"
                "DSA" -> "SHA256withDSA"
                else -> "SHA256withRSA"
            }
            val sig = Signature.getInstance(algorithm)
            sig.initVerify(certificate.publicKey)
            sig.update(manifestBytes)
            sig.verify(signature)
        } catch (e: Exception) {
            false
        }
    }
}

private fun ByteArray.contentHash(): String = java.security.MessageDigest.getInstance("SHA-256")
    .digest(this)
    .joinToString("") { "%02x".format(it) }

