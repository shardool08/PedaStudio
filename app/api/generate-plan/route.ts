import { NextRequest, NextResponse } from "next/server";
import { allLessonsServer as balbharatiLessons } from "@/lib/curriculum";
import { requireApiUser } from "@/lib/api-auth";
import { anthropicMessages } from "@/lib/api-utils";
import { buildPlanPrompt, normalizePlan, type PlanMode } from "@/lib/plan-prompt";
import { getAccountWithSubscription } from "@/lib/subscription-service";
import {
  assertPlanGenerationAllowed,
  incrementUsage,
  TierLimitError,
  accountToJson,
  getTeacherAccount,
} from "@/lib/tier-service";

export const maxDuration = 120;

export async function POST(req: NextRequest) {
  try {
    const auth = await requireApiUser(req);
    if (auth instanceof NextResponse) return auth;

    const body = await req.json();
    const { lessonId, day, selections, teacherProfile, customPrompt, mode, afterUnitTest } = body;

    if (!lessonId) {
      return NextResponse.json({ error: "Missing lessonId" }, { status: 400 });
    }
    if (customPrompt && typeof customPrompt === "string" && customPrompt.length > 12000) {
      return NextResponse.json({ error: "Prompt too long" }, { status: 400 });
    }

    const planMode: PlanMode =
      mode === "practice" || mode === "reteach" || mode === "continue" ? mode : null;

    let account;
    try {
      account = await assertPlanGenerationAllowed(
        auth.uid,
        planMode,
        afterUnitTest === true,
      );
    } catch (err) {
      if (err instanceof TierLimitError) {
        return NextResponse.json(
          {
            error: err.message,
            code: err.code,
            upgradeTier: err.upgradeTier,
            account: accountToJson(await getAccountWithSubscription(auth.uid)),
          },
          { status: err.status },
        );
      }
      throw err;
    }

    const lessonGrade = parseInt(String(lessonId).split(".")[0].replace(/\D/g, ""), 10) || 1;
    if (!account.features.gradesAvailable.includes(lessonGrade)) {
      return NextResponse.json(
        {
          error: `Grade ${lessonGrade} English requires ${lessonGrade <= 3 ? "Prime" : "Max"}.`,
          code: "GRADE_LOCKED",
          upgradeTier: lessonGrade <= 5 ? "prime" : "max",
          account: accountToJson(account),
        },
        { status: 403 },
      );
    }

    const lesson = balbharatiLessons.find((l) => l.id === lessonId);
    if (!lesson) {
      return NextResponse.json({ error: "Lesson not found" }, { status: 404 });
    }

    const dayNum = day || 1;
    const prompt =
      customPrompt ||
      buildPlanPrompt(lesson, dayNum, selections || {}, teacherProfile || {}, planMode);

    const result = await anthropicMessages({
      model: "claude-sonnet-4-20250514",
      max_tokens: 4096,
      messages: [{ role: "user", content: prompt }],
    });
    if (!result.ok) return result.response;

    await incrementUsage(auth.uid, "plans");

    const text = result.data.content?.[0]?.text || "{}";
    const clean = text.replace(/```json|```/g, "").trim();
    const raw = JSON.parse(clean);
    const plan = normalizePlan(raw, selections);
    const updatedAccount = await getAccountWithSubscription(auth.uid);
    return NextResponse.json({ plan, account: updatedAccount });
  } catch (error) {
    console.error("PLAN GEN ERROR:", error);
    return NextResponse.json({ error: "Could not generate plan" }, { status: 500 });
  }
}
