package com.danyastry.keystore.feature.ecdsa

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.danyastry.keystore.core.ui.FeatureCategory
import com.danyastry.keystore.core.ui.FeatureEntry
import com.danyastry.keystore.core.ui.FeatureInfo
import com.danyastry.keystore.feature.ecdsa.ui.EcdsaRoute

object EcdsaFeature : FeatureEntry {

    override val info = FeatureInfo(
        id = "ecdsa",
        name = "ECDSA signatures",
        category = FeatureCategory.SIGNING,
        summary = "Sign on this device, verify anywhere",
    )

    @Composable
    override fun Content(modifier: Modifier) {
        EcdsaRoute(modifier = modifier)
    }
}
