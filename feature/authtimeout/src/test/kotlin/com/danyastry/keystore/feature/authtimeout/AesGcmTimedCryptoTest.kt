package com.danyastry.keystore.feature.authtimeout

import com.danyastry.keystore.core.keystore.EncryptedMessage
import com.google.common.truth.Truth.assertThat
import javax.crypto.AEADBadTagException
import javax.crypto.KeyGenerator
import org.junit.Assert.assertThrows
import org.junit.Test

class AesGcmTimedCryptoTest {

    private val crypto = AesGcmTimedCrypto()
    private val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

    @Test
    fun `encrypts and decrypts`() {
        val sealed = crypto.encrypt(key, "Attack at dawn".encodeToByteArray())

        assertThat(crypto.decrypt(key, sealed).decodeToString()).isEqualTo("Attack at dawn")
    }

    @Test
    fun `uses a fresh IV every time`() {
        val first = crypto.encrypt(key, "Hello".encodeToByteArray())
        val second = crypto.encrypt(key, "Hello".encodeToByteArray())

        assertThat(first.iv).hasLength(12)
        assertThat(first.iv).isNotEqualTo(second.iv)
    }

    @Test
    fun `rejects tampered ciphertext`() {
        val sealed = crypto.encrypt(key, "Hello".encodeToByteArray())
        val tampered = sealed.ciphertext.copyOf().also { it[0] = (it[0].toInt() xor 1).toByte() }

        assertThrows(AEADBadTagException::class.java) { crypto.decrypt(key, EncryptedMessage(sealed.iv, tampered)) }
    }


}
