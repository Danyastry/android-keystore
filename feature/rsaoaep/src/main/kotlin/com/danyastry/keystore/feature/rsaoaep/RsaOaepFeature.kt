package com.danyastry.keystore.feature.rsaoaep

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.danyastry.keystore.core.ui.FeatureCategory
import com.danyastry.keystore.core.ui.FeatureEntry
import com.danyastry.keystore.core.ui.FeatureInfo
import com.danyastry.keystore.feature.rsaoaep.ui.RsaOaepRoute

object RsaOaepFeature : FeatureEntry {

    override val info = FeatureInfo(
        id = "rsa-oaep",
        name = "RSA-OAEP",
        category = FeatureCategory.ENCRYPTION,
        summary = "Anyone can encrypt, only this device can decrypt",
    )

    @Composable
    override fun Content(modifier: Modifier) {
        RsaOaepRoute(modifier = modifier)
    }
}
