import { NextResponse } from "next/server";

export const dynamic = "force-dynamic";

export async function GET() {
  const anthropicKey = process.env.ANTHROPIC_API_KEY?.trim() || "";
  return NextResponse.json({
    ok: true,
    service: "pedastudio-api",
    node: process.version,
    anthropicKeyConfigured: anthropicKey.length > 0,
    pilotAuth: ["1", "true", "yes"].includes(
      (process.env.ALLOW_PILOT_AUTH || "").trim().toLowerCase(),
    ),
  });
}
