import type { TierId, UsageSnapshot } from "@/lib/tier-config";
import type { SubscriptionRecord } from "@/lib/subscription-service";

/** Firestore: users/{uid} */
export interface TeacherProfile {
  teacherName: string;
  phoneNumber: string;
  language: string;
  state: string;
  district: string;
  adminType: string;
  zpName: string;
  corpName: string;
  medium: string;
  englishComfort: string;
  schoolName: string;
  location: string;
  pinCode: string;
  studentCount: number;
  internetAccess: string;
  printingAccess: string;
  teacherGrades: number[];
  teacherSubjects: string[];
  teacherResources: string[];
  currentLessons: Record<string, string>;
  profileComplete: boolean;
  updatedAt?: FirebaseFirestore.Timestamp;
}

/** Server-only fields on users/{uid} — clients cannot write these. */
export interface TeacherAccountFields {
  tier: TierId;
  tierExpiresAt?: FirebaseFirestore.Timestamp;
  usage: UsageSnapshot;
  subscription: Partial<SubscriptionRecord>;
}

export type UserDocument = TeacherProfile & Partial<TeacherAccountFields>;

/** Summary row for admin teacher list. */
export interface TeacherSummary {
  uid: string;
  teacherName: string;
  phoneNumber: string;
  district: string;
  schoolName: string;
  medium: string;
  tier: TierId;
  tierExpiresAt: string | null;
  profileComplete: boolean;
  usage: UsageSnapshot;
  planCount: number;
  assessmentCount: number;
  updatedAt: string | null;
  createdAt: string | null;
}
