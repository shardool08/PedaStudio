/** Firestore: users/{uid}/plans/{lessonId_dayN} */

export interface PlanObjective {
  blooms_level: string;
  text: string;
  success_criteria: string;
}

export interface PlanHook {
  duration: string;
  steps: string[];
  teacher_says: string;
  youtube_query: string;
}

export interface PlanTlmItem {
  name: string;
  is_printable: boolean;
  description: string;
}

export interface PlanActivity {
  name: string;
  duration: string;
  steps: string[];
  tip: string;
}

export interface PlanPractice {
  mode: string;
  duration: string;
  steps: string[];
  student_action: string;
}

export interface PlanAssessment {
  type: string;
  duration: string;
  questions: string[];
  exit_token: string;
  needs_worksheet: boolean;
}

export interface PlanClosure {
  duration: string;
  instruction: string;
}

export interface LessonPlanContent {
  objective: PlanObjective;
  hook: PlanHook;
  tlm: { items: PlanTlmItem[] };
  activity: PlanActivity;
  practice: PlanPractice;
  assessment: PlanAssessment;
  closure: PlanClosure;
  vocabulary_focus: string[];
  board_plan: string;
}

export type PlanStatus = "not_started" | "planned" | "completed";

export interface PlanDocument {
  lessonId: string;
  day: number;
  plan: LessonPlanContent;
  selections: Record<string, string>;
  teacherResources: string;
  status: PlanStatus;
  feedback?: string;
  savedAt: number;
  completedAt?: number;
  updatedAt?: FirebaseFirestore.Timestamp;
}
