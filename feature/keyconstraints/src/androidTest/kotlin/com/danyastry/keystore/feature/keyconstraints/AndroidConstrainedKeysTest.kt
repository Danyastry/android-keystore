package com.danyastry.keystore.feature.keyconstraints

import android.os.Build
import android.security.keystore.KeyInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import com.danyastry.keystore.core.keystore.KeystoreEntries
import com.google.common.truth.Truth.assertThat
import javax.crypto.SecretKeyFactory
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidConstrainedKeysTest {

    private val keys = AndroidConstrainedKeys()
    private val entries = KeystoreEntries()

    @After
    fun tearDown() {
        ConstrainedKey.entries.forEach(keys::delete)
    }

    @Test
    fun windowKeyCarriesItsValidityDates() {
        val now = System.currentTimeMillis()
        keys.createWindowKey(now + 60_000, now + 120_000)

        val info = keyInfo(ConstrainedKey.WINDOW)

        assertThat(info.keyValidityStart?.time).isEqualTo(now + 60_000)
        assertThat(info.keyValidityForOriginationEnd?.time).isEqualTo(now + 120_000)
        assertThat(info.keyValidityForConsumptionEnd?.time).isEqualTo(now + 120_000)
    }

    @Test
    fun windowKeyIsRefusedBeforeItsStart() {
        val now = System.currentTimeMillis()
        keys.createWindowKey(now + 60_000, now + 120_000)

        assertThat(keys.use(ConstrainedKey.WINDOW)).isEqualTo(UseResult.Refused("KeyNotYetValidException"))
    }

    @Test
    fun windowKeyWorksInsideItsWindow() {
        val now = System.currentTimeMillis()
        keys.createWindowKey(now - 1_000, now + 60_000)

        assertThat(keys.use(ConstrainedKey.WINDOW)).isEqualTo(UseResult.Allowed)
    }

    @Test
    fun windowKeyIsRefusedAfterItsEnd() {
        val now = System.currentTimeMillis()
        keys.createWindowKey(now - 2_000, now + 1_000)

        Thread.sleep(1_500)

        assertThat(keys.use(ConstrainedKey.WINDOW)).isEqualTo(UseResult.Refused("KeyExpiredException"))
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    fun keystoreDeletesTheLimitedKeyAfterTheLastUse() {
        keys.createLimitedKey(3)

        val results = List(4) { keys.use(ConstrainedKey.LIMITED) }

        assertThat(results.take(3)).containsExactly(UseResult.Allowed, UseResult.Allowed, UseResult.Allowed)
        assertThat(results[3]).isEqualTo(UseResult.Missing)
        assertThat(keys.exists(ConstrainedKey.LIMITED)).isFalse()
    }

    @Test
    fun missingKeyIsReported() {
        assertThat(keys.use(ConstrainedKey.WINDOW)).isEqualTo(UseResult.Missing)
        assertThat(keys.exists(ConstrainedKey.WINDOW)).isFalse()
    }

    private fun keyInfo(key: ConstrainedKey): KeyInfo {
        val secret = entries.secretKey(AndroidConstrainedKeys.aliasOf(key))!!
        return SecretKeyFactory.getInstance(secret.algorithm, KeystoreEntries.ANDROID_KEY_STORE).getKeySpec(secret, KeyInfo::class.java) as KeyInfo
    }
}
