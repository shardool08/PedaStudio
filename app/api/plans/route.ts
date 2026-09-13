import { NextRequest, NextResponse } from "next/server";
import { requireApiUser } from "@/lib/api-auth";
import { listPlans, upsertPlan } from "@/lib/supabase/plans";

export const dynamic = "force-dynamic";

/**
 * This teacher's saved plans, for the app's two-way sync on startup.
 * Optional `lessonId` and `day` narrow it to a single plan.
 */
export async function GET(req: NextRequest) {
  const auth = await requireApiUser(req);
  if (auth instanceof NextResponse) return auth;

  const params = new URL(req.url).searchParams;
  const lessonId = params.get("lessonId")?.trim() || undefined;
  const dayRaw = params.get("day");
  const day = dayRaw ? Number(dayRaw) : undefined;

  const plans = await listPlans(auth.uid, {
    lessonId,
    day: Number.isInteger(day) ? day : undefined,
  });
  return NextResponse.json({ plans });
}

/** Save one plan. Omitted fields keep their stored value. */
export async function PUT(req: NextRequest) {
  const auth = await requireApiUser(req);
  if (auth instanceof NextResponse) return auth;

  let body: Record<string, unknown>;
  try {
    body = (await req.json()) as Record<string, unknown>;
  } catch {
    return NextResponse.json({ error: "Invalid JSON body" }, { status: 400 });
  }

  try {
    const plan = await upsertPlan(auth.uid, body);
    return NextResponse.json({ plan });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Could not save plan";
    console.error("Plan save failed:", message);
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
