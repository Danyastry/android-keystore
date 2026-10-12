package com.danyastry.keystore.feature.rsaoaep.ui

import com.danyastry.keystore.core.ui.components.CipherMode

enum class RsaOaepError { NO_KEY, TOO_LONG, INVALID_MESSAGE, DECRYPTION_FAILED }

data class RsaOaepUiState(
    val hasKey: Boolean = false,
    val publicKeyBase64: String = "",
    val isPrivateKeyHidden: Boolean = false,
    val mode: CipherMode = CipherMode.ENCRYPT,
    val input: String = "",
    val inputBytes: Int = 0,
    val output: String = "",
    val error: RsaOaepError? = null,
)
