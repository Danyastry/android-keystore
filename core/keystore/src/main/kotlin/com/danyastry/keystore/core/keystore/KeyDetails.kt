package com.danyastry.keystore.core.keystore

import android.security.keystore.KeyProperties

enum class KeyKind { SECRET, PRIVATE }

enum class KeyLocation { STRONGBOX, TRUSTED_ENVIRONMENT, SECURE_HARDWARE, SOFTWARE, UNKNOWN }

enum class KeyPurpose { ENCRYPT, DECRYPT, SIGN, VERIFY, WRAP_KEY, AGREE_KEY }

enum class KeyOrigin { GENERATED, IMPORTED, SECURELY_IMPORTED, UNKNOWN }

data class KeyDetails(
    val alias: String,
    val algorithm: String,
    val sizeBits: Int,
    val kind: KeyKind,
    val location: KeyLocation,
    val purposes: Set<KeyPurpose>,
    val origin: KeyOrigin,
    val requiresUserAuthentication: Boolean,
)

object KeyInfoMapper {

    private const val PURPOSE_WRAP_KEY = 32
    private const val PURPOSE_AGREE_KEY = 256
    private const val SECURITY_LEVEL_UNKNOWN_SECURE = -1
    private const val SECURITY_LEVEL_UNKNOWN = -2
    private const val SECURITY_LEVEL_SOFTWARE = 0
    private const val SECURITY_LEVEL_TRUSTED_ENVIRONMENT = 1
    private const val SECURITY_LEVEL_STRONGBOX = 2

    fun purposes(flags: Int): Set<KeyPurpose> = buildSet {
        if (flags and KeyProperties.PURPOSE_ENCRYPT != 0) add(KeyPurpose.ENCRYPT)
        if (flags and KeyProperties.PURPOSE_DECRYPT != 0) add(KeyPurpose.DECRYPT)
        if (flags and KeyProperties.PURPOSE_SIGN != 0) add(KeyPurpose.SIGN)
        if (flags and KeyProperties.PURPOSE_VERIFY != 0) add(KeyPurpose.VERIFY)
        if (flags and PURPOSE_WRAP_KEY != 0) add(KeyPurpose.WRAP_KEY)
        if (flags and PURPOSE_AGREE_KEY != 0) add(KeyPurpose.AGREE_KEY)
    }

    fun location(securityLevel: Int?, insideSecureHardware: Boolean): KeyLocation = when (securityLevel) {
        null -> if (insideSecureHardware) KeyLocation.SECURE_HARDWARE else KeyLocation.SOFTWARE
        SECURITY_LEVEL_STRONGBOX -> KeyLocation.STRONGBOX
        SECURITY_LEVEL_TRUSTED_ENVIRONMENT -> KeyLocation.TRUSTED_ENVIRONMENT
        SECURITY_LEVEL_UNKNOWN_SECURE -> KeyLocation.SECURE_HARDWARE
        SECURITY_LEVEL_SOFTWARE -> KeyLocation.SOFTWARE
        SECURITY_LEVEL_UNKNOWN -> KeyLocation.UNKNOWN
        else -> KeyLocation.UNKNOWN
    }

    fun origin(origin: Int): KeyOrigin = when (origin) {
        KeyProperties.ORIGIN_GENERATED -> KeyOrigin.GENERATED
        KeyProperties.ORIGIN_IMPORTED -> KeyOrigin.IMPORTED
        KeyProperties.ORIGIN_SECURELY_IMPORTED -> KeyOrigin.SECURELY_IMPORTED
        else -> KeyOrigin.UNKNOWN
    }
}
