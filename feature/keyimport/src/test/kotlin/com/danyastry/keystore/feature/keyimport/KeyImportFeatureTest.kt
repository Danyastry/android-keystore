package com.danyastry.keystore.feature.keyimport

import com.danyastry.keystore.core.ui.FeatureCategory
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class KeyImportFeatureTest {

    @Test
    fun `is listed under key management and works on every supported version`() {
        assertThat(KeyImportFeature.info.id).isEqualTo("key-import")
        assertThat(KeyImportFeature.info.category).isEqualTo(FeatureCategory.KEY_MANAGEMENT)
        assertThat(KeyImportFeature.info.minSdk).isEqualTo(26)
    }
}
