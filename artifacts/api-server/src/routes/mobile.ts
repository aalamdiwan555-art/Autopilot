import { Router, type IRouter, type Request, type Response } from "express";
import { desc, eq } from "drizzle-orm";
import {
  db,
  usersTable,
  type User,
} from "@workspace/db";
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
  createSessionToken,
  getUserIdFromSessionToken,
  hashPassword,
  newReferralCode,
  newUserId,
  normalizeEmail,
  publicUser,
  verifyPassword,
} from "../lib/mobile-auth";

const router: IRouter = Router();
const ONE_HOUR_MS = 3_600_000;
const ONE_DAY_MS = 86_400_000;
const REWARD_THRESHOLD = 10;

function error(res: Response, status: number, message: string): void {
  res.status(status).json({ error: message });
}

async function userFromRequest(req: Request): Promise<User | null> {
  const header = req.get("authorization") ?? "";
  if (!header.toLowerCase().startsWith("bearer ")) return null;
  const token = header.slice(7).trim();
  const userId = getUserIdFromSessionToken(token);
  if (!userId) return null;
  const [user] = await db.select().from(usersTable).where(eq(usersTable.id, userId)).limit(1);
  return user ?? null;
}

async function uniqueReferralCode(): Promise<string> {
  for (let attempt = 0; attempt < 5; attempt += 1) {
    const code = newReferralCode();
    const [existing] = await db
      .select({ id: usersTable.id })
      .from(usersTable)
      .where(eq(usersTable.referralCode, code))
      .limit(1);
    if (!existing) return code;
  }
  throw new Error("Could not allocate a referral code.");
}

function validEmail(email: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
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

  const [existing] = await db.select().from(usersTable).where(eq(usersTable.email, email)).limit(1);
  if (existing) {
    error(res, 409, "An account with this email already exists.");
    return;
  }

  let referrer: User | undefined;
  if (referralCode) {
    [referrer] = await db
      .select()
      .from(usersTable)
      .where(eq(usersTable.referralCode, referralCode))
      .limit(1);
    if (!referrer) {
      error(res, 400, "That referral code is not valid.");
      return;
    }
  }

  const now = new Date();
  const userValues = {
    id: newUserId(),
    name,
    email,
    passwordHash: hashPassword(parsed.data.password),
    referralCode: await uniqueReferralCode(),
    referredById: referrer?.id ?? null,
    subscriptionUntil: new Date(now.getTime() + ONE_HOUR_MS),
    rewardProgress: 0,
    totalAdsWatched: 0,
    isAdmin:
      email === normalizeEmail(process.env.ADMIN_EMAIL ?? "aalamdiwan555@gmail.com"),
  };

  const created = await db.transaction(async (tx) => {
    const [user] = await tx.insert(usersTable).values(userValues).returning();
    if (referrer) {
      const currentUntil = Math.max(referrer.subscriptionUntil.getTime(), now.getTime());
      await tx
        .update(usersTable)
        .set({ subscriptionUntil: new Date(currentUntil + 2 * ONE_DAY_MS) })
        .where(eq(usersTable.id, referrer.id));
    }
    return user;
  });

  const response = MobileSignupResponse.parse({
    token: createSessionToken(created.id),
    user: publicUser(created),
  });
  res.status(201).json(response);
});

router.post("/mobile/auth/login", async (req, res): Promise<void> => {
  const parsed = MobileLoginBody.safeParse(req.body);
  if (!parsed.success) {
    error(res, 400, "Enter a valid email and password.");
    return;
  }

  const email = normalizeEmail(parsed.data.email);
  const [user] = await db.select().from(usersTable).where(eq(usersTable.email, email)).limit(1);
  if (!user || !verifyPassword(parsed.data.password, user.passwordHash)) {
    error(res, 401, "Email or password is incorrect.");
    return;
  }

  const response = MobileLoginResponse.parse({
    token: createSessionToken(user.id),
    user: publicUser(user),
  });
  res.json(response);
});

router.get("/mobile/me", async (req, res): Promise<void> => {
  const user = await userFromRequest(req);
  if (!user) {
    error(res, 401, "Session expired.");
    return;
  }
  res.json(MobileMeResponse.parse({ user: publicUser(user) }));
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

  const [referrer] = await db
    .select()
    .from(usersTable)
    .where(eq(usersTable.referralCode, parsed.data.referralCode.trim().toUpperCase()))
    .limit(1);
  if (!referrer) {
    error(res, 400, "That referral code is not valid.");
    return;
  }
  if (referrer.id === user.id) {
    error(res, 400, "You cannot redeem your own code.");
    return;
  }

  const now = new Date();
  await db.transaction(async (tx) => {
    await tx
      .update(usersTable)
      .set({ referredById: referrer.id })
      .where(eq(usersTable.id, user.id));
    const currentUntil = Math.max(referrer.subscriptionUntil.getTime(), now.getTime());
    await tx
      .update(usersTable)
      .set({ subscriptionUntil: new Date(currentUntil + 2 * ONE_DAY_MS) })
      .where(eq(usersTable.id, referrer.id));
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
  const update = {
    rewardProgress: completedReward ? 0 : nextProgress,
    totalAdsWatched: user.totalAdsWatched + 1,
    ...(completedReward
      ? {
          subscriptionUntil: new Date(
            Math.max(user.subscriptionUntil.getTime(), now.getTime()) + ONE_DAY_MS,
          ),
        }
      : {}),
  };
  const [updated] = await db
    .update(usersTable)
    .set(update)
    .where(eq(usersTable.id, user.id))
    .returning();

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

  const users = await db.select().from(usersTable).orderBy(desc(usersTable.createdAt));
  res.json(
    MobileAdminUsersResponse.parse({
      users: users.map(publicUser),
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

  const [target] = await db
    .select()
    .from(usersTable)
    .where(eq(usersTable.id, params.data.uid))
    .limit(1);
  if (!target) {
    error(res, 404, "User not found.");
    return;
  }

  const now = new Date();
  const [updated] = await db
    .update(usersTable)
    .set({
      subscriptionUntil: new Date(
        Math.max(target.subscriptionUntil.getTime(), now.getTime()) +
          parsed.data.days * ONE_DAY_MS,
      ),
    })
    .where(eq(usersTable.id, target.id))
    .returning();

  res.json(
    MobileAdminGrantDaysResponse.parse({
      user: publicUser(updated),
    }),
  );
});

export default router;