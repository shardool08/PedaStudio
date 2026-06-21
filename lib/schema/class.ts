/** Firestore: users/{uid}/classes/{classId} — scaffold for Max tier ability groups. */

export interface ClassDocument {
  name: string;
  grade: number;
  subject: string;
  studentCount: number;
  abilityGroup?: string;
  createdAt: string;
  updatedAt: string;
}
