package com.danyastry.keystore.feature.envelope

import com.danyastry.keystore.core.keystore.EncryptedMessage
import java.nio.ByteBuffer
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.MGF1ParameterSpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource

class Envelope(val wrappedKey: ByteArray, val message: EncryptedMessage) {

    val size: Int
        get() = LENGTH_BYTES + wrappedKey.size + message.iv.size + message.ciphertext.size

    fun toBytes(): ByteArray =
        ByteBuffer.allocate(size)
            .putShort(wrappedKey.size.toShort())
            .put(wrappedKey)
            .put(message.iv)
            .put(message.ciphertext)
            .array()

    companion object {
        private const val LENGTH_BYTES = 2

        fun fromBytesOrNull(bytes: ByteArray): Envelope? {
            if (bytes.size < LENGTH_BYTES) return null
            val buffer = ByteBuffer.wrap(bytes)
            val keyLength = buffer.short.toInt() and 0xffff
            if (buffer.remaining() < keyLength + EncryptedMessage.IV_SIZE + EncryptedMessage.TAG_SIZE) return null
            val wrappedKey = ByteArray(keyLength).also(buffer::get)
            val iv = ByteArray(EncryptedMessage.IV_SIZE).also(buffer::get)
            val ciphertext = ByteArray(buffer.remaining()).also(buffer::get)
            return Envelope(wrappedKey, EncryptedMessage(iv, ciphertext))
        }
    }
}

class EnvelopeCipher {

    fun seal(plaintext: ByteArray, publicKey: PublicKey): Envelope {
        val dataKey = KeyGenerator.getInstance("AES").apply { init(DATA_KEY_SIZE_BITS) }.generateKey()
        val cipher = Cipher.getInstance(AES_TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, dataKey) }
        val message = EncryptedMessage(cipher.iv, cipher.doFinal(plaintext))
        val wrappedKey = Cipher.getInstance(RSA_TRANSFORMATION).run {
            init(Cipher.WRAP_MODE, publicKey, OAEP_PARAMETERS)
            wrap(dataKey)
        }
        return Envelope(wrappedKey, message)
    }

    fun open(envelope: Envelope, privateKey: PrivateKey): ByteArray {
        val dataKey = Cipher.getInstance(RSA_TRANSFORMATION).run {
            init(Cipher.UNWRAP_MODE, privateKey, OAEP_PARAMETERS)
            unwrap(envelope.wrappedKey, "AES", Cipher.SECRET_KEY) as SecretKey
        }
        return Cipher.getInstance(AES_TRANSFORMATION).run {
            init(Cipher.DECRYPT_MODE, dataKey, GCMParameterSpec(EncryptedMessage.TAG_SIZE * Byte.SIZE_BITS, envelope.message.iv))
            doFinal(envelope.message.ciphertext)
        }
    }

    fun rsaOnly(plaintext: ByteArray, publicKey: PublicKey): ByteArray = Cipher.getInstance(RSA_TRANSFORMATION).run {
        init(Cipher.ENCRYPT_MODE, publicKey, OAEP_PARAMETERS)
        doFinal(plaintext)
    }

    companion object {
        const val RSA_KEY_SIZE_BITS = 2048
        const val DATA_KEY_SIZE_BITS = 256
        private const val HASH_BYTES = 32
        const val RSA_MAX_PLAINTEXT_BYTES = RSA_KEY_SIZE_BITS / Byte.SIZE_BITS - 2 * HASH_BYTES - 2
        private const val AES_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val RSA_TRANSFORMATION = "RSA/ECB/OAEPPadding"
        private val OAEP_PARAMETERS = OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT)
    }
}
