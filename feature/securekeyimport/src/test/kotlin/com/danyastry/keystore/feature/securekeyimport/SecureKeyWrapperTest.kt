package com.danyastry.keystore.feature.securekeyimport

import com.google.common.truth.Truth.assertThat
import java.security.KeyPairGenerator
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import org.junit.Assert.assertThrows
import org.junit.Test

class SecureKeyWrapperTest {

    private val device = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    private val secret = ByteArray(32).also(SecureRandom()::nextBytes)

    @Test
    fun `the wrapper has the six keymint fields in order`() {
        val wrapped = SecureKeyWrapper.wrap(secret, device.public)

        val fields = DerReader.read(wrapped.bytes).children()

        assertThat(fields.map { it.tag }).containsExactly(0x02, 0x04, 0x04, 0x30, 0x04, 0x04).inOrder()
        assertThat(fields[0].asLong()).isEqualTo(SecureKeyWrapper.WRAPPER_FORMAT_VERSION)
        assertThat(fields[1].content).isEqualTo(wrapped.encryptedTransportKey)
        assertThat(fields[2].content).isEqualTo(wrapped.iv)
        assertThat(fields[3].encoded).isEqualTo(wrapped.keyDescription)
        assertThat(fields[4].content).isEqualTo(wrapped.encryptedKey)
        assertThat(fields[5].content).isEqualTo(wrapped.tag)
    }

    @Test
    fun `field sizes match the spec`() {
        val wrapped = SecureKeyWrapper.wrap(secret, device.public)

        assertThat(wrapped.encryptedTransportKey).hasLength(256)
        assertThat(wrapped.iv).hasLength(12)
        assertThat(wrapped.encryptedKey).hasLength(32)
        assertThat(wrapped.tag).hasLength(16)
    }

    @Test
    fun `key description is raw format with aes gcm authorizations`() {
        val description = DerReader.read(SecureKeyWrapper.wrap(secret, device.public).keyDescription).children()

        assertThat(description[0].asLong()).isEqualTo(SecureKeyWrapper.KEY_FORMAT_RAW)
        val tags = description[1].children().associate { it.tagNumber() to it.children().single() }
        assertThat(tags.keys).containsExactly(1, 2, 3, 4, 6, 8, 503)
        assertThat(tags.getValue(1).children().map { it.asLong() }).containsExactly(0L, 1L).inOrder()
        assertThat(tags.getValue(2).asLong()).isEqualTo(SecureKeyWrapper.ALGORITHM_AES)
        assertThat(tags.getValue(3).asLong()).isEqualTo(256L)
        assertThat(tags.getValue(4).children().single().asLong()).isEqualTo(SecureKeyWrapper.BLOCK_MODE_GCM)
        assertThat(tags.getValue(6).children().single().asLong()).isEqualTo(SecureKeyWrapper.PADDING_NONE)
        assertThat(tags.getValue(8).asLong()).isEqualTo(128L)
        assertThat(tags.getValue(503).tag).isEqualTo(0x05)
    }

    @Test
    fun `the device private key unwraps the original secret`() {
        val wrapped = SecureKeyWrapper.wrap(secret, device.public)

        assertThat(SoftwareUnwrapper.unwrap(wrapped.bytes, device.private)).isEqualTo(secret)
    }

    @Test
    fun `the key description is bound to the ciphertext as aad`() {
        val bytes = SecureKeyWrapper.wrap(secret, device.public).bytes
        val keySizeOffset = bytes.indexOfSequence(byteArrayOf(0xa3.toByte(), 0x04, 0x02, 0x02, 0x01, 0x00))
        bytes[keySizeOffset + 5] = 0x01

        assertThrows(AEADBadTagException::class.java) { SoftwareUnwrapper.unwrap(bytes, device.private) }
    }

    @Test
    fun `every wrap uses a fresh transport key and iv`() {
        val first = SecureKeyWrapper.wrap(secret, device.public)
        val second = SecureKeyWrapper.wrap(secret, device.public)

        assertThat(first.iv).isNotEqualTo(second.iv)
        assertThat(first.encryptedKey).isNotEqualTo(second.encryptedKey)
    }

    private fun DerNode.tagNumber(): Int = if (tag > 0xff) tag and 0xffff else tag and 0x1f

    private fun ByteArray.indexOfSequence(target: ByteArray): Int =
        (0..size - target.size).first { start -> target.indices.all { this[start + it] == target[it] } }
}
