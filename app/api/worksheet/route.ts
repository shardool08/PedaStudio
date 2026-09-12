import { NextRequest, NextResponse } from "next/server";
import { requireApiUser } from "@/lib/api-auth";
import { anthropicMessages, parseLessonGrade } from "@/lib/api-utils";
import { allLessonsServer } from "@/lib/curriculum";
import { getAccountWithSubscription } from "@/lib/subscription-service";
import {
  assertWorksheetAllowed,
  assertFeatureAllowed,
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

export async function POST(req: NextRequest) {
  try {
    const auth = await requireApiUser(req);
    if (auth instanceof NextResponse) return auth;

    const body = await req.json();
    const { lessonId, day, focus } = body;
    if (!lessonId) {
      return NextResponse.json({ error: "Missing lessonId" }, { status: 400 });
    }

    let account;
    try {
      account = await assertWorksheetAllowed(auth.uid);
    } catch (err) {
      if (err instanceof TierLimitError) {
        return tierErrorResponse(auth.uid, err);
      }
      throw err;
    }

    const lessonGrade = parseLessonGrade(String(lessonId));
    if (!account.features.gradesAvailable.includes(lessonGrade)) {
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

    const lesson = allLessonsServer.find((l) => l.id === lessonId);
    if (!lesson) {
      return NextResponse.json({ error: "Lesson not found" }, { status: 404 });
    }

    const dayNum = Number(day) || 1;
    const dayFocus =
      lesson.bloomsProgression?.find((d) => d.day === dayNum)?.focus || "Practice and assessment";

    const prompt = `You are an expert Maharashtra municipal school English teacher (Grade ${lessonGrade}).
Create a printable worksheet JSON for lesson "${lesson.en}" (ID ${lesson.id}), Day ${dayNum}.
Focus: ${focus || dayFocus}
Vocabulary: ${(lesson.vocabulary || []).slice(0, 12).join(", ")}

Return ONLY valid JSON:
{
  "title": "string",
  "instructions": "string (simple English, max 2 sentences)",
  "items": [
    { "type": "mcq|fill|match|draw", "question": "string", "options": ["a","b"], "answer": "string" }
  ],
  "teacherNotes": "string (1 sentence on how to use in class)"
}
Include 5-8 age-appropriate items. No markdown.`;

    const result = await anthropicMessages({
      max_tokens: 2048,
      messages: [{ role: "user", content: prompt }],
    });
    if (!result.ok) return result.response;

    await incrementUsage(auth.uid, "worksheets");

    const text = result.data.content?.[0]?.text || "{}";
    const clean = text.replace(/```json|```/g, "").trim();
    const worksheet = JSON.parse(clean);
    const updatedAccount = await getAccountWithSubscription(auth.uid);
    return NextResponse.json({ worksheet, account: updatedAccount });
  } catch (error) {
    console.error("WORKSHEET ERROR:", error);
    return NextResponse.json({ error: "Could not generate worksheet" }, { status: 500 });
  }
}
