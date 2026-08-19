# Autopilot Android app

Native Kotlin implementation targeting Android SDK 34.

## Build

The build reads the Start.io application ID from `STARTIO_APP_ID` and enables
Start.io test ads by default. Use `-PSTARTIO_TEST_MODE=false` for a release
build after the Start.io account is approved.

```bash
export STARTIO_APP_ID=your-startio-app-id
./gradlew :app:assembleDebug
```

## Behavior

- `OnboardingActivity` is the launcher and cannot be bypassed until Accessibility,
  overlay, MediaProjection, notification, and Internet requirements are ready.
- `MainActivity.onResume()` refreshes overlay/accessibility state after Settings.
- `AdManager` preloads Start.io rewarded inventory, retries every 30 seconds, and
  waits up to 60 seconds after a tap before reporting unavailable inventory.
- `ScreenReaderService` is a media-projection foreground service. It samples only
  supported ride apps every 1.5 seconds, runs ML Kit OCR, and asks the
  accessibility service to click a visible accept control.

Replace the sample package IDs in `ScreenReaderService` and
`RideAccessibilityService` if the installed driver apps use different IDs.