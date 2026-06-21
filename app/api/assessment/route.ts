import { NextRequest, NextResponse } from "next/server";
import { requireApiUser } from "@/lib/api-auth";
import {
  buildAssessmentCatalog,
  listAssessmentScores,
} from "@/lib/assessment-service";
import { getAccountWithSubscription } from "@/lib/subscription-service";
import { assertFeatureAllowed, TierLimitError } from "@/lib/tier-service";

export const dynamic = "force-dynamic";

async function tierErrorResponse(uid: string, err: TierLimitError) {
  const account = await getAccountWithSubscription(uid);
  return NextResponse.json(
    {
      error: err.message,
      code: err.code,
      upgradeTier: err.upgradeTier,
      account,
    },
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

  try {
    await assertFeatureAllowed(auth.uid, "baselineAssessment", "basic");
    const catalog = buildAssessmentCatalog(grade, subject, medium);
    const scores = await listAssessmentScores(auth.uid, grade);
    const account = await getAccountWithSubscription(auth.uid);
    return NextResponse.json({
      catalog,
      scores,
      features: {
        unitTests: account.features.unitTests,
        manualEntry: account.features.manualAssessmentEntry,
        fullReports: account.features.fullAssessmentReports,
      },
      account,
    });
  } catch (err) {
    if (err instanceof TierLimitError) {
      return tierErrorResponse(auth.uid, err);
    }
    throw err;
  }
}
