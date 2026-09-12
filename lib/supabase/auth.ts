import { NextRequest, NextResponse } from "next/server";
import { getSupabaseAnon, isSupabaseConfigured } from "@/lib/supabase/server";

/**
 * Require a logged-in Supabase user (Bearer access token).
 * Dev without Supabase: allows local testing as "dev-local".
 */
export async function requireApiUser(req: NextRequest): Promise<{ uid: string } | NextResponse> {
  const header = req.headers.get("authorization");
  const token = header?.startsWith("Bearer ") ? header.slice(7).trim() : "";

  if (!token) {
    if (process.env.NODE_ENV === "development" && !isSupabaseConfigured()) {
      return { uid: "00000000-0000-4000-8000-000000000001" };
    }
    return NextResponse.json({ error: "Sign in required" }, { status: 401 });
  }

  if (!isSupabaseConfigured()) {
    // Transition: accept any non-empty token in local dev only
    if (process.env.NODE_ENV === "development") {
      return { uid: "00000000-0000-4000-8000-000000000001" };
    }
    return NextResponse.json({ error: "Auth not configured" }, { status: 503 });
  }

  try {
    const { data, error } = await getSupabaseAnon().auth.getUser(token);
    if (error || !data.user?.id) {
      return NextResponse.json({ error: "Invalid or expired session" }, { status: 401 });
    }
    return { uid: data.user.id };
  } catch {
    return NextResponse.json({ error: "Invalid or expired session" }, { status: 401 });
  }
}
