package com.danyastry.keystore.feature.biometric

import android.security.keystore.KeyPermanentlyInvalidatedException
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SealedMessage(val iv: ByteArray, val ciphertext: ByteArray) {

    init {
        require(iv.size == AesGcmBiometricCrypto.IV_SIZE) { "GCM IV must be ${AesGcmBiometricCrypto.IV_SIZE} bytes, was ${iv.size}" }
        require(ciphertext.size >= AesGcmBiometricCrypto.TAG_SIZE) { "Ciphertext must include the ${AesGcmBiometricCrypto.TAG_SIZE} byte tag" }
    }

    fun toBase64(): String = Base64.getEncoder().encodeToString(iv + ciphertext)

    companion object {
        fun fromBase64OrNull(text: String): SealedMessage? {
            val bytes = runCatching { Base64.getDecoder().decode(text) }.getOrNull() ?: return null
            if (bytes.size < AesGcmBiometricCrypto.IV_SIZE + AesGcmBiometricCrypto.TAG_SIZE) return null
            return SealedMessage(
                bytes.copyOfRange(0, AesGcmBiometricCrypto.IV_SIZE),
                bytes.copyOfRange(AesGcmBiometricCrypto.IV_SIZE, bytes.size),
            )
        }
    }
}

class KeyInvalidatedException(cause: Throwable) : Exception(cause)

interface BiometricCrypto {
    fun encryptionCipher(key: SecretKey): Cipher
    fun decryptionCipher(key: SecretKey, iv: ByteArray): Cipher
    fun seal(cipher: Cipher, plaintext: ByteArray): SealedMessage
    fun open(cipher: Cipher, ciphertext: ByteArray): ByteArray
}

class AesGcmBiometricCrypto : BiometricCrypto {

    override fun encryptionCipher(key: SecretKey): Cipher = newCipher { init(Cipher.ENCRYPT_MODE, key) }

    override fun decryptionCipher(key: SecretKey, iv: ByteArray): Cipher =
        newCipher { init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_SIZE * Byte.SIZE_BITS, iv)) }

    override fun seal(cipher: Cipher, plaintext: ByteArray): SealedMessage = SealedMessage(cipher.iv, cipher.doFinal(plaintext))

    override fun open(cipher: Cipher, ciphertext: ByteArray): ByteArray = cipher.doFinal(ciphertext)

    private fun newCipher(init: Cipher.() -> Unit): Cipher =
        try {
            Cipher.getInstance(TRANSFORMATION).apply(init)
        } catch (e: KeyPermanentlyInvalidatedException) {
            throw KeyInvalidatedException(e)
        }

    companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
        const val TAG_SIZE = 16
    }
}
