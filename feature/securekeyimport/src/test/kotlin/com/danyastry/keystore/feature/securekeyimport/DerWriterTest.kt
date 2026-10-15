package com.danyastry.keystore.feature.securekeyimport

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DerWriterTest {

    @Test
    fun `encodes integers with the minimal two's complement form`() {
        assertThat(DerWriter.integer(0).hex()).isEqualTo("020100")
        assertThat(DerWriter.integer(3).hex()).isEqualTo("020103")
        assertThat(DerWriter.integer(127).hex()).isEqualTo("02017f")
        assertThat(DerWriter.integer(128).hex()).isEqualTo("02020080")
        assertThat(DerWriter.integer(256).hex()).isEqualTo("02020100")
    }

    @Test
    fun `encodes octet strings and null`() {
        assertThat(DerWriter.octetString(byteArrayOf(1, 2)).hex()).isEqualTo("04020102")
        assertThat(DerWriter.nullValue().hex()).isEqualTo("0500")
    }

    @Test
    fun `uses the long length form from 128 bytes`() {
        assertThat(DerWriter.length(127).hex()).isEqualTo("7f")
        assertThat(DerWriter.length(128).hex()).isEqualTo("8180")
        assertThat(DerWriter.length(256).hex()).isEqualTo("820100")
        assertThat(DerWriter.octetString(ByteArray(200)).copyOf(3).hex()).isEqualTo("0481c8")
    }

    @Test
    fun `wraps items in a sequence`() {
        assertThat(DerWriter.sequence(DerWriter.integer(1), DerWriter.nullValue()).hex()).isEqualTo("30050201010500")
    }

    @Test
    fun `sorts set members by their encoding`() {
        assertThat(DerWriter.setOf(DerWriter.integer(1), DerWriter.integer(0)).hex()).isEqualTo("3106020100020101")
    }

    @Test
    fun `explicit tags below 31 fit in one byte`() {
        assertThat(DerWriter.explicit(1, DerWriter.integer(5)).hex()).isEqualTo("a103020105")
        assertThat(DerWriter.explicit(30, DerWriter.nullValue()).hex()).isEqualTo("be020500")
    }

    @Test
    fun `explicit tag 503 uses the high tag number form`() {
        assertThat(DerWriter.explicit(503, DerWriter.nullValue()).hex()).isEqualTo("bf8377020500")
    }

    private fun ByteArray.hex(): String = joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
}
