package com.danyastry.keystore.feature.attestation

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class DerTest {

    @Test
    fun `reads short form lengths`() {
        val element = DerReader(byteArrayOf(0x04, 0x02, 0x0A, 0x0B)).read()

        assertThat(element.tag).isEqualTo(DerElement.TAG_OCTET_STRING)
        assertThat(element.asBytes()).isEqualTo(byteArrayOf(0x0A, 0x0B))
    }

    @Test
    fun `reads long form lengths`() {
        val content = ByteArray(300) { it.toByte() }
        val bytes = byteArrayOf(0x04, 0x82.toByte(), 0x01, 0x2C) + content

        assertThat(DerReader(bytes).read().asBytes()).isEqualTo(content)
    }

    @Test
    fun `reads integers and enumerations including negative ones`() {
        assertThat(DerReader(byteArrayOf(0x02, 0x01, 0x64)).read().asInt()).isEqualTo(100)
        assertThat(DerReader(byteArrayOf(0x02, 0x02, 0x01, 0x2C)).read().asInt()).isEqualTo(300)
        assertThat(DerReader(byteArrayOf(0x02, 0x01, 0xFF.toByte())).read().asInt()).isEqualTo(-1)
        assertThat(DerReader(byteArrayOf(0x0A, 0x01, 0x02)).read().asInt()).isEqualTo(2)
    }

    @Test
    fun `lists the children of a sequence`() {
        val sequence = DerReader(byteArrayOf(0x30, 0x06, 0x02, 0x01, 0x05, 0x04, 0x01, 0x07)).read()

        val children = sequence.children()

        assertThat(children.map { it.tag }).containsExactly(DerElement.TAG_INTEGER, DerElement.TAG_OCTET_STRING).inOrder()
        assertThat(children[0].asInt()).isEqualTo(5)
    }

    @Test
    fun `rejects truncated data`() {
        assertThrows(IllegalArgumentException::class.java) { DerReader(byteArrayOf(0x04, 0x05, 0x01)).read() }
        assertThrows(IllegalArgumentException::class.java) { DerReader(byteArrayOf(0x04)).read() }
    }

    @Test
    fun `rejects reading the wrong type`() {
        assertThrows(IllegalArgumentException::class.java) { DerReader(byteArrayOf(0x04, 0x01, 0x01)).read().asInt() }
        assertThrows(IllegalArgumentException::class.java) { DerReader(byteArrayOf(0x02, 0x01, 0x01)).read().asBytes() }
    }
}
