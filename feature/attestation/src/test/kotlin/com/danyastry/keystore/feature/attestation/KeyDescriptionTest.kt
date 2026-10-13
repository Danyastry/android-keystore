package com.danyastry.keystore.feature.attestation

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class KeyDescriptionTest {

    private val challenge = ByteArray(32) { it.toByte() }

    @Test
    fun `parses the fields of a key description`() {
        val description = KeyDescription.parse(extension(version = 200, level = 1, keymasterVersion = 200, keymasterLevel = 1))

        assertThat(description.attestationVersion).isEqualTo(200)
        assertThat(description.attestationLevel).isEqualTo(AttestationLevel.TRUSTED_ENVIRONMENT)
        assertThat(description.keymasterVersion).isEqualTo(200)
        assertThat(description.keymasterLevel).isEqualTo(AttestationLevel.TRUSTED_ENVIRONMENT)
        assertThat(description.challenge).isEqualTo(challenge)
    }

    @Test
    fun `maps security levels`() {
        assertThat(KeyDescription.levelOf(0)).isEqualTo(AttestationLevel.SOFTWARE)
        assertThat(KeyDescription.levelOf(1)).isEqualTo(AttestationLevel.TRUSTED_ENVIRONMENT)
        assertThat(KeyDescription.levelOf(2)).isEqualTo(AttestationLevel.STRONGBOX)
        assertThat(KeyDescription.levelOf(7)).isEqualTo(AttestationLevel.UNKNOWN)
    }

    @Test
    fun `reads a strongbox attestation`() {
        val description = KeyDescription.parse(extension(version = 300, level = 2, keymasterVersion = 300, keymasterLevel = 2))

        assertThat(description.attestationLevel).isEqualTo(AttestationLevel.STRONGBOX)
    }

    @Test
    fun `rejects data that is not a key description`() {
        assertThrows(IllegalArgumentException::class.java) { KeyDescription.parse(der(0x04, der(0x02, byteArrayOf(1)))) }
        assertThrows(IllegalArgumentException::class.java) { KeyDescription.parse(der(0x04, der(0x30, der(0x02, byteArrayOf(1))))) }
    }

    private fun extension(version: Int, level: Int, keymasterVersion: Int, keymasterLevel: Int): ByteArray {
        val sequence = der(
            0x30,
            der(0x02, int(version)) + der(0x0A, byteArrayOf(level.toByte())) +
                der(0x02, int(keymasterVersion)) + der(0x0A, byteArrayOf(keymasterLevel.toByte())) +
                der(0x04, challenge) + der(0x04, ByteArray(0)) + der(0x30, ByteArray(0)) + der(0x30, ByteArray(0)),
        )
        return der(0x04, sequence)
    }

    private fun int(value: Int): ByteArray = java.math.BigInteger.valueOf(value.toLong()).toByteArray()

    private fun der(tag: Int, content: ByteArray): ByteArray {
        val length = when {
            content.size < 0x80 -> byteArrayOf(content.size.toByte())
            content.size < 0x100 -> byteArrayOf(0x81.toByte(), content.size.toByte())
            else -> byteArrayOf(0x82.toByte(), (content.size shr 8).toByte(), content.size.toByte())
        }
        return byteArrayOf(tag.toByte()) + length + content
    }
}
