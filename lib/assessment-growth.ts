import type { AssessmentRecord } from "@/lib/assessment-service";

export interface GrowthComparison {
  baselinePercent: number | null;
  endlinePercent: number | null;
  growthPoints: number | null;
  baselineStudents: number | null;
  endlineStudents: number | null;
}

export function buildGrowthComparison(
  baseline: AssessmentRecord | null,
  endline: AssessmentRecord | null,
): GrowthComparison {
  const baselinePercent = baseline?.scorePercent ?? null;
  const endlinePercent = endline?.scorePercent ?? null;
  const growthPoints =
    baselinePercent != null && endlinePercent != null
      ? Math.round((endlinePercent - baselinePercent) * 10) / 10
      : null;
  return {
    baselinePercent,
    endlinePercent,
    growthPoints,
    baselineStudents: baseline?.studentsAssessed ?? null,
    endlineStudents: endline?.studentsAssessed ?? null,
  };
}
