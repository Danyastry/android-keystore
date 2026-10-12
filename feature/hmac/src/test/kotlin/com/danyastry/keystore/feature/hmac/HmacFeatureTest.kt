package com.danyastry.keystore.feature.hmac

import com.danyastry.keystore.core.ui.FeatureCategory
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HmacFeatureTest {

    @Test
    fun `is listed under signatures`() {
        assertThat(HmacFeature.info.id).isEqualTo("hmac")
        assertThat(HmacFeature.info.category).isEqualTo(FeatureCategory.SIGNING)
        assertThat(HmacFeature.info.minSdk).isEqualTo(26)
    }
}
