import { NextRequest, NextResponse } from "next/server";
import {
  clearAdminSessionCookie,
  isAdminConfigured,
  requireAdmin,
  setAdminSessionCookie,
  verifyAdminPassword,
} from "@/lib/admin-auth";

export async function POST(req: NextRequest) {
  if (!isAdminConfigured()) {
    return NextResponse.json({ error: "Admin not configured" }, { status: 503 });
  }
  const body = await req.json().catch(() => ({}));
  const password = String(body.password || "");
  if (!verifyAdminPassword(password)) {
    return NextResponse.json({ error: "Invalid password" }, { status: 401 });
  }
  const res = NextResponse.json({ ok: true });
  setAdminSessionCookie(res);
  return res;
}

export async function DELETE() {
  const denied = await requireAdmin();
  if (denied) return denied;
  const res = NextResponse.json({ ok: true });
  clearAdminSessionCookie(res);
  return res;
}

export async function GET() {
  if (!isAdminConfigured()) {
    return NextResponse.json({ configured: false, authenticated: false });
  }
  const denied = await requireAdmin();
  return NextResponse.json({ configured: true, authenticated: !denied });
}
