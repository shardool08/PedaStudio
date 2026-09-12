import { NextRequest, NextResponse } from "next/server";
import { activateFromWebhook, verifyRazorpayWebhookSignature } from "@/lib/subscription-service";

export const dynamic = "force-dynamic";

/** Razorpay Dashboard → Webhooks → payment.captured */
export async function POST(req: NextRequest) {
  const rawBody = await req.text();
  const signature = req.headers.get("x-razorpay-signature") ?? "";

  if (!verifyRazorpayWebhookSignature(rawBody, signature)) {
    return NextResponse.json({ error: "Invalid signature" }, { status: 401 });
  }

  let payload: Record<string, unknown>;
  try {
    payload = JSON.parse(rawBody) as Record<string, unknown>;
  } catch {
    return NextResponse.json({ error: "Invalid JSON" }, { status: 400 });
  }

  const event = payload.event as string | undefined;
  if (event !== "payment.captured") {
    return NextResponse.json({ ok: true, ignored: event ?? "unknown" });
  }

  const paymentEntity = (
    (payload.payload as Record<string, unknown> | undefined)?.payment as Record<string, unknown> | undefined
  )?.entity as Record<string, unknown> | undefined;

  const paymentId = paymentEntity?.id as string | undefined;
  const orderId = paymentEntity?.order_id as string | undefined;

  if (!paymentId || !orderId) {
    return NextResponse.json({ error: "Missing payment fields" }, { status: 400 });
  }

  const result = await activateFromWebhook(orderId, paymentId);
  if (!result.ok) {
    return NextResponse.json({ error: result.reason }, { status: 404 });
  }

  return NextResponse.json({ ok: true, duplicate: result.duplicate ?? false });
}
