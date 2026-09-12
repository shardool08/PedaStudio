import { NextRequest, NextResponse } from "next/server";
import { requireApiUser } from "@/lib/api-auth";
import {
  getAssessmentRecord,
  resolveAssessmentTool,
  toolToPayload,
} from "@/lib/assessment-service";
import { getAccountWithSubscription } from "@/lib/subscription-service";
import { assertFeatureAllowed, TierLimitError } from "@/lib/tier-service";
import type { AssessmentType } from "@/lib/assessment-service";

export const dynamic = "force-dynamic";

async function tierErrorResponse(uid: string, err: TierLimitError) {
  const account = await getAccountWithSubscription(uid);
  return NextResponse.json(
    { error: err.message, code: err.code, upgradeTier: err.upgradeTier, account },
    { status: err.status },
  );
}

export async function GET(req: NextRequest) {
  const auth = await requireApiUser(req);
  if (auth instanceof NextResponse) return auth;

  const { searchParams } = new URL(req.url);
  const grade = parseInt(searchParams.get("grade") || "1", 10);
  const subject = searchParams.get("subject") || "english";
  const medium = searchParams.get("medium") || "marathi";
  const type = searchParams.get("type") as AssessmentType;
  const groupId = searchParams.get("groupId") || undefined;

  if (type !== "baseline" && type !== "unit" && type !== "endline") {
    return NextResponse.json({ error: "Invalid type" }, { status: 400 });
  }

  try {
    await assertFeatureAllowed(auth.uid, "baselineAssessment", "basic");

    const tool = resolveAssessmentTool(grade, subject, medium, type, groupId);
    if (!tool) {
      return NextResponse.json({ available: false, tool: null, saved: null });
    }

    const saved = await getAssessmentRecord(auth.uid, type, grade, groupId);
    const account = await getAccountWithSubscription(auth.uid);

    return NextResponse.json({
      available: true,
      tool: toolToPayload(tool),
      saved: saved
        ? {
            studentsAssessed: saved.studentsAssessed,
            scorePercent: saved.scorePercent,
            notes: saved.notes ?? "",
            tallies: saved.tallies ?? [],
            strandScores: saved.strandScores ?? [],
            weakItems: saved.weakItems ?? [],
          }
        : null,
      account,
    });
  } catch (err) {
    if (err instanceof TierLimitError) return tierErrorResponse(auth.uid, err);
    throw err;
  }
}
