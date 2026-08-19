---
name: Android build environment
description: The native Android project’s Gradle build depends on a locally installed Android SDK.
---

Native APK verification cannot run in an environment without Android SDK platforms and build-tools. Gradle stops before compilation and requires either ANDROID_HOME or android-app/local.properties with sdk.dir.

**Why:** The project uses the Android Gradle plugin, so source checks and XML validation can succeed while APK assembly remains unavailable.

**How to apply:** Before claiming an APK build is verified, confirm the SDK is installed and rerun `./gradlew :app:assembleDebug --no-daemon`.