import { NextResponse } from "next/server";
import { getAnthropicModelCandidates, requireEnv } from "@/lib/api-utils";
import { allLessonsServer } from "@/lib/curriculum";
import { isApiAuthConfigured } from "@/lib/firebase/admin";
import { firebaseProjectId } from "@/lib/firebase/verify-id-token";
import { getSupabaseAdmin, isSupabaseConfigured } from "@/lib/supabase/server";
import { getPilotTierOverride } from "@/lib/tier-config";

export const dynamic = "force-dynamic";

/** Confirms the teachers table is actually reachable, not just that env vars exist. */
async function databaseStatus(): Promise<string> {
  if (!isSupabaseConfigured()) return "not_configured";
  try {
    const { error } = await getSupabaseAdmin()
      .from("teachers")
      .select("id", { count: "exact", head: true });
    return error ? `error: ${error.message}` : "ok";
  } catch (error) {
    return `error: ${error instanceof Error ? error.message : "unknown"}`;
  }
}

export async function GET() {
  return NextResponse.json({
    ok: true,
    service: "pedastudio-api",
    node: process.version,
    anthropicKeyConfigured: Boolean(requireEnv("ANTHROPIC_API_KEY")),
    anthropicModels: getAnthropicModelCandidates(),
    lessonCount: allLessonsServer.length,
    pilotTier: getPilotTierOverride(),
    database: await databaseStatus(),
    tokenVerification: isApiAuthConfigured() ? "firebase-admin" : "google-certs",
    firebaseProject: firebaseProjectId(),
  });
}
