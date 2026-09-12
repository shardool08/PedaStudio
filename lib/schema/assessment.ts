/** Firestore: users/{uid}/assessments/{id} */

export type AssessmentType = "baseline" | "unit" | "endline";

export interface ItemTallyMcqDoc {
  itemId: string;
  format: "mcq";
  countA: number;
  countB: number;
  countC: number;
  countD: number;
  notAssessed?: number;
}

export interface ItemTallySubjectiveDoc {
  itemId: string;
  format: "subjective";
  correctCount: number;
  notAssessed?: number;
}

export type ItemTallyDoc = ItemTallyMcqDoc | ItemTallySubjectiveDoc;

export interface StrandScoreDoc {
  strand: string;
  label: string;
  percent: number;
}

export interface AssessmentDocument {
  type: AssessmentType;
  grade: number;
  subject: string;
  groupId?: string | null;
  groupName?: string | null;
  toolId?: string | null;
  scorePercent: number;
  studentsAssessed: number;
  notes?: string;
  /** Item-level tallies when using structured assessment tools. */
  tallies?: ItemTallyDoc[];
  strandScores?: StrandScoreDoc[];
  weakItems?: string[];
  createdAt: string;
  updatedAt: string;
  savedAt?: string;
}
