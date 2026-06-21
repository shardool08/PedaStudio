import { getFirestore } from "firebase-admin/firestore";
import type { PlanMode } from "@/lib/plan-prompt";
import {
  currentUsageMonth,
  emptyUsage,
  normalizeTierId,
  TIER_FEATURES,
  TIER_LIMITS,
  TIER_LABELS,
  type TierId,
  type TierFeatures,
  type TierLimits,
  type UsageCounterKey,
  type UsageSnapshot,
} from "@/lib/tier-config";
import { getAdminApp } from "@/lib/firebase/admin";

export interface TeacherAccountSnapshot {
  tier: TierId;
  tierLabel: string;
  limits: TierLimits;
  features: TierFeatures;
  usage: UsageSnapshot;
  plansRemaining: number | null;
}

export class TierLimitError extends Error {
  code: string;
  status: number;
  upgradeTier: TierId;

  constructor(message: string, code: string, status: number, upgradeTier: TierId = "prime") {
    super(message);
    this.code = code;
    this.status = status;
    this.upgradeTier = upgradeTier;
  }
}

function isDevUid(uid: string): boolean {
  return uid === "dev-local";
}

function getDb() {
  const app = getAdminApp();
  if (!app) return null;
  return getFirestore(app);
}

function mergeUsage(raw: Record<string, unknown> | undefined): UsageSnapshot {
  const month = currentUsageMonth();
  if (!raw || raw.month !== month) return emptyUsage(month);
  return {
    month,
    plans: Number(raw.plans) || 0,
    worksheets: Number(raw.worksheets) || 0,
    scans: Number(raw.scans) || 0,
    ocrScans: Number(raw.ocrScans) || 0,
  };
}

export async function getTeacherAccount(uid: string): Promise<TeacherAccountSnapshot> {
  const tier = await readTier(uid);
  const limits = TIER_LIMITS[tier];
  const features = TIER_FEATURES[tier];
  const usage = await readUsage(uid);
  const plansRemaining =
    limits.plansPerMonth === null ? null : Math.max(0, limits.plansPerMonth - usage.plans);

  return {
    tier,
    tierLabel: TIER_LABELS[tier],
    limits,
    features,
    usage,
    plansRemaining,
  };
}

async function readTier(uid: string): Promise<TierId> {
  if (isDevUid(uid)) return "prime";
  const db = getDb();
  if (!db) return "basic";
  const snap = await db.collection("users").doc(uid).get();
  if (!snap.exists) return "basic";
  const data = snap.data();
  const expiresAt = data?.tierExpiresAt?.toDate?.() as Date | undefined;
  if (expiresAt && expiresAt.getTime() < Date.now()) return "basic";
  return normalizeTierId(data?.tier);
}

async function readUsage(uid: string): Promise<UsageSnapshot> {
  if (isDevUid(uid)) return emptyUsage();
  const db = getDb();
  if (!db) return emptyUsage();
  const snap = await db.collection("users").doc(uid).get();
  if (!snap.exists) return emptyUsage();
  return mergeUsage(snap.data()?.usage as Record<string, unknown> | undefined);
}

export async function assertPlanGenerationAllowed(
  uid: string,
  mode: PlanMode,
  afterUnitTest = false,
): Promise<TeacherAccountSnapshot> {
  const account = await getTeacherAccount(uid);
  const { limits, features, usage } = account;

  if (mode && mode !== null) {
    const allowed =
      features.planModesAlways || (features.planModesAfterUnitTest && afterUnitTest);
    if (!allowed) {
      throw new TierLimitError(
        "Re-teach, practice, and continue modes need Prime or Max.",
        "PLAN_MODE_LOCKED",
        403,
        "prime",
      );
    }
  }

  if (limits.plansPerMonth !== null && usage.plans >= limits.plansPerMonth) {
    throw new TierLimitError(
      `Basic includes ${limits.plansPerMonth} lesson plans per month. Upgrade to Prime for unlimited plans.`,
      "PLAN_LIMIT_REACHED",
      429,
      "prime",
    );
  }

  return account;
}

export async function assertFeatureAllowed(
  uid: string,
  feature: keyof TierFeatures,
  upgradeTier: TierId = "prime",
): Promise<TeacherAccountSnapshot> {
  const account = await getTeacherAccount(uid);
  if (!account.features[feature]) {
    throw new TierLimitError(
      `This feature requires ${TIER_LABELS[upgradeTier]}.`,
      "FEATURE_LOCKED",
      403,
      upgradeTier,
    );
  }
  return account;
}

const LIMIT_KEY_MAP: Record<UsageCounterKey, keyof TierLimits> = {
  plans: "plansPerMonth",
  worksheets: "worksheetsPerMonth",
  scans: "scansPerMonth",
  ocrScans: "ocrScansPerMonth",
};

export async function assertMonthlyUsageAllowed(
  uid: string,
  key: UsageCounterKey,
  upgradeTier: TierId = "prime",
): Promise<TeacherAccountSnapshot> {
  const account = await getTeacherAccount(uid);
  const limit = account.limits[LIMIT_KEY_MAP[key]];
  const used = account.usage[key];
  if (limit !== null && used >= limit) {
    const label =
      key === "worksheets"
        ? "worksheets"
        : key === "scans"
          ? "textbook scans"
          : key === "ocrScans"
            ? "OCR scans"
            : "plans";
    throw new TierLimitError(
      `Monthly ${label} limit reached (${limit}). Upgrade for more.`,
      "USAGE_LIMIT_REACHED",
      429,
      upgradeTier,
    );
  }
  return account;
}

export async function incrementUsage(uid: string, key: UsageCounterKey, by = 1): Promise<void> {
  if (isDevUid(uid)) return;
  const db = getDb();
  if (!db) return;

  const ref = db.collection("users").doc(uid);
  const month = currentUsageMonth();

  await db.runTransaction(async (tx) => {
    const snap = await tx.get(ref);
    const usage = mergeUsage(snap.data()?.usage as Record<string, unknown> | undefined);
    if (usage.month !== month) {
      usage.month = month;
      usage.plans = 0;
      usage.worksheets = 0;
      usage.scans = 0;
      usage.ocrScans = 0;
    }
    usage[key] += by;
    tx.set(ref, { usage }, { merge: true });
  });
}

export function accountToJson(account: TeacherAccountSnapshot) {
  return {
    tier: account.tier,
    tierLabel: account.tierLabel,
    limits: account.limits,
    features: account.features,
    usage: account.usage,
    plansRemaining: account.plansRemaining,
  };
}
