package com.danyastry.keystore.feature.rsaoaep

import com.danyastry.keystore.core.ui.FeatureCategory
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RsaOaepFeatureTest {

    @Test
    fun `is listed under encryption`() {
        assertThat(RsaOaepFeature.info.id).isEqualTo("rsa-oaep")
        assertThat(RsaOaepFeature.info.category).isEqualTo(FeatureCategory.ENCRYPTION)
        assertThat(RsaOaepFeature.info.minSdk).isEqualTo(26)
    }
}
