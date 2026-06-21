import { NextRequest, NextResponse } from "next/server";
import { requireApiUser } from "@/lib/api-auth";
import { saveAssessmentScore, type AssessmentType } from "@/lib/assessment-service";
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

export async function POST(req: NextRequest) {
  const auth = await requireApiUser(req);
  if (auth instanceof NextResponse) return auth;

  try {
    await assertFeatureAllowed(auth.uid, "manualAssessmentEntry", "basic");
    const body = await req.json();
    const type = body.type as AssessmentType;
    if (type !== "baseline" && type !== "unit" && type !== "endline") {
      return NextResponse.json({ error: "Invalid assessment type" }, { status: 400 });
    }

    const grade = parseInt(String(body.grade || "1"), 10);
    const scorePercent = Number(body.scorePercent);
    const studentsAssessed = Number(body.studentsAssessed) || 0;
    if (Number.isNaN(scorePercent)) {
      return NextResponse.json({ error: "Missing scorePercent" }, { status: 400 });
    }

    const record = await saveAssessmentScore(auth.uid, {
      type,
      grade,
      subject: String(body.subject || "english"),
      groupId: body.groupId ? String(body.groupId) : undefined,
      groupName: body.groupName ? String(body.groupName) : undefined,
      scorePercent,
      studentsAssessed,
      notes: body.notes ? String(body.notes) : undefined,
    });

    const account = await getAccountWithSubscription(auth.uid);
    return NextResponse.json({ record, account });
  } catch (err) {
    if (err instanceof TierLimitError) {
      return tierErrorResponse(auth.uid, err);
    }
    console.error("ASSESSMENT SAVE ERROR:", err);
    return NextResponse.json({ error: "Could not save assessment" }, { status: 500 });
  }
}
