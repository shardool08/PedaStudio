import type { PlanMode } from "@/lib/plan-prompt";
import {
  emptyUsage,
  getPilotTierOverride,
  MAX_TRIAL_DAYS,
  TIER_FEATURES,
  TIER_LIMITS,
  TIER_LABELS,
  type TierId,
  type TierFeatures,
  type TierLimits,
  type UsageCounterKey,
  type UsageSnapshot,
} from "@/lib/tier-config";
import {
  bumpUsage,
  ensureTeacher,
  getTeacher,
  isTeacherStoreReady,
  tierFromRecord,
  updateTeacher,
  usageFromRecord,
} from "@/lib/supabase/teachers";

export interface TeacherAccountSnapshot {
  tier: TierId;
  tierLabel: string;
  limits: TierLimits;
  features: TierFeatures;
  usage: UsageSnapshot;
  plansRemaining: number | null;
  scansRemaining: number | null;
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

export async function getTeacherAccount(uid: string): Promise<TeacherAccountSnapshot> {
  await ensureMaxTrial(uid);
  const record = isDevUid(uid) ? null : await getTeacher(uid);
  const tier = resolveTier(uid, record);
  const limits = TIER_LIMITS[tier];
  const features = TIER_FEATURES[tier];
  const usage = isDevUid(uid) ? emptyUsage() : usageFromRecord(record);
  const plansRemaining =
    limits.plansPerWeek === null ? null : Math.max(0, limits.plansPerWeek - usage.plans);
  const scansRemaining =
    limits.scansPerWeek === null ? null : Math.max(0, limits.scansPerWeek - usage.scans);

  return {
    tier,
    tierLabel: TIER_LABELS[tier],
    limits,
    features,
    usage,
    plansRemaining,
    scansRemaining,
  };
}

function resolveTier(uid: string, record: Awaited<ReturnType<typeof getTeacher>>): TierId {
  const pilotTier = getPilotTierOverride();
  if (pilotTier) return pilotTier;
  if (isDevUid(uid)) return "prime";
  return tierFromRecord(record);
}

/** One-time 7-day Max trial for teachers who have not paid yet. */
export async function ensureMaxTrial(uid: string): Promise<void> {
  if (isDevUid(uid) || getPilotTierOverride() || !isTeacherStoreReady()) return;

  const record = await ensureTeacher(uid);
  if (!record || record.max_trial_used) return;

  const now = Date.now();
  const tierExpiresAt = record.tier_expires_at ? new Date(record.tier_expires_at).getTime() : null;

  // Already paying: burn the trial flag so they never get a free extension later.
  if (record.razorpay_payment_id && tierExpiresAt && tierExpiresAt > now) {
    await updateTeacher(uid, { max_trial_used: true });
    return;
  }

  if (record.sub_status === "pending") return;

  const started = new Date();
  const expiresAt = new Date(started);
  expiresAt.setUTCDate(expiresAt.getUTCDate() + MAX_TRIAL_DAYS);

  await updateTeacher(uid, {
    tier: "max",
    tier_expires_at: expiresAt.toISOString(),
    max_trial_used: true,
    sub_status: "active",
    sub_plan_id: "max_trial",
    sub_billing_cycle: null,
    sub_tier: "max",
    sub_started_at: started.toISOString(),
    sub_expires_at: expiresAt.toISOString(),
  });
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

  if (limits.plansPerWeek !== null && usage.plans >= limits.plansPerWeek) {
    const upgrade = account.tier === "basic" ? "prime" : "max";
    throw new TierLimitError(
      `Weekly lesson plan limit reached (${limits.plansPerWeek} per week). Resets every Monday, or upgrade to ${TIER_LABELS[upgrade]}.`,
      "PLAN_LIMIT_REACHED",
      429,
      upgrade,
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
  plans: "plansPerWeek",
  worksheets: "worksheetsPerWeek",
  scans: "scansPerWeek",
  ocrScans: "ocrScansPerWeek",
};

export async function assertWeeklyUsageAllowed(
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
      `Weekly ${label} limit reached (${limit} per week). Resets every Monday, or upgrade for more.`,
      "USAGE_LIMIT_REACHED",
      429,
      upgradeTier,
    );
  }
  return account;
}

/** @deprecated Use assertWeeklyUsageAllowed */
export const assertMonthlyUsageAllowed = assertWeeklyUsageAllowed;

/** Prime: one worksheet per lesson plan/week. Max: unlimited. */
export async function assertWorksheetAllowed(uid: string): Promise<TeacherAccountSnapshot> {
  const account = await assertFeatureAllowed(uid, "worksheets", "prime");
  if (account.tier === "max") return account;

  const { limits, usage, tier } = account;

  if (limits.worksheetsPerWeek !== null && usage.worksheets >= limits.worksheetsPerWeek) {
    throw new TierLimitError(
      `Weekly worksheet limit reached (${limits.worksheetsPerWeek}). Resets every Monday.`,
      "USAGE_LIMIT_REACHED",
      429,
      tier === "prime" ? "max" : "prime",
    );
  }

  if (limits.worksheetsPerWeek === null) {
    if (usage.plans === 0 || usage.worksheets >= usage.plans) {
      throw new TierLimitError(
        "Includes one worksheet with each lesson plan this week. Create a lesson plan first.",
        "USAGE_LIMIT_REACHED",
        429,
        tier === "prime" ? "max" : "prime",
      );
    }
  }

  return account;
}

export async function incrementUsage(uid: string, key: UsageCounterKey, by = 1): Promise<void> {
  if (isDevUid(uid)) return;
  await bumpUsage(uid, key, by);
}

export function accountToJson(account: TeacherAccountSnapshot) {
  return {
    tier: account.tier,
    tierLabel: account.tierLabel,
    limits: account.limits,
    features: account.features,
    usage: account.usage,
    plansRemaining: account.plansRemaining,
    scansRemaining: account.scansRemaining,
  };
}
