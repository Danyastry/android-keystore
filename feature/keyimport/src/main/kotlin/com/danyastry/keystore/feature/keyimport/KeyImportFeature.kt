package com.danyastry.keystore.feature.keyimport

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.danyastry.keystore.core.ui.FeatureCategory
import com.danyastry.keystore.core.ui.FeatureEntry
import com.danyastry.keystore.core.ui.FeatureInfo
import com.danyastry.keystore.feature.keyimport.ui.KeyImportRoute

object KeyImportFeature : FeatureEntry {

    override val info = FeatureInfo(
        id = "key-import",
        name = "Key import",
        category = FeatureCategory.KEY_MANAGEMENT,
        summary = "Move an existing AES key into the Keystore",
    )

    @Composable
    override fun Content(modifier: Modifier) {
        KeyImportRoute(modifier = modifier)
    }
}
