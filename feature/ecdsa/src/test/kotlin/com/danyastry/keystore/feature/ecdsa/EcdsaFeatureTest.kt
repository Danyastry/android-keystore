package com.danyastry.keystore.feature.ecdsa

import com.danyastry.keystore.core.ui.FeatureCategory
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EcdsaFeatureTest {

    @Test
    fun `is listed under signatures`() {
        assertThat(EcdsaFeature.info.id).isEqualTo("ecdsa")
        assertThat(EcdsaFeature.info.category).isEqualTo(FeatureCategory.SIGNING)
        assertThat(EcdsaFeature.info.minSdk).isEqualTo(26)
    }
}
