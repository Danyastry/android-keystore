package com.danyastry.keystore.feature.hmac

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.danyastry.keystore.core.ui.FeatureCategory
import com.danyastry.keystore.core.ui.FeatureEntry
import com.danyastry.keystore.core.ui.FeatureInfo
import com.danyastry.keystore.feature.hmac.ui.HmacRoute

object HmacFeature : FeatureEntry {

    override val info = FeatureInfo(
        id = "hmac",
        name = "HMAC-SHA256",
        category = FeatureCategory.SIGNING,
        summary = "Integrity check with one shared secret key",
    )

    @Composable
    override fun Content(modifier: Modifier) {
        HmacRoute(modifier = modifier)
    }
}
