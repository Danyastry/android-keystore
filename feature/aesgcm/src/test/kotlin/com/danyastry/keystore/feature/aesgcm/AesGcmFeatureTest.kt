package com.danyastry.keystore.feature.aesgcm

import com.danyastry.keystore.core.ui.FeatureCategory
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AesGcmFeatureTest {

    @Test
    fun `is listed under encryption and works on every supported version`() {
        assertThat(AesGcmFeature.info.id).isEqualTo("aes-gcm")
        assertThat(AesGcmFeature.info.category).isEqualTo(FeatureCategory.ENCRYPTION)
        assertThat(AesGcmFeature.info.minSdk).isEqualTo(26)
    }
}
