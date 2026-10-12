package com.danyastry.keystore.core.ui.components

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CipherModeTest {

    @Test
    fun `toggled switches between encrypt and decrypt`() {
        assertThat(CipherMode.ENCRYPT.toggled()).isEqualTo(CipherMode.DECRYPT)
        assertThat(CipherMode.DECRYPT.toggled()).isEqualTo(CipherMode.ENCRYPT)
    }

    @Test
    fun `modes have distinct labels`() {
        assertThat(CipherMode.entries.map { it.labelRes }).containsNoDuplicates()
    }

    @Test
    fun `input and output labels swap between modes`() {
        assertThat(CipherMode.ENCRYPT.inputLabelRes).isEqualTo(CipherMode.DECRYPT.outputLabelRes)
        assertThat(CipherMode.ENCRYPT.outputLabelRes).isEqualTo(CipherMode.DECRYPT.inputLabelRes)
        assertThat(CipherMode.ENCRYPT.inputLabelRes).isNotEqualTo(CipherMode.ENCRYPT.outputLabelRes)
    }
}
