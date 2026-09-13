import { getSupabaseAdmin } from "@/lib/supabase/server";
import { ensureTeacher, isTeacherStoreReady } from "@/lib/supabase/teachers";

/**
 * Lesson plan sync for the Android app (replaces `users/{uid}/plans` in Firestore).
 * Timestamps cross the wire as epoch millis because that is what the app stores locally.
 */
export interface PlanSyncRecord {
  lessonId: string;
  day: number;
  plan: Record<string, unknown> | null;
  selections: Record<string, string>;
  status: "not_started" | "planned" | "completed";
  feedback: string | null;
  savedAt: number;
  completedAt: number;
  updatedAt: number;
}

const PLAN_STATUSES = new Set(["not_started", "planned", "completed"]);

interface PlanRow {
  lesson_id: string;
  day: number;
  plan: Record<string, unknown> | null;
  selections: Record<string, string> | null;
  status: PlanSyncRecord["status"];
  feedback: string | null;
  saved_at: string | null;
  completed_at: string | null;
  updated_at: string;
}

const millis = (iso: string | null): number => (iso ? new Date(iso).getTime() : 0);
const iso = (ms: unknown): string | null => {
  const value = Number(ms);
  return Number.isFinite(value) && value > 0 ? new Date(value).toISOString() : null;
};

function rowToRecord(row: PlanRow): PlanSyncRecord {
  return {
    lessonId: row.lesson_id,
    day: Number(row.day),
    plan: row.plan && Object.keys(row.plan).length > 0 ? row.plan : null,
    selections: row.selections ?? {},
    status: row.status,
    feedback: row.feedback || null,
    savedAt: millis(row.saved_at),
    completedAt: millis(row.completed_at),
    updatedAt: millis(row.updated_at),
  };
}

const PLAN_COLUMNS =
  "lesson_id,day,plan,selections,status,feedback,saved_at,completed_at,updated_at";

export async function listPlans(
  uid: string,
  filter?: { lessonId?: string; day?: number },
): Promise<PlanSyncRecord[]> {
  if (!isTeacherStoreReady()) return [];
  let query = getSupabaseAdmin().from("plans").select(PLAN_COLUMNS).eq("teacher_id", uid);
  if (filter?.lessonId) query = query.eq("lesson_id", filter.lessonId);
  if (filter?.day !== undefined) query = query.eq("day", filter.day);

  const { data, error } = await query;
  if (error) {
    console.error("Plan list failed:", error.message);
    return [];
  }
  return (data as unknown as PlanRow[]).map(rowToRecord);
}

export interface PlanUpsertInput {
  lessonId?: unknown;
  day?: unknown;
  plan?: unknown;
  selections?: unknown;
  status?: unknown;
  feedback?: unknown;
  savedAt?: unknown;
  completedAt?: unknown;
}

/**
 * Upserts one plan. Omitted fields keep their stored value, so the app can send a full
 * plan after generation and only status/feedback when the teacher marks a day done.
 */
export async function upsertPlan(uid: string, input: PlanUpsertInput): Promise<PlanSyncRecord> {
  if (!isTeacherStoreReady()) throw new Error("Database unavailable");

  const lessonId = String(input.lessonId ?? "").trim();
  const day = Number(input.day);
  if (!lessonId) throw new Error("lessonId is required");
  if (!Number.isInteger(day) || day < 1) throw new Error("day must be 1 or more");

  await ensureTeacher(uid);

  const patch: Record<string, unknown> = {
    teacher_id: uid,
    lesson_id: lessonId,
    day,
    updated_at: new Date().toISOString(),
  };

  if (input.plan && typeof input.plan === "object") patch.plan = input.plan;
  if (input.selections && typeof input.selections === "object") {
    const selections = input.selections as Record<string, unknown>;
    patch.selections = Object.fromEntries(
      Object.entries(selections).map(([k, v]) => [k, String(v ?? "")]),
    );
    patch.teacher_resources = String(selections.tlms ?? "");
  }
  if (typeof input.status === "string" && PLAN_STATUSES.has(input.status)) {
    patch.status = input.status;
  }
  if (typeof input.feedback === "string") patch.feedback = input.feedback || null;

  const savedAt = iso(input.savedAt);
  if (savedAt) patch.saved_at = savedAt;
  const completedAt = iso(input.completedAt);
  if (completedAt) patch.completed_at = completedAt;

  const { data, error } = await getSupabaseAdmin()
    .from("plans")
    .upsert(patch, { onConflict: "teacher_id,lesson_id,day" })
    .select(PLAN_COLUMNS)
    .single();
  if (error) throw new Error(`Could not save plan: ${error.message}`);

  return rowToRecord(data as unknown as PlanRow);
}
