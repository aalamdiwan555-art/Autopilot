plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.autopilot.driver"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.autopilot.driver"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    // This is a public Start.io application identifier, not a secret.
    // The property/env override keeps staging builds flexible while ensuring
    // a normal build is never silently shipped without an ads configuration.
    val startIoAppId = providers.gradleProperty("STARTIO_APP_ID").orElse(
        providers.environmentVariable("STARTIO_APP_ID").orElse("207133232")
    ).get()
    val escapedStartIoAppId = startIoAppId.replace("\\", "\\\\").replace("\"", "\\\"")
    val apiBaseUrl = providers.gradleProperty("API_BASE_URL").orElse(
        providers.environmentVariable("API_BASE_URL").orElse("https://api.invalid/api")
    ).get()
    val escapedApiBaseUrl = apiBaseUrl.replace("\\", "\\\\").replace("\"", "\\\"")
    buildTypes.all {
        buildConfigField("String", "STARTIO_APP_ID", "\"$escapedStartIoAppId\"")
        buildConfigField("String", "API_BASE_URL", "\"$escapedApiBaseUrl\"")
        buildConfigField(
            "Boolean",
            "STARTIO_TEST_MODE",
<<<<<<< HEAD
            providers.gradleProperty("STARTIO_TEST_MODE").orElse("true").get()
=======
         providers.gradleProperty("STARTIO_TEST_MODE").orElse("false").get()
>>>>>>> feda77c (Initialize Android project and supporting documentation files)
        )
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity-ktx:1.9.2")
    implementation("androidx.lifecycle:lifecycle-service:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("com.startapp:inapp-sdk:4.10.4")
    implementation("com.google.mlkit:text-recognition:16.0.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
}
