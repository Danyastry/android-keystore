package com.danyastry.keystore.feature.ecdh.ui

enum class EcdhError { KEY_CREATION_FAILED, NO_DEVICE_KEY, NOT_AGREED, INVALID_MESSAGE, PEER_CANNOT_DECRYPT }

data class EcdhUiState(
    val hasDeviceKey: Boolean = false,
    val devicePublicKey: String = "",
    val peerPublicKey: String = "",
    val deviceSecret: String = "",
    val peerSecret: String = "",
    val sessionKey: String = "",
    val input: String = "",
    val sealed: String = "",
    val peerDecrypted: String = "",
    val error: EcdhError? = null,
) {
    val isAgreed: Boolean
        get() = deviceSecret.isNotEmpty() && peerSecret.isNotEmpty()

    val secretsMatch: Boolean
        get() = isAgreed && deviceSecret == peerSecret

    val canAgree: Boolean
        get() = hasDeviceKey && peerPublicKey.isNotEmpty()

    val canEncrypt: Boolean
        get() = isAgreed && input.isNotEmpty()

    val canDecrypt: Boolean
        get() = isAgreed && sealed.isNotEmpty()
}
