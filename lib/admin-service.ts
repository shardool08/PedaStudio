import type { TeacherSummary, UserDocument } from "@/lib/schema";
import { parseSubscription } from "@/lib/subscription-service";
import { getSupabaseAdmin } from "@/lib/supabase/server";
import {
  ensureTeacher,
  getTeacher,
  isTeacherStoreReady,
  tierFromRecord,
  usageFromRecord,
  type TeacherRecord,
} from "@/lib/supabase/teachers";
import {
  currentUsageWeek,
  emptyUsage,
  TIER_LABELS,
  type TierId,
  type UsageSnapshot,
} from "@/lib/tier-config";

function db() {
  if (!isTeacherStoreReady()) throw new Error("Database unavailable");
  return getSupabaseAdmin();
}

const SUMMARY_COLUMNS =
  "id,teacher_name,phone_number,district,school_name,medium,language,teacher_grades,student_count,tier,tier_expires_at,profile_complete,usage_week,usage_plans,usage_worksheets,usage_scans,usage_ocr_scans,updated_at,created_at";

function rowToSummary(
  row: TeacherRecord,
  counts: { plans: number; assessments: number },
): TeacherSummary {
  return {
    uid: row.id,
    teacherName: row.teacher_name || "—",
    phoneNumber: row.phone_number || "—",
    district: row.district || "—",
    schoolName: row.school_name || "—",
    medium: row.medium || "—",
    tier: tierFromRecord(row),
    tierExpiresAt: row.tier_expires_at,
    profileComplete: Boolean(row.profile_complete),
    usage: usageFromRecord(row),
    planCount: counts.plans,
    assessmentCount: counts.assessments,
    updatedAt: row.updated_at ?? null,
    createdAt: row.created_at ?? null,
  };
}

async function countFor(table: "plans" | "assessments", teacherId: string): Promise<number> {
  const { count } = await db()
    .from(table)
    .select("id", { count: "exact", head: true })
    .eq("teacher_id", teacherId);
  return count ?? 0;
}

export interface DashboardStats {
  totalTeachers: number;
  profileComplete: number;
  tierCounts: Record<TierId, number>;
  usageThisMonth: {
    plans: number;
    worksheets: number;
    scans: number;
    ocrScans: number;
  };
  catalog: {
    tlmResources: number;
    flashcardLessons: number;
  };
}

export interface TeacherDetail extends TeacherSummary {
  profile: Partial<UserDocument>;
  subscription: ReturnType<typeof parseSubscription>;
  recentPlans: Array<{
    id: string;
    lessonId: string;
    day: number;
    status: string;
    updatedAt: string | null;
  }>;
  recentAssessments: Array<{
    id: string;
    type: string;
    grade: number;
    scorePercent: number;
    groupId?: string;
    groupName?: string;
    studentsAssessed: number;
    weakItems: string[];
    strands: Array<{ label: string; percent: number }>;
    updatedAt: string;
  }>;
  payments: Array<{
    id: string;
    tier: string;
    amountPaise: number;
    createdAt: string | null;
  }>;
}

export interface ListTeachersResult {
  teachers: TeacherSummary[];
  nextCursor: string | null;
  total: number;
}

export async function getDashboardStats(): Promise<DashboardStats> {
  const week = currentUsageWeek();

  const { data, error } = await db()
    .from("teachers")
    .select("tier,tier_expires_at,profile_complete,usage_week,usage_plans,usage_worksheets,usage_scans,usage_ocr_scans");
  if (error) throw new Error(error.message);

  const rows = (data ?? []) as unknown as TeacherRecord[];
  const tierCounts: Record<TierId, number> = { basic: 0, prime: 0, max: 0 };
  let profileComplete = 0;
  const usageThisMonth = { plans: 0, worksheets: 0, scans: 0, ocrScans: 0 };

  for (const row of rows) {
    tierCounts[tierFromRecord(row)]++;
    if (row.profile_complete) profileComplete++;
    if (row.usage_week !== week) continue;
    usageThisMonth.plans += Number(row.usage_plans) || 0;
    usageThisMonth.worksheets += Number(row.usage_worksheets) || 0;
    usageThisMonth.scans += Number(row.usage_scans) || 0;
    usageThisMonth.ocrScans += Number(row.usage_ocr_scans) || 0;
  }

  const [tlm, flashcards] = await Promise.all([
    db().from("tlm_resources").select("id", { count: "exact", head: true }),
    db().from("flashcard_lessons").select("lesson_id", { count: "exact", head: true }),
  ]);

  return {
    totalTeachers: rows.length,
    profileComplete,
    tierCounts,
    usageThisMonth,
    catalog: {
      tlmResources: tlm.count ?? 0,
      flashcardLessons: flashcards.count ?? 0,
    },
  };
}

export async function listTeachers(options: {
  limit?: number;
  cursor?: string;
  search?: string;
  tier?: TierId;
}): Promise<ListTeachersResult> {
  const limit = Math.min(options.limit ?? 50, 100);
  const offset = Math.max(0, parseInt(options.cursor || "0", 10) || 0);
  const search = options.search?.trim();

  let query = db()
    .from("teachers")
    .select(SUMMARY_COLUMNS, { count: "exact" })
    .order("updated_at", { ascending: false })
    .range(offset, offset + limit);

  if (options.tier) query = query.eq("tier", options.tier);
  if (search) {
    const term = `%${search}%`;
    query = query.or(
      `teacher_name.ilike.${term},phone_number.ilike.${term},district.ilike.${term},school_name.ilike.${term}`,
    );
  }

  const { data, error, count } = await query;
  if (error) throw new Error(error.message);

  const rows = (data ?? []) as unknown as TeacherRecord[];
  const hasMore = rows.length > limit;
  const page = rows.slice(0, limit);

  const teachers = await Promise.all(
    page.map(async (row) =>
      rowToSummary(row, {
        plans: await countFor("plans", row.id),
        assessments: await countFor("assessments", row.id),
      }),
    ),
  );

  return {
    teachers,
    nextCursor: hasMore ? String(offset + limit) : null,
    total: count ?? teachers.length,
  };
}

export async function getTeacherDetail(uid: string): Promise<TeacherDetail | null> {
  const teacher = await getTeacher(uid);
  if (!teacher) return null;

  const [plansRes, assessRes, paymentsRes, planCount, assessmentCount] = await Promise.all([
    db()
      .from("plans")
      .select("id,lesson_id,day,status,updated_at")
      .eq("teacher_id", uid)
      .order("updated_at", { ascending: false })
      .limit(10),
    db()
      .from("assessments")
      .select("id,type,grade,score_percent,group_id,group_name,students_assessed,weak_items,strand_scores,updated_at")
      .eq("teacher_id", uid)
      .order("updated_at", { ascending: false })
      .limit(10),
    db()
      .from("payments")
      .select("id,tier,amount_paise,created_at")
      .eq("teacher_id", uid)
      .order("created_at", { ascending: false })
      .limit(10),
    countFor("plans", uid),
    countFor("assessments", uid),
  ]);

  const summary = rowToSummary(teacher, { plans: planCount, assessments: assessmentCount });

  return {
    ...summary,
    profile: {
      language: teacher.language,
      teacherGrades: teacher.teacher_grades,
      studentCount: teacher.student_count,
    } as Partial<UserDocument>,
    subscription: parseSubscription(teacher),
    recentPlans: (plansRes.data ?? []).map((p) => ({
      id: String(p.id),
      lessonId: String(p.lesson_id || ""),
      day: Number(p.day) || 0,
      status: String(p.status || "not_started"),
      updatedAt: (p.updated_at as string) ?? null,
    })),
    recentAssessments: (assessRes.data ?? []).map((a) => {
      const strands = (a.strand_scores as Array<{ label?: string; percent?: number }> | null) ?? [];
      return {
        id: String(a.id),
        type: String(a.type || ""),
        grade: Number(a.grade) || 0,
        scorePercent: Number(a.score_percent) || 0,
        groupId: a.group_id ? String(a.group_id) : undefined,
        groupName: a.group_name ? String(a.group_name) : undefined,
        studentsAssessed: Number(a.students_assessed) || 0,
        weakItems: Array.isArray(a.weak_items) ? (a.weak_items as string[]) : [],
        strands: strands.map((s) => ({
          label: String(s.label || ""),
          percent: Number(s.percent) || 0,
        })),
        updatedAt: String(a.updated_at || ""),
      };
    }),
    payments: (paymentsRes.data ?? []).map((p) => ({
      id: String(p.id),
      tier: String(p.tier || ""),
      amountPaise: Number(p.amount_paise) || 0,
      createdAt: (p.created_at as string) ?? null,
    })),
  };
}

function tierPatch(tier: TierId, days?: number) {
  if (tier === "basic") return { tier, tier_expires_at: null };
  if (!days) return { tier, tier_expires_at: null };
  const expires = new Date();
  expires.setUTCDate(expires.getUTCDate() + days);
  return { tier, tier_expires_at: expires.toISOString() };
}

export async function setAllTeachersTier(tier: TierId, days?: number): Promise<number> {
  const { data, error } = await db()
    .from("teachers")
    .update(tierPatch(tier, days))
    .neq("id", "")
    .select("id");
  if (error) throw new Error(error.message);
  return (data ?? []).length;
}

export async function setTeacherTier(uid: string, tier: TierId, days?: number): Promise<void> {
  await ensureTeacher(uid);
  const { error } = await db().from("teachers").update(tierPatch(tier, days)).eq("id", uid);
  if (error) throw new Error(error.message);
}

export async function resetTeacherUsage(uid: string): Promise<void> {
  const usage: UsageSnapshot = emptyUsage();
  const { error } = await db()
    .from("teachers")
    .update({
      usage_week: usage.week,
      usage_plans: 0,
      usage_worksheets: 0,
      usage_scans: 0,
      usage_ocr_scans: 0,
    })
    .eq("id", uid);
  if (error) throw new Error(error.message);
}

export async function ensureUserDefaults(uid: string): Promise<void> {
  await ensureTeacher(uid);
}

export async function getCatalogStatus() {
  const [tlm, flashcards] = await Promise.all([
    db().from("tlm_resources").select("id,updated_at"),
    db().from("flashcard_lessons").select("lesson_id,title,cards,updated_at"),
  ]);

  const tlmRows = tlm.data ?? [];
  const fcRows = flashcards.data ?? [];
  const latestTlm = tlmRows
    .map((r) => String(r.updated_at || ""))
    .sort()
    .at(-1);

  return {
    tlm: {
      exists: tlmRows.length > 0,
      count: tlmRows.length,
      updatedAt: latestTlm || null,
    },
    flashcards: {
      lessonCount: fcRows.length,
      lessons: fcRows.map((d) => ({
        id: String(d.lesson_id),
        lessonId: String(d.lesson_id),
        title: String(d.title || ""),
        cardCount: Array.isArray(d.cards) ? d.cards.length : 0,
        updatedAt: (d.updated_at as string) ?? null,
      })),
    },
  };
}

export { TIER_LABELS };
