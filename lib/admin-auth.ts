import { createHmac, timingSafeEqual } from "crypto";
import { cookies } from "next/headers";
import { NextRequest, NextResponse } from "next/server";

const COOKIE_NAME = "pedastudio_admin";
const SESSION_TTL_MS = 12 * 60 * 60 * 1000; // 12 hours

function getAdminSecret(): string | null {
  const secret = process.env.ADMIN_SECRET?.trim();
  return secret || null;
}

function signPayload(payload: string): string {
  const secret = getAdminSecret();
  if (!secret) throw new Error("ADMIN_SECRET not configured");
  return createHmac("sha256", secret).update(payload).digest("hex");
}

function safeEqual(a: string, b: string): boolean {
  const bufA = Buffer.from(a);
  const bufB = Buffer.from(b);
  if (bufA.length !== bufB.length) return false;
  return timingSafeEqual(bufA, bufB);
}

export function isAdminConfigured(): boolean {
  return !!getAdminSecret();
}

export function verifyAdminPassword(password: string): boolean {
  const secret = getAdminSecret();
  if (!secret) return false;
  return safeEqual(password, secret);
}

export function createAdminSessionToken(): string {
  const expires = Date.now() + SESSION_TTL_MS;
  const payload = `${expires}`;
  const sig = signPayload(payload);
  return `${payload}.${sig}`;
}

export function parseAdminSessionToken(token: string | undefined): boolean {
  if (!token || !getAdminSecret()) return false;
  const dot = token.lastIndexOf(".");
  if (dot <= 0) return false;
  const payload = token.slice(0, dot);
  const sig = token.slice(dot + 1);
  const expected = signPayload(payload);
  if (!safeEqual(sig, expected)) return false;
  const expires = Number(payload);
  if (!Number.isFinite(expires) || expires < Date.now()) return false;
  return true;
}

export function setAdminSessionCookie(res: NextResponse): void {
  const token = createAdminSessionToken();
  res.cookies.set(COOKIE_NAME, token, {
    httpOnly: true,
    secure: process.env.NODE_ENV === "production",
    sameSite: "lax",
    path: "/",
    maxAge: SESSION_TTL_MS / 1000,
  });
}

export function clearAdminSessionCookie(res: NextResponse): void {
  res.cookies.set(COOKIE_NAME, "", {
    httpOnly: true,
    secure: process.env.NODE_ENV === "production",
    sameSite: "lax",
    path: "/",
    maxAge: 0,
  });
}

export async function isAdminAuthenticated(): Promise<boolean> {
  const jar = await cookies();
  const token = jar.get(COOKIE_NAME)?.value;
  return parseAdminSessionToken(token);
}

export async function requireAdmin(
  req?: NextRequest,
): Promise<NextResponse | null> {
  if (!isAdminConfigured()) {
    return NextResponse.json(
      { error: "Admin panel not configured. Set ADMIN_SECRET." },
      { status: 503 },
    );
  }

  const authHeader = req?.headers.get("authorization");
  if (authHeader?.startsWith("Bearer ")) {
    const bearer = authHeader.slice("Bearer ".length);
    if (verifyAdminPassword(bearer)) return null;
  }

  const token =
    req?.cookies.get(COOKIE_NAME)?.value ??
    (await cookies()).get(COOKIE_NAME)?.value;

  if (!parseAdminSessionToken(token)) {
    return NextResponse.json({ error: "Admin login required" }, { status: 401 });
  }
  return null;
}

export { COOKIE_NAME };
