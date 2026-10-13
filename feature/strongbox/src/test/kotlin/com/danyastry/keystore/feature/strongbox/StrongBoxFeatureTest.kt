package com.danyastry.keystore.feature.strongbox

import com.danyastry.keystore.core.ui.FeatureCategory
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StrongBoxFeatureTest {

    @Test
    fun `needs android 9 and is listed under hardware security`() {
        assertThat(StrongBoxFeature.info.id).isEqualTo("strongbox")
        assertThat(StrongBoxFeature.info.category).isEqualTo(FeatureCategory.HARDWARE)
        assertThat(StrongBoxFeature.info.minSdk).isEqualTo(28)
        assertThat(StrongBoxFeature.info.isSupportedOn(27)).isFalse()
    }
}
