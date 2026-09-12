import type { AssessmentItem, AssessmentTool } from "./types";

export interface PrintOptions {
  /** student = question paper only; assessor = includes answers + tally grid */
  copy: "student" | "assessor";
  className?: string;
  teacherName?: string;
  date?: string;
}

function tallyHeader(): string {
  return "| Item | A | B | C | D | ✓ | NA |\n|------|---|---|---|---|---|---|";
}

function tallyRow(item: AssessmentItem): string {
  if (item.format === "mcq") {
    return `| ${item.id} |   |   |   |   |   |   |`;
  }
  return `| ${item.id} | — | — | — | — |   |   |`;
}

function formatMcqOptions(item: AssessmentItem): string {
  if (!item.options) return "";
  return item.options.map((o) => `   ${o.key}) ${o.text}`).join("\n");
}

function formatItemStudent(item: AssessmentItem, index: number): string {
  const lines = [`**Q${index}.** (${item.marks} mark${item.marks > 1 ? "s" : ""}) ${item.stem}`];
  if (item.format === "mcq" && item.options) {
    lines.push(formatMcqOptions(item));
  } else if (item.format === "subjective") {
    lines.push("_Answer: _________________________________");
  }
  return lines.join("\n");
}

function formatItemAssessor(item: AssessmentItem, index: number): string {
  const lines = [formatItemStudent(item, index)];
  if (item.stimulusNote) lines.push(`_Stimulus: ${item.stimulusNote}_`);
  if (item.format === "mcq" && item.correctOption) {
    lines.push(`**Answer: ${item.correctOption}** — ${item.markingScheme}`);
  } else {
    lines.push(`**Marking:** ${item.markingScheme}`);
    if (item.correctResponse) lines.push(`**Correct:** ${item.correctResponse}`);
  }
  if (item.distractorNotes) lines.push(`_Note: ${item.distractorNotes}_`);
  lines.push(`FLN: ${item.flnStrand} | ${item.competency} | ${item.deliveryMode}`);
  return lines.join("\n");
}

/** Generate markdown suitable for PDF export or in-app print preview. */
export function renderAssessmentMarkdown(tool: AssessmentTool, opts: PrintOptions): string {
  const date = opts.date ?? new Date().toLocaleDateString("en-IN");
  const copyLabel = opts.copy === "student" ? "Student Copy" : "Assessor Copy (with Marking Scheme & Data Entry)";
  const header = [
    `# ${tool.title}`,
    `**${copyLabel}** | Grade ${tool.grade} English | ${tool.bookTrack.toUpperCase()} | v${tool.version}`,
    tool.groupName ? `**Unit test:** ${tool.groupName}` : "",
    tool.focus ? `_Focus: ${tool.focus}_` : "",
    opts.className ? `Class: ${opts.className}` : "",
    opts.teacherName ? `Teacher: ${opts.teacherName}` : "",
    `Date: _______________  |  Students assessed: _______`,
    "",
    `Total marks: **${tool.totalMarks}** | Time: ~${tool.recommendedMinutes} min`,
    "",
  ]
    .filter(Boolean)
    .join("\n");

  if (opts.copy === "assessor") {
    const notes = tool.administrationNotes.map((n) => `- ${n}`).join("\n");
    var adminBlock = `\n## Administration\n${notes}\n`;
  } else {
    var adminBlock = "\n_Please listen to your teacher. For oral questions, show your answer card._\n";
  }

  let itemIndex = 0;
  const sections = tool.sections
    .map((section) => {
      const items = section.items
        .map((item) => {
          itemIndex += 1;
          return opts.copy === "student"
            ? formatItemStudent(item, itemIndex)
            : formatItemAssessor(item, itemIndex);
        })
        .join("\n\n");
      return `## ${section.title}\n\n${items}`;
    })
    .join("\n\n---\n\n");

  let tallyBlock = "";
  if (opts.copy === "assessor") {
    const allItems = tool.sections.flatMap((s) => s.items);
    tallyBlock = [
      "",
      "---",
      "",
      "## Data Entry Sheet (Class Aggregate)",
      "",
      "Enter **how many students** chose each option (MCQ) or answered correctly (✓ column).",
      "NA = absent / not assessed for that item.",
      "",
      tallyHeader(),
      ...allItems.map(tallyRow),
      "",
      "_Transfer these numbers into the PedaStudio app to generate FLN & LO report._",
    ].join("\n");
  }

  return [header, adminBlock, sections, tallyBlock].join("\n");
}

/** Plain-text data entry template for offline use. */
export function renderDataEntryTemplate(tool: AssessmentTool): string {
  const rows = tool.sections.flatMap((s) => s.items);
  const lines = [
    `DATA ENTRY — ${tool.id}`,
    `Students assessed: _____`,
    "",
    "Format: itemId | A | B | C | D | correct | NA",
  ];
  for (const item of rows) {
    if (item.format === "mcq") {
      lines.push(`${item.id} | | | | | - |`);
    } else {
      lines.push(`${item.id} | - | - | - | - | |`);
    }
  }
  return lines.join("\n");
}
