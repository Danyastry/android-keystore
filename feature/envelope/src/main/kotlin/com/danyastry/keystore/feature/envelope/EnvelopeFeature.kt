package com.danyastry.keystore.feature.envelope

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.danyastry.keystore.core.ui.FeatureCategory
import com.danyastry.keystore.core.ui.FeatureEntry
import com.danyastry.keystore.core.ui.FeatureInfo
import com.danyastry.keystore.feature.envelope.ui.EnvelopeRoute

object EnvelopeFeature : FeatureEntry {

    override val info = FeatureInfo(
        id = "envelope",
        name = "Envelope encryption",
        category = FeatureCategory.ENCRYPTION,
        summary = "RSA wraps an AES key so data of any size fits",
    )

    @Composable
    override fun Content(modifier: Modifier) {
        EnvelopeRoute(modifier = modifier)
    }
}
