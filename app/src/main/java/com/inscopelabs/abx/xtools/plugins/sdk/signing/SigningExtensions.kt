package com.inscopelabs.abx.xtools.plugins.sdk.signing

import java.security.PrivateKey
import java.security.Signature

/**
 * Small extension to keep the signer's call site readable. Not an Android
 * extension — operates on a [PrivateKey] directly.
 */
fun PrivateKey.signWithSha256(data: ByteArray): ByteArray {
    val s = Signature.getInstance("SHA256withRSA")
    s.initSign(this)
    s.update(data)
    return s.sign()
}
