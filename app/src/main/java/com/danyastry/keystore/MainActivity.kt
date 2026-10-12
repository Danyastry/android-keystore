package com.danyastry.keystore

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.danyastry.keystore.catalog.FeatureCatalog
import com.danyastry.keystore.catalog.FeatureRegistry
import com.danyastry.keystore.core.ui.theme.KeystoreTheme
import com.danyastry.keystore.navigation.KeystoreNavHost

class MainActivity : ComponentActivity() {

    private val catalog = FeatureCatalog(FeatureRegistry.features, deviceSdk = Build.VERSION.SDK_INT)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KeystoreTheme {
                KeystoreNavHost(catalog)
            }
        }
    }
}
