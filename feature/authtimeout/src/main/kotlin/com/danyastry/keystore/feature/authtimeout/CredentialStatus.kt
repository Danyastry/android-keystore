package com.danyastry.keystore.feature.authtimeout

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt

enum class CredentialAvailability { AVAILABLE, NO_LOCK_SCREEN, UNAVAILABLE }

fun interface CredentialStatus {
    fun check(): CredentialAvailability
}

class AndroidCredentialStatus(private val context: Context) : CredentialStatus {

    override fun check(): CredentialAvailability = availabilityOf(BiometricManager.from(context).canAuthenticate(AUTHENTICATORS))

    companion object {
        const val AUTHENTICATORS = BIOMETRIC_STRONG or DEVICE_CREDENTIAL

        fun availabilityOf(code: Int): CredentialAvailability = when (code) {
            BiometricManager.BIOMETRIC_SUCCESS -> CredentialAvailability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> CredentialAvailability.NO_LOCK_SCREEN
            else -> CredentialAvailability.UNAVAILABLE
        }
    }
}

fun isCancellation(errorCode: Int): Boolean = errorCode in CANCELLATION_CODES

private val CANCELLATION_CODES = setOf(
    BiometricPrompt.ERROR_USER_CANCELED,
    BiometricPrompt.ERROR_NEGATIVE_BUTTON,
    BiometricPrompt.ERROR_CANCELED,
)
