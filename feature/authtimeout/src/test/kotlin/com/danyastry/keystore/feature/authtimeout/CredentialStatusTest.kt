package com.danyastry.keystore.feature.authtimeout

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CredentialStatusTest {

    @Test
    fun `accepts a strong biometric or the screen lock`() {
        assertThat(AndroidCredentialStatus.AUTHENTICATORS).isEqualTo(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
    }

    @Test
    fun `maps biometric manager codes`() {
        assertThat(AndroidCredentialStatus.availabilityOf(BiometricManager.BIOMETRIC_SUCCESS)).isEqualTo(CredentialAvailability.AVAILABLE)
        assertThat(AndroidCredentialStatus.availabilityOf(BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED)).isEqualTo(CredentialAvailability.NO_LOCK_SCREEN)
        assertThat(AndroidCredentialStatus.availabilityOf(BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE)).isEqualTo(CredentialAvailability.UNAVAILABLE)
    }

    @Test
    fun `user and system cancellations are not failures`() {
        assertThat(isCancellation(BiometricPrompt.ERROR_USER_CANCELED)).isTrue()
        assertThat(isCancellation(BiometricPrompt.ERROR_CANCELED)).isTrue()
        assertThat(isCancellation(BiometricPrompt.ERROR_LOCKOUT)).isFalse()
    }
}
