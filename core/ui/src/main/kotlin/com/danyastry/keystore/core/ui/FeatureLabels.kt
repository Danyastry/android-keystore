package com.danyastry.keystore.core.ui

import androidx.annotation.StringRes

@get:StringRes
val FeatureCategory.titleRes: Int
    get() = when (this) {
        FeatureCategory.ENCRYPTION -> R.string.category_encryption
        FeatureCategory.SIGNING -> R.string.category_signing
        FeatureCategory.USER_AUTHENTICATION -> R.string.category_user_authentication
        FeatureCategory.HARDWARE -> R.string.category_hardware
        FeatureCategory.KEY_MANAGEMENT -> R.string.category_key_management
    }

@get:StringRes
val FeatureCategory.descriptionRes: Int
    get() = when (this) {
        FeatureCategory.ENCRYPTION -> R.string.category_encryption_description
        FeatureCategory.SIGNING -> R.string.category_signing_description
        FeatureCategory.USER_AUTHENTICATION -> R.string.category_user_authentication_description
        FeatureCategory.HARDWARE -> R.string.category_hardware_description
        FeatureCategory.KEY_MANAGEMENT -> R.string.category_key_management_description
    }
