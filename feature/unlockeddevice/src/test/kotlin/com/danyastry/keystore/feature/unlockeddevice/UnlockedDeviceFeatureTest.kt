package com.danyastry.keystore.feature.unlockeddevice

import com.danyastry.keystore.core.ui.FeatureCategory
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class UnlockedDeviceFeatureTest {

    @Test
    fun `needs android 9 and is listed under user authentication`() {
        assertThat(UnlockedDeviceFeature.info.id).isEqualTo("unlocked-device")
        assertThat(UnlockedDeviceFeature.info.category).isEqualTo(FeatureCategory.USER_AUTHENTICATION)
        assertThat(UnlockedDeviceFeature.info.minSdk).isEqualTo(28)
        assertThat(UnlockedDeviceFeature.info.isSupportedOn(27)).isFalse()
    }
}
