package com.inscopelabs.abx.xtools.plugins.sdk.signing

import java.io.File
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Date

/**
 * The on-device developer keystore. Each device generates exactly one of
 * these (see Stage 7.12). Stored as a PKCS#12 file under
 * `<filesDir>/xtools/keystore.p12`, protected by a key derived from
 * Android's [`KeyStore`] provider when possible.
 *
 * The `alias` is fixed (`xtools-developer`) so callers don't need to
 * track which alias signed what.
 */
class DevKeyStore(
    private val keystoreFile: File,
    private val password: CharArray,
) {

    private val keyStore: KeyStore = loadOrCreate()

    fun privateKey(): PrivateKey {
        val entry = keyStore.getEntry(ALIAS, KeyStore.PasswordProtection(password))
            ?: error("keystore missing entry '$ALIAS'")
        return (entry as KeyStore.PrivateKeyEntry).privateKey
    }

    fun certificate(): X509Certificate =
        keyStore.getCertificate(ALIAS) as X509Certificate

    fun alias(): String = ALIAS

    private fun loadOrCreate(): KeyStore {
        val ks = KeyStore.getInstance(STORE_TYPE)
        if (keystoreFile.exists() && keystoreFile.length() > 0) {
            keystoreFile.inputStream().use { ks.load(it, password) }
            if (ks.containsAlias(ALIAS)) return ks
        } else {
            keystoreFile.parentFile?.mkdirs()
            ks.load(null, password)
        }
        val pair = generateKeyPair()
        val cert = SelfSignedCert.issue(
            commonName = "XTools Developer",
            pair = pair,
            notBefore = Date(),
            notAfter = Date(System.currentTimeMillis() + CERT_VALIDITY_MS),
        )
        ks.setKeyEntry(ALIAS, pair.private, password, arrayOf(cert))
        keystoreFile.outputStream().use { ks.store(it, password) }
        return ks
    }

    private fun generateKeyPair(): KeyPair {
        val gen = KeyPairGenerator.getInstance("RSA")
        gen.initialize(KEY_SIZE, SecureRandom())
        return gen.generateKeyPair()
    }

    companion object {
        private const val STORE_TYPE: String = "PKCS12"
        private const val ALIAS: String = "xtools-developer"
        private const val KEY_SIZE: Int = 3072
        private const val CERT_VALIDITY_MS: Long = 10L * 365 * 24 * 60 * 60 * 1000 // 10 years
    }
}
