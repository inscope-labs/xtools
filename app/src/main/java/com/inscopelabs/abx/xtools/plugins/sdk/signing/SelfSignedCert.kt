package com.inscopelabs.abx.xtools.plugins.sdk.signing

import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.asn1.x509.ExtendedKeyUsage
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.KeyPurposeId
import org.bouncycastle.asn1.x509.KeyUsage
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.ContentSigner
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.math.BigInteger
import java.security.KeyPair
import java.security.cert.X509Certificate
import java.util.Date

/**
 * Issues a self-signed X.509 certificate for the developer keypair. The
 * certificate is consumed by [PluginSigner] and persisted alongside the
 * keystore.
 *
 * Pure Bouncy Castle — Android's `Certificate.generate` is not
 * available on older API levels.
 */
object SelfSignedCert {

    fun issue(
        commonName: String,
        pair: KeyPair,
        notBefore: Date,
        notAfter: Date,
    ): X509Certificate {
        val serial = BigInteger.valueOf(System.currentTimeMillis())
        val name = X500Name("CN=$commonName, OU=XTools, O=LocalDeveloper")
        val builder: X509v3CertificateBuilder = JcaX509v3CertificateBuilder(
            name, serial, notBefore, notAfter, name, pair.public,
        )
        builder.addExtension(Extension.basicConstraints, true, BasicConstraints(false))
        builder.addExtension(
            Extension.keyUsage,
            true,
            KeyUsage(KeyUsage.digitalSignature or KeyUsage.keyCertSign),
        )
        builder.addExtension(
            Extension.extendedKeyUsage,
            false,
            ExtendedKeyUsage(arrayOf(KeyPurposeId.id_kp_codeSigning)),
        )
        val signer: ContentSigner = JcaContentSignerBuilder("SHA256WithRSA")
            .build(pair.private)
        return JcaX509CertificateConverter()
            .getCertificate(builder.build(signer))
    }
}
