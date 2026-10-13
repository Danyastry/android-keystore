package com.danyastry.keystore.feature.keyinfo

import com.danyastry.keystore.core.ui.FeatureCategory
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class KeyInfoFeatureTest {

    @Test
    fun `is listed under hardware security`() {
        assertThat(KeyInfoFeature.info.id).isEqualTo("key-info")
        assertThat(KeyInfoFeature.info.category).isEqualTo(FeatureCategory.HARDWARE)
        assertThat(KeyInfoFeature.info.minSdk).isEqualTo(26)
    }
}
