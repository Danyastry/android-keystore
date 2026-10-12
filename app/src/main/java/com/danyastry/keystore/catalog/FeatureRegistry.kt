package com.danyastry.keystore.catalog

import com.danyastry.keystore.core.ui.FeatureEntry
import com.danyastry.keystore.feature.aesgcm.AesGcmFeature

object FeatureRegistry {
    val features: List<FeatureEntry> = listOf(
        AesGcmFeature,
    )
}
