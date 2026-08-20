# Autopilot redesign implementation plan

## Product structure

- **Home** is a single task-focused dashboard: automation status, subscription/reward status, permission setup, and supported driver apps.
- **Refer & Earn** is the second bottom-navigation destination. It exposes the generated referral code, system sharing, redemption, and lightweight invite/reward metrics.
- **Profile** opens the About/Subscription surface with support, Terms of Service, and Privacy Policy entry points.
- **Admin** is the only place where Advanced Matching is exposed. The user app has no matching toggle or matching settings card.

## Ad system

- Keep the top and bottom anchor placements inside the scroll shell so they are visible without obscuring controls.
- Continue using the existing Start.io adapter, but preload interstitial and rewarded inventory and retry failures with capped exponential backoff (1s → 30s).
- Keep rewarded ads behind an explicit user action and use the existing server-side reward completion endpoint so the client never grants subscription time locally.
- A production fallback network should be configured in the ad adapter when the chosen provider is unavailable; the UI now reports unavailable inventory instead of appearing stuck.

## Backend schema and API work required for production

The current Supabase auth metadata already stores user-owned referral, subscription, and reward fields. The next server migration should add a singleton `app_settings` record:

```sql
create table app_settings (
  id text primary key default 'global',
  advanced_matching_enabled boolean not null default false,
  updated_by uuid references auth.users(id),
  updated_at timestamptz not null default now()
);
```

Add two admin-protected endpoints:

- `GET /mobile/config` → `{ advancedMatchingEnabled: boolean }`
- `PUT /mobile/admin/config` with `{ advancedMatchingEnabled: boolean }` → the updated config

The mobile app should fetch `/mobile/config` after authentication and use the value in the accessibility service. The admin panel should update it through the protected endpoint rather than relying on device-local preferences. Referral analytics should later be backed by a `referrals` table (`referrer_id`, `referred_user_id`, `reward_days`, `created_at`) instead of the current aggregate-only presentation.

## Permission behavior

Camera, microphone, media, and notifications are requested only from the friendly setup prompt. Overlay access remains a system-settings flow because Android does not allow a normal runtime dialog for it. Denial is non-fatal and the setup card remains available.

## Verification checklist

1. Run an Android debug build on a machine with Android SDK 34 configured.
2. Verify first-run permission denial, retry from Settings, accessibility setup, and overlay setup.
3. Verify Home has no Advanced Matching control.
4. Verify admin-only matching control and global API persistence.
5. Verify referral sharing and redemption across two accounts.
6. Verify ad preload retry and rewarded completion after a failed inventory load.