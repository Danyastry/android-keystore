package com.danyastry.keystore.feature.attestation

import com.danyastry.keystore.core.ui.FeatureCategory
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AttestationFeatureTest {

    @Test
    fun `is listed under hardware security`() {
        assertThat(AttestationFeature.info.id).isEqualTo("key-attestation")
        assertThat(AttestationFeature.info.category).isEqualTo(FeatureCategory.HARDWARE)
        assertThat(AttestationFeature.info.minSdk).isEqualTo(26)
    }
}
