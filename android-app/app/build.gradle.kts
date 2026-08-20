plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    // Keep the namespace aligned with the primary Kotlin source package.
    // The installable Android application ID remains com.autopilot.app.
    namespace = "com.autopilot.driver"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.autopilot.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        val apiBaseUrl = providers.gradleProperty("API_BASE_URL").orElse("https://api.invalid").get()
        val startIoAppId = providers.gradleProperty("STARTIO_APP_ID").orElse("207133232").get()
        val startIoTestMode = providers.gradleProperty("STARTIO_TEST_MODE").orElse("false").get().toBoolean()
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        buildConfigField("String", "STARTIO_APP_ID", "\"$startIoAppId\"")
        buildConfigField("boolean", "STARTIO_TEST_MODE", startIoTestMode.toString())
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    
    // 1. StartApp Ads
    implementation("com.startapp:inapp-sdk:4.10.4")
    
    // 2. EncryptedSharedPreferences ke liye
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("com.google.mlkit:text-recognition:16.0.1")
}
