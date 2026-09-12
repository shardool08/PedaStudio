import { anthropicMessages } from "@/lib/api-utils";
import type { AssessmentTool, ItemTally, ItemTallyMcq, ItemTallySubjective } from "@/lib/assessment-tools/types";

export type PaperScanMode = "papers" | "tally_sheet";

export interface ScanImageInput {
  base64: string;
  mediaType?: string;
}

export interface PerStudentMark {
  index: number;
  label: string;
  scorePercent: number;
}

export interface PaperMarkResult {
  studentsProcessed: number;
  tallies: ItemTally[];
  perStudent: PerStudentMark[];
  warnings: string[];
}

interface StudentAnswerRow {
  itemId: string;
  mcq?: "A" | "B" | "C" | "D" | null;
  subjectiveCorrect?: boolean | null;
  notAssessed?: boolean;
}

interface StudentMarkResponse {
  label?: string;
  answers?: StudentAnswerRow[];
  notes?: string;
}

interface TallySheetResponse {
  studentsAssessed?: number;
  tallies?: Array<Record<string, unknown>>;
}

function allItems(tool: AssessmentTool) {
  return tool.sections.flatMap((s) => s.items);
}

function itemMap(tool: AssessmentTool) {
  return new Map(allItems(tool).map((i) => [i.id, i]));
}

function emptyTallies(tool: AssessmentTool): Map<string, ItemTally> {
  const map = new Map<string, ItemTally>();
  for (const item of allItems(tool)) {
    map.set(
      item.id,
      item.format === "mcq"
        ? { itemId: item.id, format: "mcq", countA: 0, countB: 0, countC: 0, countD: 0 }
        : { itemId: item.id, format: "subjective", correctCount: 0 },
    );
  }
  return map;
}

function buildAnswerKeyBlock(tool: AssessmentTool): string {
  return allItems(tool)
    .map((item) => {
      if (item.format === "mcq") {
        const opts = item.options?.map((o) => `${o.key}) ${o.text}`).join(" | ") ?? "";
        return `- ${item.id} [MCQ] ${item.stem} | Options: ${opts} | Correct: ${item.correctOption}`;
      }
      return `- ${item.id} [SUBJECTIVE] ${item.stem} | Marking: ${item.markingScheme}`;
    })
    .join("\n");
}

function parseJsonFromText(text: string): unknown {
  const clean = text.replace(/```json|```/g, "").trim();
  return JSON.parse(clean);
}

function scoreStudent(tool: AssessmentTool, answers: StudentAnswerRow[]): number {
  const items = itemMap(tool);
  let obtained = 0;
  let possible = 0;
  for (const row of answers) {
    const item = items.get(row.itemId);
    if (!item || row.notAssessed) continue;
    possible += item.marks;
    if (item.format === "mcq" && row.mcq && item.correctOption) {
      if (row.mcq === item.correctOption) obtained += item.marks;
    } else if (item.format === "subjective" && row.subjectiveCorrect === true) {
      obtained += item.marks;
    }
  }
  return possible > 0 ? Math.round((obtained / possible) * 1000) / 10 : 0;
}

function applyStudentAnswers(
  tallyMap: Map<string, ItemTally>,
  tool: AssessmentTool,
  answers: StudentAnswerRow[],
) {
  const items = itemMap(tool);
  for (const row of answers) {
    const item = items.get(row.itemId);
    const tally = tallyMap.get(row.itemId);
    if (!item || !tally) continue;

    if (row.notAssessed) {
      if (tally.format === "mcq") {
        const t = tally as ItemTallyMcq;
        tallyMap.set(row.itemId, { ...t, notAssessed: (t.notAssessed ?? 0) + 1 });
      } else {
        const t = tally as ItemTallySubjective;
        tallyMap.set(row.itemId, { ...t, notAssessed: (t.notAssessed ?? 0) + 1 });
      }
      continue;
    }

    if (item.format === "mcq" && tally.format === "mcq" && row.mcq) {
      const key = row.mcq.toUpperCase();
      const next = { ...tally };
      if (key === "A") next.countA += 1;
      else if (key === "B") next.countB += 1;
      else if (key === "C") next.countC += 1;
      else if (key === "D") next.countD += 1;
      tallyMap.set(row.itemId, next);
    } else if (item.format === "subjective" && tally.format === "subjective" && row.subjectiveCorrect === true) {
      tallyMap.set(row.itemId, { ...tally, correctCount: tally.correctCount + 1 });
    }
  }
}

async function markOnePaper(
  tool: AssessmentTool,
  image: ScanImageInput,
  studentIndex: number,
): Promise<{ answers: StudentAnswerRow[]; label: string; warning?: string }> {
  const mime = image.mediaType || "image/jpeg";
  const data = image.base64.replace(/^data:[^;]+;base64,/, "");

  const result = await anthropicMessages({
    max_tokens: 2048,
    messages: [
      {
        role: "user",
        content: [
          {
            type: "image",
            source: { type: "base64", media_type: mime, data },
          },
          {
            type: "text",
            text: `You mark Grade ${tool.grade} English municipal-school assessment papers in Maharashtra.

Assessment: ${tool.title}
Answer key:
${buildAnswerKeyBlock(tool)}

This photo is ONE student's completed paper (circled/written answers, tick marks, or oral response sheet).
For each item id, detect the student's response.

Return ONLY valid JSON:
{
  "label": "Student ${studentIndex + 1}",
  "answers": [
    { "itemId": "A1-01", "mcq": "A", "subjectiveCorrect": null, "notAssessed": false }
  ]
}

Rules:
- mcq: one of "A","B","C","D" or null if unreadable
- subjective: subjectiveCorrect true/false/null
- notAssessed true if item missing on paper
- Include every item id from the answer key`,
          },
        ],
      },
    ],
  });

  if (!result.ok) {
    throw new Error(`Could not mark paper ${studentIndex + 1}`);
  }

  const text = result.data.content?.[0]?.text || "{}";
  let parsed: StudentMarkResponse;
  try {
    parsed = parseJsonFromText(text) as StudentMarkResponse;
  } catch {
    return { answers: [], label: `Student ${studentIndex + 1}`, warning: `Paper ${studentIndex + 1}: could not parse AI response` };
  }

  return {
    answers: Array.isArray(parsed.answers) ? parsed.answers : [],
    label: parsed.label?.trim() || `Student ${studentIndex + 1}`,
    warning: parsed.answers?.length ? undefined : `Paper ${studentIndex + 1}: no answers detected`,
  };
}

async function markTallySheet(
  tool: AssessmentTool,
  image: ScanImageInput,
): Promise<{ tallies: ItemTally[]; studentsAssessed: number; warnings: string[] }> {
  const mime = image.mediaType || "image/jpeg";
  const data = image.base64.replace(/^data:[^;]+;base64,/, "");

  const result = await anthropicMessages({
    max_tokens: 4096,
    messages: [
      {
        role: "user",
        content: [
          {
            type: "image",
            source: { type: "base64", media_type: mime, data },
          },
          {
            type: "text",
            text: `Extract class aggregate tallies from a PedaStudio assessor data-entry sheet.

Assessment: ${tool.title}
Items: ${allItems(tool).map((i) => i.id).join(", ")}

The sheet has columns: Item | A | B | C | D | ✓ | NA (counts per item).

Return ONLY valid JSON:
{
  "studentsAssessed": 30,
  "tallies": [
    { "itemId": "A1-01", "format": "mcq", "countA": 5, "countB": 2, "countC": 1, "countD": 0, "notAssessed": 0 },
    { "itemId": "A1-02", "format": "subjective", "correctCount": 20, "notAssessed": 2 }
  ]
}`,
          },
        ],
      },
    ],
  });

  if (!result.ok) {
    throw new Error("Could not read tally sheet");
  }

  const text = result.data.content?.[0]?.text || "{}";
  const parsed = parseJsonFromText(text) as TallySheetResponse;
  const warnings: string[] = [];
  const items = itemMap(tool);
  const tallies: ItemTally[] = [];

  for (const raw of parsed.tallies ?? []) {
    const itemId = String(raw.itemId || "");
    const item = items.get(itemId);
    if (!item) continue;
    if (item.format === "mcq" && raw.format === "mcq") {
      tallies.push({
        itemId,
        format: "mcq",
        countA: Number(raw.countA) || 0,
        countB: Number(raw.countB) || 0,
        countC: Number(raw.countC) || 0,
        countD: Number(raw.countD) || 0,
        notAssessed: Number(raw.notAssessed) || 0,
      });
    } else if (item.format === "subjective") {
      tallies.push({
        itemId,
        format: "subjective",
        correctCount: Number(raw.correctCount) || 0,
        notAssessed: Number(raw.notAssessed) || 0,
      });
    }
  }

  if (!tallies.length) warnings.push("No tally rows detected — try clearer photo or enter manually");

  return {
    tallies,
    studentsAssessed: Math.max(1, Number(parsed.studentsAssessed) || 0),
    warnings,
  };
}

function parseTallyPayloads(raw: unknown[], tool: AssessmentTool): ItemTally[] {
  const items = itemMap(tool);
  const out: ItemTally[] = [];
  for (const row of raw) {
    if (!row || typeof row !== "object") continue;
    const o = row as Record<string, unknown>;
    const itemId = String(o.itemId || "");
    const item = items.get(itemId);
    if (!item) continue;
    if (item.format === "mcq" && o.format === "mcq") {
      out.push({
        itemId,
        format: "mcq",
        countA: Number(o.countA) || 0,
        countB: Number(o.countB) || 0,
        countC: Number(o.countC) || 0,
        countD: Number(o.countD) || 0,
        notAssessed: o.notAssessed != null ? Number(o.notAssessed) : undefined,
      });
    } else if (item.format === "subjective") {
      out.push({
        itemId,
        format: "subjective",
        correctCount: Number(o.correctCount) || 0,
        notAssessed: o.notAssessed != null ? Number(o.notAssessed) : undefined,
      });
    }
  }
  return out;
}

export async function markAssessmentPapers(
  tool: AssessmentTool,
  images: ScanImageInput[],
  mode: PaperScanMode = "papers",
): Promise<PaperMarkResult> {
  const warnings: string[] = [];
  const perStudent: PerStudentMark[] = [];

  if (mode === "tally_sheet") {
    const sheet = images[0];
    if (!sheet) throw new Error("Add one photo of the completed tally sheet");
    const { tallies, studentsAssessed, warnings: w } = await markTallySheet(tool, sheet);
    return {
      studentsProcessed: studentsAssessed,
      tallies,
      perStudent: [],
      warnings: w,
    };
  }

  const tallyMap = emptyTallies(tool);
  const batchSize = 3;
  let processed = 0;

  for (let i = 0; i < images.length; i += batchSize) {
    const batch = images.slice(i, i + batchSize);
    const results = await Promise.all(
      batch.map((img, j) => markOnePaper(tool, img, i + j).catch((err) => ({
        answers: [] as StudentAnswerRow[],
        label: `Student ${i + j + 1}`,
        warning: err instanceof Error ? err.message : "Marking failed",
      }))),
    );

    for (let j = 0; j < results.length; j++) {
      const r = results[j];
      if (r.warning) warnings.push(r.warning);
      if (!r.answers.length) continue;
      applyStudentAnswers(tallyMap, tool, r.answers);
      processed += 1;
      perStudent.push({
        index: i + j,
        label: r.label,
        scorePercent: scoreStudent(tool, r.answers),
      });
    }
  }

  if (!processed) {
    warnings.push("No papers could be marked — check lighting and try the tally sheet option");
  }

  return {
    studentsProcessed: processed,
    tallies: [...tallyMap.values()],
    perStudent,
    warnings,
  };
}

export { parseTallyPayloads };
