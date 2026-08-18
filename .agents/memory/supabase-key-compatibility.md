---
name: Supabase server-key compatibility
description: Supabase Auth admin requests require a valid active server key matching the project URL.
---

Supabase Auth admin requests are not usable until the server-only key is accepted by
the project; a healthy API and valid publishable key do not prove the admin key is
valid.

**Why:** The project uses the newer `sb_publishable_*` and `sb_secret_*` key family,
and Auth admin calls returned `401 Invalid API key` even after secure replacement.

**How to apply:** Validate the secret-key path with a read-only admin request before
testing signup or any mutation. Never expose the server key in the Android client.