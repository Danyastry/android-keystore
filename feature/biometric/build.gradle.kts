plugins {
    alias(libs.plugins.keystore.android.feature)
}

android {
    namespace = "com.danyastry.keystore.feature.biometric"
}

dependencies {
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.fragment)
}
