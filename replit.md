# Mama Bhutnika

Mama Bhutnika is an Android accessibility assistant for guarded ride-offer acceptance across supported Indian driver apps, with account subscriptions, referrals, rewarded access, and an admin desk.

## Run & Operate

- `pnpm --filter @workspace/api-server run dev` — run the API server (port 5000)
- `pnpm run typecheck` — full typecheck across all packages
- `pnpm run build` — typecheck + build all packages
- `pnpm --filter @workspace/api-spec run codegen` — regenerate API hooks and Zod schemas from the OpenAPI spec
- `./android-app/gradlew -p android-app assembleDebug -PAPI_BASE_URL="https://your-api.example/api"` — build the Android debug APK when the Android SDK is available
- Required env: `SUPABASE_URL`, `SUPABASE_PUBLISHABLE_KEY`, `SUPABASE_SECRET_KEY`, and `SUPABASE_JWKS_URL`
- Optional env: `ADMIN_EMAIL`, `API_BASE_URL`

## Stack

- pnpm workspaces, Node.js 24, TypeScript 5.9
- API: Express 5
- Auth and account metadata: Supabase Auth
- Validation: Zod (`zod/v4`)
- API codegen: Orval (from OpenAPI spec)
- Build: esbuild (CJS bundle)

## Where things live

- `android-app/` — native Android client, XML screens, accessibility service, floating controls, and subscription UI
- `artifacts/api-server/` — Express API for mobile auth, subscriptions, referrals, rewards, and admin operations
- `lib/api-spec/openapi.yaml` — source of truth for API contracts
- `supabase/README.md` — Supabase Auth setup and server/client key boundaries
- `lib/api-client-react/` and `lib/api-zod/` — generated API clients and validation schemas
- `.github/workflows/android.yml` — CI build and APK artifact workflow

## Architecture decisions

- Supabase Auth owns mobile credentials and access tokens; the Supabase secret key is used only by the API server and never ships in the Android build.
- Referral, subscription, rewarded-ad, and administrator state is stored in each Supabase Auth user's server-controlled `app_metadata`.
- Auto-clicking is guarded to the supported driver packages and exact positive labels; a blank target package means all supported packages, not arbitrary apps.
- Subscription, referral, and rewarded-ad state is server-backed; the Android client keeps only an encrypted local cache for continuity.
- The main app UI uses XML/Material components so it remains lightweight and works without a web runtime.

## Product

- Driver sign-up and sign-in with a one-hour starter subscription
- Guarded auto-click settings for Rapido, Ola, Uber, or all supported apps
- Custom matching labels, accessibility setup, and optional floating controls
- Referral sharing/redemption and server-confirmed rewarded-ad progress
- Admin user list and subscription-day grants

## User preferences

- The user wants the full Android UI/UX refreshed with lively but restrained animation and all major bugs fixed, not just a code review.

## Gotchas

- Android APK compilation requires a full Android SDK; CI installs it with `android-actions/setup-android`.
- Set `API_BASE_URL` to a deployed URL ending in `/api`; an unconfigured build now stays open and reports a clear account-service error instead of crashing on launch.
- Supabase Auth must allow email/password sign-in. New accounts are marked email-confirmed by the server because the Android app does not yet include an email verification screen.
- After changing `lib/api-spec/openapi.yaml`, run codegen before using generated schemas or clients.
- Never put `SUPABASE_SECRET_KEY` or other server secrets in `android-app`.

## Pointers

- See the `pnpm-workspace` skill for workspace structure, TypeScript setup, and package details
