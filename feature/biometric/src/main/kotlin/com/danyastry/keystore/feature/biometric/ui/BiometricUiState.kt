package com.danyastry.keystore.feature.biometric.ui

import com.danyastry.keystore.feature.biometric.BiometricAvailability
import javax.crypto.Cipher

enum class BiometricOperation { ENCRYPT, DECRYPT }

enum class BiometricError { NO_KEY, KEY_CREATION_FAILED, KEY_INVALIDATED, KEY_REFUSED, CANCELED, AUTH_FAILED }

class AuthRequest(val operation: BiometricOperation, val cipher: Cipher, val payload: ByteArray)

data class BiometricUiState(
    val availability: BiometricAvailability? = null,
    val hasKey: Boolean = false,
    val input: String = "",
    val sealed: String = "",
    val decrypted: String = "",
    val isAwaitingAuth: Boolean = false,
    val error: BiometricError? = null,
    val authMessage: String = "",
) {
    val canCreateKey: Boolean
        get() = availability == BiometricAvailability.AVAILABLE && !hasKey

    val canEncrypt: Boolean
        get() = hasKey && input.isNotEmpty() && !isAwaitingAuth

    val canDecrypt: Boolean
        get() = hasKey && sealed.isNotEmpty() && !isAwaitingAuth
}
