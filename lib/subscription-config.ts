import type { TierId } from "@/lib/tier-config";
import { TIER_LABELS } from "@/lib/tier-config";

export type BillingCycle = "monthly" | "yearly";

export interface PaidPlan {
  id: string;
  tier: TierId;
  billingCycle: BillingCycle;
  amountPaise: number;
  amountInr: number;
  currency: "INR";
  label: string;
  periodLabel: string;
  savingsInr?: number;
}

export interface TierMarketing {
  tier: TierId;
  label: string;
  tagline: string;
  highlights: string[];
  badge?: string;
}

export const TIER_MARKETING: TierMarketing[] = [
  {
    tier: "basic",
    label: TIER_LABELS.basic,
    tagline: "Start planning and assessing your class",
    highlights: [
      "20 AI lesson plans per month",
      "Baseline, unit tests & endline (print + score)",
      "FLN, competency & LO reports",
      "Current unit teaching kit",
      "Grades 1–3 English",
    ],
  },
  {
    tier: "prime",
    label: TIER_LABELS.prime,
    tagline: "Save hours every week",
    badge: "Most popular",
    highlights: [
      "Unlimited lesson plans",
      "Worksheets & textbook scan",
      "Bulk paper scan + AI marking",
      "Full year TLM procurement list",
      "Grades 1–5 English",
    ],
  },
  {
    tier: "max",
    label: TIER_LABELS.max,
    tagline: "For SRGs and lead teachers",
    highlights: [
      "Everything in Prime",
      "Multiple classes & ability groups",
      "Cluster / HM export packs",
      "Priority scan processing",
      "Grades 1–8 English",
    ],
  },
];

export const PAID_PLANS: PaidPlan[] = [
  {
    id: "prime_monthly",
    tier: "prime",
    billingCycle: "monthly",
    amountPaise: 14900,
    amountInr: 149,
    currency: "INR",
    label: "Prime — Monthly",
    periodLabel: "per month",
  },
  {
    id: "prime_yearly",
    tier: "prime",
    billingCycle: "yearly",
    amountPaise: 129900,
    amountInr: 1299,
    currency: "INR",
    label: "Prime — Yearly",
    periodLabel: "per year",
    savingsInr: 489,
  },
  {
    id: "max_monthly",
    tier: "max",
    billingCycle: "monthly",
    amountPaise: 39900,
    amountInr: 399,
    currency: "INR",
    label: "Max — Monthly",
    periodLabel: "per month",
  },
  {
    id: "max_yearly",
    tier: "max",
    billingCycle: "yearly",
    amountPaise: 399900,
    amountInr: 3999,
    currency: "INR",
    label: "Max — Yearly",
    periodLabel: "per year",
    savingsInr: 789,
  },
];

export function findPaidPlan(planId: string): PaidPlan | undefined {
  return PAID_PLANS.find((p) => p.id === planId);
}

export function plansForTier(tier: TierId, cycle: BillingCycle): PaidPlan | undefined {
  return PAID_PLANS.find((p) => p.tier === tier && p.billingCycle === cycle);
}

export function isPaymentsConfigured(): boolean {
  return !!(process.env.RAZORPAY_KEY_ID && process.env.RAZORPAY_KEY_SECRET);
}

export function razorpayPublicKey(): string | null {
  const key = process.env.RAZORPAY_KEY_ID || process.env.NEXT_PUBLIC_RAZORPAY_KEY_ID;
  return key && key.length > 0 ? key : null;
}

export function subscriptionPeriodDays(cycle: BillingCycle): number {
  return cycle === "yearly" ? 365 : 30;
}
