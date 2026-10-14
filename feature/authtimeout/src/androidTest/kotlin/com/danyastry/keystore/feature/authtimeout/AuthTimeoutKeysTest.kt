package com.danyastry.keystore.feature.authtimeout

import android.app.KeyguardManager
import android.os.Build
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.danyastry.keystore.core.keystore.KeystoreEntries
import com.google.common.truth.Truth.assertThat
import java.security.GeneralSecurityException
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
class AuthTimeoutKeysTest {

    private val keys = AuthTimeoutKeys.store()
    private val crypto = AesGcmTimedCrypto()
    private val hasScreenLock = InstrumentationRegistry.getInstrumentation().targetContext
        .getSystemService(KeyguardManager::class.java).isDeviceSecure

    @After
    fun tearDown() {
        keys.delete(ALIAS)
    }

    @Test
    fun keyCannotBeCreatedWithoutAScreenLock() {
        assumeFalse(hasScreenLock)

        assertThrows(GeneralSecurityException::class.java) { keys.create(ALIAS) }
    }

    @Test
    fun keyOpensForAWindowWithBiometricOrScreenLock() {
        assumeTrue(hasScreenLock)

        val info = keyInfo(keys.create(ALIAS))

        assertThat(info.isUserAuthenticationRequired).isTrue()
        assertThat(info.userAuthenticationValidityDurationSeconds).isEqualTo(AuthTimeoutKeys.TIMEOUT_SECONDS)
        assertThat(info.userAuthenticationType).isEqualTo(KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL)
    }

    @Test
    fun keyMaterialCannotBeRead() {
        assumeTrue(hasScreenLock)

        assertThat(keys.create(ALIAS).encoded).isNull()
    }

    @Test
    fun keystoreRefusesWithoutARecentUnlock() {
        assumeTrue(hasScreenLock)
        val key = keys.create(ALIAS)

        assertThrows(KeyLockedException::class.java) { crypto.encrypt(key, "Attack at dawn".encodeToByteArray()) }
    }

    private fun keyInfo(key: SecretKey): KeyInfo =
        SecretKeyFactory.getInstance(key.algorithm, KeystoreEntries.ANDROID_KEY_STORE).getKeySpec(key, KeyInfo::class.java) as KeyInfo

    private companion object {
        const val ALIAS = "auth-timeout-test"
    }
}
