plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.mamabhutnika.rideaccepter"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.mamabhutnika.rideaccepter"
        minSdk = 24
        targetSdk = 34
        versionCode = 4
        versionName = "4.0"
    }

    buildFeatures {
        buildConfig = true
    }

    val configuredApiBaseUrl = project.findProperty("API_BASE_URL")?.toString()
        ?: System.getenv("API_BASE_URL")
        ?: "https://api.invalid/api"
    val escapedApiBaseUrl = configuredApiBaseUrl
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
    buildTypes.all {
        buildConfigField("String", "API_BASE_URL", "\"$escapedApiBaseUrl\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-ktx:1.9.2") // <-- lifecycleScope ke liye zaroori
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Start.io (StartApp) SDK
    implementation("com.startapp:inapp-sdk:5.2.1")
}
