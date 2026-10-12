package com.danyastry.keystore.feature.hmac.ui

enum class TagCheck { VALID, INVALID, NOT_HEX }

data class HmacUiState(
    val hasKey: Boolean = false,
    val isKeyMaterialHidden: Boolean = false,
    val message: String = DEFAULT_MESSAGE,
    val tag: String = "",
    val check: TagCheck? = null,
) {
    companion object {
        const val DEFAULT_MESSAGE = "Pay Alice 100 dollars"
    }
}
