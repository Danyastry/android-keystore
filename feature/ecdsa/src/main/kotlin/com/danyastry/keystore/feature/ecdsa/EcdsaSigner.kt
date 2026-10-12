package com.danyastry.keystore.feature.ecdsa

import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.SignatureException

class EcdsaSigner {

    fun sign(data: ByteArray, key: PrivateKey): ByteArray = Signature.getInstance(ALGORITHM).run {
        initSign(key)
        update(data)
        sign()
    }

    fun verify(data: ByteArray, signature: ByteArray, key: PublicKey): Boolean = Signature.getInstance(ALGORITHM).run {
        initVerify(key)
        update(data)
        try {
            verify(signature)
        } catch (_: SignatureException) {
            false
        }
    }

    companion object {
        const val ALGORITHM = "SHA256withECDSA"
    }
}
