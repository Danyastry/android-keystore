package com.danyastry.keystore.feature.aesgcm

import com.danyastry.keystore.core.keystore.EncryptedMessage
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class AesGcmCipher {

    fun encrypt(plaintext: ByteArray, key: SecretKey): EncryptedMessage {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        return EncryptedMessage(iv = cipher.iv, ciphertext = cipher.doFinal(plaintext))
    }

    fun decrypt(message: EncryptedMessage, key: SecretKey): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(EncryptedMessage.TAG_SIZE * Byte.SIZE_BITS, message.iv))
        return cipher.doFinal(message.ciphertext)
    }

    companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
