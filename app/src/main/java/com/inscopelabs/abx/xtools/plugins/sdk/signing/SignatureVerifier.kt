package com.inscopelabs.abx.xtools.plugins.sdk.signing

import java.io.File
import java.security.MessageDigest
import java.security.Signature
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

/**
 * Verifies the integrity of a signed plugin bundle. Returns a
 * [VerificationResult] — never throws. Production code should treat
 * `Invalid` as a hard fail before invoking plugin code.
 */
class SignatureVerifier {

    sealed interface VerificationResult {
        data class Valid(val cert: X509Certificate) : VerificationResult
        data object Missing : VerificationResult
        data class Invalid(val reason: String) : VerificationResult
    }

    fun verify(bundle: File, signatureDir: File): VerificationResult {
        val json = File(signatureDir, "signature.json")
        val sig = File(signatureDir, "signature.sig")
        val cert = File(signatureDir, "certificate.pem")
        if (!json.isFile || !sig.isFile || !cert.isFile) return VerificationResult.Missing
        val env = runCatching { org.json.JSONObject(json.readText()) }.getOrElse {
            return VerificationResult.Invalid("signature.json is malformed")
        }
        val expectedHash = env.optString("hash")
        val actualHash = sha256(bundle)
        if (expectedHash != actualHash) {
            return VerificationResult.Invalid("hash mismatch — bundle may be tampered with")
        }
        val certObj = runCatching {
            CertificateFactory.getInstance("X.509")
                .generateCertificate(cert.inputStream()) as X509Certificate
        }.getOrElse {
            return VerificationResult.Invalid("certificate unreadable: ${it.message}")
        }
        val ok = runCatching {
            val s = Signature.getInstance("SHA256withRSA")
            s.initVerify(certObj.publicKey)
            s.update(actualHash.toByteArray(Charsets.US_ASCII))
            s.verify(sig.readBytes())
        }.getOrElse { false }
        if (!ok) return VerificationResult.Invalid("signature does not verify against certificate")
        return VerificationResult.Valid(certObj)
    }

    private fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { stream ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = stream.read(buf); if (n <= 0) break
                md.update(buf, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
