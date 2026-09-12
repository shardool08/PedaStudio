import { FLN_STRAND_LABELS } from "./fln-goals";
import type {
  AssessmentItem,
  AssessmentReport,
  AssessmentResultInput,
  AssessmentTool,
  ItemResult,
  ItemTally,
  ItemTallyMcq,
  ItemTallySubjective,
  StrandResult,
} from "./types";

function allItems(tool: AssessmentTool): AssessmentItem[] {
  return tool.sections.flatMap((s) => s.items);
}

function findItem(tool: AssessmentTool, itemId: string): AssessmentItem | undefined {
  return allItems(tool).find((i) => i.id === itemId);
}

function mcqCorrectCount(tally: ItemTallyMcq, correct: "A" | "B" | "C" | "D"): number {
  switch (correct) {
    case "A":
      return tally.countA;
    case "B":
      return tally.countB;
    case "C":
      return tally.countC;
    case "D":
      return tally.countD;
  }
}

function effectiveDenominator(studentsAssessed: number, notAssessed = 0): number {
  const d = studentsAssessed - notAssessed;
  return d > 0 ? d : studentsAssessed;
}

export function scoreItem(
  item: AssessmentItem,
  tally: ItemTally,
  studentsAssessed: number,
): ItemResult {
  const denom = effectiveDenominator(studentsAssessed, tally.notAssessed ?? 0);

  if (item.format === "mcq" && tally.format === "mcq" && item.correctOption) {
    const correct = mcqCorrectCount(tally, item.correctOption);
    const pValue = denom > 0 ? correct / denom : 0;
    const marksObtained = pValue * item.marks;
    return {
      itemId: item.id,
      flnStrand: item.flnStrand,
      competency: item.competency,
      marksPossible: item.marks,
      marksObtained: Math.round(marksObtained * 100) / 100,
      percentCorrect: Math.round(pValue * 1000) / 10,
      pValue: Math.round(pValue * 1000) / 1000,
    };
  }

  if (item.format === "subjective" && tally.format === "subjective") {
    const pValue = denom > 0 ? tally.correctCount / denom : 0;
    const marksObtained = pValue * item.marks;
    return {
      itemId: item.id,
      flnStrand: item.flnStrand,
      competency: item.competency,
      marksPossible: item.marks,
      marksObtained: Math.round(marksObtained * 100) / 100,
      percentCorrect: Math.round(pValue * 1000) / 10,
      pValue: Math.round(pValue * 1000) / 1000,
    };
  }

  return {
    itemId: item.id,
    flnStrand: item.flnStrand,
    competency: item.competency,
    marksPossible: item.marks,
    marksObtained: 0,
    percentCorrect: 0,
    pValue: 0,
  };
}

export function buildAssessmentReport(
  tool: AssessmentTool,
  input: AssessmentResultInput,
): AssessmentReport {
  const items = allItems(tool);
  const tallyMap = new Map(input.tallies.map((t) => [t.itemId, t]));

  const itemResults: ItemResult[] = items.map((item) => {
    const tally = tallyMap.get(item.id);
    if (!tally) {
      return scoreItem(item, item.format === "mcq"
        ? { itemId: item.id, format: "mcq", countA: 0, countB: 0, countC: 0, countD: 0 }
        : { itemId: item.id, format: "subjective", correctCount: 0 },
      input.studentsAssessed);
    }
    return scoreItem(item, tally, input.studentsAssessed);
  });

  const strandAgg = new Map<string, { possible: number; obtained: number; count: number }>();
  for (const r of itemResults) {
    const prev = strandAgg.get(r.flnStrand) ?? { possible: 0, obtained: 0, count: 0 };
    prev.possible += r.marksPossible;
    prev.obtained += r.marksObtained;
    prev.count += 1;
    strandAgg.set(r.flnStrand, prev);
  }

  const strands: StrandResult[] = [...strandAgg.entries()].map(([strand, agg]) => ({
    strand: strand as StrandResult["strand"],
    label: FLN_STRAND_LABELS[strand as keyof typeof FLN_STRAND_LABELS],
    marksPossible: agg.possible,
    marksObtained: Math.round(agg.obtained * 100) / 100,
    percent: agg.possible > 0 ? Math.round((agg.obtained / agg.possible) * 1000) / 10 : 0,
    itemCount: agg.count,
  }));

  const totalMarksPossible = tool.totalMarks;
  const totalMarksObtained = itemResults.reduce((s, r) => s + r.marksObtained, 0);
  const scorePercent =
    totalMarksPossible > 0
      ? Math.round((totalMarksObtained / totalMarksPossible) * 1000) / 10
      : 0;

  const weakItems = itemResults
    .filter((r) => r.pValue < 0.4)
    .map((r) => r.itemId);

  const misfitItems = itemResults
    .filter((r) => r.pValue < 0.2 || r.pValue > 0.95)
    .map((r) => r.itemId);

  return {
    toolId: tool.id,
    title: tool.title,
    type: tool.type,
    grade: tool.grade,
    studentsAssessed: input.studentsAssessed,
    totalMarksPossible,
    totalMarksObtained: Math.round(totalMarksObtained * 100) / 100,
    scorePercent,
    items: itemResults,
    strands,
    weakItems,
    misfitItems,
    generatedAt: new Date().toISOString(),
  };
}

/** Validate tallies before save — every item must have a tally row. */
export function validateTallies(
  tool: AssessmentTool,
  input: AssessmentResultInput,
): string[] {
  const errors: string[] = [];
  if (input.studentsAssessed < 1) errors.push("studentsAssessed must be at least 1");

  const items = allItems(tool);
  const tallyMap = new Map(input.tallies.map((t) => [t.itemId, t]));

  for (const item of items) {
    const tally = tallyMap.get(item.id);
    if (!tally) {
      errors.push(`Missing tally for item ${item.id}`);
      continue;
    }
    if (item.format !== tally.format) {
      errors.push(`Format mismatch for item ${item.id}`);
      continue;
    }

    if (tally.format === "mcq") {
      const sum = tally.countA + tally.countB + tally.countC + tally.countD + (tally.notAssessed ?? 0);
      if (sum > input.studentsAssessed) {
        errors.push(`Item ${item.id}: counts exceed students assessed`);
      }
    } else {
      const subj = tally as ItemTallySubjective;
      if (subj.correctCount + (subj.notAssessed ?? 0) > input.studentsAssessed) {
        errors.push(`Item ${item.id}: correct count exceeds students assessed`);
      }
    }
  }

  return errors;
}
