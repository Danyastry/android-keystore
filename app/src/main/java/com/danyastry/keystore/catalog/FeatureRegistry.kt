package com.danyastry.keystore.catalog

import com.danyastry.keystore.core.ui.FeatureEntry
import com.danyastry.keystore.feature.aesgcm.AesGcmFeature
import com.danyastry.keystore.feature.ecdsa.EcdsaFeature
import com.danyastry.keystore.feature.rsaoaep.RsaOaepFeature

object FeatureRegistry {
    val features: List<FeatureEntry> = listOf(
        AesGcmFeature,
        RsaOaepFeature,
        EcdsaFeature,
    )
}
