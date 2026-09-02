plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

fun secret(name: String): String =
    (project.findProperty(name) as String?) ?: System.getenv(name) ?: ""

android {
    namespace = "com.nirwaox.notifybridge"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.nirwaox.notifybridge"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "TELEGRAM_BOT_TOKEN", "\"${secret("TELEGRAM_BOT_TOKEN")}\"")
        buildConfigField("String", "TELEGRAM_CHAT_ID", "\"${secret("TELEGRAM_CHAT_ID")}\"")
    }

    buildFeatures {
        buildConfig = true
        viewBinding = false
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
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
