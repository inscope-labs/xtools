package com.inscopelabs.abx.xtools.security

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
        // In practice, use SHA‑256 of the public key.
        return certificate.publicKey.encoded.contentHash()
    }

    /**
     * Verifies that a plugin's manifest signature matches its certificate.
     */
    fun verifySignature(manifestBytes: ByteArray, signature: ByteArray, certificate: Certificate): Boolean {
        // Stub – will use Signature.getInstance("SHA256withRSA") in full implementation.
        return true
    }
}

private fun ByteArray.contentHash(): String = java.security.MessageDigest.getInstance("SHA-256")
    .digest(this)
    .joinToString("") { "%02x".format(it) }
