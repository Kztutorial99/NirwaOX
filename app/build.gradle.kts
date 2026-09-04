plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

fun secret(name: String): String =
    (project.findProperty(name) as String?) ?: System.getenv(name) ?: ""

fun environment(name: String): String? =
    System.getenv(name)?.takeIf { it.isNotBlank() }

val releaseKeystorePath = environment("RELEASE_KEYSTORE_PATH")
val releaseKeystorePassword = environment("RELEASE_KEYSTORE_PASSWORD")
val releaseKeyAlias = environment("RELEASE_KEY_ALIAS")
val releaseKeyPassword = environment("RELEASE_KEY_PASSWORD")
val releaseSigningReady = listOf(
    releaseKeystorePath,
    releaseKeystorePassword,
    releaseKeyAlias,
    releaseKeyPassword
).all { it != null }

android {
    namespace = "com.nirwaos.notifybridge"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.nirwaos.notifybridge"
        minSdk = 24
        targetSdk = 34
        versionCode = 2
        versionName = "1.1"

        buildConfigField("String", "TELEGRAM_BOT_TOKEN", "\"${secret("TELEGRAM_BOT_TOKEN")}\"")
        buildConfigField("String", "TELEGRAM_CHAT_ID", "\"${secret("TELEGRAM_CHAT_ID")}\"")
    }

    buildFeatures {
        buildConfig = true
        viewBinding = false
    }

    signingConfigs {
        create("release") {
            if (releaseSigningReady) {
                storeFile = file(releaseKeystorePath!!)
                storePassword = releaseKeystorePassword!!
                keyAlias = releaseKeyAlias!!
                keyPassword = releaseKeyPassword!!
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Never silently fall back to the debug key. The CI workflow supplies
            // the stable release keystore and AGP will fail clearly if it is absent.
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
