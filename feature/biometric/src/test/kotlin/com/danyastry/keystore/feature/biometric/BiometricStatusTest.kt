package com.danyastry.keystore.feature.biometric

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BiometricStatusTest {

    @Test
    fun `maps biometric manager codes`() {
        assertThat(AndroidBiometricStatus.availabilityOf(BiometricManager.BIOMETRIC_SUCCESS)).isEqualTo(BiometricAvailability.AVAILABLE)
        assertThat(AndroidBiometricStatus.availabilityOf(BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED)).isEqualTo(BiometricAvailability.NONE_ENROLLED)
        assertThat(AndroidBiometricStatus.availabilityOf(BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE)).isEqualTo(BiometricAvailability.NO_HARDWARE)
        assertThat(AndroidBiometricStatus.availabilityOf(BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE)).isEqualTo(BiometricAvailability.UNAVAILABLE)
        assertThat(AndroidBiometricStatus.availabilityOf(BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED)).isEqualTo(BiometricAvailability.UNAVAILABLE)
    }

    @Test
    fun `user and system cancellations are not failures`() {
        assertThat(isCancellation(BiometricPrompt.ERROR_USER_CANCELED)).isTrue()
        assertThat(isCancellation(BiometricPrompt.ERROR_NEGATIVE_BUTTON)).isTrue()
        assertThat(isCancellation(BiometricPrompt.ERROR_CANCELED)).isTrue()
        assertThat(isCancellation(BiometricPrompt.ERROR_LOCKOUT)).isFalse()
        assertThat(isCancellation(BiometricPrompt.ERROR_TIMEOUT)).isFalse()
    }
}
