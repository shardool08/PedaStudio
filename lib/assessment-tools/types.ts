/** Structured assessment tools — student paper, assessor copy, item-level data entry. */

export type AssessmentToolType = "baseline" | "unit" | "endline";

export type FlnStrand =
  | "OL"  // Oral language — listen, speak, follow instructions
  | "PA"  // Phonological awareness — rhyme, syllables, onset-rime
  | "DEC" // Decoding — letter-sound, CVC, digraphs (grade-dependent)
  | "RF"  // Reading fluency — word/sentence reading
  | "RC"  // Reading comprehension
  | "WR"  // Writing — letters, words, sentences (grade-dependent)
  | "VOC"; // Vocabulary in context

export type BloomsLevel = "Remember" | "Understand" | "Apply";

export type ItemFormat = "mcq" | "subjective";

/** How the teacher should administer the item in a municipal classroom. */
export type DeliveryMode =
  | "oral-group"      // Whole class; students show A/B/C/D cards or fingers
  | "oral-individual" // One-by-one (use for baseline spot-check or small class)
  | "written"         // Students mark on paper (Grade 3+)
  | "performance";    // TPR / demonstration (count students who perform correctly)

export interface McqOption {
  key: "A" | "B" | "C" | "D";
  text: string;
}

export interface AssessmentItem {
  id: string;
  section: string;
  flnStrand: FlnStrand;
  /** Lesson ids from curriculum (e.g. "1.1", "L1-2.3"). */
  lessonIds: string[];
  /** Short label from lesson competencies — for LO reports. */
  competency: string;
  bloomsLevel: BloomsLevel;
  format: ItemFormat;
  marks: number;
  deliveryMode: DeliveryMode;
  /** Question stem — keep short for oral delivery. */
  stem: string;
  /** e.g. "Show picture chart: body parts" — printed on assessor copy only. */
  stimulusNote?: string;
  options?: McqOption[];
  /** Required when format is mcq. */
  correctOption?: "A" | "B" | "C" | "D";
  markingScheme: string;
  /** For subjective / performance items — what counts as correct. */
  correctResponse?: string;
  /** Common wrong answers — helps inter-rater reliability during moderation. */
  distractorNotes?: string;
}

export interface AssessmentSection {
  id: string;
  title: string;
  flnStrand: FlnStrand;
  items: AssessmentItem[];
}

export interface AssessmentTool {
  id: string;
  type: AssessmentToolType;
  grade: number;
  subject: string;
  /** l1 = English Balbharati, l2 = My English Book */
  bookTrack: "l1" | "l2";
  title: string;
  description: string;
  /** For unit tests — maps to assessmentGroups id (e.g. A1, L1-A3). */
  groupId?: string;
  groupName?: string;
  lessonIds?: string[];
  focus?: string;
  totalMarks: number;
  recommendedMinutes: number;
  /** Teacher-facing steps before administering. */
  administrationNotes: string[];
  sections: AssessmentSection[];
  version: string;
}

/** Item-level tallies entered by teacher after conducting assessment. */
export interface ItemTallyMcq {
  itemId: string;
  format: "mcq";
  countA: number;
  countB: number;
  countC: number;
  countD: number;
  /** Students absent / not assessed for this item. */
  notAssessed?: number;
}

export interface ItemTallySubjective {
  itemId: string;
  format: "subjective";
  correctCount: number;
  notAssessed?: number;
}

export type ItemTally = ItemTallyMcq | ItemTallySubjective;

export interface AssessmentResultInput {
  toolId: string;
  studentsAssessed: number;
  tallies: ItemTally[];
  conductedAt?: string;
  notes?: string;
}

export interface ItemResult {
  itemId: string;
  flnStrand: FlnStrand;
  competency: string;
  marksPossible: number;
  marksObtained: number;
  percentCorrect: number;
  /** Item difficulty — proportion answering correctly. */
  pValue: number;
}

export interface StrandResult {
  strand: FlnStrand;
  label: string;
  marksPossible: number;
  marksObtained: number;
  percent: number;
  itemCount: number;
}

export interface AssessmentReport {
  toolId: string;
  title: string;
  type: AssessmentToolType;
  grade: number;
  studentsAssessed: number;
  totalMarksPossible: number;
  totalMarksObtained: number;
  scorePercent: number;
  items: ItemResult[];
  strands: StrandResult[];
  /** Items below 40% correct — flag for reteach. */
  weakItems: string[];
  /** Items with p below 0.2 or above 0.95 — flag for item review in pilot. */
  misfitItems: string[];
  generatedAt: string;
}
