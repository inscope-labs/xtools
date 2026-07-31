package com.inscopelabs.abx.xtools.plugin.download

import java.security.Signature
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

/**
 * Validates plugin authenticity using the developer's signing certificate.
 *
 * @see §4.2 Step 3.2.4
 */
object SignatureVerifier {

    /**
     * Verifies a plugin's signature against its manifest and certificate.
     * @param manifestBytes The raw manifest JSON bytes.
     * @param signatureBytes The signature bytes from the plugin package.
     * @param certificatePem The public certificate in PEM format (stored in catalog).
     */
    @Throws(SecurityException::class)
    fun verify(manifestBytes: ByteArray, signatureBytes: ByteArray, certificatePem: String): Boolean {
        return try {
            val certificate = CertificateFactory.getInstance("X.509")
                .generateCertificate(certificatePem.byteInputStream()) as X509Certificate

            val sig = Signature.getInstance("SHA256withRSA")
            sig.initVerify(certificate.publicKey)
            sig.update(manifestBytes)
            val verified = sig.verify(signatureBytes)

            if (!verified) throw SecurityException("Signature verification failed")
            verified
        } catch (e: Exception) {
            throw SecurityException("Signature verification error: ${e.message}", e)
        }
    }

    /**
     * Extracts the plugin ID from a certificate (for identity matching).
     */
    fun derivePluginId(certificatePem: String): String {
        // Stub – return a placeholder derived from the certificate.
        // Real implementation would compute SHA-256 of the public key.
        return "com.developer.${certificatePem.hashCode()}"
    }
}
