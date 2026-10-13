package com.danyastry.keystore.feature.keyinfo

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.danyastry.keystore.core.keystore.AndroidKeyStoreKeyPairs
import com.danyastry.keystore.core.keystore.KeyKind
import com.danyastry.keystore.core.keystore.KeyLocation
import com.danyastry.keystore.core.keystore.KeyOrigin
import com.danyastry.keystore.core.keystore.KeyPurpose
import com.danyastry.keystore.core.keystore.KeystoreEntries
import com.google.common.truth.Truth.assertThat
import java.security.spec.ECGenParameterSpec
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidKeyInspectorTest {

    private val entries = KeystoreEntries()
    private val inspector = AndroidKeyInspector(entries)

    @After
    fun tearDown() {
        listOf(AndroidKeyInspector.SAMPLE_ALIAS, EC_ALIAS).forEach(entries::delete)
    }

    @Test
    fun describesTheSampleAesKey() {
        inspector.createSampleKey()

        val details = inspector.keys().single { it.alias == AndroidKeyInspector.SAMPLE_ALIAS }

        assertThat(details.algorithm).isEqualTo("AES")
        assertThat(details.sizeBits).isEqualTo(256)
        assertThat(details.kind).isEqualTo(KeyKind.SECRET)
        assertThat(details.purposes).containsExactly(KeyPurpose.ENCRYPT, KeyPurpose.DECRYPT)
        assertThat(details.origin).isEqualTo(KeyOrigin.GENERATED)
        assertThat(details.requiresUserAuthentication).isFalse()
        assertThat(details.location).isNotEqualTo(KeyLocation.UNKNOWN)
    }

    @Test
    fun describesAnEcSigningKeyMadeElsewhere() {
        AndroidKeyStoreKeyPairs(KeyProperties.KEY_ALGORITHM_EC, { alias ->
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY)
                .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                .setDigests(KeyProperties.DIGEST_SHA256)
                .build()
        }, entries).create(EC_ALIAS)

        val details = inspector.keys().single { it.alias == EC_ALIAS }

        assertThat(details.algorithm).isEqualTo("EC")
        assertThat(details.sizeBits).isEqualTo(256)
        assertThat(details.kind).isEqualTo(KeyKind.PRIVATE)
        assertThat(details.purposes).containsExactly(KeyPurpose.SIGN, KeyPurpose.VERIFY)
    }

    @Test
    fun listsNothingForMissingKeys() {
        assertThat(inspector.keys().map { it.alias }).doesNotContain(AndroidKeyInspector.SAMPLE_ALIAS)
    }

    private companion object {
        const val EC_ALIAS = "key-info-ec-test"
    }
}
