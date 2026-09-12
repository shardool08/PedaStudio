import { NextRequest, NextResponse } from "next/server";
import { requireApiUser } from "@/lib/api-auth";
import { resolveAssessmentTool } from "@/lib/assessment-service";
import { renderAssessmentMarkdown } from "@/lib/assessment-tools";
import { renderAssessmentToolHtml } from "@/lib/assessment-tool-html";
import { assertFeatureAllowed, TierLimitError } from "@/lib/tier-service";
import { getAccountWithSubscription } from "@/lib/subscription-service";
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
  const copy = searchParams.get("copy") === "assessor" ? "assessor" : "student";
  const format = searchParams.get("format") === "html" ? "html" : "markdown";
  const className = searchParams.get("className") || undefined;

  if (type !== "baseline" && type !== "unit" && type !== "endline") {
    return NextResponse.json({ error: "Invalid type" }, { status: 400 });
  }

  try {
    await assertFeatureAllowed(auth.uid, "baselineAssessment", "basic");

    const tool = resolveAssessmentTool(grade, subject, medium, type, groupId);
    if (!tool) {
      return NextResponse.json({ error: "Tool not available" }, { status: 404 });
    }

    const printOpts = { copy, className } as const;

    if (format === "html") {
      const html = renderAssessmentToolHtml(tool, printOpts);
      const suffix = copy === "assessor" ? "assessor" : "student";
      return new NextResponse(html, {
        headers: {
          "Content-Type": "text/html; charset=utf-8",
          "Content-Disposition": `inline; filename="${tool.id}-${suffix}.html"`,
        },
      });
    }

    const markdown = renderAssessmentMarkdown(tool, printOpts);

    const suffix = copy === "assessor" ? "assessor" : "student";
    const filename = `${tool.id}-${suffix}.md`;

    return new NextResponse(markdown, {
      headers: {
        "Content-Type": "text/markdown; charset=utf-8",
        "Content-Disposition": `attachment; filename="${filename}"`,
      },
    });
  } catch (err) {
    if (err instanceof TierLimitError) return tierErrorResponse(auth.uid, err);
    throw err;
  }
}
