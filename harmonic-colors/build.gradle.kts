plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "dev.jahir.harmonic.colors"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.androidx.core)
    implementation(libs.androidx.palette)
}

kotlin {
    explicitApi()
}
