package com.danyastry.keystore.feature.authtimeout.ui

import com.danyastry.keystore.feature.authtimeout.CredentialAvailability

enum class TimedOperation { ENCRYPT, DECRYPT }

enum class AuthTimeoutError { NO_KEY, KEY_CREATION_FAILED, KEY_LOCKED, CANCELED, AUTH_FAILED }

data class Attempt(val operation: TimedOperation, val isAllowed: Boolean, val secondsSinceUnlock: Long?)

data class AuthTimeoutUiState(
    val availability: CredentialAvailability? = null,
    val hasKey: Boolean = false,
    val input: String = "",
    val sealed: String = "",
    val decrypted: String = "",
    val secondsLeft: Int = 0,
    val isAwaitingAuth: Boolean = false,
    val attempts: List<Attempt> = emptyList(),
    val error: AuthTimeoutError? = null,
    val authMessage: String = "",
) {
    val canCreateKey: Boolean
        get() = availability == CredentialAvailability.AVAILABLE && !hasKey

    val canUnlock: Boolean
        get() = hasKey && !isAwaitingAuth

    val canEncrypt: Boolean
        get() = hasKey && input.isNotEmpty()

    val canDecrypt: Boolean
        get() = hasKey && sealed.isNotEmpty()

    val isUnlocked: Boolean
        get() = secondsLeft > 0
}
