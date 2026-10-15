package com.danyastry.keystore.feature.securekeyimport

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.danyastry.keystore.core.ui.FeatureCategory
import com.danyastry.keystore.core.ui.FeatureEntry
import com.danyastry.keystore.core.ui.FeatureInfo
import com.danyastry.keystore.feature.securekeyimport.ui.SecureKeyImportRoute

object SecureKeyImportFeature : FeatureEntry {

    override val info = FeatureInfo(
        id = "secure-key-import",
        name = "Secure key import",
        category = FeatureCategory.KEY_MANAGEMENT,
        summary = "A server sends a key that only this device's Keystore can unwrap",
        minSdk = Build.VERSION_CODES.P,
    )

    @Composable
    override fun Content(modifier: Modifier) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            SecureKeyImportRoute(modifier = modifier)
        }
    }
}
