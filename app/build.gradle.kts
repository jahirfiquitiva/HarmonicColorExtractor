plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "dev.jahir.harmonic.colors.demo"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "dev.jahir.harmonic.colors.demo"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.compileSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":harmonic-colors"))
    implementation(libs.androidx.appcompat)
}
