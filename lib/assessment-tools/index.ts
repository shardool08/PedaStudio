import type { AssessmentTool } from "./types";
import { grade1L2Baseline } from "./content/grade1-l2/baseline";
import { grade1L2Endline } from "./content/grade1-l2/endline";
import { grade1L2UnitTests, getGrade1L2UnitTest } from "./content/grade1-l2/units";
import { generateAssessmentTool, mediumForBookTrack } from "./generate";
import { getAssessmentGroupsForMedium } from "@/lib/curriculum";

export type { AssessmentTool, AssessmentItem, AssessmentReport, ItemTally, AssessmentResultInput } from "./types";
export { FLN_STRAND_LABELS, GRADE_FLN_PROFILES, getGradeFlnProfile } from "./fln-goals";
export { buildAssessmentReport, validateTallies, scoreItem } from "./scoring";
export { renderAssessmentMarkdown, renderDataEntryTemplate } from "./render";
export { generateAssessmentTool } from "./generate";

export interface AssessmentToolKey {
  grade: number;
  bookTrack: "l1" | "l2";
  subject?: string;
  type: "baseline" | "unit" | "endline";
  groupId?: string;
}

function handcraftedTool(key: AssessmentToolKey): AssessmentTool | undefined {
  const { grade, bookTrack, type, groupId } = key;
  if (grade !== 1 || bookTrack !== "l2") return undefined;
  if (type === "baseline") return grade1L2Baseline;
  if (type === "endline") return grade1L2Endline;
  if (type === "unit" && groupId) return getGrade1L2UnitTest(groupId);
  return undefined;
}

/** Resolve assessment tool by grade, book track, and type. */
export function getAssessmentTool(key: AssessmentToolKey): AssessmentTool | undefined {
  const handcrafted = handcraftedTool(key);
  if (handcrafted) return handcrafted;

  const subject = key.subject || "english";
  return generateAssessmentTool({
    grade: key.grade,
    subject,
    bookTrack: key.bookTrack,
    type: key.type,
    groupId: key.groupId,
  });
}

/** List all tools available for a grade + book track + subject. */
export function listAssessmentTools(
  grade: number,
  bookTrack: "l1" | "l2",
  subject = "english",
): AssessmentTool[] {
  const tools: AssessmentTool[] = [];

  const baseline = getAssessmentTool({ grade, bookTrack, subject, type: "baseline" });
  if (baseline) tools.push(baseline);

  const endline = getAssessmentTool({ grade, bookTrack, subject, type: "endline" });
  if (endline) tools.push(endline);

  const medium = mediumForBookTrack(bookTrack);
  const groups = getAssessmentGroupsForMedium(grade, subject, medium);

  for (const groupId of Object.keys(groups)) {
    const unit = getAssessmentTool({ grade, bookTrack, subject, type: "unit", groupId });
    if (unit) tools.push(unit);
  }

  return tools;
}

/** Map teacher medium string to book track. */
export function mediumToBookTrack(medium: string): "l1" | "l2" {
  const m = medium.toLowerCase().replace("_", "-");
  if (m === "english" || m === "semi-english") return "l1";
  return "l2";
}
