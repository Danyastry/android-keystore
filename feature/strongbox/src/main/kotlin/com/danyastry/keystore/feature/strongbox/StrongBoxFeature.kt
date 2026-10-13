package com.danyastry.keystore.feature.strongbox

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.danyastry.keystore.core.ui.FeatureCategory
import com.danyastry.keystore.core.ui.FeatureEntry
import com.danyastry.keystore.core.ui.FeatureInfo
import com.danyastry.keystore.feature.strongbox.ui.StrongBoxRoute

object StrongBoxFeature : FeatureEntry {

    override val info = FeatureInfo(
        id = "strongbox",
        name = "StrongBox",
        category = FeatureCategory.HARDWARE,
        summary = "Keys inside a separate security chip",
        minSdk = Build.VERSION_CODES.P,
    )

    @Composable
    override fun Content(modifier: Modifier) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            StrongBoxRoute(modifier = modifier)
        }
    }
}
