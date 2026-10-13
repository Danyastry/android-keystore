package com.danyastry.keystore.core.keystore

import android.security.keystore.KeyProperties
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class KeyInfoMapperTest {

    @Test
    fun `reads every purpose flag`() {
        val flags = KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT or
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY or KeyProperties.PURPOSE_WRAP_KEY or 256

        assertThat(KeyInfoMapper.purposes(flags)).containsExactlyElementsIn(KeyPurpose.entries)
    }

    @Test
    fun `reads single purposes`() {
        assertThat(KeyInfoMapper.purposes(KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY))
            .containsExactly(KeyPurpose.SIGN, KeyPurpose.VERIFY)
        assertThat(KeyInfoMapper.purposes(0)).isEmpty()
    }

    @Test
    fun `reads the security level on android 12 and newer`() {
        assertThat(KeyInfoMapper.location(KeyProperties.SECURITY_LEVEL_STRONGBOX, insideSecureHardware = true))
            .isEqualTo(KeyLocation.STRONGBOX)
        assertThat(KeyInfoMapper.location(KeyProperties.SECURITY_LEVEL_TRUSTED_ENVIRONMENT, insideSecureHardware = true))
            .isEqualTo(KeyLocation.TRUSTED_ENVIRONMENT)
        assertThat(KeyInfoMapper.location(KeyProperties.SECURITY_LEVEL_SOFTWARE, insideSecureHardware = false))
            .isEqualTo(KeyLocation.SOFTWARE)
        assertThat(KeyInfoMapper.location(KeyProperties.SECURITY_LEVEL_UNKNOWN_SECURE, insideSecureHardware = true))
            .isEqualTo(KeyLocation.SECURE_HARDWARE)
        assertThat(KeyInfoMapper.location(KeyProperties.SECURITY_LEVEL_UNKNOWN, insideSecureHardware = false))
            .isEqualTo(KeyLocation.UNKNOWN)
    }

    @Test
    fun `falls back to the hardware flag before android 12`() {
        assertThat(KeyInfoMapper.location(null, insideSecureHardware = true)).isEqualTo(KeyLocation.SECURE_HARDWARE)
        assertThat(KeyInfoMapper.location(null, insideSecureHardware = false)).isEqualTo(KeyLocation.SOFTWARE)
    }

    @Test
    fun `reads the origin`() {
        assertThat(KeyInfoMapper.origin(KeyProperties.ORIGIN_GENERATED)).isEqualTo(KeyOrigin.GENERATED)
        assertThat(KeyInfoMapper.origin(KeyProperties.ORIGIN_IMPORTED)).isEqualTo(KeyOrigin.IMPORTED)
        assertThat(KeyInfoMapper.origin(KeyProperties.ORIGIN_SECURELY_IMPORTED)).isEqualTo(KeyOrigin.SECURELY_IMPORTED)
        assertThat(KeyInfoMapper.origin(99)).isEqualTo(KeyOrigin.UNKNOWN)
    }
}
