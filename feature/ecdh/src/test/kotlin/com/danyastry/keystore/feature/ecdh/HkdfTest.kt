package com.danyastry.keystore.feature.ecdh

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class HkdfTest {

    @Test
    fun `rfc 5869 test case 1`() {
        val ikm = ByteArray(22) { 0x0b }
        val salt = hex("000102030405060708090a0b0c")
        val info = hex("f0f1f2f3f4f5f6f7f8f9")

        val prk = Hkdf.extract(salt, ikm)

        assertThat(prk.toHex()).isEqualTo("077709362c2e32df0ddc3f0dc47bba6390b6c73bb50f9c3122ec844ad7c2b3e5")
        assertThat(Hkdf.expand(prk, info, 42).toHex())
            .isEqualTo("3cb25f25faacd57a90434f64d0362f2a2d2d0a90cf1a5a4c5db02d56ecc4c5bf34007208d5b887185865")
    }

    @Test
    fun `rfc 5869 test case 3 with empty salt and info`() {
        val ikm = ByteArray(22) { 0x0b }

        assertThat(Hkdf.extract(ByteArray(0), ikm).toHex())
            .isEqualTo("19ef24a32c717b167f33a91d6f648bdf96596776afdb6377ac434c1c293ccb04")
        assertThat(Hkdf.derive(ikm, ByteArray(0), ByteArray(0), 42).toHex())
            .isEqualTo("8da4e775a563c18f715f802a063c5a31b8a11f5c5ee1879ec3454e5f3c738d2d9d201395faa4b61a96c8")
    }

    @Test
    fun `shorter output is a prefix of longer output`() {
        val ikm = "secret".encodeToByteArray()

        val long = Hkdf.derive(ikm, ByteArray(0), ByteArray(0), 64)
        val short = Hkdf.derive(ikm, ByteArray(0), ByteArray(0), 20)

        assertThat(long.copyOf(20)).isEqualTo(short)
    }

    @Test
    fun `different info gives a different key`() {
        val ikm = "secret".encodeToByteArray()

        assertThat(Hkdf.derive(ikm, ByteArray(0), "a".encodeToByteArray(), 32))
            .isNotEqualTo(Hkdf.derive(ikm, ByteArray(0), "b".encodeToByteArray(), 32))
    }

    @Test
    fun `rejects lengths outside the rfc limit`() {
        assertThrows(IllegalArgumentException::class.java) { Hkdf.expand(ByteArray(32), ByteArray(0), 0) }
        assertThrows(IllegalArgumentException::class.java) { Hkdf.expand(ByteArray(32), ByteArray(0), 255 * 32 + 1) }
    }

    private fun hex(text: String): ByteArray = ByteArray(text.length / 2) { text.substring(it * 2, it * 2 + 2).toInt(16).toByte() }

    private fun ByteArray.toHex(): String = joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
}
