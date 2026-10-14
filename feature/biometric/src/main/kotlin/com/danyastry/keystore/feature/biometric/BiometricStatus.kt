package com.danyastry.keystore.feature.biometric

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt

enum class BiometricAvailability { AVAILABLE, NONE_ENROLLED, NO_HARDWARE, UNAVAILABLE }

fun interface BiometricStatus {
    fun check(): BiometricAvailability
}

class AndroidBiometricStatus(private val context: Context) : BiometricStatus {

    override fun check(): BiometricAvailability = availabilityOf(BiometricManager.from(context).canAuthenticate(BIOMETRIC_STRONG))

    companion object {
        fun availabilityOf(code: Int): BiometricAvailability = when (code) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NONE_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricAvailability.NO_HARDWARE
            else -> BiometricAvailability.UNAVAILABLE
        }
    }
}

fun isCancellation(errorCode: Int): Boolean = errorCode in CANCELLATION_CODES

private val CANCELLATION_CODES = setOf(
    BiometricPrompt.ERROR_USER_CANCELED,
    BiometricPrompt.ERROR_NEGATIVE_BUTTON,
    BiometricPrompt.ERROR_CANCELED,
)
