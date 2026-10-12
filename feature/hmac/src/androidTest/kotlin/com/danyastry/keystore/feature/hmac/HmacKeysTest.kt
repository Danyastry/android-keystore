package com.danyastry.keystore.feature.hmac

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HmacKeysTest {

    private val keys = HmacKeys.store()
    private val signer = HmacSigner()

    @After
    fun tearDown() {
        keys.delete(ALIAS)
    }

    @Test
    fun createsAHiddenHmacKey() {
        val key = keys.create(ALIAS)

        assertThat(key.algorithm).isEqualTo("HmacSHA256")
        assertThat(key.encoded).isNull()
    }

    @Test
    fun computesAndVerifiesTagsInsideTheKeystore() {
        keys.create(ALIAS)
        val key = keys.get(ALIAS)!!

        val tag = signer.tag("Pay Alice 100 dollars".encodeToByteArray(), key)

        assertThat(tag).hasLength(32)
        assertThat(signer.verify("Pay Alice 100 dollars".encodeToByteArray(), tag, key)).isTrue()
        assertThat(signer.verify("Pay Alice 900 dollars".encodeToByteArray(), tag, key)).isFalse()
    }

    @Test
    fun tagIsStableAcrossKeyLookups() {
        keys.create(ALIAS)

        val first = signer.tag("Hello".encodeToByteArray(), keys.get(ALIAS)!!)
        val second = signer.tag("Hello".encodeToByteArray(), keys.get(ALIAS)!!)

        assertThat(second).isEqualTo(first)
    }

    @Test
    fun deleteRemovesTheKey() {
        keys.create(ALIAS)

        keys.delete(ALIAS)

        assertThat(keys.get(ALIAS)).isNull()
    }

    private companion object {
        const val ALIAS = "hmac-test"
    }
}
