package com.danyastry.keystore.feature.authtimeout

import com.danyastry.keystore.core.ui.FeatureCategory
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AuthTimeoutFeatureTest {

    @Test
    fun `needs android 11 and is listed under user authentication`() {
        assertThat(AuthTimeoutFeature.info.id).isEqualTo("auth-timeout")
        assertThat(AuthTimeoutFeature.info.category).isEqualTo(FeatureCategory.USER_AUTHENTICATION)
        assertThat(AuthTimeoutFeature.info.minSdk).isEqualTo(30)
        assertThat(AuthTimeoutFeature.info.isSupportedOn(29)).isFalse()
    }
}
