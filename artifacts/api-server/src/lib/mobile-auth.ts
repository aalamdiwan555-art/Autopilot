import { randomBytes } from "node:crypto";

type SupabaseUser = {
  id: string;
  email?: string | null;
  user_metadata?: Record<string, unknown> | null;
  app_metadata?: Record<string, unknown> | null;
};

type SupabaseSession = {
  access_token: string;
  user?: SupabaseUser;
};

type SupabaseUserList = {
  users: SupabaseUser[];
};

export type AppUser = {
  id: string;
  name: string;
  email: string;
  referralCode: string;
  referredById: string | null;
  subscriptionUntil: Date;
  rewardProgress: number;
  totalAdsWatched: number;
  isAdmin: boolean;
};

export class SupabaseRequestError extends Error {
  constructor(
    message: string,
    public readonly status: number,
  ) {
    super(message);
    this.name = "SupabaseRequestError";
  }
}

function supabaseConfig(): {
  url: string;
  publishableKey: string;
  secretKey: string;
  jwksUrl: string;
} {
  const url = process.env.SUPABASE_URL?.trim().replace(/\/+$/, "");
  const publishableKey = process.env.SUPABASE_PUBLISHABLE_KEY?.trim();
  const secretKey = process.env.SUPABASE_SECRET_KEY?.trim();
  const jwksUrl = process.env.SUPABASE_JWKS_URL?.trim();
  if (!url || !publishableKey || !secretKey || !jwksUrl) {
    throw new Error(
      "SUPABASE_URL, SUPABASE_PUBLISHABLE_KEY, SUPABASE_SECRET_KEY, and SUPABASE_JWKS_URL must be configured.",
    );
  }
  return { url, publishableKey, secretKey, jwksUrl };
}

async function supabaseRequest<T>(
  path: string,
  init: RequestInit = {},
  options: { admin?: boolean; accessToken?: string } = {},
): Promise<T> {
  const { url, publishableKey, secretKey } = supabaseConfig();
  const headers = new Headers(init.headers);
  headers.set("Accept", "application/json");
  headers.set("apikey", options.admin ? secretKey : publishableKey);
  if (options.accessToken) {
    headers.set("Authorization", `Bearer ${options.accessToken}`);
  } else if (!options.admin) {
    headers.set("Authorization", `Bearer ${publishableKey}`);
  }
  if (init.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  const response = await fetch(`${url}${path}`, { ...init, headers });
  const text = await response.text();
  let payload: unknown = null;
  try {
    payload = text ? JSON.parse(text) : null;
  } catch {
    payload = null;
  }

  if (!response.ok) {
    const message =
      typeof payload === "object" &&
      payload !== null &&
      "msg" in payload &&
      typeof payload.msg === "string"
        ? payload.msg
        : typeof payload === "object" &&
            payload !== null &&
            "message" in payload &&
            typeof payload.message === "string"
          ? payload.message
          : `Supabase request failed (${response.status}).`;
    throw new SupabaseRequestError(message, response.status);
  }

  return payload as T;
}

export function normalizeEmail(email: string): string {
  return email.trim().toLowerCase();
}

export function validEmail(email: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

function serverMetadata(user: AppUser): Record<string, unknown> {
  return {
    referralCode: user.referralCode,
    referredById: user.referredById,
    subscriptionUntil: user.subscriptionUntil.toISOString(),
    rewardProgress: user.rewardProgress,
    totalAdsWatched: user.totalAdsWatched,
    isAdmin: user.isAdmin,
  };
}

function dateFromMetadata(value: unknown): Date {
  if (typeof value !== "string") return new Date(0);
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? new Date(0) : parsed;
}

export function appUserFromSupabase(user: SupabaseUser): AppUser {
  const profile = user.user_metadata ?? {};
  const data = user.app_metadata ?? {};
  const email = normalizeEmail(user.email ?? "");
  return {
    id: user.id,
    name:
      typeof profile.name === "string" && profile.name.trim()
        ? profile.name.trim()
        : email.split("@")[0] || "Mama Bhutnika user",
    email,
    referralCode: typeof data.referralCode === "string" ? data.referralCode : "",
    referredById: typeof data.referredById === "string" ? data.referredById : null,
    subscriptionUntil: dateFromMetadata(data.subscriptionUntil),
    rewardProgress:
      typeof data.rewardProgress === "number" && Number.isInteger(data.rewardProgress)
        ? data.rewardProgress
        : 0,
    totalAdsWatched:
      typeof data.totalAdsWatched === "number" && Number.isInteger(data.totalAdsWatched)
        ? data.totalAdsWatched
        : 0,
    isAdmin: data.isAdmin === true,
  };
}

export function publicUser(user: AppUser) {
  return {
    uid: user.id,
    name: user.name,
    email: user.email,
    referralCode: user.referralCode,
    subscriptionUntil: user.subscriptionUntil,
    subscriptionActive: user.subscriptionUntil.getTime() > Date.now(),
    rewardProgress: user.rewardProgress,
    totalAdsWatched: user.totalAdsWatched,
    isAdmin: user.isAdmin,
  };
}

export function newReferralCode(): string {
  const alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  const bytes = randomBytes(8);
  return Array.from(bytes, (byte) => alphabet[byte % alphabet.length]).join("");
}

export async function listAppUsers(): Promise<AppUser[]> {
  const users: AppUser[] = [];
  for (let page = 1; page <= 100; page += 1) {
    const response = await supabaseRequest<SupabaseUserList>(
      `/auth/v1/admin/users?page=${page}&per_page=1000`,
      {},
      { admin: true },
    );
    users.push(...response.users.map(appUserFromSupabase));
    if (response.users.length < 1000) break;
  }
  return users;
}

export async function getAppUserForAccessToken(token: string): Promise<AppUser | null> {
  try {
    // Supabase Auth validates the access token here. The configured JWKS endpoint
    // remains available for deployments that choose local verification later.
    const user = await supabaseRequest<SupabaseUser>("/auth/v1/user", {}, { accessToken: token });
    return appUserFromSupabase(user);
  } catch (error) {
    if (error instanceof SupabaseRequestError && [401, 403].includes(error.status)) {
      return null;
    }
    throw error;
  }
}

export async function signupWithSupabase(
  name: string,
  email: string,
  password: string,
  referralCode: string,
): Promise<{ token: string; user: AppUser }> {
  const users = await listAppUsers();
  if (users.some((user) => user.email === email)) {
    throw new SupabaseRequestError("An account with this email already exists.", 409);
  }

  const referrer = referralCode
    ? users.find((user) => user.referralCode === referralCode)
    : undefined;
  if (referralCode && !referrer) {
    throw new SupabaseRequestError("That referral code is not valid.", 400);
  }

  const now = new Date();
  const newUser: AppUser = {
    id: "",
    name,
    email,
    referralCode: newReferralCode(),
    referredById: referrer?.id ?? null,
    subscriptionUntil: new Date(now.getTime() + 3_600_000),
    rewardProgress: 0,
    totalAdsWatched: 0,
    isAdmin: email === normalizeEmail(process.env.ADMIN_EMAIL ?? "aalamdiwan555@gmail.com"),
  };

  const created = await supabaseRequest<SupabaseUser>(
    "/auth/v1/admin/users",
    {
      method: "POST",
      body: JSON.stringify({
        email,
        password,
        email_confirm: true,
        user_metadata: { name },
        app_metadata: serverMetadata(newUser),
      }),
    },
    { admin: true },
  );
  const createdUser = appUserFromSupabase(created);

  if (referrer) {
    const currentUntil = Math.max(referrer.subscriptionUntil.getTime(), now.getTime());
    await updateAppUser(referrer, {
      subscriptionUntil: new Date(currentUntil + 2 * 86_400_000),
    });
  }

  return signInWithSupabase(email, password, createdUser);
}

export async function signInWithSupabase(
  email: string,
  password: string,
  fallbackUser?: AppUser,
): Promise<{ token: string; user: AppUser }> {
  const session = await supabaseRequest<SupabaseSession>(
    "/auth/v1/token?grant_type=password",
    {
      method: "POST",
      body: JSON.stringify({ email, password }),
    },
  );
  const user = session.user
    ? appUserFromSupabase(session.user)
    : (await getAppUserForAccessToken(session.access_token)) ?? fallbackUser;
  if (!user) {
    throw new Error("Supabase returned a session without an account profile.");
  }
  return { token: session.access_token, user };
}

export async function updateAppUser(
  user: AppUser,
  changes: Partial<AppUser>,
): Promise<AppUser> {
  const nextUser: AppUser = { ...user, ...changes };
  const updated = await supabaseRequest<SupabaseUser>(
    `/auth/v1/admin/users/${encodeURIComponent(user.id)}`,
    {
      method: "PUT",
      body: JSON.stringify({
        user_metadata: { name: nextUser.name },
        app_metadata: serverMetadata(nextUser),
      }),
    },
    { admin: true },
  );
  return appUserFromSupabase(updated);
}