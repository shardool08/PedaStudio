import type { BillingCycle } from "@/lib/subscription-config";
import type { TierId } from "@/lib/tier-config";

/** Firestore: users/{uid}/payments/{paymentId} — server write only. */

export interface PaymentDocument {
  planId: string;
  tier: TierId;
  billingCycle: BillingCycle;
  amountPaise: number;
  orderId: string;
  paymentId: string;
  createdAt: string;
}
