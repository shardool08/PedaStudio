import { NextRequest, NextResponse } from "next/server";
import { isApiAuthConfigured, verifyFirebaseIdToken } from "@/lib/firebase/admin";
import { verifyIdTokenWithGoogleCerts } from "@/lib/firebase/verify-id-token";

/**
 * Require a signed-in teacher.
 *
 * Tokens are verified with the Admin SDK when a service account is available, and
 * otherwise against Google's public signing certificates — the deployment has no
 * Firebase credentials, but the signature check is what actually matters.
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

  const uid = (await verifyFirebaseIdToken(token)) ?? (await verifyIdTokenWithGoogleCerts(token));
  if (!uid) {
    if (process.env.NODE_ENV === "development") return { uid: "dev-local" };
    return NextResponse.json({ error: "Invalid or expired session" }, { status: 401 });
  }
  return { uid };
}
