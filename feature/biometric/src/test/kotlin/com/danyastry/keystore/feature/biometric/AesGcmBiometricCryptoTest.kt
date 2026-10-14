package com.danyastry.keystore.feature.biometric

import com.google.common.truth.Truth.assertThat
import javax.crypto.AEADBadTagException
import javax.crypto.KeyGenerator
import org.junit.Assert.assertThrows
import org.junit.Test

class AesGcmBiometricCryptoTest {

    private val crypto = AesGcmBiometricCrypto()
    private val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

    @Test
    fun `seals and opens a message`() {
        val sealed = crypto.seal(crypto.encryptionCipher(key), "Attack at dawn".encodeToByteArray())

        val opened = crypto.open(crypto.decryptionCipher(key, sealed.iv), sealed.ciphertext)

        assertThat(opened.decodeToString()).isEqualTo("Attack at dawn")
    }

    @Test
    fun `uses a fresh 12 byte IV every time`() {
        val first = crypto.seal(crypto.encryptionCipher(key), "Hello".encodeToByteArray())
        val second = crypto.seal(crypto.encryptionCipher(key), "Hello".encodeToByteArray())

        assertThat(first.iv).hasLength(12)
        assertThat(first.iv).isNotEqualTo(second.iv)
        assertThat(first.toBase64()).isNotEqualTo(second.toBase64())
    }

    @Test
    fun `ciphertext carries a 16 byte tag`() {
        val sealed = crypto.seal(crypto.encryptionCipher(key), "Hello".encodeToByteArray())

        assertThat(sealed.ciphertext).hasLength(5 + 16)
    }

    @Test
    fun `rejects tampered ciphertext`() {
        val sealed = crypto.seal(crypto.encryptionCipher(key), "Hello".encodeToByteArray())
        val tampered = sealed.ciphertext.copyOf().also { it[0] = (it[0].toInt() xor 1).toByte() }

        assertThrows(AEADBadTagException::class.java) { crypto.open(crypto.decryptionCipher(key, sealed.iv), tampered) }
    }

    @Test
    fun `base64 round trip keeps IV and ciphertext`() {
        val sealed = crypto.seal(crypto.encryptionCipher(key), "Hello".encodeToByteArray())

        val restored = SealedMessage.fromBase64OrNull(sealed.toBase64())!!

        assertThat(restored.iv).isEqualTo(sealed.iv)
        assertThat(restored.ciphertext).isEqualTo(sealed.ciphertext)
    }

    @Test
    fun `invalid base64 or too short data is rejected`() {
        assertThat(SealedMessage.fromBase64OrNull("not base64!")).isNull()
        assertThat(SealedMessage.fromBase64OrNull("AAAA")).isNull()
    }
}
