package com.danyastry.keystore.core.keystore

import java.util.Base64

class EncryptedMessage(val iv: ByteArray, val ciphertext: ByteArray) {

    init {
        require(iv.size == IV_SIZE) { "GCM IV must be $IV_SIZE bytes, was ${iv.size}" }
        require(ciphertext.size >= TAG_SIZE) { "Ciphertext must include the $TAG_SIZE byte tag" }
    }

    fun toBase64(): String = Base64.getEncoder().encodeToString(iv + ciphertext)

    override fun equals(other: Any?): Boolean =
        other is EncryptedMessage && other.iv.contentEquals(iv) && other.ciphertext.contentEquals(ciphertext)

    override fun hashCode(): Int = 31 * iv.contentHashCode() + ciphertext.contentHashCode()

    companion object {
        const val IV_SIZE = 12
        const val TAG_SIZE = 16

        fun fromBase64OrNull(text: String): EncryptedMessage? {
            val bytes = runCatching { Base64.getDecoder().decode(text.filterNot { it.isWhitespace() }) }.getOrNull() ?: return null
            if (bytes.size < IV_SIZE + TAG_SIZE) return null
            return EncryptedMessage(bytes.copyOfRange(0, IV_SIZE), bytes.copyOfRange(IV_SIZE, bytes.size))
        }
    }
}
