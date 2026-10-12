package com.danyastry.keystore.feature.rsaoaep

import com.google.common.truth.Truth.assertThat
import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64
import org.junit.Assert.assertThrows
import org.junit.Test

class RsaOaepCipherTest {

    private val cipher = RsaOaepCipher()
    private val keyPair = newKeyPair()

    @Test
    fun `decrypts a message encrypted by openssl`() {
        val privateKey = KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(resource("openssl-oaep-private-key.b64")))

        val plaintext = cipher.decrypt(resource("openssl-oaep-ciphertext.b64"), privateKey)

        assertThat(plaintext.decodeToString()).isEqualTo("Attack at dawn")
    }

    @Test
    fun `round trips text`() {
        val ciphertext = cipher.encrypt("Attack at dawn".encodeToByteArray(), keyPair.public)

        assertThat(cipher.decrypt(ciphertext, keyPair.private).decodeToString()).isEqualTo("Attack at dawn")
    }

    @Test
    fun `ciphertext is always as long as the key`() {
        listOf(0, 1, 100, RsaOaepCipher.MAX_PLAINTEXT_BYTES).forEach { size ->
            assertThat(cipher.encrypt(ByteArray(size), keyPair.public)).hasLength(256)
        }
    }

    @Test
    fun `same text encrypts differently every time`() {
        val first = cipher.encrypt("Hello".encodeToByteArray(), keyPair.public)
        val second = cipher.encrypt("Hello".encodeToByteArray(), keyPair.public)

        assertThat(first).isNotEqualTo(second)
    }

    @Test
    fun `fits up to 190 bytes`() {
        assertThat(RsaOaepCipher.MAX_PLAINTEXT_BYTES).isEqualTo(190)
        val largest = ByteArray(190) { it.toByte() }

        assertThat(cipher.decrypt(cipher.encrypt(largest, keyPair.public), keyPair.private)).isEqualTo(largest)
        assertThrows(IllegalArgumentException::class.java) { cipher.encrypt(ByteArray(191), keyPair.public) }
    }

    @Test
    fun `another private key cannot decrypt`() {
        val ciphertext = cipher.encrypt("Hello".encodeToByteArray(), keyPair.public)

        assertThrows(GeneralSecurityException::class.java) { cipher.decrypt(ciphertext, newKeyPair().private) }
    }

    @Test
    fun `changed ciphertext is rejected`() {
        val ciphertext = cipher.encrypt("Hello".encodeToByteArray(), keyPair.public)
        ciphertext[10] = (ciphertext[10].toInt() xor 1).toByte()

        assertThrows(GeneralSecurityException::class.java) { cipher.decrypt(ciphertext, keyPair.private) }
    }

    private fun newKeyPair(): KeyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()

    private fun resource(name: String): ByteArray =
        Base64.getDecoder().decode(requireNotNull(javaClass.classLoader?.getResource(name)).readText().trim())
}
