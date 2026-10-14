package com.danyastry.keystore.feature.ecdh

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.danyastry.keystore.core.ui.FeatureCategory
import com.danyastry.keystore.core.ui.FeatureEntry
import com.danyastry.keystore.core.ui.FeatureInfo
import com.danyastry.keystore.feature.ecdh.ui.EcdhRoute

object EcdhFeature : FeatureEntry {

    override val info = FeatureInfo(
        id = "ecdh",
        name = "ECDH key agreement",
        category = FeatureCategory.ENCRYPTION,
        summary = "Two sides agree on one AES key without sending it",
        minSdk = Build.VERSION_CODES.S,
    )

    @Composable
    override fun Content(modifier: Modifier) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            EcdhRoute(modifier = modifier)
        }
    }
}
