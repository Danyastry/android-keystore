package com.danyastry.keystore.feature.aesgcm

import com.danyastry.keystore.core.keystore.EncryptedMessage
import com.google.common.truth.Truth.assertThat
import javax.crypto.AEADBadTagException
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import org.junit.Assert.assertThrows
import org.junit.Test

class AesGcmCipherTest {

    private val cipher = AesGcmCipher()
    private val key = newKey()

    @Test
    fun `decrypts the gcm specification test case 14`() {
        val zeroKey = SecretKeySpec(ByteArray(32), "AES")
        val message = EncryptedMessage(
            iv = ByteArray(12),
            ciphertext = hex("cea7403d4d606b6e074ec5d3baf39d18" + "d0d1c8a799996bf0265b98b5d48ab919"),
        )

        assertThat(cipher.decrypt(message, zeroKey)).isEqualTo(ByteArray(16))
    }

    @Test
    fun `round trips text`() {
        val message = cipher.encrypt("Attack at dawn".encodeToByteArray(), key)

        assertThat(cipher.decrypt(message, key).decodeToString()).isEqualTo("Attack at dawn")
    }

    @Test
    fun `uses a 12 byte iv and appends a 16 byte tag`() {
        val message = cipher.encrypt("Hello".encodeToByteArray(), key)

        assertThat(message.iv).hasLength(12)
        assertThat(message.ciphertext).hasLength(5 + 16)
    }

    @Test
    fun `same text encrypts differently every time`() {
        val first = cipher.encrypt("Hello".encodeToByteArray(), key)
        val second = cipher.encrypt("Hello".encodeToByteArray(), key)

        assertThat(first.iv).isNotEqualTo(second.iv)
        assertThat(first.ciphertext).isNotEqualTo(second.ciphertext)
    }

    @Test
    fun `changed ciphertext fails authentication`() {
        val message = cipher.encrypt("Hello".encodeToByteArray(), key)
        val tampered = message.ciphertext.copyOf().also { it[0] = (it[0].toInt() xor 1).toByte() }

        assertThrows(AEADBadTagException::class.java) { cipher.decrypt(EncryptedMessage(message.iv, tampered), key) }
    }

    @Test
    fun `changed iv fails authentication`() {
        val message = cipher.encrypt("Hello".encodeToByteArray(), key)
        val otherIv = message.iv.copyOf().also { it[0] = (it[0].toInt() xor 1).toByte() }

        assertThrows(AEADBadTagException::class.java) { cipher.decrypt(EncryptedMessage(otherIv, message.ciphertext), key) }
    }

    @Test
    fun `another key fails authentication`() {
        val message = cipher.encrypt("Hello".encodeToByteArray(), key)

        assertThrows(AEADBadTagException::class.java) { cipher.decrypt(message, newKey()) }
    }

    @Test
    fun `empty text still carries a tag`() {
        val message = cipher.encrypt(ByteArray(0), key)

        assertThat(message.ciphertext).hasLength(16)
        assertThat(cipher.decrypt(message, key)).isEmpty()
    }




    private fun newKey(): SecretKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

    private fun hex(text: String): ByteArray = ByteArray(text.length / 2) { text.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
}
