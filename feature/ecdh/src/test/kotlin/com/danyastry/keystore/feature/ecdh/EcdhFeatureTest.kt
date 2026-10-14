package com.danyastry.keystore.feature.ecdh

import com.danyastry.keystore.core.ui.FeatureCategory
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EcdhFeatureTest {

    @Test
    fun `needs android 12 and is listed under encryption`() {
        assertThat(EcdhFeature.info.id).isEqualTo("ecdh")
        assertThat(EcdhFeature.info.category).isEqualTo(FeatureCategory.ENCRYPTION)
        assertThat(EcdhFeature.info.minSdk).isEqualTo(31)
        assertThat(EcdhFeature.info.isSupportedOn(30)).isFalse()
    }
}
