package com.danyastry.keystore.core.keystore

import com.google.common.truth.Truth.assertThat
import java.util.Base64
import org.junit.Assert.assertThrows
import org.junit.Test

class EncryptedMessageTest {

    private val message = EncryptedMessage(iv = ByteArray(12) { it.toByte() }, ciphertext = ByteArray(21) { (it * 7).toByte() })

    @Test
    fun `survives base64`() {
        assertThat(EncryptedMessage.fromBase64OrNull(message.toBase64())).isEqualTo(message)
    }

    @Test
    fun `ignores whitespace around pasted base64`() {
        assertThat(EncryptedMessage.fromBase64OrNull(" " + message.toBase64() + "\n")).isEqualTo(message)
    }

    @Test
    fun `base64 holds the iv first and then the ciphertext`() {
        val bytes = Base64.getDecoder().decode(message.toBase64())

        assertThat(bytes.copyOfRange(0, 12)).isEqualTo(message.iv)
        assertThat(bytes.copyOfRange(12, bytes.size)).isEqualTo(message.ciphertext)
    }

    @Test
    fun `rejects text that is not a message`() {
        assertThat(EncryptedMessage.fromBase64OrNull("not base64!")).isNull()
        assertThat(EncryptedMessage.fromBase64OrNull("AAAA")).isNull()
        assertThat(EncryptedMessage.fromBase64OrNull("")).isNull()
    }

    @Test
    fun `rejects a wrong iv size`() {
        assertThrows(IllegalArgumentException::class.java) { EncryptedMessage(ByteArray(16), ByteArray(16)) }
    }

    @Test
    fun `rejects ciphertext shorter than the tag`() {
        assertThrows(IllegalArgumentException::class.java) { EncryptedMessage(ByteArray(12), ByteArray(15)) }
    }

    @Test
    fun `compares by content`() {
        val copy = EncryptedMessage(message.iv.copyOf(), message.ciphertext.copyOf())

        assertThat(copy).isEqualTo(message)
        assertThat(copy.hashCode()).isEqualTo(message.hashCode())
    }
}
