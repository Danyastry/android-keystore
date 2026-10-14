package com.danyastry.keystore.feature.biometric

import android.os.Build
import android.security.keystore.KeyInfo
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.test.ext.junit.runners.AndroidJUnit4
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
class BiometricKeysTest {

    private val keys = BiometricKeys.store()
    private val crypto = AesGcmBiometricCrypto()
    private val isEnrolled = BiometricManager.from(InstrumentationRegistry.getInstrumentation().targetContext)
        .canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS

    @After
    fun tearDown() {
        keys.delete(ALIAS)
    }

    @Test
    fun keyCannotBeCreatedWithoutAnEnrolledBiometric() {
        assumeFalse(isEnrolled)

        assertThrows(GeneralSecurityException::class.java) { keys.create(ALIAS) }
    }

    @Test
    fun keyRequiresABiometricForEveryUse() {
        assumeTrue(isEnrolled)

        val info = keyInfo(keys.create(ALIAS))

        assertThat(info.isUserAuthenticationRequired).isTrue()
        assertThat(info.isInvalidatedByBiometricEnrollment).isTrue()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            assertThat(info.userAuthenticationValidityDurationSeconds).isEqualTo(0)
            assertThat(info.userAuthenticationType).isEqualTo(android.security.keystore.KeyProperties.AUTH_BIOMETRIC_STRONG)
        }
    }

    @Test
    fun keyMaterialCannotBeRead() {
        assumeTrue(isEnrolled)

        assertThat(keys.create(ALIAS).encoded).isNull()
    }

    @Test
    fun cipherIsReadyBeforeTheFingerprint() {
        assumeTrue(isEnrolled)
        val key = keys.create(ALIAS)

        val cipher = crypto.encryptionCipher(key)

        assertThat(cipher.iv).hasLength(AesGcmBiometricCrypto.IV_SIZE)
    }

    @Test
    fun keystoreRefusesToEncryptWithoutAFingerprint() {
        assumeTrue(isEnrolled)
        val key = keys.create(ALIAS)

        assertThrows(GeneralSecurityException::class.java) {
            crypto.seal(crypto.encryptionCipher(key), "Attack at dawn".encodeToByteArray())
        }
    }

    private fun keyInfo(key: SecretKey): KeyInfo =
        SecretKeyFactory.getInstance(key.algorithm, KeystoreEntries.ANDROID_KEY_STORE).getKeySpec(key, KeyInfo::class.java) as KeyInfo

    private companion object {
        const val ALIAS = "biometric-test"
    }
}
