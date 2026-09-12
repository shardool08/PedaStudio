import { NextResponse } from "next/server";
import { PAID_PLANS, TIER_COMPARISON, TIER_MARKETING } from "@/lib/subscription-config";
import { plansCatalogJson } from "@/lib/subscription-service";

export const dynamic = "force-dynamic";

export async function GET() {
  return NextResponse.json({
    ...plansCatalogJson(),
    marketing: TIER_MARKETING,
    comparison: TIER_COMPARISON,
    plans: PAID_PLANS,
  });
}
