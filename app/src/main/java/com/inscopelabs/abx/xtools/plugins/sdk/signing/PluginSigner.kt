package com.inscopelabs.abx.xtools.plugins.sdk.signing

import java.io.File
import java.security.MessageDigest

/**
 * The artifact a build emits alongside the signed ZIP. Contains:
 *
 *  - the SHA-256 of the bundle (`hash.sha256`)
 *  - a detached signature over that hash (`signature.sig`)
 *  - the developer's certificate (`certificate.pem`)
 *  - a JSON envelope tying them together (`signature.json`)
 *
 * The [Verifier] reads this folder to confirm a plugin is intact and
 * signed by the device that built it.
 */
class PluginSigner(private val devKeyStore: DevKeyStore) {

    data class Artifact(
        val signatureJson: File,
        val signatureFile: File,
        val certificateFile: File,
        val hashFile: File,
    )

    fun sign(bundle: File, outDir: File): Artifact {
        outDir.mkdirs()
        val hash = sha256(bundle)
        val hashFile = File(outDir, "hash.sha256").apply { writeText(hash) }
        val signature = devKeyStore.privateKey().signWithSha256(hash.toByteArray(Charsets.US_ASCII))
        val signatureFile = File(outDir, "signature.sig").apply { writeBytes(signature) }
        val certificateFile = File(outDir, "certificate.pem").apply {
            writeText("-----BEGIN CERTIFICATE-----\n" +
                java.util.Base64.getMimeEncoder(64, "\n".toByteArray())
                    .encodeToString(devKeyStore.certificate().encoded) +
                "\n-----END CERTIFICATE-----\n")
        }
        val envelope = org.json.JSONObject().apply {
            put("pluginName", bundle.nameWithoutExtension)
            put("signedAtMs", System.currentTimeMillis())
            put("hashAlgorithm", "SHA-256")
            put("hash", hash)
            put("signatureAlgorithm", "SHA256withRSA")
            put("certificateFingerprint", sha256Fingerprint(devKeyStore.certificate().encoded))
        }
        val signatureJson = File(outDir, "signature.json").apply { writeText(envelope.toString(2)) }
        return Artifact(signatureJson, signatureFile, certificateFile, hashFile)
    }

    private fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { stream ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = stream.read(buf)
                if (n <= 0) break
                md.update(buf, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    private fun sha256Fingerprint(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
