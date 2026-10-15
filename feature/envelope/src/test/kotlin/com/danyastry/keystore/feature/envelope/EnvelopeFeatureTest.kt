package com.danyastry.keystore.feature.envelope

import com.danyastry.keystore.core.ui.FeatureCategory
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EnvelopeFeatureTest {

    @Test
    fun `is listed under encryption and works on every supported version`() {
        assertThat(EnvelopeFeature.info.id).isEqualTo("envelope")
        assertThat(EnvelopeFeature.info.category).isEqualTo(FeatureCategory.ENCRYPTION)
        assertThat(EnvelopeFeature.info.minSdk).isEqualTo(26)
    }
}
