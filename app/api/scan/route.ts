import { NextRequest, NextResponse } from "next/server";
import { requireApiUser } from "@/lib/api-auth";
import { anthropicMessages, parseLessonGrade } from "@/lib/api-utils";
import { allLessonsServer } from "@/lib/curriculum";
import { getAccountWithSubscription } from "@/lib/subscription-service";
import {
  assertFeatureAllowed,
  assertMonthlyUsageAllowed,
  incrementUsage,
  TierLimitError,
} from "@/lib/tier-service";

export const maxDuration = 120;

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

const SCAN_ACTIONS = [
  "quick_plan",
  "worksheet",
  "vocabulary",
  "assessment",
  "tlm_gap",
  "reteach",
] as const;

export async function POST(req: NextRequest) {
  try {
    const auth = await requireApiUser(req);
    if (auth instanceof NextResponse) return auth;

    const body = await req.json();
    const { imageBase64, mediaType, lessonId } = body;
    if (!imageBase64 || typeof imageBase64 !== "string") {
      return NextResponse.json({ error: "Missing imageBase64" }, { status: 400 });
    }

    let account;
    try {
      account = await assertFeatureAllowed(auth.uid, "textbookScan", "max");
      account = await assertMonthlyUsageAllowed(auth.uid, "scans", "max");
    } catch (err) {
      if (err instanceof TierLimitError) {
        return tierErrorResponse(auth.uid, err);
      }
      throw err;
    }

    const lesson = lessonId
      ? allLessonsServer.find((l) => l.id === lessonId)
      : undefined;
    const lessonGrade = lesson ? parseLessonGrade(lesson.id) : 1;
    if (lesson && !account.features.gradesAvailable.includes(lessonGrade)) {
      const updated = await getAccountWithSubscription(auth.uid);
      return NextResponse.json(
        {
          error: `Grade ${lessonGrade} requires a higher plan.`,
          code: "GRADE_LOCKED",
          upgradeTier: "prime",
          account: updated,
        },
        { status: 403 },
      );
    }

    const mime = typeof mediaType === "string" ? mediaType : "image/jpeg";
    const context = lesson
      ? `Teacher is on Balbharati lesson "${lesson.en}" (${lesson.id}), Unit ${lesson.unit}.`
      : "Teacher scanned a textbook page.";

    const result = await anthropicMessages({
      max_tokens: 2048,
      messages: [
        {
          role: "user",
          content: [
            {
              type: "image",
              source: {
                type: "base64",
                media_type: mime,
                data: imageBase64.replace(/^data:[^;]+;base64,/, ""),
              },
            },
            {
              type: "text",
              text: `${context}
Analyze this textbook page for a municipal school teacher in Maharashtra.
Return ONLY valid JSON:
{
  "detectedLesson": "lesson title or topic if visible",
  "detectedPage": "page number if visible",
  "summary": "2 sentences on what the page teaches",
  "vocabulary": ["word1","word2"],
  "suggestedActions": ["quick_plan","worksheet","vocabulary","assessment","tlm_gap","reteach"],
  "actionHints": {
    "quick_plan": "one line hint",
    "worksheet": "one line hint",
    "vocabulary": "one line hint",
    "assessment": "one line hint",
    "tlm_gap": "one line hint",
    "reteach": "one line hint"
  }
}
Use only these action ids: ${SCAN_ACTIONS.join(", ")}. Pick 2-4 most relevant.`,
            },
          ],
        },
      ],
    });
    if (!result.ok) return result.response;

    await incrementUsage(auth.uid, "scans");

    const text = result.data.content?.[0]?.text || "{}";
    const clean = text.replace(/```json|```/g, "").trim();
    const analysis = JSON.parse(clean);
    const updatedAccount = await getAccountWithSubscription(auth.uid);
    return NextResponse.json({ analysis, account: updatedAccount });
  } catch (error) {
    console.error("SCAN ERROR:", error);
    return NextResponse.json({ error: "Could not analyze scan" }, { status: 500 });
  }
}
