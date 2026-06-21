import { balbharatiLessons as grade1EnglishL2, assessmentGroups as grade1EnglishL2Assessments } from "./grade1-english";
import { grade1EnglishL1Lessons as grade1EnglishL1, grade1EnglishL1AssessmentGroups as grade1EnglishL1Assessments } from "./grade1-english-l1";
import { grade2EnglishL1Lessons as grade2EnglishL1, grade2EnglishL1AssessmentGroups as grade2EnglishL1Assessments } from "./grade2-english-l1";
import { grade2EnglishL2Lessons as grade2EnglishL2, grade2EnglishL2AssessmentGroups as grade2EnglishL2Assessments } from "./grade2-english-l2";
import { grade3EnglishL2Lessons as grade3EnglishL2, grade3EnglishL2AssessmentGroups as grade3EnglishL2Assessments } from "./grade3-english-l2";
import { grade4EnglishL2Lessons as grade4EnglishL2, grade4EnglishL2AssessmentGroups as grade4EnglishL2Assessments } from "./grade4-english-l2";
import { grade5EnglishL2Lessons as grade5EnglishL2, grade5EnglishL2AssessmentGroups as grade5EnglishL2Assessments } from "./grade5-english-l2";
import type { BalbharatiLesson } from "./grade1-english";

export type { BalbharatiLesson };

export interface GradeSubject {
  grade: number; subject: string; label: string; available: boolean; totalLessons: number;
}

export const allGradeSubjects: GradeSubject[] = [
  { grade: 1, subject: "english", label: "Grade 1 English", available: true, totalLessons: 44 },
  { grade: 2, subject: "english", label: "Grade 2 English", available: true, totalLessons: 44 },
  { grade: 3, subject: "english", label: "Grade 3 English", available: true, totalLessons: 48 },
  { grade: 4, subject: "english", label: "Grade 4 English", available: true, totalLessons: 39 },
  { grade: 5, subject: "english", label: "Grade 5 English", available: true, totalLessons: 36 },
  { grade: 6, subject: "english", label: "Grade 6 English", available: false, totalLessons: 0 },
  { grade: 7, subject: "english", label: "Grade 7 English", available: false, totalLessons: 0 },
  { grade: 8, subject: "english", label: "Grade 8 English", available: false, totalLessons: 0 },
];

export const allSubjects = [
  { id: "english", en: "English", mr: "\u0907\u0902\u0917\u094D\u0930\u091C\u0940", hi: "\u0905\u0902\u0917\u094D\u0930\u0947\u091C\u093C\u0940", ur: "\u0627\u0646\u06AF\u0631\u06CC\u0632\u06CC" },
  { id: "marathi", en: "Marathi", mr: "\u092E\u0930\u093E\u0920\u0940", hi: "\u092E\u0930\u093E\u0920\u0940", ur: "\u0645\u0631\u0627\u0679\u06BE\u06CC" },
  { id: "hindi", en: "Hindi", mr: "\u0939\u093F\u0902\u0926\u0940", hi: "\u0939\u093F\u0902\u0926\u0940", ur: "\u06C1\u0646\u062F\u06CC" },
  { id: "maths", en: "Mathematics", mr: "\u0917\u0923\u093F\u0924", hi: "\u0917\u0923\u093F\u0924", ur: "\u0631\u06CC\u0627\u0636\u06CC" },
  { id: "evs", en: "EVS / Science", mr: "\u092A\u0930\u093F\u0938\u0930 \u0905\u092D\u094D\u092F\u093E\u0938", hi: "\u092A\u0930\u094D\u092F\u093E\u0935\u0930\u0923 \u0905\u0927\u094D\u092F\u092F\u0928", ur: "\u0645\u0627\u062D\u0648\u0644\u06CC\u0627\u062A" },
  { id: "social", en: "Social Studies", mr: "\u0938\u092E\u093E\u091C\u0936\u093E\u0938\u094D\u0924\u094D\u0930", hi: "\u0938\u092E\u093E\u091C \u0935\u093F\u091C\u094D\u091E\u093E\u0928", ur: "\u0645\u0639\u0627\u0634\u0631\u062A\u06CC\u0627\u062A" },
];

const ENGLISH_MEDIUMS = ["english", "English", "semi-english", "Semi-English"];

function isEnglishMedium(): boolean {
  if (typeof window === "undefined") return false;
  const medium = localStorage.getItem("medium") || "";
  return ENGLISH_MEDIUMS.some(m => medium.toLowerCase() === m.toLowerCase());
}

function normalizeLessons(lessons: any[], assessments: Record<string, { name: string; lessons: string[]; focus: string }>): BalbharatiLesson[] {
  return lessons.map(l => ({
    ...l,
    mr: l.mr || l.en,
    hi: l.hi || l.en,
    ur: l.ur || l.en,
    assessmentGroup: Object.entries(assessments)
      .find(([_, g]) => g.lessons.includes(l.id))?.[0] || "",
  })) as BalbharatiLesson[];
}

// Grade-specific lesson/assessment data maps
const GRADE_DATA: Record<string, {
  l1?: { lessons: any[]; assessments: Record<string, any> };
  l2: { lessons: any[]; assessments: Record<string, any> };
}> = {
  "1-english": {
    l1: { lessons: grade1EnglishL1, assessments: grade1EnglishL1Assessments },
    l2: { lessons: grade1EnglishL2, assessments: grade1EnglishL2Assessments },
  },
  "2-english": {
    l1: { lessons: grade2EnglishL1, assessments: grade2EnglishL1Assessments },
    l2: { lessons: grade2EnglishL2, assessments: grade2EnglishL2Assessments },
  },
  "3-english": {
    l2: { lessons: grade3EnglishL2, assessments: grade3EnglishL2Assessments },
  },
  "4-english": {
    l2: { lessons: grade4EnglishL2, assessments: grade4EnglishL2Assessments },
  },
  "5-english": {
    l2: { lessons: grade5EnglishL2, assessments: grade5EnglishL2Assessments },
  },
};

function getGradeData(grade: number, subject: string) {
  const key = `${grade}-${subject}`;
  const data = GRADE_DATA[key];
  if (!data) return null;
  const eng = isEnglishMedium();
  if (eng && data.l1) return data.l1;
  return data.l2;
}

export function getLessons(grade: number, subject: string): BalbharatiLesson[] {
  const data = getGradeData(grade, subject);
  if (!data) return [];
  // Grade 1 L2 is already in BalbharatiLesson shape
  if (grade === 1 && !isEnglishMedium()) return data.lessons as BalbharatiLesson[];
  return normalizeLessons(data.lessons, data.assessments);
}

export function getAssessmentGroups(grade: number, subject: string): Record<string, { name: string; lessons: string[]; focus: string }> {
  const data = getGradeData(grade, subject);
  return data?.assessments || {};
}

function isEnglishMediumValue(medium: string): boolean {
  const m = medium.toLowerCase().replace("_", "-");
  return ENGLISH_MEDIUMS.some((v) => v.toLowerCase() === m);
}

/** Server/API: pick L1 vs L2 from explicit medium string. */
export function getLessonsForMedium(
  grade: number,
  subject: string,
  medium: string,
): BalbharatiLesson[] {
  const key = `${grade}-${subject}`;
  const data = GRADE_DATA[key];
  if (!data) return [];
  const eng = isEnglishMediumValue(medium);
  const bucket = eng && data.l1 ? data.l1 : data.l2;
  if (grade === 1 && !eng) return bucket.lessons as BalbharatiLesson[];
  return normalizeLessons(bucket.lessons, bucket.assessments);
}

export function getAssessmentGroupsForMedium(
  grade: number,
  subject: string,
  medium: string,
): Record<string, { name: string; lessons: string[]; focus: string }> {
  const key = `${grade}-${subject}`;
  const data = GRADE_DATA[key];
  if (!data) return {};
  const eng = isEnglishMediumValue(medium);
  const bucket = eng && data.l1 ? data.l1 : data.l2;
  return bucket.assessments;
}

export function isAvailable(grade: number, subject: string): boolean {
  const gs = allGradeSubjects.find(g => g.grade === grade && g.subject === subject);
  return gs?.available || false;
}

export function getTextbookInfo(grade: number, subject: string): { name: string; code: string; type: "L1" | "L2" } {
  const eng = isEnglishMedium();
  if (subject === "english") {
    const names: Record<number, { l1: string; l2: string }> = {
      1: { l1: "English Balbharati Grade One", l2: "My English Book Grade One" },
      2: { l1: "English Balbharati Standard Two", l2: "My English Book Two" },
      3: { l1: "English Balbharati Standard Three", l2: "My English Book Three" },
      4: { l1: "English Balbharati Standard Four", l2: "My English Book Four" },
      5: { l1: "English Balbharati Standard Five", l2: "My English Book Five" },
    };
    const n = names[grade];
    if (n) return { name: eng ? n.l1 : n.l2, code: "", type: eng ? "L1" : "L2" };
  }
  return { name: "", code: "", type: "L2" };
}

export function getAllLessons(): BalbharatiLesson[] {
  const all: BalbharatiLesson[] = [];
  for (const [key, data] of Object.entries(GRADE_DATA)) {
    const eng = isEnglishMedium();
    const src = (eng && data.l1) ? data.l1 : data.l2;
    if (key === "1-english" && !eng) {
      all.push(...(src.lessons as BalbharatiLesson[]));
    } else {
      all.push(...normalizeLessons(src.lessons, src.assessments));
    }
  }
  return all;
}

export function getAllAssessmentGroups(): Record<string, { name: string; lessons: string[]; focus: string }> {
  const all: Record<string, any> = {};
  for (const [, data] of Object.entries(GRADE_DATA)) {
    const eng = isEnglishMedium();
    const src = (eng && data.l1) ? data.l1 : data.l2;
    Object.assign(all, src.assessments);
  }
  return all;
}

// Backward compat
export const balbharatiLessons = grade1EnglishL2;
export const assessmentGroups = grade1EnglishL2Assessments;

// Server-safe combined list for API routes
export const allLessonsServer: BalbharatiLesson[] = (() => {
  const all: BalbharatiLesson[] = [];
  for (const [, data] of Object.entries(GRADE_DATA)) {
    if (data.l1) all.push(...normalizeLessons(data.l1.lessons, data.l1.assessments));
    all.push(...normalizeLessons(data.l2.lessons, data.l2.assessments));
  }
  return all;
})();
