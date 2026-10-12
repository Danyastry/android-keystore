package com.danyastry.keystore.feature.aesgcm

import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class EncryptedMessage(val iv: ByteArray, val ciphertext: ByteArray) {

    init {
        require(iv.size == AesGcmCipher.IV_SIZE) { "GCM IV must be ${AesGcmCipher.IV_SIZE} bytes, was ${iv.size}" }
        require(ciphertext.size >= AesGcmCipher.TAG_SIZE) { "Ciphertext must include the ${AesGcmCipher.TAG_SIZE} byte tag" }
    }

    fun toBase64(): String = Base64.getEncoder().encodeToString(iv + ciphertext)

    override fun equals(other: Any?): Boolean =
        other is EncryptedMessage && other.iv.contentEquals(iv) && other.ciphertext.contentEquals(ciphertext)

    override fun hashCode(): Int = 31 * iv.contentHashCode() + ciphertext.contentHashCode()

    companion object {
        fun fromBase64OrNull(text: String): EncryptedMessage? {
            val bytes = runCatching { Base64.getDecoder().decode(text.filterNot { it.isWhitespace() }) }.getOrNull() ?: return null
            if (bytes.size < AesGcmCipher.IV_SIZE + AesGcmCipher.TAG_SIZE) return null
            return EncryptedMessage(bytes.copyOfRange(0, AesGcmCipher.IV_SIZE), bytes.copyOfRange(AesGcmCipher.IV_SIZE, bytes.size))
        }
    }
}

class AesGcmCipher {

    fun encrypt(plaintext: ByteArray, key: SecretKey): EncryptedMessage {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        return EncryptedMessage(iv = cipher.iv, ciphertext = cipher.doFinal(plaintext))
    }

    fun decrypt(message: EncryptedMessage, key: SecretKey): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_SIZE * Byte.SIZE_BITS, message.iv))
        return cipher.doFinal(message.ciphertext)
    }

    companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
        const val TAG_SIZE = 16
    }
}
