package com.danyastry.keystore.feature.biometric

import com.danyastry.keystore.core.ui.FeatureCategory
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BiometricFeatureTest {

    @Test
    fun `is listed under user authentication and works on every supported version`() {
        assertThat(BiometricFeature.info.id).isEqualTo("biometric")
        assertThat(BiometricFeature.info.category).isEqualTo(FeatureCategory.USER_AUTHENTICATION)
        assertThat(BiometricFeature.info.minSdk).isEqualTo(26)
    }
}
