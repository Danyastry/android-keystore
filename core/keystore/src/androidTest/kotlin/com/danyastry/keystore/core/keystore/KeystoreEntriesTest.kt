package com.danyastry.keystore.core.keystore

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KeystoreEntriesTest {

    private val entries = KeystoreEntries()
    private val secretKeys = AndroidKeyStoreSecretKeys(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, ::hmacSpec)
    private val keyPairs = AndroidKeyStoreKeyPairs(KeyProperties.KEY_ALGORITHM_EC, ::ecSpec)

    @After
    fun tearDown() {
        listOf(SECRET_ALIAS, PAIR_ALIAS).forEach(entries::delete)
    }

    @Test
    fun createdSecretKeyIsFoundAndListed() {
        secretKeys.create(SECRET_ALIAS)

        assertThat(entries.contains(SECRET_ALIAS)).isTrue()
        assertThat(entries.aliases()).contains(SECRET_ALIAS)
        assertThat(secretKeys.get(SECRET_ALIAS)).isNotNull()
        assertThat(entries.keyPair(SECRET_ALIAS)).isNull()
    }

    @Test
    fun createdKeyPairIsFoundWithItsPublicKey() {
        keyPairs.create(PAIR_ALIAS)

        val pair = keyPairs.get(PAIR_ALIAS)
        assertThat(pair).isNotNull()
        assertThat(pair!!.public.algorithm).isEqualTo("EC")
        assertThat(entries.secretKey(PAIR_ALIAS)).isNull()
    }

    @Test
    fun aliasesAreSorted() {
        keyPairs.create(PAIR_ALIAS)
        secretKeys.create(SECRET_ALIAS)

        val ours = entries.aliases().filter { it in listOf(SECRET_ALIAS, PAIR_ALIAS) }

        assertThat(ours).containsExactly(PAIR_ALIAS, SECRET_ALIAS).inOrder()
    }

    @Test
    fun deleteRemovesEntriesAndIgnoresMissingOnes() {
        secretKeys.create(SECRET_ALIAS)

        secretKeys.delete(SECRET_ALIAS)
        secretKeys.delete(SECRET_ALIAS)

        assertThat(entries.contains(SECRET_ALIAS)).isFalse()
        assertThat(secretKeys.get(SECRET_ALIAS)).isNull()
    }

    @Test
    fun missingAliasesGiveNull() {
        assertThat(entries.secretKey("missing-alias")).isNull()
        assertThat(entries.keyPair("missing-alias")).isNull()
    }

    private fun hmacSpec(alias: String) = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN).build()

    private fun ecSpec(alias: String) = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN)
        .setDigests(KeyProperties.DIGEST_SHA256)
        .build()

    private companion object {
        const val SECRET_ALIAS = "core-test-secret"
        const val PAIR_ALIAS = "core-test-pair"
    }
}
