package com.danyastry.keystore.feature.aesgcm

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.danyastry.keystore.core.ui.FeatureCategory
import com.danyastry.keystore.core.ui.FeatureEntry
import com.danyastry.keystore.core.ui.FeatureInfo
import com.danyastry.keystore.feature.aesgcm.ui.AesGcmRoute

object AesGcmFeature : FeatureEntry {

    override val info = FeatureInfo(
        id = "aes-gcm",
        name = "AES-GCM",
        category = FeatureCategory.ENCRYPTION,
        summary = "AES-256 key that never leaves the Keystore",
    )

    @Composable
    override fun Content(modifier: Modifier) {
        AesGcmRoute(modifier = modifier)
    }
}
