import { getAssessmentGroupsForMedium, getLessonsForMedium } from "@/lib/curriculum";
import {
  buildAssessmentReport,
  getAssessmentTool,
  listAssessmentTools,
  mediumToBookTrack,
  validateTallies,
  type AssessmentTool,
  type ItemTally,
} from "@/lib/assessment-tools";
import { getSupabaseAdmin } from "@/lib/supabase/server";
import {
  ensureTeacher,
  getTeacher,
  isTeacherStoreReady,
  tierFromRecord,
} from "@/lib/supabase/teachers";
import type { TierId } from "@/lib/tier-config";
import { getPilotTierOverride } from "@/lib/tier-config";
import type { AssessmentReport } from "@/lib/assessment-tools/types";

export type AssessmentType = "baseline" | "unit" | "endline";

export interface AssessmentGroupCatalog {
  id: string;
  name: string;
  focus: string;
  lessons: string[];
  unit: number;
  /** Structured tool exists (download + item entry). */
  hasTool: boolean;
}

export interface AssessmentRecord {
  id: string;
  type: AssessmentType;
  grade: number;
  subject: string;
  groupId?: string;
  groupName?: string;
  toolId?: string;
  scorePercent: number;
  studentsAssessed: number;
  notes?: string;
  tallies?: ItemTally[];
  strandScores?: AssessmentReport["strands"];
  weakItems?: string[];
  createdAt: string;
  updatedAt: string;
}

/** Client-facing tool payload for assessor entry (includes marking keys). */
export interface AssessmentToolPayload {
  id: string;
  type: AssessmentType;
  grade: number;
  subject: string;
  bookTrack: "l1" | "l2";
  title: string;
  description: string;
  groupId?: string;
  groupName?: string;
  focus?: string;
  totalMarks: number;
  recommendedMinutes: number;
  administrationNotes: string[];
  items: {
    id: string;
    section: string;
    format: "mcq" | "subjective";
    marks: number;
    stem: string;
    flnStrand: string;
    competency: string;
    deliveryMode: string;
    lessonIds?: string[];
    options?: { key: string; text: string }[];
    correctOption?: string;
    markingScheme: string;
    stimulusNote?: string;
  }[];
}

function parseMedium(raw: unknown): string {
  const m = String(raw || "marathi").toLowerCase();
  if (m === "english" || m === "semi-english" || m === "semi_english") return "english";
  return "marathi";
}

function assessmentDocId(type: AssessmentType, groupId?: string): string {
  return type === "unit" && groupId ? `unit_${groupId}` : type;
}

function toolHasContent(
  grade: number,
  bookTrack: "l1" | "l2",
  subject: string,
  type: AssessmentType,
  groupId?: string,
): boolean {
  return !!getAssessmentTool({ grade, bookTrack, subject, type, groupId });
}

export function resolveAssessmentTool(
  grade: number,
  subject: string,
  mediumRaw: unknown,
  type: AssessmentType,
  groupId?: string,
): AssessmentTool | undefined {
  const bookTrack = mediumToBookTrack(parseMedium(mediumRaw));
  return getAssessmentTool({ grade, bookTrack, subject, type, groupId });
}

export function toolToPayload(tool: AssessmentTool): AssessmentToolPayload {
  return {
    id: tool.id,
    type: tool.type,
    grade: tool.grade,
    subject: tool.subject,
    bookTrack: tool.bookTrack,
    title: tool.title,
    description: tool.description,
    groupId: tool.groupId,
    groupName: tool.groupName,
    focus: tool.focus,
    totalMarks: tool.totalMarks,
    recommendedMinutes: tool.recommendedMinutes,
    administrationNotes: tool.administrationNotes,
    items: tool.sections.flatMap((s) =>
      s.items.map((item) => ({
        id: item.id,
        section: s.title,
        format: item.format,
        marks: item.marks,
        stem: item.stem,
        flnStrand: item.flnStrand,
        competency: item.competency,
        deliveryMode: item.deliveryMode,
        lessonIds: item.lessonIds,
        options: item.options?.map((o) => ({ key: o.key, text: o.text })),
        correctOption: item.correctOption,
        markingScheme: item.markingScheme,
        stimulusNote: item.stimulusNote,
      })),
    ),
  };
}

export function buildAssessmentCatalog(
  grade: number,
  subject: string,
  mediumRaw: unknown,
): {
  baseline: { id: string; title: string; description: string; hasTool: boolean };
  endline: { id: string; title: string; description: string; hasTool: boolean };
  unitTests: AssessmentGroupCatalog[];
} {
  const medium = parseMedium(mediumRaw);
  const bookTrack = mediumToBookTrack(medium);
  const groups = getAssessmentGroupsForMedium(grade, subject, medium);
  const lessons = getLessonsForMedium(grade, subject, medium);
  const unitTests: AssessmentGroupCatalog[] = Object.entries(groups).map(([id, g]) => {
    const unit =
      lessons.find((l) => g.lessons.includes(l.id))?.unit ??
      parseInt(g.lessons[0]?.split(".")[0] || "1", 10);
    return {
      id,
      name: g.name,
      focus: g.focus,
      lessons: g.lessons,
      unit,
      hasTool: toolHasContent(grade, bookTrack, subject, "unit", id),
    };
  });

  return {
    baseline: {
      id: "baseline",
      title: `Baseline — Grade ${grade} ${subject === "english" ? "English" : subject}`,
      description: "FLN + competency check before teaching begins.",
      hasTool: toolHasContent(grade, bookTrack, subject, "baseline"),
    },
    endline: {
      id: "endline",
      title: `Endline — Grade ${grade} ${subject === "english" ? "English" : subject}`,
      description: "Year-end competency and LO attainment check.",
      hasTool: toolHasContent(grade, bookTrack, subject, "endline"),
    },
    unitTests: unitTests.sort((a, b) => a.unit - b.unit || a.id.localeCompare(b.id)),
  };
}

/** Row shape of public.assessments. */
interface AssessmentRow {
  type: AssessmentType;
  grade: number;
  subject: string;
  group_id: string | null;
  group_name: string | null;
  tool_id: string | null;
  score_percent: number | string;
  students_assessed: number;
  notes: string | null;
  tallies: ItemTally[] | null;
  strand_scores: AssessmentReport["strands"] | null;
  weak_items: string[] | null;
  created_at: string;
  updated_at: string;
}

function rowToRecord(row: AssessmentRow, grade: number): AssessmentRecord {
  const groupId = row.group_id || undefined;
  return {
    id: assessmentDocId(row.type, groupId),
    type: row.type || "unit",
    grade: Number(row.grade) || grade,
    subject: row.subject || "english",
    groupId,
    groupName: row.group_name || undefined,
    toolId: row.tool_id ?? undefined,
    scorePercent: Number(row.score_percent) || 0,
    studentsAssessed: Number(row.students_assessed) || 0,
    notes: row.notes ?? undefined,
    tallies: row.tallies?.length ? row.tallies : undefined,
    strandScores: row.strand_scores?.length ? row.strand_scores : undefined,
    weakItems: row.weak_items?.length ? row.weak_items : undefined,
    createdAt: row.created_at,
    updatedAt: row.updated_at,
  };
}

const ASSESSMENT_COLUMNS =
  "type,grade,subject,group_id,group_name,tool_id,score_percent,students_assessed,notes,tallies,strand_scores,weak_items,created_at,updated_at";

export async function listAssessmentScores(uid: string, grade: number): Promise<AssessmentRecord[]> {
  if (uid === "dev-local" || !isTeacherStoreReady()) return [];
  const { data, error } = await getSupabaseAdmin()
    .from("assessments")
    .select(ASSESSMENT_COLUMNS)
    .eq("teacher_id", uid)
    .eq("grade", grade);
  if (error) {
    console.error("Assessment list failed:", error.message);
    return [];
  }
  return (data as unknown as AssessmentRow[]).map((row) => rowToRecord(row, grade));
}

export async function getAssessmentRecord(
  uid: string,
  type: AssessmentType,
  grade: number,
  groupId?: string,
): Promise<AssessmentRecord | null> {
  if (uid === "dev-local" || !isTeacherStoreReady()) return null;
  const { data, error } = await getSupabaseAdmin()
    .from("assessments")
    .select(ASSESSMENT_COLUMNS)
    .eq("teacher_id", uid)
    .eq("type", type)
    .eq("grade", grade)
    .eq("group_id", groupId ?? "")
    .maybeSingle();
  if (error) {
    console.error("Assessment read failed:", error.message);
    return null;
  }
  return data ? rowToRecord(data as unknown as AssessmentRow, grade) : null;
}

export async function saveAssessmentScore(
  uid: string,
  payload: {
    type: AssessmentType;
    grade: number;
    subject: string;
    groupId?: string;
    groupName?: string;
    scorePercent: number;
    studentsAssessed: number;
    notes?: string;
    toolId?: string;
    tallies?: ItemTally[];
    strandScores?: AssessmentReport["strands"];
    weakItems?: string[];
  },
): Promise<AssessmentRecord> {
  const now = new Date().toISOString();
  const id = assessmentDocId(payload.type, payload.groupId);

  if (uid === "dev-local") {
    return { id, ...payload, createdAt: now, updatedAt: now };
  }

  if (!isTeacherStoreReady()) throw new Error("Database unavailable");

  // assessments.teacher_id has an FK to teachers, so the teacher row must exist first.
  await ensureTeacher(uid);

  const scorePercent = Math.min(100, Math.max(0, payload.scorePercent));
  const studentsAssessed = Math.max(0, payload.studentsAssessed);

  const { data, error } = await getSupabaseAdmin()
    .from("assessments")
    .upsert(
      {
        teacher_id: uid,
        type: payload.type,
        grade: payload.grade,
        subject: payload.subject,
        group_id: payload.groupId ?? "",
        group_name: payload.groupName ?? "",
        tool_id: payload.toolId ?? null,
        score_percent: scorePercent,
        students_assessed: studentsAssessed,
        notes: payload.notes ?? "",
        tallies: payload.tallies ?? [],
        strand_scores:
          payload.strandScores?.map((s) => ({
            strand: s.strand,
            label: s.label,
            percent: s.percent,
          })) ?? [],
        weak_items: payload.weakItems ?? [],
        updated_at: now,
      },
      { onConflict: "teacher_id,type,grade,group_id" },
    )
    .select("created_at")
    .single();

  if (error) throw new Error(`Could not save assessment: ${error.message}`);

  return {
    id,
    type: payload.type,
    grade: payload.grade,
    subject: payload.subject,
    groupId: payload.groupId,
    groupName: payload.groupName,
    toolId: payload.toolId,
    scorePercent,
    studentsAssessed,
    notes: payload.notes,
    tallies: payload.tallies,
    strandScores: payload.strandScores,
    weakItems: payload.weakItems,
    createdAt: String(data?.created_at ?? now),
    updatedAt: now,
  };
}

/** Save with item tallies — computes score + FLN report server-side. */
export async function saveAssessmentWithTallies(
  uid: string,
  payload: {
    type: AssessmentType;
    grade: number;
    subject: string;
    medium: string;
    groupId?: string;
    groupName?: string;
    studentsAssessed: number;
    tallies: ItemTally[];
    notes?: string;
  },
): Promise<{ record: AssessmentRecord; report: AssessmentReport }> {
  const tool = resolveAssessmentTool(
    payload.grade,
    payload.subject,
    payload.medium,
    payload.type,
    payload.groupId,
  );
  if (!tool) throw new Error("Assessment tool not available for this grade");

  const errors = validateTallies(tool, {
    toolId: tool.id,
    studentsAssessed: payload.studentsAssessed,
    tallies: payload.tallies,
  });
  if (errors.length > 0) throw new Error(errors[0]);

  const report = buildAssessmentReport(tool, {
    toolId: tool.id,
    studentsAssessed: payload.studentsAssessed,
    tallies: payload.tallies,
    notes: payload.notes,
  });

  const record = await saveAssessmentScore(uid, {
    type: payload.type,
    grade: payload.grade,
    subject: payload.subject,
    groupId: payload.groupId,
    groupName: payload.groupName,
    toolId: tool.id,
    scorePercent: report.scorePercent,
    studentsAssessed: payload.studentsAssessed,
    notes: payload.notes,
    tallies: payload.tallies,
    strandScores: report.strands,
    weakItems: report.weakItems,
  });

  return { record, report };
}

export function listToolsForGrade(grade: number, mediumRaw: unknown, subject = "english") {
  const bookTrack = mediumToBookTrack(parseMedium(mediumRaw));
  return listAssessmentTools(grade, bookTrack, subject);
}

const DEFAULT_TLM_IDS = ["blackboard", "textbook", "notebook"];

export async function readTeacherTlmIds(uid: string): Promise<string[]> {
  if (uid === "dev-local" || !isTeacherStoreReady()) return DEFAULT_TLM_IDS;
  const resources = (await getTeacher(uid))?.teacher_resources;
  if (!Array.isArray(resources) || resources.length === 0) return DEFAULT_TLM_IDS;
  return resources.map(String);
}

export async function readTeacherTier(uid: string): Promise<TierId> {
  const pilotTier = getPilotTierOverride();
  if (pilotTier) return pilotTier;
  if (uid === "dev-local") return "prime";
  if (!isTeacherStoreReady()) return "basic";
  return tierFromRecord(await getTeacher(uid));
}
