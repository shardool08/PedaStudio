import { NextRequest, NextResponse } from "next/server";
import { createClient } from "@supabase/supabase-js";
import { getSupabaseAdmin, isSupabaseConfigured } from "@/lib/supabase/server";
import { ensureTeacher } from "@/lib/supabase/teachers";

export const dynamic = "force-dynamic";

/**
 * Temporary pilot login — no SMS provider needed.
 *
 * Enable with ALLOW_DEV_OTP=true on Railway. Any 10-digit Indian number +
 * PILOT_OTP (default 123456) signs the teacher in.
 *
 * Remove this route before a public launch; use real Supabase Phone + Textlocal/Twilio.
 */

function pilotEnabled(): boolean {
  const v = process.env.ALLOW_DEV_OTP?.trim().toLowerCase();
  return v === "1" || v === "true" || v === "yes";
}

function expectedOtp(): string {
  return process.env.PILOT_OTP?.trim() || "123456";
}

function digitsOnly(phone: string): string {
  return phone.replace(/\D/g, "").slice(-10);
}

export async function POST(req: NextRequest) {
  if (!pilotEnabled()) {
    return NextResponse.json(
      { error: "Pilot OTP is off. Set ALLOW_DEV_OTP=true on the server, or configure Textlocal/Twilio." },
      { status: 403 },
    );
  }
  if (!isSupabaseConfigured()) {
    return NextResponse.json({ error: "Supabase is not configured" }, { status: 500 });
  }

  let body: { phone?: string; otp?: string };
  try {
    body = (await req.json()) as { phone?: string; otp?: string };
  } catch {
    return NextResponse.json({ error: "Invalid JSON" }, { status: 400 });
  }

  const digits = digitsOnly(String(body.phone || ""));
  const otp = String(body.otp || "").trim();
  if (digits.length !== 10) {
    return NextResponse.json({ error: "Enter a valid 10-digit mobile number" }, { status: 400 });
  }
  if (otp !== expectedOtp()) {
    return NextResponse.json({ error: "Invalid OTP" }, { status: 401 });
  }

  const e164 = `+91${digits}`;
  const email = `pilot.${digits}@pedastudio.local`;
  const admin = getSupabaseAdmin();

  // Create (or ignore if already exists) a confirmed email user — no SMS required.
  const created = await admin.auth.admin.createUser({
    email,
    email_confirm: true,
    user_metadata: { phone: e164, pilot: true },
    phone: e164,
  });
  if (created.error && !created.error.message.toLowerCase().includes("already")) {
    // Phone column may fail if Phone provider is off — retry email-only.
    const emailOnly = await admin.auth.admin.createUser({
      email,
      email_confirm: true,
      user_metadata: { phone: e164, pilot: true },
    });
    if (emailOnly.error && !emailOnly.error.message.toLowerCase().includes("already")) {
      console.error("Pilot create failed:", emailOnly.error.message);
      return NextResponse.json({ error: "Could not create pilot user" }, { status: 500 });
    }
  }

  const link = await admin.auth.admin.generateLink({
    type: "magiclink",
    email,
  });
  if (link.error || !link.data?.properties?.hashed_token) {
    console.error("generateLink failed:", link.error?.message);
    return NextResponse.json({ error: "Could not start pilot session" }, { status: 500 });
  }

  const url = process.env.NEXT_PUBLIC_SUPABASE_URL?.trim();
  const anon = process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY?.trim();
  if (!url || !anon) {
    return NextResponse.json({ error: "Missing public Supabase keys" }, { status: 500 });
  }

  const anonClient = createClient(url, anon, {
    auth: { persistSession: false, autoRefreshToken: false },
  });
  const verified = await anonClient.auth.verifyOtp({
    token_hash: link.data.properties.hashed_token,
    type: "email",
  });
  if (verified.error || !verified.data.session) {
    console.error("verifyOtp failed:", verified.error?.message);
    return NextResponse.json({ error: "Could not verify pilot session" }, { status: 500 });
  }

  const session = verified.data.session;
  await ensureTeacher(session.user.id, { phone_number: digits }).catch((err) => {
    console.error("ensureTeacher failed:", err);
  });

  return NextResponse.json({
    access_token: session.access_token,
    refresh_token: session.refresh_token,
    expires_in: session.expires_in,
    user: { id: session.user.id, phone: e164 },
    pilot: true,
  });
}
