package com.danyastry.keystore.feature.envelope

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import java.security.GeneralSecurityException
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EnvelopeKeysTest {

    private val keys = EnvelopeKeys.store()
    private val cipher = EnvelopeCipher()

    @After
    fun tearDown() {
        keys.delete(ALIAS)
    }

    @Test
    fun keystoreKeyOpensALargeEnvelope() {
        val pair = keys.create(ALIAS)
        val plaintext = ByteArray(64 * 1024) { (it % 251).toByte() }

        val envelope = cipher.seal(plaintext, pair.public)

        assertThat(cipher.open(envelope, keys.get(ALIAS)!!.private)).isEqualTo(plaintext)
    }

    @Test
    fun privateKeyMaterialCannotBeRead() {
        assertThat(keys.create(ALIAS).private.encoded).isNull()
    }

    @Test
    fun keystoreKeyRefusesDataLargerThanOneRsaBlock() {
        val pair = keys.create(ALIAS)

        assertThrows(GeneralSecurityException::class.java) { cipher.rsaOnly(ByteArray(EnvelopeCipher.RSA_MAX_PLAINTEXT_BYTES + 1), pair.public) }
    }

    @Test
    fun aNewKeyPairCannotOpenAnOldEnvelope() {
        val envelope = cipher.seal("Attack at dawn".encodeToByteArray(), keys.create(ALIAS).public)

        keys.delete(ALIAS)
        val newPair = keys.create(ALIAS)

        assertThrows(GeneralSecurityException::class.java) { cipher.open(envelope, newPair.private) }
    }

    private companion object {
        const val ALIAS = "envelope-test"
    }
}
