package com.danyastry.keystore.feature.hmac

import com.google.common.truth.Truth.assertThat
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import org.junit.Test

class HmacSignerTest {

    private val signer = HmacSigner()
    private val key = newKey()

    @Test
    fun `matches rfc 4231 test case 1`() {
        val key = SecretKeySpec(ByteArray(20) { 0x0b }, HmacSigner.ALGORITHM)

        assertThat(signer.tag("Hi There".encodeToByteArray(), key).toHex())
            .isEqualTo("b0344c61d8db38535ca8afceaf0bf12b881dc200c9833da726e9376c2e32cff7")
    }

    @Test
    fun `matches rfc 4231 test case 2`() {
        val key = SecretKeySpec("Jefe".encodeToByteArray(), HmacSigner.ALGORITHM)

        assertThat(signer.tag("what do ya want for nothing?".encodeToByteArray(), key).toHex())
            .isEqualTo("5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843")
    }

    @Test
    fun `tag is 32 bytes and deterministic`() {
        val first = signer.tag("Hello".encodeToByteArray(), key)

        assertThat(first).hasLength(HmacSigner.TAG_SIZE)
        assertThat(signer.tag("Hello".encodeToByteArray(), key)).isEqualTo(first)
    }

    @Test
    fun `verifies its own tag`() {
        val tag = signer.tag("Pay Alice 100 dollars".encodeToByteArray(), key)

        assertThat(signer.verify("Pay Alice 100 dollars".encodeToByteArray(), tag, key)).isTrue()
    }

    @Test
    fun `one changed letter breaks the tag`() {
        val tag = signer.tag("Pay Alice 100 dollars".encodeToByteArray(), key)

        assertThat(signer.verify("Pay Alice 900 dollars".encodeToByteArray(), tag, key)).isFalse()
    }

    @Test
    fun `another key or a changed tag is rejected`() {
        val tag = signer.tag("Hello".encodeToByteArray(), key)
        val changed = tag.copyOf().also { it[0] = (it[0].toInt() xor 1).toByte() }

        assertThat(signer.verify("Hello".encodeToByteArray(), tag, newKey())).isFalse()
        assertThat(signer.verify("Hello".encodeToByteArray(), changed, key)).isFalse()
        assertThat(signer.verify("Hello".encodeToByteArray(), tag.copyOf(16), key)).isFalse()
    }

    private fun newKey(): SecretKey = KeyGenerator.getInstance(HmacSigner.ALGORITHM).generateKey()

    private fun ByteArray.toHex(): String = joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
}
