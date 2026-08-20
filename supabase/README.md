# Supabase setup

The API server now uses Supabase Auth for the Mama Bhutnika mobile account
flow. Account credentials are managed by Supabase, while referral codes,
subscription dates, rewarded-ad progress, and administrator status are stored
in server-controlled Auth `app_metadata`. The display name is kept in the
profile metadata.

Configure these server-side Replit Secrets:

- `SUPABASE_URL`
- `SUPABASE_PUBLISHABLE_KEY`
- `SUPABASE_SECRET_KEY`
- `SUPABASE_JWKS_URL`

The publishable key may be used by public client applications, but the secret
key must only exist in the API server environment. The Android app continues to
send requests only to `API_BASE_URL`; it must never contain either Supabase key.

In the Supabase dashboard, enable the Email provider under Authentication
providers. The API confirms email addresses when creating mobile accounts
because this app does not currently include an email-verification screen.