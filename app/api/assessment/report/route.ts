import { NextRequest, NextResponse } from "next/server";
import { requireApiUser } from "@/lib/api-auth";
import {
  getAssessmentRecord,
  resolveAssessmentTool,
  type AssessmentType,
} from "@/lib/assessment-service";
import { buildAssessmentReport } from "@/lib/assessment-tools";
import { buildGrowthComparison } from "@/lib/assessment-growth";
import { renderAssessmentReportHtml } from "@/lib/assessment-report-html";
import { getTeacher } from "@/lib/supabase/teachers";
import { assertFeatureAllowed, TierLimitError } from "@/lib/tier-service";
import { getAccountWithSubscription } from "@/lib/subscription-service";

export const dynamic = "force-dynamic";

async function tierErrorResponse(uid: string, err: TierLimitError) {
  const account = await getAccountWithSubscription(uid);
  return NextResponse.json(
    { error: err.message, code: err.code, upgradeTier: err.upgradeTier, account },
    { status: err.status },
  );
}

async function readTeacherProfile(uid: string) {
  const teacher = await getTeacher(uid).catch(() => null);
  if (!teacher) return {};
  return {
    teacherName: teacher.teacher_name || "",
    schoolName: teacher.school_name || "",
    district: teacher.district || "",
    medium: teacher.medium || "",
  };
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

  if (type !== "baseline" && type !== "unit" && type !== "endline") {
    return NextResponse.json({ error: "Invalid type" }, { status: 400 });
  }

  try {
    await assertFeatureAllowed(auth.uid, "reportPdfExport", "prime");

    const saved = await getAssessmentRecord(auth.uid, type, grade, groupId);
    if (!saved?.tallies?.length) {
      return NextResponse.json(
        { error: "Save assessment scores first, then export the report" },
        { status: 404 },
      );
    }

    const tool = resolveAssessmentTool(grade, subject, medium, type, groupId);
    if (!tool) {
      return NextResponse.json({ error: "Assessment tool not found" }, { status: 404 });
    }

    const report = buildAssessmentReport(tool, {
      toolId: tool.id,
      studentsAssessed: saved.studentsAssessed,
      tallies: saved.tallies,
      notes: saved.notes,
    });

    const profile = await readTeacherProfile(auth.uid);
    const baseline = await getAssessmentRecord(auth.uid, "baseline", grade);
    const endline = await getAssessmentRecord(auth.uid, "endline", grade);
    const growth =
      type === "endline" ? buildGrowthComparison(baseline, endline) : null;

    const html = renderAssessmentReportHtml(
      report,
      {
        teacherName: profile.teacherName,
        schoolName: profile.schoolName,
        district: profile.district,
        medium: profile.medium,
        classLabel: `Grade ${grade} English`,
        notes: saved.notes,
      },
      growth,
    );

    const filename = `${tool.id}-report.html`;
    return new NextResponse(html, {
      headers: {
        "Content-Type": "text/html; charset=utf-8",
        "Content-Disposition": `inline; filename="${filename}"`,
      },
    });
  } catch (err) {
    if (err instanceof TierLimitError) return tierErrorResponse(auth.uid, err);
    console.error("ASSESSMENT REPORT:", err);
    return NextResponse.json({ error: "Could not generate report" }, { status: 500 });
  }
}
