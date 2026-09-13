import { NextRequest, NextResponse } from "next/server";
import { verifyIdTokenWithGoogleCerts } from "@/lib/firebase/verify-id-token";
import { getSupabaseAnon, isSupabaseAnonConfigured } from "@/lib/supabase/server";

/**
 * Dev uid used when no auth provider is configured. A uuid-shaped string so it
 * still inserts into `teachers.id` after the text-id migration.
 */
export const DEV_LOCAL_UID = "00000000-0000-4000-8000-000000000001";

const FIREBASE_ISSUER_PREFIX = "https://securetoken.google.com/";

/**
 * Reads `iss` without verifying anything, purely to pick a verifier.
 * Both verifiers below do the real signature and claim checks.
 */
function unverifiedIssuer(token: string): string | null {
  const payload = token.split(".")[1];
  if (!payload) return null;
  try {
    const decoded = JSON.parse(Buffer.from(payload, "base64url").toString("utf8"));
    return typeof decoded?.iss === "string" ? decoded.iss : null;
  } catch {
    return null;
  }
}

/** Supabase access token → uid (auth.users uuid), or null. */
async function verifySupabaseToken(token: string): Promise<string | null> {
  if (!isSupabaseAnonConfigured()) return null;
  try {
    const { data, error } = await getSupabaseAnon().auth.getUser(token);
    if (error || !data.user?.id) return null;
    return data.user.id;
  } catch {
    return null;
  }
}

/**
 * Require a signed-in teacher.
 *
 * New clients send a Supabase access token. Older Android builds may still send
 * a Firebase ID token; those are checked against Google's public certificates
 * (no Firebase Admin SDK, no billing).
 */
export async function requireApiUser(req: NextRequest): Promise<{ uid: string } | NextResponse> {
  const header = req.headers.get("authorization");
  const token = header?.startsWith("Bearer ") ? header.slice(7).trim() : "";

  if (!token) {
    if (process.env.NODE_ENV === "development" && !isSupabaseAnonConfigured()) {
      return { uid: DEV_LOCAL_UID };
    }
    return NextResponse.json({ error: "Sign in required" }, { status: 401 });
  }

  const looksLikeFirebase = unverifiedIssuer(token)?.startsWith(FIREBASE_ISSUER_PREFIX) ?? false;
  const uid = looksLikeFirebase
    ? ((await verifyIdTokenWithGoogleCerts(token)) ?? (await verifySupabaseToken(token)))
    : ((await verifySupabaseToken(token)) ?? (await verifyIdTokenWithGoogleCerts(token)));

  if (!uid) {
    if (process.env.NODE_ENV === "development") return { uid: DEV_LOCAL_UID };
    return NextResponse.json({ error: "Invalid or expired session" }, { status: 401 });
  }
  return { uid };
}
