package com.danyastry.keystore.feature.authtimeout

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.danyastry.keystore.core.ui.FeatureCategory
import com.danyastry.keystore.core.ui.FeatureEntry
import com.danyastry.keystore.core.ui.FeatureInfo
import com.danyastry.keystore.feature.authtimeout.ui.AuthTimeoutRoute

object AuthTimeoutFeature : FeatureEntry {

    override val info = FeatureInfo(
        id = "auth-timeout",
        name = "Unlock window",
        category = FeatureCategory.USER_AUTHENTICATION,
        summary = "Fingerprint or PIN opens the key for 15 seconds",
        minSdk = Build.VERSION_CODES.R,
    )

    @Composable
    override fun Content(modifier: Modifier) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            AuthTimeoutRoute(modifier = modifier)
        }
    }
}
