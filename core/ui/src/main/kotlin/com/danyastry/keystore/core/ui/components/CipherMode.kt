package com.danyastry.keystore.core.ui.components

import androidx.annotation.StringRes
import com.danyastry.keystore.core.ui.R

enum class CipherMode {
    ENCRYPT,
    DECRYPT;

    fun toggled(): CipherMode = if (this == ENCRYPT) DECRYPT else ENCRYPT
}

@get:StringRes
val CipherMode.labelRes: Int
    get() = when (this) {
        CipherMode.ENCRYPT -> R.string.mode_encrypt
        CipherMode.DECRYPT -> R.string.mode_decrypt
    }

@get:StringRes
val CipherMode.inputLabelRes: Int
    get() = when (this) {
        CipherMode.ENCRYPT -> R.string.label_plaintext
        CipherMode.DECRYPT -> R.string.label_ciphertext
    }

@get:StringRes
val CipherMode.outputLabelRes: Int
    get() = when (this) {
        CipherMode.ENCRYPT -> R.string.label_ciphertext
        CipherMode.DECRYPT -> R.string.label_plaintext
    }
