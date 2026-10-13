package com.danyastry.keystore.feature.attestation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.danyastry.keystore.core.ui.FeatureCategory
import com.danyastry.keystore.core.ui.FeatureEntry
import com.danyastry.keystore.core.ui.FeatureInfo
import com.danyastry.keystore.feature.attestation.ui.AttestationRoute

object AttestationFeature : FeatureEntry {

    override val info = FeatureInfo(
        id = "key-attestation",
        name = "Key attestation",
        category = FeatureCategory.HARDWARE,
        summary = "Certificates that prove how and where a key was made",
    )

    @Composable
    override fun Content(modifier: Modifier) {
        AttestationRoute(modifier = modifier)
    }
}
