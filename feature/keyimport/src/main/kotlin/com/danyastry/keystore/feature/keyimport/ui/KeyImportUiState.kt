package com.danyastry.keystore.feature.keyimport.ui

import com.danyastry.keystore.core.keystore.KeyDetails
import com.danyastry.keystore.feature.keyimport.KeyHex

enum class KeyImportError { IMPORT_FAILED, NO_IMPORTED_KEY, NO_ORIGINAL, INVALID_MESSAGE, WRONG_KEY }

enum class Reader { KEYSTORE, ORIGINAL }

data class KeyImportUiState(
    val originalHex: String = "",
    val imported: KeyDetails? = null,
    val isImportedMaterialHidden: Boolean = false,
    val input: String = "",
    val sealed: String = "",
    val decrypted: String = "",
    val decryptedBy: Reader? = null,
    val error: KeyImportError? = null,
) {
    val isOriginalValid: Boolean
        get() = KeyHex.parseOrNull(originalHex) != null

    val isOriginalInvalid: Boolean
        get() = originalHex.isNotBlank() && !isOriginalValid

    val canImport: Boolean
        get() = isOriginalValid && imported == null

    val canEncrypt: Boolean
        get() = imported != null && input.isNotEmpty()

    val canDecryptWithKeystore: Boolean
        get() = imported != null && sealed.isNotEmpty()
}
