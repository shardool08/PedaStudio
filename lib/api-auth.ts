import { NextRequest, NextResponse } from "next/server";
import { isApiAuthConfigured, verifyFirebaseIdToken } from "@/lib/firebase/admin";

/** Decode JWT payload without verifying signature (pilot / transition only). */
function uidFromJwtPayload(token: string): string | null {
  try {
    const parts = token.split(".");
    if (parts.length < 2) return null;
    const json = Buffer.from(parts[1]!.replace(/-/g, "+").replace(/_/g, "/"), "base64").toString(
      "utf8",
    );
    const payload = JSON.parse(json) as { sub?: string; user_id?: string };
    const uid = payload.user_id || payload.sub;
    return uid && typeof uid === "string" ? uid : null;
  } catch {
    return null;
  }
}

function pilotAuthEnabled(): boolean {
  const v = process.env.ALLOW_PILOT_AUTH?.trim().toLowerCase();
  return v === "1" || v === "true" || v === "yes";
}

/**
 * Require a logged-in user (Firebase ID token).
 * - Production: verifies with Firebase Admin when configured.
 * - ALLOW_PILOT_AUTH=true: accepts Android Firebase tokens without Admin SDK
 *   (use while migrating off Blaze / before Supabase Auth is wired).
 * - Development: falls back to "dev-local" when Admin is missing.
 */
export async function requireApiUser(req: NextRequest): Promise<{ uid: string } | NextResponse> {
  const header = req.headers.get("authorization");
  const token = header?.startsWith("Bearer ") ? header.slice(7).trim() : "";

  if (!token) {
    if (process.env.NODE_ENV === "development" && !isApiAuthConfigured()) {
      return { uid: "dev-local" };
    }
    return NextResponse.json({ error: "Sign in required" }, { status: 401 });
  }

  const uid = await verifyFirebaseIdToken(token);
  if (uid) return { uid };

  if (pilotAuthEnabled()) {
    const pilotUid = uidFromJwtPayload(token);
    if (pilotUid) return { uid: pilotUid };
  }

  if (process.env.NODE_ENV === "development") {
    return { uid: "dev-local" };
  }

  return NextResponse.json({ error: "Invalid or expired session" }, { status: 401 });
}
