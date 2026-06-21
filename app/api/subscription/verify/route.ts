import { NextRequest, NextResponse } from "next/server";
import { requireApiUser } from "@/lib/api-auth";
import { verifyAndActivatePayment } from "@/lib/subscription-service";

export const dynamic = "force-dynamic";

export async function POST(req: NextRequest) {
  try {
    const auth = await requireApiUser(req);
    if (auth instanceof NextResponse) return auth;

    const body = await req.json();
    const { orderId, paymentId, signature, planId } = body;

    if (!orderId || !paymentId || !signature || !planId) {
      return NextResponse.json({ error: "Missing payment fields" }, { status: 400 });
    }

    const account = await verifyAndActivatePayment(
      auth.uid,
      orderId,
      paymentId,
      signature,
      planId,
    );
    return NextResponse.json({ ok: true, account });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Payment verification failed";
    return NextResponse.json({ error: message }, { status: 400 });
  }
}
