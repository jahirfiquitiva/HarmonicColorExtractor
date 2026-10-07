import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    `maven-publish`
    signing
}

group = "dev.jahir"
version = "1.0.0"

android {
    namespace = "dev.jahir.harmonic.colors"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

dependencies {
    implementation(libs.androidx.core)
    implementation(libs.androidx.palette)

    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.runner)
}

kotlin {
    explicitApi()
}

// Secrets come from local.properties when publishing locally and from environment variables on CI
val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}

fun secret(property: String, environmentVariable: String): String? =
    localProperties.getProperty(property) ?: System.getenv(environmentVariable)

val signingKeyId = secret("signing.keyId", "SIGNING_KEY_ID")
// The signing plugin reads these as project properties
extra["signing.keyId"] = signingKeyId
extra["signing.password"] = secret("signing.password", "SIGNING_PASSWORD")
extra["signing.secretKeyRingFile"] = secret("signing.secretKeyRingFile", "SIGNING_SECRET_KEY_RING_FILE")

publishing {
    publications {
        register<MavenPublication>("release") {
            artifactId = "harmonic-colors"
            afterEvaluate { from(components["release"]) }

            pom {
                name = "Harmonic Colors"
                description = "Extracts a background color and readable text colors from a bitmap"
                url = "https://github.com/jahirfiquitiva/HarmonicColorExtractor"
                licenses {
                    license {
                        name = "The Apache License, Version 2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                }
                developers {
                    developer {
                        id = "jahirfiquitiva"
                        name = "Jahir Fiquitiva"
                        email = "hola@jahir.dev"
                    }
                    developer {
                        id = "LeonardoSM04"
                        name = "Leonardo Salazar"
                    }
                }
                scm {
                    connection = "scm:git:github.com/jahirfiquitiva/HarmonicColorExtractor.git"
                    developerConnection = "scm:git:ssh://github.com/jahirfiquitiva/HarmonicColorExtractor.git"
                    url = "https://github.com/jahirfiquitiva/HarmonicColorExtractor"
                }
            }
        }
    }
    repositories {
        maven {
            name = "sonatype"
            url = uri("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/")
            credentials {
                username = secret("ossrhUsername", "OSSRH_USERNAME")
                password = secret("ossrhPassword", "OSSRH_PASSWORD")
            }
        }
    }
}

signing {
    // Lets publishToMavenLocal work without a signing key
    isRequired = signingKeyId != null
    sign(publishing.publications)
}
