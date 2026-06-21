import type { BalbharatiLesson } from "@/lib/curriculum/grade1-english";
import { TLM_RESOURCES } from "@/lib/tlm";

const TYPE_TLM: Record<string, string[]> = {
  song: ["phone", "blackboard", "ball"],
  conversation: ["blackboard", "notebook", "picture_cards"],
  phonics: ["chart", "flashcards", "blackboard", "notebook", "printer"],
  story: ["picture_cards", "blackboard", "puppets"],
  poem: ["phone", "blackboard", "chart"],
  "picture-talk": ["chart", "picture_cards", "real_objects", "blackboard"],
};

const UNIT_ESSENTIALS = ["blackboard", "textbook", "notebook"];

export interface TlmKitItem {
  id: string;
  label: string;
  emoji?: string;
  essential: boolean;
  reason: string;
  owned: boolean;
}

export interface TlmKitSummary {
  grade: number;
  unit: number | null;
  scope: "unit" | "year";
  items: TlmKitItem[];
  ownedCount: number;
  gapCount: number;
  procurementNote: string;
}

function labelFor(id: string): string {
  return TLM_RESOURCES.find((r) => r.id === id)?.label ?? id;
}

function recommendedForLesson(lesson: BalbharatiLesson): string[] {
  const fromType = TYPE_TLM[lesson.type] ?? ["blackboard", "notebook"];
  if (lesson.vocabulary.length >= 8) fromType.push("flashcards", "chart");
  if (lesson.type === "phonics") fromType.push("printer");
  return Array.from(new Set([...UNIT_ESSENTIALS, ...fromType]));
}

export function buildUnitTlmKit(
  lessons: BalbharatiLesson[],
  unit: number,
  teacherTlmIds: string[],
): TlmKitSummary {
  const unitLessons = lessons.filter((l) => l.unit === unit);
  const owned = new Set(teacherTlmIds);
  const reasons = new Map<string, Set<string>>();

  for (const lesson of unitLessons) {
    for (const id of recommendedForLesson(lesson)) {
      if (!reasons.has(id)) reasons.set(id, new Set());
      reasons.get(id)!.add(lesson.en.slice(0, 40));
    }
  }

  const items: TlmKitItem[] = Array.from(reasons.entries())
    .map(([id, lessonNames]) => ({
      id,
      label: labelFor(id),
      essential: UNIT_ESSENTIALS.includes(id),
      reason: `Used in: ${Array.from(lessonNames).slice(0, 3).join("; ")}`,
      owned: owned.has(id),
    }))
    .sort((a, b) => Number(b.essential) - Number(a.essential) || a.label.localeCompare(b.label));

  const gapCount = items.filter((i) => !i.owned).length;
  return {
    grade: unitLessons[0] ? parseInt(unitLessons[0].id.split(".")[0], 10) || 1 : 1,
    unit,
    scope: "unit",
    items,
    ownedCount: items.filter((i) => i.owned).length,
    gapCount,
    procurementNote:
      gapCount > 0
        ? `${gapCount} item(s) to arrange before Unit ${unit} teaching.`
        : "Your classroom has the core materials for this unit.",
  };
}

export function buildYearTlmKit(
  lessons: BalbharatiLesson[],
  teacherTlmIds: string[],
): TlmKitSummary {
  const owned = new Set(teacherTlmIds);
  const reasons = new Map<string, Set<string>>();

  for (const lesson of lessons) {
    for (const id of recommendedForLesson(lesson)) {
      if (!reasons.has(id)) reasons.set(id, new Set());
      reasons.get(id)!.add(`Unit ${lesson.unit}`);
    }
  }

  const items: TlmKitItem[] = Array.from(reasons.entries())
    .map(([id, units]) => ({
      id,
      label: labelFor(id),
      essential: UNIT_ESSENTIALS.includes(id),
      reason: `Needed across ${Array.from(units).slice(0, 4).join(", ")}`,
      owned: owned.has(id),
    }))
    .sort((a, b) => Number(b.essential) - Number(a.essential) || a.label.localeCompare(b.label));

  const gapCount = items.filter((i) => !i.owned).length;
  const grade = lessons[0] ? parseInt(lessons[0].id.split(".")[0], 10) || 1 : 1;
  return {
    grade,
    unit: null,
    scope: "year",
    items,
    ownedCount: items.filter((i) => i.owned).length,
    gapCount,
    procurementNote:
      gapCount > 0
        ? `Year procurement list: ${gapCount} item(s) still needed for Grade ${grade}.`
        : "Your classroom is well equipped for the year.",
  };
}
