import { NextRequest, NextResponse } from "next/server";
import { requireApiUser } from "@/lib/api-auth";
import {
  saveAssessmentScore,
  saveAssessmentWithTallies,
  type AssessmentType,
} from "@/lib/assessment-service";
import type { ItemTally } from "@/lib/assessment-tools";
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

function parseTallies(raw: unknown): ItemTally[] | undefined {
  if (!Array.isArray(raw)) return undefined;
  return raw
    .map((t) => {
      if (!t || typeof t !== "object") return null;
      const o = t as Record<string, unknown>;
      const itemId = String(o.itemId || "");
      if (!itemId) return null;
      if (o.format === "mcq") {
        return {
          itemId,
          format: "mcq" as const,
          countA: Number(o.countA) || 0,
          countB: Number(o.countB) || 0,
          countC: Number(o.countC) || 0,
          countD: Number(o.countD) || 0,
          notAssessed: o.notAssessed != null ? Number(o.notAssessed) : undefined,
        };
      }
      if (o.format === "subjective") {
        return {
          itemId,
          format: "subjective" as const,
          correctCount: Number(o.correctCount) || 0,
          notAssessed: o.notAssessed != null ? Number(o.notAssessed) : undefined,
        };
      }
      return null;
    })
    .filter(Boolean) as ItemTally[];
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
    const studentsAssessed = Number(body.studentsAssessed) || 0;
    const tallies = parseTallies(body.tallies);
    const medium = String(body.medium || "marathi");
    const subject = String(body.subject || "english");

    if (tallies && tallies.length > 0) {
      const { record, report } = await saveAssessmentWithTallies(auth.uid, {
        type,
        grade,
        subject,
        medium,
        groupId: body.groupId ? String(body.groupId) : undefined,
        groupName: body.groupName ? String(body.groupName) : undefined,
        studentsAssessed,
        tallies,
        notes: body.notes ? String(body.notes) : undefined,
      });
      const account = await getAccountWithSubscription(auth.uid);
      return NextResponse.json({ record, report, account });
    }

    const scorePercent = Number(body.scorePercent);
    if (Number.isNaN(scorePercent)) {
      return NextResponse.json({ error: "Missing scorePercent or tallies" }, { status: 400 });
    }

    const record = await saveAssessmentScore(auth.uid, {
      type,
      grade,
      subject,
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
    const message = err instanceof Error ? err.message : "Could not save assessment";
    console.error("ASSESSMENT SAVE ERROR:", err);
    return NextResponse.json({ error: message }, { status: 400 });
  }
}
