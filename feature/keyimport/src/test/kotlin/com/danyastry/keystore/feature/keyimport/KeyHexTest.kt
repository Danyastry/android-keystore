package com.danyastry.keystore.feature.keyimport

import com.google.common.truth.Truth.assertThat
import java.security.SecureRandom
import org.junit.Test

class KeyHexTest {

    @Test
    fun `parses 128, 192 and 256 bit keys`() {
        assertThat(KeyHex.parseOrNull("00".repeat(16))).hasLength(16)
        assertThat(KeyHex.parseOrNull("00".repeat(24))).hasLength(24)
        assertThat(KeyHex.parseOrNull("00".repeat(32))).hasLength(32)
    }

    @Test
    fun `accepts upper case, spaces and colons`() {
        val bytes = KeyHex.parseOrNull("2B7E1516 28AED2A6 abf71588 09cf4f3c")!!

        assertThat(KeyHex.format(bytes)).isEqualTo("2b7e151628aed2a6abf7158809cf4f3c")
        assertThat(KeyHex.parseOrNull("2b:7e:15:16:28:ae:d2:a6:ab:f7:15:88:09:cf:4f:3c")).isEqualTo(bytes)
    }

    @Test
    fun `rejects other sizes and non hex text`() {
        assertThat(KeyHex.parseOrNull("00".repeat(20))).isNull()
        assertThat(KeyHex.parseOrNull("0".repeat(63))).isNull()
        assertThat(KeyHex.parseOrNull("zz".repeat(16))).isNull()
        assertThat(KeyHex.parseOrNull("")).isNull()
    }

    @Test
    fun `random key is 256 bits and parses back`() {
        val hex = KeyHex.random(SecureRandom())

        assertThat(hex).hasLength(64)
        assertThat(KeyHex.parseOrNull(hex)).hasLength(32)
        assertThat(KeyHex.random()).isNotEqualTo(hex)
    }
}
