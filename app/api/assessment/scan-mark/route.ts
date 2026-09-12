import { NextRequest, NextResponse } from "next/server";
import { requireApiUser } from "@/lib/api-auth";
import {
  markAssessmentPapers,
  type PaperScanMode,
  type ScanImageInput,
} from "@/lib/assessment-paper-marking";
import { resolveAssessmentTool, type AssessmentType } from "@/lib/assessment-service";
import { getAccountWithSubscription } from "@/lib/subscription-service";
import {
  assertFeatureAllowed,
  incrementUsage,
  TierLimitError,
} from "@/lib/tier-service";

export const maxDuration = 120;

const MAX_IMAGES = 15;

async function tierErrorResponse(uid: string, err: TierLimitError) {
  const account = await getAccountWithSubscription(uid);
  return NextResponse.json(
    { error: err.message, code: err.code, upgradeTier: err.upgradeTier, account },
    { status: err.status },
  );
}

export async function POST(req: NextRequest) {
  try {
    const auth = await requireApiUser(req);
    if (auth instanceof NextResponse) return auth;

    const body = await req.json();
    const type = body.type as AssessmentType;
    if (type !== "baseline" && type !== "unit" && type !== "endline") {
      return NextResponse.json({ error: "Invalid assessment type" }, { status: 400 });
    }

    const grade = parseInt(String(body.grade || "1"), 10);
    const subject = String(body.subject || "english");
    const medium = String(body.medium || "marathi");
    const groupId = body.groupId ? String(body.groupId) : undefined;
    const scanMode: PaperScanMode = body.scanMode === "tally_sheet" ? "tally_sheet" : "papers";

    const rawImages = Array.isArray(body.images) ? body.images : [];
    const images: ScanImageInput[] = rawImages
      .slice(0, MAX_IMAGES)
      .map((img: Record<string, unknown>) => ({
        base64: String(img.base64 || ""),
        mediaType: typeof img.mediaType === "string" ? img.mediaType : "image/jpeg",
      }))
      .filter((img: ScanImageInput) => img.base64.length > 100);

    if (!images.length) {
      return NextResponse.json({ error: "Add at least one photo" }, { status: 400 });
    }

    if (scanMode === "tally_sheet" && images.length > 1) {
      images.splice(1);
    }

    try {
      await assertFeatureAllowed(auth.uid, "bulkPaperScan", "prime");
      await assertFeatureAllowed(auth.uid, "aiAutoMark", "prime");
      const account = await getAccountWithSubscription(auth.uid);
      const ocrNeeded = scanMode === "tally_sheet" ? 1 : images.length;
      const ocrLimit = account.limits.ocrScansPerWeek;
      if (ocrLimit !== null && account.usage.ocrScans + ocrNeeded > ocrLimit) {
        throw new TierLimitError(
          `Weekly OCR scan limit reached (${ocrLimit} per week). Resets every Monday.`,
          "USAGE_LIMIT_REACHED",
          429,
          "prime",
        );
      }
    } catch (err) {
      if (err instanceof TierLimitError) return tierErrorResponse(auth.uid, err);
      throw err;
    }

    const tool = resolveAssessmentTool(grade, subject, medium, type, groupId);
    if (!tool) {
      return NextResponse.json(
        { error: "No structured assessment tool for this grade, subject, and unit window yet." },
        { status: 404 },
      );
    }

    const result = await markAssessmentPapers(tool, images, scanMode);

    const ocrUsed = scanMode === "tally_sheet" ? 1 : images.length;
    for (let i = 0; i < ocrUsed; i++) {
      await incrementUsage(auth.uid, "ocrScans");
    }

    const account = await getAccountWithSubscription(auth.uid);
    return NextResponse.json({ ...result, account });
  } catch (error) {
    console.error("ASSESSMENT SCAN-MARK:", error);
    const message = error instanceof Error ? error.message : "Could not mark papers";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
