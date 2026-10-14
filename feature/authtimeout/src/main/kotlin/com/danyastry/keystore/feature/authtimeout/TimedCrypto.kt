package com.danyastry.keystore.feature.authtimeout

import android.security.keystore.UserNotAuthenticatedException
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SealedMessage(val iv: ByteArray, val ciphertext: ByteArray) {

    init {
        require(iv.size == AesGcmTimedCrypto.IV_SIZE) { "GCM IV must be ${AesGcmTimedCrypto.IV_SIZE} bytes, was ${iv.size}" }
        require(ciphertext.size >= AesGcmTimedCrypto.TAG_SIZE) { "Ciphertext must include the ${AesGcmTimedCrypto.TAG_SIZE} byte tag" }
    }

    fun toBase64(): String = Base64.getEncoder().encodeToString(iv + ciphertext)

    companion object {
        fun fromBase64OrNull(text: String): SealedMessage? {
            val bytes = runCatching { Base64.getDecoder().decode(text) }.getOrNull() ?: return null
            if (bytes.size < AesGcmTimedCrypto.IV_SIZE + AesGcmTimedCrypto.TAG_SIZE) return null
            return SealedMessage(
                bytes.copyOfRange(0, AesGcmTimedCrypto.IV_SIZE),
                bytes.copyOfRange(AesGcmTimedCrypto.IV_SIZE, bytes.size),
            )
        }
    }
}

class KeyLockedException(cause: Throwable) : Exception(cause)

interface TimedCrypto {
    fun encrypt(key: SecretKey, plaintext: ByteArray): SealedMessage
    fun decrypt(key: SecretKey, message: SealedMessage): ByteArray
}

class AesGcmTimedCrypto : TimedCrypto {

    override fun encrypt(key: SecretKey, plaintext: ByteArray): SealedMessage {
        val cipher = newCipher { init(Cipher.ENCRYPT_MODE, key) }
        return SealedMessage(cipher.iv, cipher.doFinal(plaintext))
    }

    override fun decrypt(key: SecretKey, message: SealedMessage): ByteArray =
        newCipher { init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_SIZE * Byte.SIZE_BITS, message.iv)) }
            .doFinal(message.ciphertext)

    private fun newCipher(init: Cipher.() -> Unit): Cipher =
        try {
            Cipher.getInstance(TRANSFORMATION).apply(init)
        } catch (e: UserNotAuthenticatedException) {
            throw KeyLockedException(e)
        }

    companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
        const val TAG_SIZE = 16
    }
}
