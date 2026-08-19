# Autopilot

An Android Accessibility Service app that auto-detects "Accept" buttons across all major Indian languages and auto-clicks them.

## Subscription and referrals

- Every account receives a one-hour starter subscription from the shared API.
- Each account has a unique referral code.
- When a new user signs up with a referral code, the referrer receives two free subscription days.
- After a subscription expires, auto-clicking is locked.
- Completing ten rewarded ads grants one free subscription day. Rewards are confirmed by the API, not only stored on the phone.
- The administrator can grant subscription days to any account from the admin panel.

The referral and subscription features require the API server from the companion workspace to be deployed. Set `API_BASE_URL` to the deployed API URL ending in `/api` when building the APK. Never put the server session secret in the APK.

The administrator account is the normal account registered with `aalamdiwan555@gmail.com`. The API promotes that verified account to administrator using the shared `ADMIN_EMAIL` configuration; there is no default password or hidden admin login.

## Supported Languages
- Hindi (स्वीकार करें)
- Bengali (গ্রহণ করুন)
- Telugu (అంగీకరించండి)
- Tamil (ஏற்றுக்கொள்ளுங்கள்)
- Marathi (स्वीकारा)
- Urdu (قبول کریں)
- Gujarati (સ્વીકારો)
- Kannada (ಸ್ವೀಕರಿಸಿ)
- Odia (ଗ୍ରହଣ କରନ୍ତୁ)
- Malayalam (സ്വീകരിക്കുക)
- Punjabi (ਸਵੀਕਾਰ ਕਰੋ)
- Assamese (স্বীকাৰ কৰক)
- Maithili (मान्य करात)
- Meitei (ꯌꯥꯅꯕꯤꯒꯅꯨ)
- Bodo (গনায় জাগ্রহি)
- Santali (ᱧᱟᱢ ᱢᱮ)
- Kashmiri (قبول کَرُن)
- Nepali (स्वीकार गर्नुहोस्)
- Sindhi (قبول ڪيو)
- English (Accept)

## Setup
1. Build or install the APK
2. Open the app
3. Tap "Open Accessibility Settings"
4. Enable "Ride Accepter Service"
5. Return to app and toggle "Enable Auto-Click"
6. Optionally set target app package and custom trigger texts

## GitHub Actions Build
Push to `main` or manually run the `Build Android APK` workflow. The APK will be available as the `mama-bhutnika-debug-apk` downloadable artifact.

Before building, add a repository variable or secret named `API_BASE_URL`, for example:

```text
https://your-published-api.example.com/api
```

If it is not configured, the APK still compiles but account, referral, reward, and subscription requests will show a configuration error instead of silently falling back to local data.

## Warning
Using automation on ride-hailing platforms may violate their Terms of Service and result in account suspension. Use at your own risk.
