import {
  createHmac,
  randomBytes,
  randomUUID,
  scryptSync,
  timingSafeEqual,
} from "node:crypto";
import type { User } from "@workspace/db";

const SESSION_TTL_MS = 30 * 24 * 60 * 60 * 1000;
const PASSWORD_KEY_LENGTH = 64;

function sessionSecret(): string {
  const secret = process.env.SESSION_SECRET;
  if (!secret) {
    throw new Error("SESSION_SECRET must be configured for mobile sessions.");
  }
  return secret;
}

export function normalizeEmail(email: string): string {
  return email.trim().toLowerCase();
}

export function hashPassword(password: string): string {
  const salt = randomBytes(16).toString("hex");
  const hash = scryptSync(password, salt, PASSWORD_KEY_LENGTH).toString("hex");
  return `${salt}:${hash}`;
}

export function verifyPassword(password: string, storedHash: string): boolean {
  const [salt, expectedHex] = storedHash.split(":");
  if (!salt || !expectedHex) return false;

  try {
    const actual = scryptSync(password, salt, PASSWORD_KEY_LENGTH);
    const expected = Buffer.from(expectedHex, "hex");
    return actual.length === expected.length && timingSafeEqual(actual, expected);
  } catch {
    return false;
  }
}

export function createSessionToken(userId: string): string {
  const payload = `${userId}.${Date.now() + SESSION_TTL_MS}`;
  const encodedPayload = Buffer.from(payload).toString("base64url");
  const signature = createHmac("sha256", sessionSecret())
    .update(encodedPayload)
    .digest("base64url");
  return `${encodedPayload}.${signature}`;
}

export function getUserIdFromSessionToken(token: string): string | null {
  const [encodedPayload, receivedSignature] = token.split(".");
  if (!encodedPayload || !receivedSignature) return null;

  const expectedSignature = createHmac("sha256", sessionSecret())
    .update(encodedPayload)
    .digest();
  const actualSignature = Buffer.from(receivedSignature, "base64url");
  if (
    actualSignature.length !== expectedSignature.length ||
    !timingSafeEqual(actualSignature, expectedSignature)
  ) {
    return null;
  }

  const decoded = Buffer.from(encodedPayload, "base64url").toString("utf8");
  const separator = decoded.lastIndexOf(".");
  if (separator <= 0) return null;

  const userId = decoded.slice(0, separator);
  const expiresAt = Number(decoded.slice(separator + 1));
  if (!userId || !Number.isFinite(expiresAt) || expiresAt <= Date.now()) {
    return null;
  }
  return userId;
}

export function newUserId(): string {
  return randomUUID();
}

export function newReferralCode(): string {
  return randomBytes(5).toString("hex").toUpperCase();
}

export function publicUser(user: User) {
  const subscriptionUntil = user.subscriptionUntil;
  return {
    uid: user.id,
    name: user.name,
    email: user.email,
    referralCode: user.referralCode,
    subscriptionUntil,
    subscriptionActive: subscriptionUntil.getTime() > Date.now(),
    rewardProgress: user.rewardProgress,
    totalAdsWatched: user.totalAdsWatched,
    isAdmin: user.isAdmin,
  };
}