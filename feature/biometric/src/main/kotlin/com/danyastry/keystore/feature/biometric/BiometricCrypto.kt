package com.danyastry.keystore.feature.biometric

import android.security.keystore.KeyPermanentlyInvalidatedException
import com.danyastry.keystore.core.keystore.EncryptedMessage
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class KeyInvalidatedException(cause: Throwable) : Exception(cause)

interface BiometricCrypto {
    fun encryptionCipher(key: SecretKey): Cipher
    fun decryptionCipher(key: SecretKey, iv: ByteArray): Cipher
    fun seal(cipher: Cipher, plaintext: ByteArray): EncryptedMessage
    fun open(cipher: Cipher, ciphertext: ByteArray): ByteArray
}

class AesGcmBiometricCrypto : BiometricCrypto {

    override fun encryptionCipher(key: SecretKey): Cipher = newCipher { init(Cipher.ENCRYPT_MODE, key) }

    override fun decryptionCipher(key: SecretKey, iv: ByteArray): Cipher =
        newCipher { init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(EncryptedMessage.TAG_SIZE * Byte.SIZE_BITS, iv)) }

    override fun seal(cipher: Cipher, plaintext: ByteArray): EncryptedMessage = EncryptedMessage(cipher.iv, cipher.doFinal(plaintext))

    override fun open(cipher: Cipher, ciphertext: ByteArray): ByteArray = cipher.doFinal(ciphertext)

    private fun newCipher(init: Cipher.() -> Unit): Cipher =
        try {
            Cipher.getInstance(TRANSFORMATION).apply(init)
        } catch (e: KeyPermanentlyInvalidatedException) {
            throw KeyInvalidatedException(e)
        }

    companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
