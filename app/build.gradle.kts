plugins {
    alias(libs.plugins.keystore.android.application)
    alias(libs.plugins.keystore.android.application.compose)
}

android {
    namespace = "com.danyastry.keystore"

    defaultConfig {
        applicationId = "com.danyastry.keystore"
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

dependencies {
    implementation(projects.core.ui)

    implementation(projects.feature.aesgcm)
    implementation(projects.feature.rsaoaep)
    implementation(projects.feature.ecdsa)
    implementation(projects.feature.hmac)
    implementation(projects.feature.keyinfo)
    implementation(projects.feature.strongbox)
    implementation(projects.feature.attestation)
    implementation(projects.feature.biometric)
    implementation(projects.feature.authtimeout)
    implementation(projects.feature.unlockeddevice)
    implementation(projects.feature.ecdh)
    implementation(projects.feature.keyimport)
    implementation(projects.feature.keyconstraints)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.material3)
}
