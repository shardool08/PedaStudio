import { NextRequest, NextResponse } from "next/server";
import { allLessonsServer as balbharatiLessons } from "@/lib/curriculum";
import { requireApiUser } from "@/lib/api-auth";
import { anthropicMessages } from "@/lib/api-utils";
import { buildPlanPrompt, normalizePlan, type PlanMode } from "@/lib/plan-prompt";

export const maxDuration = 120;

export async function POST(req: NextRequest) {
  try {
    const auth = await requireApiUser(req);
    if (auth instanceof NextResponse) return auth;

    const body = await req.json();
    const { lessonId, day, selections, teacherProfile, customPrompt, mode } = body;

    if (!lessonId) {
      return NextResponse.json({ error: "Missing lessonId" }, { status: 400 });
    }
    if (customPrompt && typeof customPrompt === "string" && customPrompt.length > 12000) {
      return NextResponse.json({ error: "Prompt too long" }, { status: 400 });
    }

    const lesson = balbharatiLessons.find((l) => l.id === lessonId);
    if (!lesson) {
      return NextResponse.json({ error: "Lesson not found" }, { status: 404 });
    }

    const dayNum = day || 1;
    const planMode: PlanMode =
      mode === "practice" || mode === "reteach" || mode === "continue" ? mode : null;

    const prompt =
      customPrompt ||
      buildPlanPrompt(lesson, dayNum, selections || {}, teacherProfile || {}, planMode);

    const result = await anthropicMessages({
      max_tokens: 4096,
      messages: [{ role: "user", content: prompt }],
    });
    if (!result.ok) return result.response;

    const text = result.data.content?.[0]?.text || "{}";
    const clean = text.replace(/```json|```/g, "").trim();
    const raw = JSON.parse(clean);
    const plan = normalizePlan(raw, selections);
    return NextResponse.json({ plan });
  } catch (error) {
    console.error("PLAN GEN ERROR:", error);
    return NextResponse.json({ error: "Could not generate plan" }, { status: 500 });
  }
}
