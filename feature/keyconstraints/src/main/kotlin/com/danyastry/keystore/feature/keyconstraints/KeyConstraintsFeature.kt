package com.danyastry.keystore.feature.keyconstraints

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.danyastry.keystore.core.ui.FeatureCategory
import com.danyastry.keystore.core.ui.FeatureEntry
import com.danyastry.keystore.core.ui.FeatureInfo
import com.danyastry.keystore.feature.keyconstraints.ui.KeyConstraintsRoute

object KeyConstraintsFeature : FeatureEntry {

    override val info = FeatureInfo(
        id = "key-constraints",
        name = "Key limits",
        category = FeatureCategory.KEY_MANAGEMENT,
        summary = "Keys that expire or wear out after a few uses",
    )

    @Composable
    override fun Content(modifier: Modifier) {
        KeyConstraintsRoute(modifier = modifier)
    }
}
