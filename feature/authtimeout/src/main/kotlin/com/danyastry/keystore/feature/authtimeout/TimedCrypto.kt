package com.danyastry.keystore.feature.authtimeout

import android.security.keystore.UserNotAuthenticatedException
import com.danyastry.keystore.core.keystore.EncryptedMessage
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class KeyLockedException(cause: Throwable) : Exception(cause)

interface TimedCrypto {
    fun encrypt(key: SecretKey, plaintext: ByteArray): EncryptedMessage
    fun decrypt(key: SecretKey, message: EncryptedMessage): ByteArray
}

class AesGcmTimedCrypto : TimedCrypto {

    override fun encrypt(key: SecretKey, plaintext: ByteArray): EncryptedMessage {
        val cipher = newCipher { init(Cipher.ENCRYPT_MODE, key) }
        return EncryptedMessage(cipher.iv, cipher.doFinal(plaintext))
    }

    override fun decrypt(key: SecretKey, message: EncryptedMessage): ByteArray =
        newCipher { init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(EncryptedMessage.TAG_SIZE * Byte.SIZE_BITS, message.iv)) }
            .doFinal(message.ciphertext)

    private fun newCipher(init: Cipher.() -> Unit): Cipher =
        try {
            Cipher.getInstance(TRANSFORMATION).apply(init)
        } catch (e: UserNotAuthenticatedException) {
            throw KeyLockedException(e)
        }

    companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
