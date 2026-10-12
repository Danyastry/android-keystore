package com.danyastry.keystore.feature.rsaoaep

import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.MGF1ParameterSpec
import javax.crypto.Cipher
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource

class RsaOaepCipher {

    fun encrypt(plaintext: ByteArray, key: PublicKey): ByteArray {
        require(plaintext.size <= MAX_PLAINTEXT_BYTES) {
            "RSA-OAEP with a 2048-bit key fits at most $MAX_PLAINTEXT_BYTES bytes, got ${plaintext.size}"
        }
        return Cipher.getInstance(TRANSFORMATION).run {
            init(Cipher.ENCRYPT_MODE, key, PARAMETERS)
            doFinal(plaintext)
        }
    }

    fun decrypt(ciphertext: ByteArray, key: PrivateKey): ByteArray = Cipher.getInstance(TRANSFORMATION).run {
        init(Cipher.DECRYPT_MODE, key, PARAMETERS)
        doFinal(ciphertext)
    }

    companion object {
        const val TRANSFORMATION = "RSA/ECB/OAEPPadding"
        const val KEY_SIZE_BITS = 2048
        private const val HASH_BYTES = 32
        const val MAX_PLAINTEXT_BYTES = KEY_SIZE_BITS / Byte.SIZE_BITS - 2 * HASH_BYTES - 2

        val PARAMETERS = OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT)
    }
}
