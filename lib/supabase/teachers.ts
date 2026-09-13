import type { PostgrestError } from "@supabase/supabase-js";
import { getSupabaseAdmin, isSupabaseConfigured } from "@/lib/supabase/server";
import type { TierId, UsageCounterKey, UsageSnapshot } from "@/lib/tier-config";
import { currentUsageWeek, emptyUsage, normalizeTierId } from "@/lib/tier-config";

/**
 * Teacher store backed by Supabase Postgres (replaces the old Firestore `users/{uid}` doc).
 * `id` is the uid from the ID token (Supabase auth uuid; older Firebase uids still fit).
 */
export interface TeacherRecord {
  id: string;
  teacher_name: string;
  phone_number: string;
  language: string;
  medium: string;
  school_name: string;
  district: string;
  teacher_grades: number[];
  teacher_resources: string[];
  student_count: number;
  profile_complete: boolean;
  created_at?: string;
  updated_at?: string;

  tier: TierId;
  tier_expires_at: string | null;
  max_trial_used: boolean;

  usage_week: string;
  usage_plans: number;
  usage_worksheets: number;
  usage_scans: number;
  usage_ocr_scans: number;

  sub_status: "none" | "active" | "expired" | "pending";
  sub_plan_id: string | null;
  sub_billing_cycle: "monthly" | "yearly" | null;
  sub_tier: TierId;
  sub_started_at: string | null;
  sub_expires_at: string | null;
  razorpay_order_id: string | null;
  razorpay_payment_id: string | null;
}

const USAGE_COLUMN: Record<UsageCounterKey, keyof TeacherRecord> = {
  plans: "usage_plans",
  worksheets: "usage_worksheets",
  scans: "usage_scans",
  ocrScans: "usage_ocr_scans",
};

export function isTeacherStoreReady(): boolean {
  return isSupabaseConfigured();
}

/**
 * Reads and writes here degrade instead of throwing: a database problem should cost the
 * teacher their tier/usage sync, not their lesson plan. `/api/health` reports the real
 * state so failures stay visible.
 */
function warn(context: string, error: PostgrestError): null {
  console.error(`Supabase ${context} failed [${error.code}]: ${error.message}`);
  return null;
}

export async function getTeacher(uid: string): Promise<TeacherRecord | null> {
  if (!isTeacherStoreReady()) return null;
  const { data, error } = await getSupabaseAdmin()
    .from("teachers")
    .select("*")
    .eq("id", uid)
    .maybeSingle();
  if (error) return warn("teacher read", error);
  return (data as TeacherRecord | null) ?? null;
}

/** Create the row on first API call — no auth trigger exists any more. */
export async function ensureTeacher(
  uid: string,
  extras?: Partial<Pick<TeacherRecord, "phone_number" | "teacher_name">>,
): Promise<TeacherRecord | null> {
  if (!isTeacherStoreReady()) return null;
  const existing = await getTeacher(uid);
  if (existing) return existing;

  const { data, error } = await getSupabaseAdmin()
    .from("teachers")
    .upsert({ id: uid, ...extras }, { onConflict: "id" })
    .select("*")
    .single();
  if (error) return warn("teacher create", error);
  return data as TeacherRecord;
}

export async function updateTeacher(
  uid: string,
  patch: Partial<Omit<TeacherRecord, "id">>,
): Promise<void> {
  if (!isTeacherStoreReady()) return;
  const { error } = await getSupabaseAdmin()
    .from("teachers")
    .upsert({ id: uid, ...patch }, { onConflict: "id" });
  if (error) warn("teacher update", error);
}

/** Effective tier, with expiry applied. */
export function tierFromRecord(record: TeacherRecord | null): TierId {
  if (!record) return "basic";
  if (record.tier_expires_at && new Date(record.tier_expires_at).getTime() < Date.now()) {
    return "basic";
  }
  return normalizeTierId(record.tier);
}

/** Usage for the current week; a stale week reads as zeroes. */
export function usageFromRecord(record: TeacherRecord | null): UsageSnapshot {
  const week = currentUsageWeek();
  if (!record || record.usage_week !== week) return emptyUsage(week);
  return {
    week,
    plans: Number(record.usage_plans) || 0,
    worksheets: Number(record.usage_worksheets) || 0,
    scans: Number(record.usage_scans) || 0,
    ocrScans: Number(record.usage_ocr_scans) || 0,
  };
}

export async function bumpUsage(uid: string, key: UsageCounterKey, by = 1): Promise<void> {
  if (!isTeacherStoreReady()) return;
  const record = await ensureTeacher(uid);
  const usage = usageFromRecord(record);
  const week = currentUsageWeek();

  await updateTeacher(uid, {
    usage_week: week,
    usage_plans: usage.plans,
    usage_worksheets: usage.worksheets,
    usage_scans: usage.scans,
    usage_ocr_scans: usage.ocrScans,
    [USAGE_COLUMN[key]]: usage[key] + by,
  } as Partial<Omit<TeacherRecord, "id">>);
}

export async function findTeacherByOrderId(orderId: string): Promise<string | null> {
  if (!isTeacherStoreReady()) return null;
  const { data, error } = await getSupabaseAdmin()
    .from("teachers")
    .select("id")
    .eq("razorpay_order_id", orderId)
    .limit(1)
    .maybeSingle();
  if (error) return warn("order lookup", error);
  return (data as { id: string } | null)?.id ?? null;
}

export async function paymentRecorded(uid: string, paymentId: string): Promise<boolean> {
  if (!isTeacherStoreReady()) return false;
  const { data, error } = await getSupabaseAdmin()
    .from("payments")
    .select("id")
    .eq("teacher_id", uid)
    .eq("payment_id", paymentId)
    .limit(1)
    .maybeSingle();
  if (error) {
    warn("payment lookup", error);
    return false;
  }
  return Boolean(data);
}

export async function recordPayment(payment: {
  teacherId: string;
  planId: string;
  tier: TierId;
  billingCycle: "monthly" | "yearly";
  amountPaise: number;
  orderId: string;
  paymentId: string;
}): Promise<void> {
  if (!isTeacherStoreReady()) return;
  const { error } = await getSupabaseAdmin()
    .from("payments")
    .upsert(
      {
        teacher_id: payment.teacherId,
        plan_id: payment.planId,
        tier: payment.tier,
        billing_cycle: payment.billingCycle,
        amount_paise: payment.amountPaise,
        order_id: payment.orderId,
        payment_id: payment.paymentId,
      },
      { onConflict: "payment_id" },
    );
  // A payment that cannot be recorded must surface — it affects what the teacher paid for.
  if (error) throw new Error(`Could not record payment: ${error.message}`);
}
