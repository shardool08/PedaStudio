import { NextRequest, NextResponse } from "next/server";
import { requireApiUser } from "@/lib/api-auth";
import { createRazorpayOrder } from "@/lib/subscription-service";

export const dynamic = "force-dynamic";

export async function POST(req: NextRequest) {
  try {
    const auth = await requireApiUser(req);
    if (auth instanceof NextResponse) return auth;

    const body = await req.json();
    const planId = body.planId as string;
    const teacherName = body.teacherName as string | undefined;

    if (!planId) {
      return NextResponse.json({ error: "Missing planId" }, { status: 400 });
    }

    const order = await createRazorpayOrder(auth.uid, planId, teacherName);
    return NextResponse.json(order);
  } catch (error) {
    const message = error instanceof Error ? error.message : "Could not create order";
    return NextResponse.json({ error: message }, { status: 400 });
  }
}
