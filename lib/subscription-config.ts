import type { TierId } from "@/lib/tier-config";
import { BASIC_PLANS_PER_WEEK, TIER_LABELS } from "@/lib/tier-config";

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
  /** Shown on yearly cards — e.g. 999/12 ≈ 83 */
  monthlyEquivalentInr?: number;
}

export interface TierMarketing {
  tier: TierId;
  label: string;
  tagline: string;
  highlights: string[];
  badge?: string;
}

/**
 * Sales comparison table (Basic | Prime | Max).
 * Served from API → Android subscription screen.
 */
export const TIER_COMPARISON: Array<{
  feature: string;
  basic: string;
  prime: string;
  max: string;
}> = [
  {
    feature: "Grades (English)",
    basic: "All Grades 1–5",
    prime: "All Grades 1–5",
    max: "All Grades 1–5",
  },
  {
    feature: "Lesson plans",
    basic: "2 per week",
    prime: "6 per week",
    max: "6 per week",
  },
  {
    feature: "Re-teach, practice & continue",
    basic: "✓",
    prime: "✓",
    max: "✓",
  },
  {
    feature: "Worksheets",
    basic: "—",
    prime: "1 with each lesson plan",
    max: "Unlimited",
  },
  {
    feature: "Unit tests",
    basic: "✓",
    prime: "✓",
    max: "✓",
  },
  {
    feature: "Annual assessment (baseline & endline)",
    basic: "✓",
    prime: "✓",
    max: "✓",
  },
  {
    feature: "Student skill report",
    basic: "Class summary",
    prime: "Full class skill map",
    max: "Full + PDF export",
  },
  {
    feature: "Learning-based lesson plans",
    basic: "—",
    prime: "From your test scores",
    max: "Priority tailoring",
  },
  {
    feature: "Bulk answer-sheet marking",
    basic: "—",
    prime: "✓",
    max: "✓ + faster queue",
  },
  {
    feature: "Scan & plan (textbook photo)",
    basic: "—",
    prime: "—",
    max: "2 per week",
  },
  {
    feature: "School cluster report pack",
    basic: "—",
    prime: "—",
    max: "✓",
  },
];

export const TIER_MARKETING: TierMarketing[] = [
  {
    tier: "basic",
    label: TIER_LABELS.basic,
    tagline: "Free — all grades, two plans a week",
    highlights: [
      `${BASIC_PLANS_PER_WEEK} lesson plans per week`,
      "All Grades 1–5 English",
      "Unit tests + annual baseline & endline",
      "Re-teach when students need more help",
    ],
  },
  {
    tier: "prime",
    label: TIER_LABELS.prime,
    tagline: "All Grades 1–5 · ₹2.75/day on annual plan",
    badge: "Best value",
    highlights: [
      "6 lesson plans per week",
      "Worksheet with every plan you make",
      "Learning-based plans from your test scores",
      "Bulk answer-sheet marking + skill reports",
    ],
  },
  {
    tier: "max",
    label: TIER_LABELS.max,
    tagline: "Scan-to-plan & cluster reports for SRG",
    highlights: [
      "Everything in Prime",
      "2 textbook scans per week → instant lesson plan",
      "School cluster report pack for SRG / Head Master",
      "Unlimited worksheets + PDF skill reports",
    ],
  },
];

/** Market-recommended pricing (Jun 2026). Prime is the hero tier; Max anchors Prime as the smart buy. */
const PAID_PLANS_RAW: PaidPlan[] = [
  {
    id: "prime_monthly",
    tier: "prime",
    billingCycle: "monthly",
    amountPaise: 12900,
    amountInr: 129,
    currency: "INR",
    label: "Prime — Monthly",
    periodLabel: "per month",
  },
  {
    id: "prime_yearly",
    tier: "prime",
    billingCycle: "yearly",
    amountPaise: 99900,
    amountInr: 999,
    currency: "INR",
    label: "Prime — Yearly",
    periodLabel: "per year",
  },
  {
    id: "max_monthly",
    tier: "max",
    billingCycle: "monthly",
    amountPaise: 27900,
    amountInr: 279,
    currency: "INR",
    label: "Max — Monthly",
    periodLabel: "per month",
  },
  {
    id: "max_yearly",
    tier: "max",
    billingCycle: "yearly",
    amountPaise: 229900,
    amountInr: 2299,
    currency: "INR",
    label: "Max — Yearly",
    periodLabel: "per year",
  },
];

function enrichYearlyPlans(plans: PaidPlan[]): PaidPlan[] {
  return plans.map((plan) => {
    if (plan.billingCycle !== "yearly") return plan;
    const monthly = plans.find((p) => p.tier === plan.tier && p.billingCycle === "monthly");
    if (!monthly) return plan;
    const savingsInr = Math.max(0, monthly.amountInr * 12 - plan.amountInr);
    const monthlyEquivalentInr = Math.round(plan.amountInr / 12);
    return { ...plan, savingsInr, monthlyEquivalentInr };
  });
}

export const PAID_PLANS: PaidPlan[] = enrichYearlyPlans(PAID_PLANS_RAW);

export function findPaidPlan(planId: string): PaidPlan | undefined {
  return PAID_PLANS.find((p) => p.id === planId);
}

export function plansForTier(tier: TierId, cycle: BillingCycle): PaidPlan | undefined {
  return PAID_PLANS.find((p) => p.tier === tier && p.billingCycle === cycle);
}

export function isPaymentsConfigured(): boolean {
  return !!(process.env.RAZORPAY_KEY_ID?.trim() && process.env.RAZORPAY_KEY_SECRET?.trim());
}

export function razorpayPublicKey(): string | null {
  const key = process.env.RAZORPAY_KEY_ID?.trim() || process.env.NEXT_PUBLIC_RAZORPAY_KEY_ID?.trim();
  return key && key.length > 0 ? key : null;
}

export function isRazorpayTestMode(): boolean {
  const key = razorpayPublicKey();
  return !!key?.startsWith("rzp_test_");
}

export function subscriptionPeriodDays(cycle: BillingCycle): number {
  return cycle === "yearly" ? 365 : 30;
}
