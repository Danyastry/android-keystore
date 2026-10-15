package com.danyastry.keystore.feature.envelope

import com.google.common.truth.Truth.assertThat
import java.security.GeneralSecurityException
import java.security.KeyPairGenerator
import org.junit.Assert.assertThrows
import org.junit.Test

class EnvelopeCipherTest {

    private val cipher = EnvelopeCipher()
    private val keyPair = newKeyPair()

    @Test
    fun `seals and opens large data`() {
        val plaintext = ByteArray(64 * 1024) { (it % 251).toByte() }

        val envelope = cipher.seal(plaintext, keyPair.public)

        assertThat(cipher.open(envelope, keyPair.private)).isEqualTo(plaintext)
    }

    @Test
    fun `wrapped key is one rsa block and the payload grows only by the tag`() {
        val envelope = cipher.seal(ByteArray(1000), keyPair.public)

        assertThat(envelope.wrappedKey).hasLength(256)
        assertThat(envelope.message.iv).hasLength(12)
        assertThat(envelope.message.ciphertext).hasLength(1000 + 16)
        assertThat(envelope.size).isEqualTo(2 + 256 + 12 + 1016)
    }

    @Test
    fun `every envelope uses a fresh data key`() {
        val first = cipher.seal("Hello".encodeToByteArray(), keyPair.public)
        val second = cipher.seal("Hello".encodeToByteArray(), keyPair.public)

        assertThat(first.wrappedKey).isNotEqualTo(second.wrappedKey)
        assertThat(first.message.ciphertext).isNotEqualTo(second.message.ciphertext)
    }

    @Test
    fun `survives the byte format`() {
        val envelope = cipher.seal("Attack at dawn".encodeToByteArray(), keyPair.public)

        val restored = Envelope.fromBytesOrNull(envelope.toBytes())!!

        assertThat(restored.wrappedKey).isEqualTo(envelope.wrappedKey)
        assertThat(restored.message).isEqualTo(envelope.message)
        assertThat(cipher.open(restored, keyPair.private).decodeToString()).isEqualTo("Attack at dawn")
    }

    @Test
    fun `rejects truncated bytes`() {
        val bytes = cipher.seal("Hello".encodeToByteArray(), keyPair.public).toBytes()

        assertThat(Envelope.fromBytesOrNull(bytes.copyOf(100))).isNull()
        assertThat(Envelope.fromBytesOrNull(ByteArray(1))).isNull()
    }

    @Test
    fun `another key pair cannot open it`() {
        val envelope = cipher.seal("Attack at dawn".encodeToByteArray(), keyPair.public)

        assertThrows(GeneralSecurityException::class.java) { cipher.open(envelope, newKeyPair().private) }
    }

    @Test
    fun `rsa alone fits 190 bytes and no more`() {
        assertThat(EnvelopeCipher.RSA_MAX_PLAINTEXT_BYTES).isEqualTo(190)
        assertThat(cipher.rsaOnly(ByteArray(190), keyPair.public)).hasLength(256)
        assertThrows(GeneralSecurityException::class.java) { cipher.rsaOnly(ByteArray(191), keyPair.public) }
    }

    private fun newKeyPair() = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
}
