package com.danyastry.keystore.feature.ecdsa.ui

enum class Verification { VALID, INVALID, NOT_BASE64 }

data class EcdsaUiState(
    val hasKey: Boolean = false,
    val publicKeyBase64: String = "",
    val isPrivateKeyHidden: Boolean = false,
    val message: String = DEFAULT_MESSAGE,
    val signature: String = "",
    val signatureBytes: Int = 0,
    val verification: Verification? = null,
) {
    companion object {
        const val DEFAULT_MESSAGE = "Pay Alice 100 dollars"
    }
}
