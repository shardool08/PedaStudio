import { FieldValue } from "firebase-admin/firestore";
import { getAssessmentGroupsForMedium, getLessonsForMedium } from "@/lib/curriculum";
import { getAdminApp } from "@/lib/firebase/admin";
import { getFirestore } from "firebase-admin/firestore";
import type { TierId } from "@/lib/tier-config";
import { normalizeTierId } from "@/lib/tier-config";

export type AssessmentType = "baseline" | "unit" | "endline";

export interface AssessmentGroupCatalog {
  id: string;
  name: string;
  focus: string;
  lessons: string[];
  unit: number;
}

export interface AssessmentRecord {
  id: string;
  type: AssessmentType;
  grade: number;
  subject: string;
  groupId?: string;
  groupName?: string;
  scorePercent: number;
  studentsAssessed: number;
  notes?: string;
  createdAt: string;
  updatedAt: string;
}

function getDb() {
  const app = getAdminApp();
  if (!app) return null;
  return getFirestore(app);
}

function parseMedium(raw: unknown): string {
  const m = String(raw || "marathi").toLowerCase();
  if (m === "english" || m === "semi-english" || m === "semi_english") return "english";
  return "marathi";
}

export function buildAssessmentCatalog(
  grade: number,
  subject: string,
  mediumRaw: unknown,
): {
  baseline: { id: string; title: string; description: string };
  endline: { id: string; title: string; description: string };
  unitTests: AssessmentGroupCatalog[];
} {
  const medium = parseMedium(mediumRaw);
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
    };
  });

  return {
    baseline: {
      id: "baseline",
      title: "Baseline (start of year)",
      description: "FLN + competency check before teaching begins.",
    },
    endline: {
      id: "endline",
      title: "Endline (end of year)",
      description: "Year-end competency and LO attainment check.",
    },
    unitTests: unitTests.sort((a, b) => a.unit - b.unit || a.id.localeCompare(b.id)),
  };
}

export async function listAssessmentScores(uid: string, grade: number): Promise<AssessmentRecord[]> {
  if (uid === "dev-local") return [];
  const db = getDb();
  if (!db) return [];
  const snap = await db
    .collection("users")
    .doc(uid)
    .collection("assessments")
    .where("grade", "==", grade)
    .get();
  return snap.docs.map((doc) => {
    const d = doc.data();
    return {
      id: doc.id,
      type: (d.type as AssessmentType) || "unit",
      grade: Number(d.grade) || grade,
      subject: String(d.subject || "english"),
      groupId: d.groupId as string | undefined,
      groupName: d.groupName as string | undefined,
      scorePercent: Number(d.scorePercent) || 0,
      studentsAssessed: Number(d.studentsAssessed) || 0,
      notes: d.notes as string | undefined,
      createdAt: String(d.createdAt || ""),
      updatedAt: String(d.updatedAt || ""),
    };
  });
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
  },
): Promise<AssessmentRecord> {
  const now = new Date().toISOString();
  const id =
    payload.type === "unit" && payload.groupId
      ? `unit_${payload.groupId}`
      : payload.type;

  if (uid === "dev-local") {
    return { id, ...payload, createdAt: now, updatedAt: now };
  }

  const db = getDb();
  if (!db) throw new Error("Database unavailable");

  const ref = db.collection("users").doc(uid).collection("assessments").doc(id);
  const existing = await ref.get();
  const createdAt = existing.exists ? String(existing.data()?.createdAt || now) : now;

  const data = {
    type: payload.type,
    grade: payload.grade,
    subject: payload.subject,
    groupId: payload.groupId ?? null,
    groupName: payload.groupName ?? null,
    scorePercent: Math.min(100, Math.max(0, payload.scorePercent)),
    studentsAssessed: Math.max(0, payload.studentsAssessed),
    notes: payload.notes ?? "",
    createdAt,
    updatedAt: now,
    savedAt: FieldValue.serverTimestamp(),
  };

  await ref.set(data, { merge: true });

  return {
    id,
    type: payload.type,
    grade: payload.grade,
    subject: payload.subject,
    groupId: payload.groupId,
    groupName: payload.groupName,
    scorePercent: data.scorePercent,
    studentsAssessed: data.studentsAssessed,
    notes: payload.notes,
    createdAt,
    updatedAt: now,
  };
}

export async function readTeacherTlmIds(uid: string): Promise<string[]> {
  if (uid === "dev-local") return ["blackboard", "textbook", "notebook"];
  const db = getDb();
  if (!db) return ["blackboard", "textbook", "notebook"];
  const snap = await db.collection("users").doc(uid).get();
  const raw = snap.data()?.teacherResources;
  if (!Array.isArray(raw)) return ["blackboard", "textbook", "notebook"];
  return raw.map(String);
}

export async function readTeacherTier(uid: string): Promise<TierId> {
  if (uid === "dev-local") return "prime";
  const db = getDb();
  if (!db) return "basic";
  const snap = await db.collection("users").doc(uid).get();
  return normalizeTierId(snap.data()?.tier);
}
