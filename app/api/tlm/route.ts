import { NextRequest, NextResponse } from "next/server";
import { requireApiUser } from "@/lib/api-auth";
import { getLessonsForMedium } from "@/lib/curriculum";
import { readTeacherTlmIds } from "@/lib/assessment-service";
import { getAccountWithSubscription } from "@/lib/subscription-service";
import { buildUnitTlmKit, buildYearTlmKit } from "@/lib/tlm-kit";
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
  const unitRaw = searchParams.get("unit");
  const scope = searchParams.get("scope") || (unitRaw ? "unit" : "year");
  const medium = searchParams.get("medium") || "marathi";
  const subject = searchParams.get("subject") || "english";

  try {
    const account = await assertFeatureAllowed(auth.uid, "unitTlmKit", "basic");
    const lessons = getLessonsForMedium(grade, subject, medium);
    if (lessons.length === 0) {
      return NextResponse.json({ error: "Curriculum not found" }, { status: 404 });
    }

    const teacherTlmIds = await readTeacherTlmIds(auth.uid);
    const fullList = scope === "year" && account.features.yearTlmListFull;

    let kit;
    if (scope === "year") {
      kit = buildYearTlmKit(lessons, teacherTlmIds);
      if (!fullList) {
        kit = {
          ...kit,
          items: kit.items.filter((i) => i.essential || i.owned).slice(0, 8),
          procurementNote: "Upgrade to Prime for the full year procurement list.",
        };
      }
    } else {
      const unit = parseInt(unitRaw || "1", 10);
      kit = buildUnitTlmKit(lessons, unit, teacherTlmIds);
    }

    const updatedAccount = await getAccountWithSubscription(auth.uid);
    return NextResponse.json({
      kit,
      canExportPdf: account.features.yearTlmPdfShare,
      account: updatedAccount,
    });
  } catch (err) {
    if (err instanceof TierLimitError) {
      return tierErrorResponse(auth.uid, err);
    }
    throw err;
  }
}
