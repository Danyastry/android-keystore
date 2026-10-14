package com.danyastry.keystore.feature.ecdh

import com.google.common.truth.Truth.assertThat
import javax.crypto.AEADBadTagException
import org.junit.Assert.assertThrows
import org.junit.Test

class KeyAgreementsTest {

    private val agreement = SoftwareAgreement()
    private val alice = EcdhKeys.softwareKeyPair()
    private val bob = EcdhKeys.softwareKeyPair()

    @Test
    fun `both sides compute the same 32 byte secret`() {
        val aliceSecret = agreement.sharedSecret(alice.private, bob.public)
        val bobSecret = agreement.sharedSecret(bob.private, alice.public)

        assertThat(aliceSecret).hasLength(32)
        assertThat(aliceSecret).isEqualTo(bobSecret)
    }

    @Test
    fun `a different peer gives a different secret`() {
        val carol = EcdhKeys.softwareKeyPair()

        assertThat(agreement.sharedSecret(alice.private, bob.public))
            .isNotEqualTo(agreement.sharedSecret(alice.private, carol.public))
    }

    @Test
    fun `session key is a 256 bit aes key derived the same way on both sides`() {
        val aliceKey = SessionKeys.derive(agreement.sharedSecret(alice.private, bob.public))
        val bobKey = SessionKeys.derive(agreement.sharedSecret(bob.private, alice.public))

        assertThat(aliceKey.algorithm).isEqualTo("AES")
        assertThat(aliceKey.encoded).hasLength(32)
        assertThat(aliceKey.encoded).isEqualTo(bobKey.encoded)
    }

    @Test
    fun `session key is not the raw secret`() {
        val secret = agreement.sharedSecret(alice.private, bob.public)

        assertThat(SessionKeys.derive(secret).encoded).isNotEqualTo(secret)
    }

    @Test
    fun `one side encrypts and the other decrypts`() {
        val aliceKey = SessionKeys.derive(agreement.sharedSecret(alice.private, bob.public))
        val bobKey = SessionKeys.derive(agreement.sharedSecret(bob.private, alice.public))

        val message = SessionCipher.encrypt(aliceKey, "Attack at dawn".encodeToByteArray())

        assertThat(SessionCipher.decrypt(bobKey, message).decodeToString()).isEqualTo("Attack at dawn")
    }

    @Test
    fun `a third party cannot decrypt`() {
        val carol = EcdhKeys.softwareKeyPair()
        val aliceKey = SessionKeys.derive(agreement.sharedSecret(alice.private, bob.public))
        val carolKey = SessionKeys.derive(agreement.sharedSecret(carol.private, alice.public))

        val message = SessionCipher.encrypt(aliceKey, "Attack at dawn".encodeToByteArray())

        assertThrows(AEADBadTagException::class.java) { SessionCipher.decrypt(carolKey, message) }
    }

    @Test
    fun `fingerprint shows the first 8 bytes of sha-256`() {
        assertThat(Fingerprint.of("abc".encodeToByteArray())).isEqualTo("ba:78:16:bf:8f:01:cf:ea")
    }
}
