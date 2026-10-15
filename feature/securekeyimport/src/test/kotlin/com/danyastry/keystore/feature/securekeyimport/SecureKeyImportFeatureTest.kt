package com.danyastry.keystore.feature.securekeyimport

import com.danyastry.keystore.core.ui.FeatureCategory
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SecureKeyImportFeatureTest {

    @Test
    fun `needs android 9 and is listed under key management`() {
        assertThat(SecureKeyImportFeature.info.id).isEqualTo("secure-key-import")
        assertThat(SecureKeyImportFeature.info.category).isEqualTo(FeatureCategory.KEY_MANAGEMENT)
        assertThat(SecureKeyImportFeature.info.minSdk).isEqualTo(28)
        assertThat(SecureKeyImportFeature.info.isSupportedOn(27)).isFalse()
    }
}
