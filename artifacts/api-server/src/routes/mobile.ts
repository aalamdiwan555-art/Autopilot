import { Router, type IRouter, type Request, type Response } from "express";
import {
  MobileAdminGrantDaysBody,
  MobileAdminGrantDaysParams,
  MobileAdminGrantDaysResponse,
  MobileAdminUsersResponse,
  MobileLoginBody,
  MobileLoginResponse,
  MobileMeResponse,
  MobileRedeemReferralBody,
  MobileRedeemReferralResponse,
  MobileRewardAdCompletedResponse,
  MobileSignupBody,
  MobileSignupResponse,
} from "@workspace/api-zod";
import {
  SupabaseRequestError,
  getAppUserForAccessToken,
  listAppUsers,
  normalizeEmail,
  publicUser,
  requestPasswordReset,
  signInWithSupabase,
  signupWithSupabase,
  updateAppUser,
  validEmail,
  type AppUser,
} from "../lib/mobile-auth";

const router: IRouter = Router();
const ONE_DAY_MS = 86_400_000;
const REWARD_THRESHOLD = 10;

function error(res: Response, status: number, message: string): void {
  res.status(status).json({ error: message });
}

async function userFromRequest(req: Request): Promise<AppUser | null> {
  const header = req.get("authorization") ?? "";
  if (!header.toLowerCase().startsWith("bearer ")) return null;
  return getAppUserForAccessToken(header.slice(7).trim());
}

function supabaseErrorStatus(errorValue: unknown, fallback = 500): number {
  return errorValue instanceof SupabaseRequestError ? errorValue.status : fallback;
}

function supabaseErrorMessage(errorValue: unknown, fallback: string): string {
  return errorValue instanceof SupabaseRequestError ? errorValue.message : fallback;
}

router.post("/mobile/auth/signup", async (req, res): Promise<void> => {
  const parsed = MobileSignupBody.safeParse(req.body);
  if (!parsed.success) {
    error(res, 400, "Use a name, valid email, and password with at least 6 characters.");
    return;
  }

  const name = parsed.data.name.trim();
  const email = normalizeEmail(parsed.data.email);
  const referralCode = parsed.data.referralCode.trim().toUpperCase();
  if (name.length < 2 || !validEmail(email)) {
    error(res, 400, "Enter a valid name and email address.");
    return;
  }

  try {
    const result = await signupWithSupabase(name, email, parsed.data.password, referralCode);
    res.status(201).json(
      MobileSignupResponse.parse({
        token: result.token,
        user: publicUser(result.user),
      }),
    );
  } catch (errorValue) {
    error(
      res,
      supabaseErrorStatus(errorValue),
      supabaseErrorMessage(errorValue, "The account service is unavailable."),
    );
  }
});

router.post("/mobile/auth/login", async (req, res): Promise<void> => {
  const parsed = MobileLoginBody.safeParse(req.body);
  if (!parsed.success) {
    error(res, 400, "Enter a valid email and password.");
    return;
  }

  try {
    const result = await signInWithSupabase(
      normalizeEmail(parsed.data.email),
      parsed.data.password,
    );
    res.json(
      MobileLoginResponse.parse({
        token: result.token,
        user: publicUser(result.user),
      }),
    );
  } catch (errorValue) {
    const status = supabaseErrorStatus(errorValue, 401);
    error(
      res,
      status === 500 ? 500 : status,
      status === 500 ? "The account service is unavailable." : "Email or password is incorrect.",
    );
  }
});

router.post("/mobile/auth/forgot-password", async (req, res): Promise<void> => {
  const email =
    typeof req.body?.email === "string" ? normalizeEmail(req.body.email) : "";
  if (!validEmail(email)) {
    error(res, 400, "Enter a valid email address.");
    return;
  }

  try {
    await requestPasswordReset(email);
    res.json({ message: "If an account exists for that email, a reset link is on its way." });
  } catch (errorValue) {
    error(
      res,
      supabaseErrorStatus(errorValue),
      supabaseErrorMessage(errorValue, "The password reset service is unavailable."),
    );
  }
});

router.get("/mobile/me", async (req, res): Promise<void> => {
  try {
    const user = await userFromRequest(req);
    if (!user) {
      error(res, 401, "Session expired.");
      return;
    }
    res.json(MobileMeResponse.parse({ user: publicUser(user) }));
  } catch {
    error(res, 401, "Session expired.");
  }
});

router.post("/mobile/referrals/redeem", async (req, res): Promise<void> => {
  const user = await userFromRequest(req);
  if (!user) {
    error(res, 401, "Session expired.");
    return;
  }

  const parsed = MobileRedeemReferralBody.safeParse(req.body);
  if (!parsed.success) {
    error(res, 400, "Enter a referral code.");
    return;
  }
  if (user.referredById) {
    error(res, 409, "You have already redeemed a referral code.");
    return;
  }

  const users = await listAppUsers();
  const referrer = users.find(
    (candidate) =>
      candidate.referralCode === parsed.data.referralCode.trim().toUpperCase(),
  );
  if (!referrer) {
    error(res, 400, "That referral code is not valid.");
    return;
  }
  if (referrer.id === user.id) {
    error(res, 400, "You cannot redeem your own code.");
    return;
  }

  const now = new Date();
  await updateAppUser(user, { referredById: referrer.id });
  await updateAppUser(referrer, {
    subscriptionUntil: new Date(
      Math.max(referrer.subscriptionUntil.getTime(), now.getTime()) + 2 * ONE_DAY_MS,
    ),
  });

  res.json(
    MobileRedeemReferralResponse.parse({
      message: "Referral accepted. The referrer received 2 free subscription days.",
    }),
  );
});

router.post("/mobile/rewards/ad-completed", async (req, res): Promise<void> => {
  const user = await userFromRequest(req);
  if (!user) {
    error(res, 401, "Session expired.");
    return;
  }

  const nextProgress = user.rewardProgress + 1;
  const completedReward = nextProgress >= REWARD_THRESHOLD;
  const now = new Date();
  const updated = await updateAppUser(user, {
    rewardProgress: completedReward ? 0 : nextProgress,
    totalAdsWatched: user.totalAdsWatched + 1,
    ...(completedReward
      ? {
          subscriptionUntil: new Date(
            Math.max(user.subscriptionUntil.getTime(), now.getTime()) + ONE_DAY_MS,
          ),
        }
      : {}),
  });

  res.json(
    MobileRewardAdCompletedResponse.parse({
      user: publicUser(updated),
    }),
  );
});

router.get("/mobile/admin/users", async (req, res): Promise<void> => {
  const user = await userFromRequest(req);
  if (!user) {
    error(res, 401, "Session expired.");
    return;
  }
  if (!user.isAdmin) {
    error(res, 403, "Admin access required.");
    return;
  }

  const users = await listAppUsers();
  res.json(
    MobileAdminUsersResponse.parse({
      users: users
        .sort((left, right) => right.id.localeCompare(left.id))
        .map(publicUser),
    }),
  );
});

router.post("/mobile/admin/users/:uid/grant", async (req, res): Promise<void> => {
  const user = await userFromRequest(req);
  if (!user) {
    error(res, 401, "Session expired.");
    return;
  }
  if (!user.isAdmin) {
    error(res, 403, "Admin access required.");
    return;
  }

  const params = MobileAdminGrantDaysParams.safeParse(req.params);
  const parsed = MobileAdminGrantDaysBody.safeParse(req.body);
  if (!params.success || !parsed.success || !Number.isInteger(parsed.data.days)) {
    error(res, 400, "Enter between 1 and 3650 whole days.");
    return;
  }

  const target = (await listAppUsers()).find((candidate) => candidate.id === params.data.uid);
  if (!target) {
    error(res, 404, "User not found.");
    return;
  }

  const now = new Date();
  const updated = await updateAppUser(target, {
    subscriptionUntil: new Date(
      Math.max(target.subscriptionUntil.getTime(), now.getTime()) +
        parsed.data.days * ONE_DAY_MS,
    ),
  });

  res.json(
    MobileAdminGrantDaysResponse.parse({
      user: publicUser(updated),
    }),
  );
});

export default router;