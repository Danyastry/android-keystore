package com.danyastry.keystore.feature.biometric

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.danyastry.keystore.core.ui.FeatureCategory
import com.danyastry.keystore.core.ui.FeatureEntry
import com.danyastry.keystore.core.ui.FeatureInfo
import com.danyastry.keystore.feature.biometric.ui.BiometricRoute

object BiometricFeature : FeatureEntry {

    override val info = FeatureInfo(
        id = "biometric",
        name = "Biometric key",
        category = FeatureCategory.USER_AUTHENTICATION,
        summary = "AES key that works only after a fingerprint",
    )

    @Composable
    override fun Content(modifier: Modifier) {
        BiometricRoute(modifier = modifier)
    }
}
