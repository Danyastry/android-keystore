package com.danyastry.keystore.catalog

import com.danyastry.keystore.core.ui.FeatureEntry
import com.danyastry.keystore.feature.aesgcm.AesGcmFeature
import com.danyastry.keystore.feature.attestation.AttestationFeature
import com.danyastry.keystore.feature.authtimeout.AuthTimeoutFeature
import com.danyastry.keystore.feature.biometric.BiometricFeature
import com.danyastry.keystore.feature.ecdh.EcdhFeature
import com.danyastry.keystore.feature.ecdsa.EcdsaFeature
import com.danyastry.keystore.feature.hmac.HmacFeature
import com.danyastry.keystore.feature.keyconstraints.KeyConstraintsFeature
import com.danyastry.keystore.feature.keyimport.KeyImportFeature
import com.danyastry.keystore.feature.keyinfo.KeyInfoFeature
import com.danyastry.keystore.feature.rsaoaep.RsaOaepFeature
import com.danyastry.keystore.feature.strongbox.StrongBoxFeature
import com.danyastry.keystore.feature.unlockeddevice.UnlockedDeviceFeature

object FeatureRegistry {
    val features: List<FeatureEntry> = listOf(
        AesGcmFeature,
        RsaOaepFeature,
        EcdsaFeature,
        HmacFeature,
        KeyInfoFeature,
        StrongBoxFeature,
        AttestationFeature,
        BiometricFeature,
        AuthTimeoutFeature,
        UnlockedDeviceFeature,
        EcdhFeature,
        KeyImportFeature,
        KeyConstraintsFeature,
    )
}
