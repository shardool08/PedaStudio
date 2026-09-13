import { getSupabaseAdmin } from "@/lib/supabase/server";
import { ensureTeacher, getTeacher, isTeacherStoreReady } from "@/lib/supabase/teachers";

/**
 * Teacher profile sync for the Android app (replaces the Firestore `users/{uid}` doc).
 * Tier, usage and subscription columns live on the same row but are server-owned, so
 * only the fields listed here can be written by a client.
 */
export interface TeacherProfile {
  teacherName: string;
  phoneNumber: string;
  language: string;
  state: string;
  district: string;
  adminType: string;
  zpName: string;
  corpName: string;
  medium: string;
  englishComfort: string;
  schoolName: string;
  location: string;
  pinCode: string;
  studentCount: number;
  internetAccess: string;
  printingAccess: string;
  teacherGrades: number[];
  teacherSubjects: string[];
  teacherResources: string[];
  currentLessons: Record<string, string>;
  profileComplete: boolean;
}

const TEXT_FIELDS = {
  teacherName: "teacher_name",
  phoneNumber: "phone_number",
  language: "language",
  state: "state",
  district: "district",
  adminType: "admin_type",
  zpName: "zp_name",
  corpName: "corp_name",
  medium: "medium",
  englishComfort: "english_comfort",
  schoolName: "school_name",
  location: "location",
  pinCode: "pin_code",
  internetAccess: "internet_access",
  printingAccess: "printing_access",
} as const;

export async function readProfile(uid: string): Promise<TeacherProfile | null> {
  const row = await getTeacher(uid);
  if (!row) return null;

  const columns = row as unknown as Record<string, unknown>;
  const text = (column: string): string => String(columns[column] ?? "");

  return {
    teacherName: text(TEXT_FIELDS.teacherName),
    phoneNumber: text(TEXT_FIELDS.phoneNumber),
    language: text(TEXT_FIELDS.language),
    state: text(TEXT_FIELDS.state),
    district: text(TEXT_FIELDS.district),
    adminType: text(TEXT_FIELDS.adminType),
    zpName: text(TEXT_FIELDS.zpName),
    corpName: text(TEXT_FIELDS.corpName),
    medium: text(TEXT_FIELDS.medium),
    englishComfort: text(TEXT_FIELDS.englishComfort),
    schoolName: text(TEXT_FIELDS.schoolName),
    location: text(TEXT_FIELDS.location),
    pinCode: text(TEXT_FIELDS.pinCode),
    internetAccess: text(TEXT_FIELDS.internetAccess),
    printingAccess: text(TEXT_FIELDS.printingAccess),
    studentCount: Number(row.student_count) || 0,
    teacherGrades: row.teacher_grades ?? [],
    teacherSubjects: (columns.teacher_subjects as string[] | undefined) ?? [],
    teacherResources: row.teacher_resources ?? [],
    currentLessons: (columns.current_lessons as Record<string, string> | undefined) ?? {},
    profileComplete: Boolean(row.profile_complete),
  };
}

function stringArray(value: unknown): string[] | undefined {
  if (!Array.isArray(value)) return undefined;
  return value.map(String);
}

/** Builds a column patch from client JSON, ignoring anything not in the whitelist. */
export function profilePatchFromJson(body: Record<string, unknown>): Record<string, unknown> {
  const patch: Record<string, unknown> = {};

  for (const [camel, column] of Object.entries(TEXT_FIELDS)) {
    const value = body[camel];
    if (typeof value === "string") patch[column] = value;
  }

  if (body.studentCount !== undefined) {
    const count = Number(body.studentCount);
    if (Number.isFinite(count)) patch.student_count = Math.max(0, Math.trunc(count));
  }

  if (Array.isArray(body.teacherGrades)) {
    patch.teacher_grades = body.teacherGrades
      .map((g) => Number(g))
      .filter((g) => Number.isInteger(g) && g >= 1 && g <= 12);
  }

  const subjects = stringArray(body.teacherSubjects);
  if (subjects) patch.teacher_subjects = subjects;

  const resources = stringArray(body.teacherResources);
  if (resources) patch.teacher_resources = resources;

  if (body.currentLessons && typeof body.currentLessons === "object") {
    const lessons: Record<string, string> = {};
    for (const [key, value] of Object.entries(body.currentLessons as Record<string, unknown>)) {
      if (typeof value === "string" && value) lessons[key] = value;
    }
    patch.current_lessons = lessons;
  }

  if (typeof body.profileComplete === "boolean") patch.profile_complete = body.profileComplete;

  return patch;
}

export async function writeProfile(
  uid: string,
  patch: Record<string, unknown>,
): Promise<TeacherProfile | null> {
  if (!isTeacherStoreReady()) return null;
  await ensureTeacher(uid);

  if (Object.keys(patch).length > 0) {
    const { error } = await getSupabaseAdmin().from("teachers").update(patch).eq("id", uid);
    if (error) throw new Error(`Could not save profile: ${error.message}`);
  }
  return readProfile(uid);
}
