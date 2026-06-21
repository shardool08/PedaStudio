export type { TeacherProfile, TeacherAccountFields, UserDocument, TeacherSummary } from "./user";
export type {
  LessonPlanContent,
  PlanDocument,
  PlanStatus,
} from "./plan";
export type { AssessmentDocument, AssessmentType } from "./assessment";
export type {
  TlmResourceItem,
  TlmCatalogDocument,
  FlashcardItem,
  FlashcardLessonDocument,
} from "./catalog";
export type { PaymentDocument } from "./payment";
export type { ClassDocument } from "./class";

/** Top-level Firestore collection paths. */
export const COLLECTIONS = {
  users: "users",
  catalog: "catalog",
  tlmResources: "catalog/tlmResources",
  flashcards: "catalog/flashcards/lessons",
} as const;

export const USER_SUBCOLLECTIONS = {
  plans: "plans",
  assessments: "assessments",
  classes: "classes",
  payments: "payments",
} as const;
