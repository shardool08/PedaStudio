import { createHmac, timingSafeEqual } from "crypto";
import {
  findPaidPlan,
  isPaymentsConfigured,
  isRazorpayTestMode,
  razorpayPublicKey,
  subscriptionPeriodDays,
  type BillingCycle,
  type PaidPlan,
} from "@/lib/subscription-config";
import { TIER_LABELS, getPilotTierOverride, type TierId } from "@/lib/tier-config";
import { getTeacherAccount, accountToJson } from "@/lib/tier-service";
import {
  findTeacherByOrderId,
  getTeacher,
  isTeacherStoreReady,
  paymentRecorded,
  recordPayment,
  tierFromRecord,
  updateTeacher,
  type TeacherRecord,
} from "@/lib/supabase/teachers";

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

const NO_SUBSCRIPTION: SubscriptionRecord = {
  status: "none",
  planId: null,
  billingCycle: null,
  tier: "basic",
  startedAt: null,
  expiresAt: null,
  razorpayOrderId: null,
  razorpayPaymentId: null,
};

export function parseSubscription(record: TeacherRecord | null): SubscriptionRecord {
  if (!record) return { ...NO_SUBSCRIPTION };

  const expiresIso = record.tier_expires_at ?? record.sub_expires_at ?? null;
  let tier = tierFromRecord({ ...record, tier_expires_at: null });
  const statusRaw = record.sub_status || "none";
  let status: SubscriptionRecord["status"] =
    statusRaw === "active" || statusRaw === "expired" || statusRaw === "pending" ? statusRaw : "none";

  const expired = !!expiresIso && new Date(expiresIso).getTime() < Date.now();

  if (expired) {
    tier = "basic";
    status = "expired";
  } else if (statusRaw === "pending") {
    status = "pending";
  } else if (tier !== "basic" && expiresIso) {
    status = "active";
  } else if (tier === "basic") {
    status = "none";
  }

  return {
    status,
    planId: record.sub_plan_id,
    billingCycle: (record.sub_billing_cycle as BillingCycle | null) ?? null,
    tier,
    startedAt: record.sub_started_at,
    expiresAt: expiresIso,
    razorpayOrderId: record.razorpay_order_id,
    razorpayPaymentId: record.razorpay_payment_id,
  };
}

export async function getSubscription(uid: string): Promise<SubscriptionRecord> {
  const pilotTier = getPilotTierOverride();
  if (pilotTier) {
    return {
      status: pilotTier === "basic" ? "none" : "active",
      planId: pilotTier === "basic" ? null : `${pilotTier}_pilot`,
      billingCycle: "monthly",
      tier: pilotTier,
      startedAt: new Date().toISOString(),
      expiresAt: null,
      razorpayOrderId: null,
      razorpayPaymentId: null,
    };
  }
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
  if (!isTeacherStoreReady()) return { ...NO_SUBSCRIPTION };
  return parseSubscription(await getTeacher(uid));
}

export async function getAccountWithSubscription(uid: string) {
  const account = await getTeacherAccount(uid);
  const subscription = await getSubscription(uid);
  const effectiveSubscription = { ...subscription, tier: account.tier };
  return {
    ...accountToJson(account),
    subscription: effectiveSubscription,
    isTrial:
      effectiveSubscription.planId === "max_trial" &&
      effectiveSubscription.status === "active" &&
      account.tier === "max",
    paymentsEnabled: isPaymentsConfigured(),
    razorpayKeyId: razorpayPublicKey(),
    razorpayTestMode: isRazorpayTestMode(),
  };
}

export async function createRazorpayOrder(
  uid: string,
  planId: string,
  teacherName?: string,
  teacherPhone?: string,
) {
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

  await updateTeacher(uid, {
    sub_status: "pending",
    sub_plan_id: plan.id,
    sub_billing_cycle: plan.billingCycle,
    sub_tier: plan.tier,
    razorpay_order_id: data.id as string,
  });

  const phoneDigits = teacherPhone?.replace(/\D/g, "").slice(-10);

  return {
    orderId: data.id as string,
    amountPaise: plan.amountPaise,
    amountInr: plan.amountInr,
    currency: plan.currency,
    keyId: razorpayPublicKey(),
    plan,
    prefill: {
      name: teacherName || "Teacher",
      contact: phoneDigits && phoneDigits.length === 10 ? phoneDigits : undefined,
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

export function verifyRazorpayWebhookSignature(rawBody: string, signature: string): boolean {
  const secret = process.env.RAZORPAY_WEBHOOK_SECRET?.trim();
  if (!secret) return false;
  const expected = createHmac("sha256", secret).update(rawBody).digest("hex");
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
  if (!isTeacherStoreReady()) throw new Error("Database unavailable");

  const days = subscriptionPeriodDays(plan.billingCycle);
  const expiresAt = new Date();
  expiresAt.setUTCDate(expiresAt.getUTCDate() + days);

  await updateTeacher(uid, {
    tier: plan.tier,
    tier_expires_at: expiresAt.toISOString(),
    sub_status: "active",
    sub_plan_id: plan.id,
    sub_billing_cycle: plan.billingCycle,
    sub_tier: plan.tier,
    sub_started_at: new Date().toISOString(),
    sub_expires_at: expiresAt.toISOString(),
    razorpay_order_id: orderId,
    razorpay_payment_id: paymentId,
  });

  await recordPayment({
    teacherId: uid,
    planId: plan.id,
    tier: plan.tier,
    billingCycle: plan.billingCycle,
    amountPaise: plan.amountPaise,
    orderId,
    paymentId,
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

  const pendingOrderId = (await getTeacher(uid))?.razorpay_order_id;
  if (pendingOrderId && pendingOrderId !== orderId) {
    throw new Error("Order does not match your pending checkout");
  }

  if (await paymentRecorded(uid, paymentId)) {
    return getAccountWithSubscription(uid);
  }

  await activateSubscription(uid, plan, orderId, paymentId);
  return getAccountWithSubscription(uid);
}

/** Idempotent activation from Razorpay webhook (payment.captured). */
export async function activateFromWebhook(orderId: string, paymentId: string) {
  if (!isTeacherStoreReady()) return { ok: false as const, reason: "database_unavailable" };

  const uid = await findTeacherByOrderId(orderId);
  if (!uid) {
    console.warn("Razorpay webhook: no user for order", orderId);
    return { ok: false as const, reason: "user_not_found" };
  }

  const planId = (await getTeacher(uid))?.sub_plan_id;
  if (!planId) {
    console.warn("Razorpay webhook: no planId on pending order", orderId);
    return { ok: false as const, reason: "no_plan" };
  }

  const plan = findPaidPlan(planId);
  if (!plan) {
    console.warn("Razorpay webhook: invalid planId", planId);
    return { ok: false as const, reason: "invalid_plan" };
  }

  if (await paymentRecorded(uid, paymentId)) {
    return { ok: true as const, uid, duplicate: true };
  }

  await activateSubscription(uid, plan, orderId, paymentId);
  return { ok: true as const, uid, duplicate: false };
}

export function plansCatalogJson() {
  return {
    paymentsEnabled: isPaymentsConfigured(),
    razorpayKeyId: razorpayPublicKey(),
    razorpayTestMode: isRazorpayTestMode(),
    supportWhatsApp: process.env.PEDASTUDIO_SUPPORT_WHATSAPP || "919876543210",
    supportEmail: process.env.PEDASTUDIO_SUPPORT_EMAIL || "support@pedastudio.in",
    tiers: TIER_LABELS,
  };
}
