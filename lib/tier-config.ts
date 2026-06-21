/** Subscription tier for PedaStudio teachers. */
export type TierId = "basic" | "prime" | "max";

export const TIER_IDS: TierId[] = ["basic", "prime", "max"];

export const TIER_LABELS: Record<TierId, string> = {
  basic: "Basic",
  prime: "Prime",
  max: "Max",
};

export interface TierLimits {
  plansPerMonth: number | null;
  worksheetsPerMonth: number | null;
  scansPerMonth: number | null;
  maxClasses: number | null;
  maxStudentsPerClass: number;
  ocrScansPerMonth: number | null;
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

const GRADES_BASIC = [1, 2, 3];
const GRADES_PRIME = [1, 2, 3, 4, 5];
const GRADES_MAX = [1, 2, 3, 4, 5, 6, 7, 8];

export const TIER_LIMITS: Record<TierId, TierLimits> = {
  basic: {
    plansPerMonth: 20,
    worksheetsPerMonth: 0,
    scansPerMonth: 0,
    maxClasses: 1,
    maxStudentsPerClass: 45,
    ocrScansPerMonth: 0,
  },
  prime: {
    plansPerMonth: null,
    worksheetsPerMonth: 10,
    scansPerMonth: 15,
    maxClasses: 2,
    maxStudentsPerClass: 45,
    ocrScansPerMonth: 120,
  },
  max: {
    plansPerMonth: null,
    worksheetsPerMonth: null,
    scansPerMonth: 60,
    maxClasses: null,
    maxStudentsPerClass: 60,
    ocrScansPerMonth: null,
  },
};

export const TIER_FEATURES: Record<TierId, TierFeatures> = {
  basic: {
    unlimitedPlans: false,
    planModesAlways: false,
    planModesAfterUnitTest: true,
    baselineAssessment: true,
    endlineAssessment: true,
    unitTests: true,
    manualAssessmentEntry: true,
    fullAssessmentReports: true,
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
    hindiUrduUi: false,
    gradesAvailable: GRADES_BASIC,
  },
  prime: {
    unlimitedPlans: true,
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
    abilityGroups: false,
    clusterExport: false,
    hindiUrduUi: true,
    gradesAvailable: GRADES_PRIME,
  },
  max: {
    unlimitedPlans: true,
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
    gradesAvailable: GRADES_MAX,
  },
};

export function normalizeTierId(value: unknown): TierId {
  if (value === "prime" || value === "max" || value === "basic") return value;
  return "basic";
}

export function currentUsageMonth(): string {
  const now = new Date();
  return `${now.getUTCFullYear()}-${String(now.getUTCMonth() + 1).padStart(2, "0")}`;
}

export type UsageCounterKey = "plans" | "worksheets" | "scans" | "ocrScans";

export interface UsageSnapshot {
  month: string;
  plans: number;
  worksheets: number;
  scans: number;
  ocrScans: number;
}

export function emptyUsage(month = currentUsageMonth()): UsageSnapshot {
  return { month, plans: 0, worksheets: 0, scans: 0, ocrScans: 0 };
}
