package com.danyastry.keystore.feature.keyinfo

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.danyastry.keystore.core.ui.FeatureCategory
import com.danyastry.keystore.core.ui.FeatureEntry
import com.danyastry.keystore.core.ui.FeatureInfo
import com.danyastry.keystore.feature.keyinfo.ui.KeyInfoRoute

object KeyInfoFeature : FeatureEntry {

    override val info = FeatureInfo(
        id = "key-info",
        name = "Key info",
        category = FeatureCategory.HARDWARE,
        summary = "Where each key really lives: software, TEE or StrongBox",
    )

    @Composable
    override fun Content(modifier: Modifier) {
        KeyInfoRoute(modifier = modifier)
    }
}
