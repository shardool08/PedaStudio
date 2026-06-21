import { createHmac, timingSafeEqual } from "crypto";
import { FieldValue, Timestamp } from "firebase-admin/firestore";
import {
  findPaidPlan,
  isPaymentsConfigured,
  razorpayPublicKey,
  subscriptionPeriodDays,
  type BillingCycle,
  type PaidPlan,
} from "@/lib/subscription-config";
import { getAdminApp } from "@/lib/firebase/admin";
import { getFirestore } from "firebase-admin/firestore";
import { normalizeTierId, TIER_LABELS, type TierId } from "@/lib/tier-config";
import { getTeacherAccount, accountToJson } from "@/lib/tier-service";

export interface SubscriptionRecord {
  status: "none" | "active" | "expired" | "pending";
  planId: string | null;
  billingCycle: BillingCycle | null;
  tier: TierId;
  startedAt: string | null;
  expiresAt: string | null;
  razorpayOrderId: string | null;
  razorpayPaymentId: string | null;
}

function getDb() {
  const app = getAdminApp();
  if (!app) return null;
  return getFirestore(app);
}

export function parseSubscription(data: Record<string, unknown> | undefined): SubscriptionRecord {
  const sub = (data?.subscription as Record<string, unknown> | undefined) ?? {};
  const tierExpiresAt = data?.tierExpiresAt as Timestamp | undefined;
  const expiresIso = tierExpiresAt?.toDate?.()?.toISOString() ?? (sub.expiresAt as string | null) ?? null;
  const tier = normalizeTierId(data?.tier);
  const statusRaw = (sub.status as string) || "none";
  let status: SubscriptionRecord["status"] =
    statusRaw === "active" || statusRaw === "expired" || statusRaw === "pending" ? statusRaw : "none";

  if (tier !== "basic" && expiresIso) {
    const expired = new Date(expiresIso).getTime() < Date.now();
    status = expired ? "expired" : "active";
  } else if (tier === "basic") {
    status = "none";
  }

  return {
    status,
    planId: (sub.planId as string) ?? null,
    billingCycle: (sub.billingCycle as BillingCycle) ?? null,
    tier,
    startedAt: (sub.startedAt as string) ?? null,
    expiresAt: expiresIso,
    razorpayOrderId: (sub.razorpayOrderId as string) ?? null,
    razorpayPaymentId: (sub.razorpayPaymentId as string) ?? null,
  };
}

export async function getSubscription(uid: string): Promise<SubscriptionRecord> {
  if (uid === "dev-local") {
    return {
      status: "active",
      planId: "prime_monthly",
      billingCycle: "monthly",
      tier: "prime",
      startedAt: new Date().toISOString(),
      expiresAt: new Date(Date.now() + 30 * 86400000).toISOString(),
      razorpayOrderId: null,
      razorpayPaymentId: null,
    };
  }
  const db = getDb();
  if (!db) {
    return {
      status: "none",
      planId: null,
      billingCycle: null,
      tier: "basic",
      startedAt: null,
      expiresAt: null,
      razorpayOrderId: null,
      razorpayPaymentId: null,
    };
  }
  const snap = await db.collection("users").doc(uid).get();
  return parseSubscription(snap.data() as Record<string, unknown> | undefined);
}

export async function getAccountWithSubscription(uid: string) {
  const account = await getTeacherAccount(uid);
  const subscription = await getSubscription(uid);
  return {
    ...accountToJson(account),
    subscription,
    paymentsEnabled: isPaymentsConfigured(),
    razorpayKeyId: razorpayPublicKey(),
  };
}

export async function createRazorpayOrder(uid: string, planId: string, teacherName?: string) {
  if (!isPaymentsConfigured()) {
    throw new Error("Online payments are not configured yet. Contact support for pilot access.");
  }

  const plan = findPaidPlan(planId);
  if (!plan) throw new Error("Invalid plan");

  const keyId = process.env.RAZORPAY_KEY_ID!;
  const keySecret = process.env.RAZORPAY_KEY_SECRET!;

  const receipt = `${uid.slice(0, 8)}_${plan.id}_${Date.now()}`.slice(0, 40);
  const body = {
    amount: plan.amountPaise,
    currency: plan.currency,
    receipt,
    notes: { uid, planId: plan.id, tier: plan.tier },
  };

  const response = await fetch("https://api.razorpay.com/v1/orders", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Basic ${Buffer.from(`${keyId}:${keySecret}`).toString("base64")}`,
    },
    body: JSON.stringify(body),
  });

  const data = await response.json();
  if (!response.ok) {
    console.error("Razorpay order error:", data);
    throw new Error("Could not start payment. Try again.");
  }

  const db = getDb();
  if (db) {
    await db.collection("users").doc(uid).set(
      {
        subscription: {
          status: "pending",
          planId: plan.id,
          billingCycle: plan.billingCycle,
          tier: plan.tier,
          razorpayOrderId: data.id,
          updatedAt: FieldValue.serverTimestamp(),
        },
      },
      { merge: true },
    );
  }

  return {
    orderId: data.id as string,
    amountPaise: plan.amountPaise,
    amountInr: plan.amountInr,
    currency: plan.currency,
    keyId: razorpayPublicKey(),
    plan,
    prefill: {
      name: teacherName || "Teacher",
    },
  };
}

export function verifyRazorpaySignature(orderId: string, paymentId: string, signature: string): boolean {
  const secret = process.env.RAZORPAY_KEY_SECRET;
  if (!secret) return false;
  const expected = createHmac("sha256", secret).update(`${orderId}|${paymentId}`).digest("hex");
  try {
    return timingSafeEqual(Buffer.from(expected), Buffer.from(signature));
  } catch {
    return false;
  }
}

export async function activateSubscription(
  uid: string,
  plan: PaidPlan,
  orderId: string,
  paymentId: string,
): Promise<void> {
  const db = getDb();
  if (!db) throw new Error("Database unavailable");

  const days = subscriptionPeriodDays(plan.billingCycle);
  const expiresAt = new Date();
  expiresAt.setUTCDate(expiresAt.getUTCDate() + days);

  await db.collection("users").doc(uid).set(
    {
      tier: plan.tier,
      tierExpiresAt: Timestamp.fromDate(expiresAt),
      subscription: {
        status: "active",
        planId: plan.id,
        billingCycle: plan.billingCycle,
        tier: plan.tier,
        startedAt: new Date().toISOString(),
        expiresAt: expiresAt.toISOString(),
        razorpayOrderId: orderId,
        razorpayPaymentId: paymentId,
        updatedAt: FieldValue.serverTimestamp(),
      },
      updatedAt: FieldValue.serverTimestamp(),
    },
    { merge: true },
  );

  await db.collection("users").doc(uid).collection("payments").doc(paymentId).set({
    planId: plan.id,
    tier: plan.tier,
    billingCycle: plan.billingCycle,
    amountPaise: plan.amountPaise,
    orderId,
    paymentId,
    createdAt: FieldValue.serverTimestamp(),
  });
}

export async function verifyAndActivatePayment(
  uid: string,
  orderId: string,
  paymentId: string,
  signature: string,
  planId: string,
) {
  if (!verifyRazorpaySignature(orderId, paymentId, signature)) {
    throw new Error("Payment verification failed");
  }

  const plan = findPaidPlan(planId);
  if (!plan) throw new Error("Invalid plan");

  await activateSubscription(uid, plan, orderId, paymentId);
  return getAccountWithSubscription(uid);
}

export function plansCatalogJson() {
  return {
    paymentsEnabled: isPaymentsConfigured(),
    razorpayKeyId: razorpayPublicKey(),
    supportWhatsApp: process.env.PEDASTUDIO_SUPPORT_WHATSAPP || "919876543210",
    supportEmail: process.env.PEDASTUDIO_SUPPORT_EMAIL || "support@pedastudio.in",
    tiers: TIER_LABELS,
  };
}
