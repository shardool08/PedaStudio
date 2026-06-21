import { FieldValue, Timestamp, getFirestore } from "firebase-admin/firestore";
import { getAdminApp } from "@/lib/firebase/admin";
import type { TeacherSummary, UserDocument } from "@/lib/schema";
import { parseSubscription } from "@/lib/subscription-service";
import {
  currentUsageMonth,
  emptyUsage,
  normalizeTierId,
  TIER_LABELS,
  type TierId,
  type UsageSnapshot,
} from "@/lib/tier-config";

function getDb() {
  const app = getAdminApp();
  if (!app) return null;
  return getFirestore(app);
}

function mergeUsage(raw: Record<string, unknown> | undefined): UsageSnapshot {
  const month = currentUsageMonth();
  if (!raw || raw.month !== month) return emptyUsage(month);
  return {
    month,
    plans: Number(raw.plans) || 0,
    worksheets: Number(raw.worksheets) || 0,
    scans: Number(raw.scans) || 0,
    ocrScans: Number(raw.ocrScans) || 0,
  };
}

function tsToIso(ts: Timestamp | undefined): string | null {
  return ts?.toDate?.()?.toISOString() ?? null;
}

function docToSummary(
  uid: string,
  data: FirebaseFirestore.DocumentData,
  counts: { plans: number; assessments: number },
): TeacherSummary {
  const tier = normalizeTierId(data.tier);
  const tierExpiresAt = tsToIso(data.tierExpiresAt as Timestamp | undefined);
  return {
    uid,
    teacherName: String(data.teacherName || "—"),
    phoneNumber: String(data.phoneNumber || "—"),
    district: String(data.district || "—"),
    schoolName: String(data.schoolName || "—"),
    medium: String(data.medium || "—"),
    tier,
    tierExpiresAt,
    profileComplete: Boolean(data.profileComplete),
    usage: mergeUsage(data.usage as Record<string, unknown> | undefined),
    planCount: counts.plans,
    assessmentCount: counts.assessments,
    updatedAt: tsToIso(data.updatedAt as Timestamp | undefined),
    createdAt: tsToIso(data.createdAt as Timestamp | undefined),
  };
}

export interface DashboardStats {
  totalTeachers: number;
  profileComplete: number;
  tierCounts: Record<TierId, number>;
  usageThisMonth: {
    plans: number;
    worksheets: number;
    scans: number;
    ocrScans: number;
  };
  catalog: {
    tlmResources: number;
    flashcardLessons: number;
  };
}

export interface TeacherDetail extends TeacherSummary {
  profile: Partial<UserDocument>;
  subscription: ReturnType<typeof parseSubscription>;
  recentPlans: Array<{
    id: string;
    lessonId: string;
    day: number;
    status: string;
    updatedAt: string | null;
  }>;
  recentAssessments: Array<{
    id: string;
    type: string;
    grade: number;
    scorePercent: number;
    updatedAt: string;
  }>;
  payments: Array<{
    id: string;
    tier: string;
    amountPaise: number;
    createdAt: string | null;
  }>;
}

export interface ListTeachersResult {
  teachers: TeacherSummary[];
  nextCursor: string | null;
  total: number;
}

export async function getDashboardStats(): Promise<DashboardStats> {
  const db = getDb();
  if (!db) throw new Error("Database unavailable");

  const usersSnap = await db.collection("users").get();
  const tierCounts: Record<TierId, number> = { basic: 0, prime: 0, max: 0 };
  let profileComplete = 0;
  const usageThisMonth = { plans: 0, worksheets: 0, scans: 0, ocrScans: 0 };

  for (const doc of usersSnap.docs) {
    const data = doc.data();
    const tier = normalizeTierId(data.tier);
    tierCounts[tier]++;
    if (data.profileComplete) profileComplete++;
    const usage = mergeUsage(data.usage as Record<string, unknown> | undefined);
    usageThisMonth.plans += usage.plans;
    usageThisMonth.worksheets += usage.worksheets;
    usageThisMonth.scans += usage.scans;
    usageThisMonth.ocrScans += usage.ocrScans;
  }

  let tlmResources = 0;
  let flashcardLessons = 0;
  const tlmSnap = await db.doc("catalog/tlmResources").get();
  if (tlmSnap.exists) {
    const resources = tlmSnap.data()?.resources;
    tlmResources = Array.isArray(resources) ? resources.length : 0;
  }
  const fcSnap = await db.collection("catalog/flashcards/lessons").get();
  flashcardLessons = fcSnap.size;

  return {
    totalTeachers: usersSnap.size,
    profileComplete,
    tierCounts,
    usageThisMonth,
    catalog: { tlmResources, flashcardLessons },
  };
}

export async function listTeachers(options: {
  limit?: number;
  cursor?: string;
  search?: string;
  tier?: TierId;
}): Promise<ListTeachersResult> {
  const db = getDb();
  if (!db) throw new Error("Database unavailable");

  const limit = Math.min(options.limit ?? 50, 100);
  let query: FirebaseFirestore.Query = db
    .collection("users")
    .orderBy("updatedAt", "desc");

  if (options.tier) {
    query = db.collection("users").where("tier", "==", options.tier).orderBy("updatedAt", "desc");
  }

  if (options.cursor) {
    const cursorDoc = await db.collection("users").doc(options.cursor).get();
    if (cursorDoc.exists) query = query.startAfter(cursorDoc);
  }

  const snap = await query.limit(limit + 1).get();
  const docs = snap.docs.slice(0, limit);
  const hasMore = snap.docs.length > limit;

  const search = options.search?.trim().toLowerCase();
  const teachers: TeacherSummary[] = [];

  for (const doc of docs) {
    const data = doc.data();
    if (search) {
      const haystack = [
        data.teacherName,
        data.phoneNumber,
        data.district,
        data.schoolName,
      ]
        .map(String)
        .join(" ")
        .toLowerCase();
      if (!haystack.includes(search)) continue;
    }

    const [plansSnap, assessSnap] = await Promise.all([
      doc.ref.collection("plans").count().get(),
      doc.ref.collection("assessments").count().get(),
    ]);

    teachers.push(
      docToSummary(doc.id, data, {
        plans: plansSnap.data().count,
        assessments: assessSnap.data().count,
      }),
    );
  }

  const totalSnap = await db.collection("users").count().get();

  return {
    teachers,
    nextCursor: hasMore ? docs[docs.length - 1]?.id ?? null : null,
    total: totalSnap.data().count,
  };
}

export async function getTeacherDetail(uid: string): Promise<TeacherDetail | null> {
  const db = getDb();
  if (!db) throw new Error("Database unavailable");

  const doc = await db.collection("users").doc(uid).get();
  if (!doc.exists) return null;
  const data = doc.data()!;

  const [plansSnap, assessSnap, paymentsSnap] = await Promise.all([
    doc.ref.collection("plans").orderBy("updatedAt", "desc").limit(10).get(),
    doc.ref
      .collection("assessments")
      .orderBy("updatedAt", "desc")
      .limit(10)
      .get(),
    doc.ref.collection("payments").orderBy("createdAt", "desc").limit(10).get(),
  ]);

  const summary = docToSummary(uid, data, {
    plans: (await doc.ref.collection("plans").count().get()).data().count,
    assessments: (await doc.ref.collection("assessments").count().get()).data().count,
  });

  return {
    ...summary,
    profile: data as Partial<UserDocument>,
    subscription: parseSubscription(data),
    recentPlans: plansSnap.docs.map((p) => {
      const d = p.data();
      return {
        id: p.id,
        lessonId: String(d.lessonId || ""),
        day: Number(d.day) || 0,
        status: String(d.status || "not_started"),
        updatedAt: tsToIso(d.updatedAt as Timestamp | undefined),
      };
    }),
    recentAssessments: assessSnap.docs.map((a) => {
      const d = a.data();
      return {
        id: a.id,
        type: String(d.type || ""),
        grade: Number(d.grade) || 0,
        scorePercent: Number(d.scorePercent) || 0,
        updatedAt: String(d.updatedAt || ""),
      };
    }),
    payments: paymentsSnap.docs.map((p) => {
      const d = p.data();
      return {
        id: p.id,
        tier: String(d.tier || ""),
        amountPaise: Number(d.amountPaise) || 0,
        createdAt: tsToIso(d.createdAt as Timestamp | undefined),
      };
    }),
  };
}

export async function setTeacherTier(
  uid: string,
  tier: TierId,
  days?: number,
): Promise<void> {
  const db = getDb();
  if (!db) throw new Error("Database unavailable");

  const payload: Record<string, unknown> = {
    tier,
    updatedAt: FieldValue.serverTimestamp(),
  };

  if (days && tier !== "basic") {
    const expires = new Date();
    expires.setUTCDate(expires.getUTCDate() + days);
    payload.tierExpiresAt = expires;
  } else if (tier === "basic") {
    payload.tierExpiresAt = FieldValue.delete();
  }

  await db.collection("users").doc(uid).set(payload, { merge: true });
}

export async function resetTeacherUsage(uid: string): Promise<void> {
  const db = getDb();
  if (!db) throw new Error("Database unavailable");
  await db.collection("users").doc(uid).set(
    { usage: emptyUsage(), updatedAt: FieldValue.serverTimestamp() },
    { merge: true },
  );
}

export async function ensureUserDefaults(uid: string): Promise<void> {
  const db = getDb();
  if (!db) return;
  const ref = db.collection("users").doc(uid);
  const snap = await ref.get();
  if (!snap.exists) {
    await ref.set({
      tier: "basic",
      usage: emptyUsage(),
      createdAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    });
    return;
  }
  const data = snap.data();
  const patch: Record<string, unknown> = {};
  if (!data?.tier) patch.tier = "basic";
  if (!data?.usage) patch.usage = emptyUsage();
  if (!data?.createdAt) patch.createdAt = FieldValue.serverTimestamp();
  if (Object.keys(patch).length) {
    patch.updatedAt = FieldValue.serverTimestamp();
    await ref.set(patch, { merge: true });
  }
}

export async function getCatalogStatus() {
  const db = getDb();
  if (!db) throw new Error("Database unavailable");

  const tlmSnap = await db.doc("catalog/tlmResources").get();
  const fcSnap = await db.collection("catalog/flashcards/lessons").get();

  const tlmData = tlmSnap.data();
  const resources = Array.isArray(tlmData?.resources) ? tlmData.resources : [];

  return {
    tlm: {
      exists: tlmSnap.exists,
      count: resources.length,
      updatedAt: tsToIso(tlmData?.updatedAt as Timestamp | undefined),
    },
    flashcards: {
      lessonCount: fcSnap.size,
      lessons: fcSnap.docs.map((d) => {
        const data = d.data();
        return {
          id: d.id,
          lessonId: String(data.lessonId || d.id.replace(/_/g, ".")),
          title: String(data.title || ""),
          cardCount: Array.isArray(data.cards) ? data.cards.length : 0,
          updatedAt: tsToIso(data.updatedAt as Timestamp | undefined),
        };
      }),
    },
  };
}

export { TIER_LABELS };
