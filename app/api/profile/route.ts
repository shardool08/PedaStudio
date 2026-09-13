import { NextRequest, NextResponse } from "next/server";
import { requireApiUser } from "@/lib/api-auth";
import { profilePatchFromJson, readProfile, writeProfile } from "@/lib/supabase/profile";

export const dynamic = "force-dynamic";

/** Teacher profile pull. `exists: false` means nothing has been synced yet. */
export async function GET(req: NextRequest) {
  const auth = await requireApiUser(req);
  if (auth instanceof NextResponse) return auth;

  const profile = await readProfile(auth.uid);
  return NextResponse.json({ exists: Boolean(profile), profile });
}

/** Teacher profile push. Tier, usage and subscription fields are ignored if sent. */
export async function PUT(req: NextRequest) {
  const auth = await requireApiUser(req);
  if (auth instanceof NextResponse) return auth;

  let body: Record<string, unknown>;
  try {
    body = (await req.json()) as Record<string, unknown>;
  } catch {
    return NextResponse.json({ error: "Invalid JSON body" }, { status: 400 });
  }

  try {
    const profile = await writeProfile(auth.uid, profilePatchFromJson(body));
    return NextResponse.json({ exists: Boolean(profile), profile });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Could not save profile";
    console.error("Profile save failed:", message);
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
