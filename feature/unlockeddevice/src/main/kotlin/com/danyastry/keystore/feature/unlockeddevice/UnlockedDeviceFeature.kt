package com.danyastry.keystore.feature.unlockeddevice

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.danyastry.keystore.core.ui.FeatureCategory
import com.danyastry.keystore.core.ui.FeatureEntry
import com.danyastry.keystore.core.ui.FeatureInfo
import com.danyastry.keystore.feature.unlockeddevice.ui.UnlockedDeviceRoute

object UnlockedDeviceFeature : FeatureEntry {

    override val info = FeatureInfo(
        id = "unlocked-device",
        name = "Unlocked device only",
        category = FeatureCategory.USER_AUTHENTICATION,
        summary = "Key that stops working when the screen locks",
        minSdk = Build.VERSION_CODES.P,
    )

    @Composable
    override fun Content(modifier: Modifier) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            UnlockedDeviceRoute(modifier = modifier)
        }
    }
}
