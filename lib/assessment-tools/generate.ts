/**
 * Auto-generate structured assessment tools from Balbharati curriculum data.
 * Hand-crafted Grade 1 L2 tools take precedence in index.ts.
 */
import {
  getAssessmentGroupsForMedium,
  getLessonsForMedium,
  isAvailable,
} from "@/lib/curriculum";
import type { BalbharatiLesson } from "@/lib/curriculum/grade1-english";
import { getGradeFlnProfile } from "./fln-goals";
import type {
  AssessmentItem,
  AssessmentTool,
  AssessmentToolType,
  DeliveryMode,
  FlnStrand,
  ItemFormat,
} from "./types";

function mediumForBookTrack(bookTrack: "l1" | "l2"): string {
  return bookTrack === "l1" ? "english" : "marathi";
}

function subjectLabel(subject: string, grade: number, bookTrack: "l1" | "l2"): string {
  if (subject === "english") {
    return bookTrack === "l1"
      ? `Grade ${grade} English Balbharati (L1)`
      : `Grade ${grade} My English Book (L2)`;
  }
  return `${subject.charAt(0).toUpperCase()}${subject.slice(1)} — Grade ${grade}`;
}

function inferStrand(lesson: BalbharatiLesson): FlnStrand {
  const t = String(lesson.type || "").toLowerCase();
  if (t.includes("phonics")) return "DEC";
  if (t === "story") return "RC";
  if (t === "poem" || t === "song") return "OL";
  if (t === "picture-talk" || t === "conversation") return "VOC";
  return "VOC";
}

function deliveryFor(grade: number, format: ItemFormat): DeliveryMode {
  if (grade >= 4 && format === "mcq") return "written";
  if (grade >= 3) return format === "mcq" ? "oral-group" : "oral-individual";
  return format === "mcq" ? "oral-group" : "performance";
}

function hashSeed(s: string): number {
  let h = 0;
  for (let i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) | 0;
  return Math.abs(h);
}

function pickDistractors(correct: string, pool: string[], count: number, seed: string): string[] {
  const lower = correct.toLowerCase();
  const candidates = [...new Set(pool.filter((w) => w.toLowerCase() !== lower))];
  const start = hashSeed(seed) % Math.max(1, candidates.length);
  const out: string[] = [];
  for (let i = 0; i < candidates.length && out.length < count; i++) {
    out.push(candidates[(start + i) % candidates.length]);
  }
  while (out.length < count) {
    out.push(`option${out.length + 1}`);
  }
  return out.slice(0, count);
}

function lessonApplyFocus(lesson: BalbharatiLesson): string | undefined {
  const blooms = lesson.bloomsProgression || [];
  return (
    blooms.find((b) => b.level.includes("Apply"))?.focus ||
    blooms[blooms.length - 1]?.focus
  );
}

function mcqStem(lesson: BalbharatiLesson, word: string): string {
  const structure = lesson.structures?.[0];
  if (structure?.includes("___")) {
    return `"${lesson.en}": ${structure.replace(/___/g, "_____")} — choose: ${word}`;
  }
  const t = String(lesson.type || "").toLowerCase();
  if (t.includes("phonics")) {
    return `"${lesson.en}": Which word uses the lesson sound? — ${word}`;
  }
  if (t === "story") {
    return `From "${lesson.en}": which word belongs in this lesson? — ${word}`;
  }
  return `"${lesson.en}" — pick the word: ${word}`;
}

function mcqFromLesson(
  lesson: BalbharatiLesson,
  word: string,
  vocabPool: string[],
  itemId: string,
  section: string,
  grade: number,
): AssessmentItem {
  const distractors = pickDistractors(word, vocabPool, 3, itemId);
  const keys = ["A", "B", "C", "D"] as const;
  const withoutCorrect = distractors.filter((d) => d.toLowerCase() !== word.toLowerCase());
  const baseOptions = [
    { text: word },
    ...withoutCorrect.slice(0, 3).map((text) => ({ text })),
  ].slice(0, 4);
  const rotate = hashSeed(itemId) % baseOptions.length;
  const rotated = [...baseOptions.slice(rotate), ...baseOptions.slice(0, rotate)];
  const finalOptions: NonNullable<AssessmentItem["options"]> = rotated.map((o, i) => ({
    key: keys[i],
    text: o.text,
  }));
  const correctIndex = finalOptions.findIndex(
    (o) => o.text.toLowerCase() === word.toLowerCase(),
  );
  const correctOption = keys[Math.max(0, correctIndex)];

  return {
    id: itemId,
    section,
    flnStrand: inferStrand(lesson),
    lessonIds: [lesson.id],
    competency: lesson.competencies?.[0] || `Vocabulary from ${lesson.en}`,
    bloomsLevel: "Remember",
    format: "mcq",
    marks: 1,
    deliveryMode: deliveryFor(grade, "mcq"),
    stem: mcqStem(lesson, word),
    stimulusNote: lesson.type === "picture-talk" ? `Picture from ${lesson.en} (${lesson.pages})` : undefined,
    options: finalOptions,
    correctOption,
    markingScheme: `1 mark for ${correctOption} (${word}).`,
  };
}

function subjectiveFromLesson(
  lesson: BalbharatiLesson,
  itemId: string,
  section: string,
  grade: number,
): AssessmentItem {
  const competency = lesson.competencies?.[0] || `Demonstrate understanding of ${lesson.en}`;
  const applyFocus = lessonApplyFocus(lesson);
  const structure = lesson.structures?.[0];

  return {
    id: itemId,
    section,
    flnStrand: inferStrand(lesson),
    lessonIds: [lesson.id],
    competency,
    bloomsLevel: "Apply",
    format: "subjective",
    marks: grade <= 2 ? 2 : 1,
    deliveryMode: deliveryFor(grade, "subjective"),
    stem: applyFocus
      ? `"${lesson.en}": ${applyFocus}`
      : structure
        ? `Use: ${structure.replace(/___/g, "_____")}`
        : `Show understanding of "${lesson.en}"`,
    markingScheme:
      grade <= 2
        ? "2 marks: meets competency. 1 mark: partial. 0: no response."
        : "1 mark: meets competency. 0: incorrect or no response.",
    correctResponse: competency,
  };
}

function itemsForLessons(
  lessons: BalbharatiLesson[],
  section: string,
  idPrefix: string,
  grade: number,
  maxItems: number,
): AssessmentItem[] {
  const vocabPool = lessons.flatMap((l) => l.vocabulary || []);
  const items: AssessmentItem[] = [];
  let n = 0;

  for (const lesson of lessons) {
    if (items.length >= maxItems) break;
    const vocab = (lesson.vocabulary || []).filter(Boolean);

    if (vocab.length > 0) {
      const word = vocab[hashSeed(lesson.id + "v") % vocab.length];
      items.push(mcqFromLesson(lesson, word, vocabPool, `${idPrefix}-${++n}`, section, grade));
    }
    if (items.length >= maxItems) break;
    if ((lesson.competencies || []).length > 0) {
      items.push(subjectiveFromLesson(lesson, `${idPrefix}-${++n}`, section, grade));
    }
  }

  return items;
}

function primaryUnit(lessons: BalbharatiLesson[]): number {
  if (!lessons.length) return 1;
  const units = lessons.map((l) => l.unit).filter(Boolean);
  return units.length ? Math.min(...units) : 1;
}

function lessonsForUnitTest(
  grade: number,
  subject: string,
  bookTrack: "l1" | "l2",
  groupId: string,
): { group: { name: string; focus: string; lessons: string[] }; lessons: BalbharatiLesson[] } | null {
  const medium = mediumForBookTrack(bookTrack);
  const groups = getAssessmentGroupsForMedium(grade, subject, medium);
  const group = groups[groupId];
  if (!group) return null;
  const all = getLessonsForMedium(grade, subject, medium);
  const lessons = all.filter((l) => group.lessons.includes(l.id));
  if (!lessons.length) return null;
  return { group, lessons };
}

function adminNotes(grade: number, type: AssessmentToolType, unitNum?: number): string[] {
  const profile = getGradeFlnProfile(grade);
  const base =
    grade <= 2
      ? [
          "Conduct orally in small groups. Use A/B/C/D cards or fingers.",
          "Read each question twice. Record tallies in the app.",
        ]
      : [
          "Mix oral and written items as marked. Allow extra time for municipal multigrade classes.",
          "Record class tallies — individual papers optional from Grade 4.",
        ];
  if (type === "baseline") {
    return [
      ...base,
      "Baseline establishes starting point — not for ranking children.",
      ...(profile?.baselineFocus.slice(0, 2).map((f) => `Focus: ${f}`) ?? []),
    ];
  }
  if (type === "endline") {
    return [
      ...base,
      "Compare with baseline strand scores to show growth.",
      ...(profile?.endlineFocus.slice(0, 2).map((f) => `Focus: ${f}`) ?? []),
    ];
  }
  return [
    ...base,
    unitNum ? `Administer after completing Unit ${unitNum} lessons in this window.` : "Administer after this unit's lessons.",
    "Items are drawn from the exact lessons in this assessment window.",
  ];
}

export function generateAssessmentTool(params: {
  grade: number;
  subject: string;
  bookTrack: "l1" | "l2";
  type: AssessmentToolType;
  groupId?: string;
}): AssessmentTool | undefined {
  const { grade, subject, bookTrack, type, groupId } = params;
  if (!isAvailable(grade, subject)) return undefined;

  const medium = mediumForBookTrack(bookTrack);
  const track = bookTrack.toUpperCase();
  const version = "2025-26.gen";
  const book = subjectLabel(subject, grade, bookTrack);

  if (type === "unit" && groupId) {
    const resolved = lessonsForUnitTest(grade, subject, bookTrack, groupId);
    if (!resolved) return undefined;
    const { group, lessons } = resolved;
    const unitNum = primaryUnit(lessons);
    const section = group.name.includes("Unit") ? group.name : `Unit ${unitNum} — ${group.name}`;
    const items = itemsForLessons(lessons, section, groupId.replace(/[^A-Za-z0-9]/g, ""), grade, 12);
    if (!items.length) return undefined;
    const totalMarks = items.reduce((s, i) => s + i.marks, 0);
    const lessonTitles = lessons.map((l) => l.en).slice(0, 4).join(", ");
    return {
      id: `g${grade}-${bookTrack}-${subject}-unit-${groupId}`,
      type: "unit",
      grade,
      subject,
      bookTrack,
      groupId,
      groupName: group.name,
      lessonIds: group.lessons,
      focus: group.focus,
      title: `Unit ${unitNum} Test — ${group.name}`,
      description: `${group.focus} · Lessons: ${lessonTitles}${lessons.length > 4 ? "…" : ""}`,
      totalMarks,
      recommendedMinutes: Math.min(45, 10 + lessons.length * 2 + grade),
      administrationNotes: adminNotes(grade, "unit", unitNum),
      sections: [{ id: groupId, title: section, flnStrand: inferStrand(lessons[0]), items }],
      version,
    };
  }

  const allLessons = getLessonsForMedium(grade, subject, medium);
  if (!allLessons.length) return undefined;

  let picked: BalbharatiLesson[] = [];
  let title = "";
  let description = "";

  if (type === "baseline") {
    const firstUnit = allLessons.filter((l) => l.unit === 1).slice(0, 4);
    const rest = allLessons.filter((l) => l.unit > 1).slice(0, 4);
    picked = [...firstUnit, ...rest].slice(0, 8);
    if (picked.length < 4) picked = allLessons.slice(0, Math.min(8, allLessons.length));
    title = `Grade ${grade} — Baseline (${book})`;
    description = `${track} · Start-of-year check before Unit 1 teaching.`;
  } else if (type === "endline") {
    const byUnit = new Map<number, BalbharatiLesson[]>();
    for (const l of allLessons) {
      const list = byUnit.get(l.unit) || [];
      list.push(l);
      byUnit.set(l.unit, list);
    }
    picked = [...byUnit.entries()]
      .sort(([a], [b]) => a - b)
      .flatMap(([, ls]) => ls.slice(-1))
      .slice(0, 10);
    if (picked.length < 4) {
      picked = allLessons.slice(-Math.min(10, allLessons.length));
    }
    title = `Grade ${grade} — Endline (${book})`;
    description = `${track} · Year-end check across all units.`;
  } else {
    return undefined;
  }

  const sectionTitle = type === "baseline" ? "Baseline — sampled units" : "Endline — all units";
  const items = itemsForLessons(picked, sectionTitle, type === "baseline" ? "BL" : "EL", grade, 12);
  if (!items.length) return undefined;
  const totalMarks = items.reduce((s, i) => s + i.marks, 0);

  return {
    id: `g${grade}-${bookTrack}-${subject}-${type}`,
    type,
    grade,
    subject,
    bookTrack,
    title,
    description,
    totalMarks,
    recommendedMinutes: type === "baseline" ? 18 + grade * 2 : 22 + grade * 3,
    administrationNotes: adminNotes(grade, type),
    sections: [{ id: type, title: sectionTitle, flnStrand: "VOC", items }],
    version,
  };
}

export { mediumForBookTrack };
