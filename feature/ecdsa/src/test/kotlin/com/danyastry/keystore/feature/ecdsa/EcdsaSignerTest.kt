package com.danyastry.keystore.feature.ecdsa

import com.google.common.truth.Truth.assertThat
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import org.junit.Test

class EcdsaSignerTest {

    private val signer = EcdsaSigner()
    private val keyPair = newKeyPair()

    @Test
    fun `accepts a signature made by openssl`() {
        val publicKey = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(resource("openssl-ecdsa-public-key.b64")))

        assertThat(signer.verify("Attack at dawn".encodeToByteArray(), resource("openssl-ecdsa-signature.b64"), publicKey)).isTrue()
    }

    @Test
    fun `rejects the openssl signature for another message`() {
        val publicKey = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(resource("openssl-ecdsa-public-key.b64")))

        assertThat(signer.verify("Attack at dusk".encodeToByteArray(), resource("openssl-ecdsa-signature.b64"), publicKey)).isFalse()
    }

    @Test
    fun `signs and verifies`() {
        val signature = signer.sign("Pay Alice 100 dollars".encodeToByteArray(), keyPair.private)

        assertThat(signer.verify("Pay Alice 100 dollars".encodeToByteArray(), signature, keyPair.public)).isTrue()
    }

    @Test
    fun `one changed letter breaks the signature`() {
        val signature = signer.sign("Pay Alice 100 dollars".encodeToByteArray(), keyPair.private)

        assertThat(signer.verify("Pay Alice 900 dollars".encodeToByteArray(), signature, keyPair.public)).isFalse()
    }

    @Test
    fun `another public key rejects the signature`() {
        val signature = signer.sign("Hello".encodeToByteArray(), keyPair.private)

        assertThat(signer.verify("Hello".encodeToByteArray(), signature, newKeyPair().public)).isFalse()
    }

    @Test
    fun `changed signature bytes are rejected without throwing`() {
        val signature = signer.sign("Hello".encodeToByteArray(), keyPair.private)
        val changed = signature.copyOf().also { it[it.size - 1] = (it[it.size - 1].toInt() xor 1).toByte() }

        assertThat(signer.verify("Hello".encodeToByteArray(), changed, keyPair.public)).isFalse()
        assertThat(signer.verify("Hello".encodeToByteArray(), byteArrayOf(1, 2, 3), keyPair.public)).isFalse()
    }

    @Test
    fun `signing twice gives different signatures that both verify`() {
        val first = signer.sign("Hello".encodeToByteArray(), keyPair.private)
        val second = signer.sign("Hello".encodeToByteArray(), keyPair.private)

        assertThat(first).isNotEqualTo(second)
        assertThat(signer.verify("Hello".encodeToByteArray(), first, keyPair.public)).isTrue()
        assertThat(signer.verify("Hello".encodeToByteArray(), second, keyPair.public)).isTrue()
    }

    @Test
    fun `der signatures of p256 are at most 72 bytes`() {
        repeat(20) {
            assertThat(signer.sign("Hello".encodeToByteArray(), keyPair.private).size).isAtMost(72)
        }
    }

    private fun newKeyPair(): KeyPair =
        KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec(EcdsaKeys.CURVE)) }.generateKeyPair()

    private fun resource(name: String): ByteArray =
        Base64.getDecoder().decode(requireNotNull(javaClass.classLoader?.getResource(name)).readText().trim())
}
