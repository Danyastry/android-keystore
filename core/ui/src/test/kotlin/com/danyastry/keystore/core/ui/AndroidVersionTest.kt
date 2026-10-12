package com.danyastry.keystore.core.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AndroidVersionTest {

    @Test
    fun `names the versions that matter for the keystore`() {
        assertThat(AndroidVersion.nameOf(26)).isEqualTo("8.0")
        assertThat(AndroidVersion.nameOf(28)).isEqualTo("9")
        assertThat(AndroidVersion.nameOf(31)).isEqualTo("12")
        assertThat(AndroidVersion.nameOf(33)).isEqualTo("13")
    }

    @Test
    fun `falls back to the api level`() {
        assertThat(AndroidVersion.nameOf(99)).isEqualTo("API 99")
    }
}
