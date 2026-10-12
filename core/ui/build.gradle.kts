plugins {
    alias(libs.plugins.keystore.android.library.compose)
}

android {
    namespace = "com.danyastry.keystore.core.ui"
}

dependencies {
    api(libs.androidx.material3)
}
