package com.danyastry.keystore.feature.keyconstraints

import com.danyastry.keystore.core.ui.FeatureCategory
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class KeyConstraintsFeatureTest {

    @Test
    fun `is listed under key management and opens on every supported version`() {
        assertThat(KeyConstraintsFeature.info.id).isEqualTo("key-constraints")
        assertThat(KeyConstraintsFeature.info.category).isEqualTo(FeatureCategory.KEY_MANAGEMENT)
        assertThat(KeyConstraintsFeature.info.minSdk).isEqualTo(26)
    }

    @Test
    fun `aliases are distinct per key`() {
        assertThat(ConstrainedKey.entries.map(AndroidConstrainedKeys::aliasOf).toSet()).hasSize(ConstrainedKey.entries.size)
    }
}
