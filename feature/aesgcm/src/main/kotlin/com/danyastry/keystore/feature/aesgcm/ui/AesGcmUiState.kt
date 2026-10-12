package com.danyastry.keystore.feature.aesgcm.ui

import com.danyastry.keystore.core.ui.components.CipherMode

enum class AesGcmError { NO_KEY, INVALID_MESSAGE, TAMPERED }

data class AesGcmUiState(
    val hasKey: Boolean = false,
    val isKeyMaterialHidden: Boolean = false,
    val mode: CipherMode = CipherMode.ENCRYPT,
    val input: String = "",
    val output: String = "",
    val ivHex: String = "",
    val error: AesGcmError? = null,
)
