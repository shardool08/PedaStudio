/** Subscription tier for PedaStudio teachers. */
export type TierId = "basic" | "prime" | "max";

export const TIER_IDS: TierId[] = ["basic", "prime", "max"];

export const TIER_LABELS: Record<TierId, string> = {
  basic: "Basic",
  prime: "Prime",
  max: "Max",
};

/** App ships Grades 1–5 English only (Balbharati). */
export const GRADES_APP = [1, 2, 3, 4, 5] as const;
const GRADES_FULL = [1, 2, 3, 4, 5];

/** Weekly AI usage caps (resets every Monday UTC). */
export const BASIC_PLANS_PER_WEEK = 2;
export const PRIME_PLANS_PER_WEEK = 6;
export const MAX_PLANS_PER_WEEK = 6;
export const PRIME_OCR_SCANS_PER_WEEK = 6;
export const MAX_SCANS_PER_WEEK = 2;
export const MAX_OCR_SCANS_PER_WEEK = 12;

/** One-time Max trial for new teachers (days). */
export const MAX_TRIAL_DAYS = 7;

export interface TierLimits {
  plansPerWeek: number | null;
  worksheetsPerWeek: number | null;
  scansPerWeek: number | null;
  maxClasses: number | null;
  maxStudentsPerClass: number;
  ocrScansPerWeek: number | null;
}

export interface TierFeatures {
  unlimitedPlans: boolean;
  planModesAlways: boolean;
  planModesAfterUnitTest: boolean;
  baselineAssessment: boolean;
  endlineAssessment: boolean;
  unitTests: boolean;
  manualAssessmentEntry: boolean;
  fullAssessmentReports: boolean;
  shortActionPlan: boolean;
  fullActionPlan: boolean;
  baselinePlanBand: boolean;
  unitTlmKit: boolean;
  tlmGapListUnit: boolean;
  yearTlmListView: boolean;
  yearTlmListFull: boolean;
  yearTlmPdfShare: boolean;
  bulkPaperScan: boolean;
  aiAutoMark: boolean;
  reportPdfExport: boolean;
  worksheets: boolean;
  textbookScan: boolean;
  perStudentLongitudinal: boolean;
  abilityGroups: boolean;
  clusterExport: boolean;
  hindiUrduUi: boolean;
  gradesAvailable: number[];
}

export const TIER_LIMITS: Record<TierId, TierLimits> = {
  basic: {
    plansPerWeek: BASIC_PLANS_PER_WEEK,
    worksheetsPerWeek: 0,
    scansPerWeek: 0,
    maxClasses: 1,
    maxStudentsPerClass: 45,
    ocrScansPerWeek: 0,
  },
  prime: {
    plansPerWeek: PRIME_PLANS_PER_WEEK,
    worksheetsPerWeek: null,
    scansPerWeek: 0,
    maxClasses: 1,
    maxStudentsPerClass: 45,
    ocrScansPerWeek: PRIME_OCR_SCANS_PER_WEEK,
  },
  max: {
    plansPerWeek: MAX_PLANS_PER_WEEK,
    worksheetsPerWeek: null,
    scansPerWeek: MAX_SCANS_PER_WEEK,
    maxClasses: 2,
    maxStudentsPerClass: 60,
    ocrScansPerWeek: MAX_OCR_SCANS_PER_WEEK,
  },
};

export const TIER_FEATURES: Record<TierId, TierFeatures> = {
  basic: {
    unlimitedPlans: false,
    planModesAlways: true,
    planModesAfterUnitTest: true,
    baselineAssessment: true,
    endlineAssessment: true,
    unitTests: true,
    manualAssessmentEntry: true,
    fullAssessmentReports: false,
    shortActionPlan: true,
    fullActionPlan: false,
    baselinePlanBand: true,
    unitTlmKit: true,
    tlmGapListUnit: true,
    yearTlmListView: true,
    yearTlmListFull: false,
    yearTlmPdfShare: false,
    bulkPaperScan: false,
    aiAutoMark: false,
    reportPdfExport: false,
    worksheets: false,
    textbookScan: false,
    perStudentLongitudinal: false,
    abilityGroups: false,
    clusterExport: false,
    hindiUrduUi: true,
    gradesAvailable: [...GRADES_FULL],
  },
  prime: {
    unlimitedPlans: false,
    planModesAlways: true,
    planModesAfterUnitTest: true,
    baselineAssessment: true,
    endlineAssessment: true,
    unitTests: true,
    manualAssessmentEntry: true,
    fullAssessmentReports: true,
    shortActionPlan: true,
    fullActionPlan: true,
    baselinePlanBand: true,
    unitTlmKit: true,
    tlmGapListUnit: true,
    yearTlmListView: true,
    yearTlmListFull: true,
    yearTlmPdfShare: true,
    bulkPaperScan: true,
    aiAutoMark: true,
    reportPdfExport: false,
    worksheets: true,
    textbookScan: false,
    perStudentLongitudinal: true,
    abilityGroups: false,
    clusterExport: false,
    hindiUrduUi: true,
    gradesAvailable: [...GRADES_FULL],
  },
  max: {
    unlimitedPlans: false,
    planModesAlways: true,
    planModesAfterUnitTest: true,
    baselineAssessment: true,
    endlineAssessment: true,
    unitTests: true,
    manualAssessmentEntry: true,
    fullAssessmentReports: true,
    shortActionPlan: true,
    fullActionPlan: true,
    baselinePlanBand: true,
    unitTlmKit: true,
    tlmGapListUnit: true,
    yearTlmListView: true,
    yearTlmListFull: true,
    yearTlmPdfShare: true,
    bulkPaperScan: true,
    aiAutoMark: true,
    reportPdfExport: true,
    worksheets: true,
    textbookScan: true,
    perStudentLongitudinal: true,
    abilityGroups: true,
    clusterExport: true,
    hindiUrduUi: true,
    gradesAvailable: [...GRADES_FULL],
  },
};

export function normalizeTierId(value: unknown): TierId {
  if (value === "prime" || value === "max" || value === "basic") return value;
  return "basic";
}

/** When set (e.g. PILOT_TIER=prime), all teachers get this tier from the API. Leave unset in production. */
export function getPilotTierOverride(): TierId | null {
  const raw = process.env.PILOT_TIER?.trim().toLowerCase();
  if (raw === "basic" || raw === "prime" || raw === "max") return raw;
  return null;
}

/** ISO date (YYYY-MM-DD) of the current week’s Monday, UTC. */
export function currentUsageWeek(): string {
  const now = new Date();
  const day = now.getUTCDay();
  const daysFromMonday = day === 0 ? 6 : day - 1;
  const monday = new Date(Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate() - daysFromMonday));
  return monday.toISOString().slice(0, 10);
}

export type UsageCounterKey = "plans" | "worksheets" | "scans" | "ocrScans";

export interface UsageSnapshot {
  week: string;
  plans: number;
  worksheets: number;
  scans: number;
  ocrScans: number;
}

export function emptyUsage(week = currentUsageWeek()): UsageSnapshot {
  return { week, plans: 0, worksheets: 0, scans: 0, ocrScans: 0 };
}
