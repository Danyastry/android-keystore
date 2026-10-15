package com.danyastry.keystore.feature.securekeyimport.ui

import com.danyastry.keystore.core.keystore.KeyDetails

enum class SecureImportError { WRAPPING_KEY_FAILED, IMPORT_REFUSED, CANNOT_DECRYPT }

data class WrappedSummary(
    val transportKeyBytes: Int,
    val ivBytes: Int,
    val descriptionBytes: Int,
    val encryptedKeyBytes: Int,
    val tagBytes: Int,
    val totalBytes: Int,
    val isTampered: Boolean = false,
)

data class SecureKeyImportUiState(
    val wrappingPublicKey: String = "",
    val serverKey: String = "",
    val wrapped: WrappedSummary? = null,
    val imported: KeyDetails? = null,
    val isImportedMaterialHidden: Boolean = false,
    val input: String = "",
    val serverMessage: String = "",
    val deviceRead: String = "",
    val error: SecureImportError? = null,
) {
    val hasWrappingKey: Boolean
        get() = wrappingPublicKey.isNotEmpty()

    val canImport: Boolean
        get() = hasWrappingKey && wrapped != null

    val canTamper: Boolean
        get() = wrapped != null && !wrapped.isTampered

    val canSend: Boolean
        get() = imported != null && serverKey.isNotEmpty() && input.isNotEmpty()

    val canDecrypt: Boolean
        get() = imported != null && serverMessage.isNotEmpty()
}
