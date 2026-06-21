/** Firestore: users/{uid}/assessments/{id} */

export type AssessmentType = "baseline" | "unit" | "endline";

export interface AssessmentDocument {
  type: AssessmentType;
  grade: number;
  subject: string;
  groupId?: string | null;
  groupName?: string | null;
  scorePercent: number;
  studentsAssessed: number;
  notes?: string;
  createdAt: string;
  updatedAt: string;
  savedAt?: FirebaseFirestore.Timestamp;
}
