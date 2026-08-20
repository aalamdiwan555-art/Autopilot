---
name: Encrypted auth preferences
description: The Android login state is stored through EncryptedSharedPreferences and must not be read as plain preferences.
---

Use the shared UserPrefs wrapper whenever checking or changing authentication state. Do not access the underlying preference file directly with getSharedPreferences, because encrypted keys and values will not be visible through the plain API.

**Why:** A plain read reported the user as logged out even after a successful login, creating a Login → Onboarding → Login loop.

**How to apply:** Reuse UserPrefs.isLoggedIn and UserPrefs.apiToken for activity gates and navigation decisions.