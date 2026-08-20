# Autopilot Android app

Native Kotlin implementation targeting Android SDK 34.

## Build

The build uses Start.io application ID `207133232` by default. You can override
it with `STARTIO_APP_ID` or `-PSTARTIO_APP_ID=...`. Test ads are enabled by
default; use `-PSTARTIO_TEST_MODE=false` for a release build after the Start.io
account is approved.

```bash
./gradlew :app:assembleDebug
```

## Behavior

- `OnboardingActivity` is the launcher and cannot be bypassed until Accessibility,
  overlay, MediaProjection, notification, and Internet requirements are ready.
- `MainActivity.onResume()` refreshes overlay/accessibility state after Settings.
- `AdManager` attaches persistent Start.io banners, preloads interstitial
  inventory, limits interstitials to one per 45 seconds, retries every 30
  seconds, and provides an explicit rewarded-video action.
- `ScreenReaderService` is a media-projection foreground service. It samples only
  supported ride apps every 1.5 seconds, runs ML Kit OCR, and asks the
  accessibility service to click a visible accept control.

Replace the sample package IDs in `ScreenReaderService` and
`RideAccessibilityService` if the installed driver apps use different IDs.