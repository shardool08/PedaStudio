import type { GrowthComparison } from "@/lib/assessment-growth";
import type { AssessmentReport } from "@/lib/assessment-tools/types";

export interface ReportMeta {
  teacherName?: string;
  schoolName?: string;
  district?: string;
  medium?: string;
  classLabel?: string;
  notes?: string;
}

function esc(s: string): string {
  return s
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

/** Printable HTML class report — open in browser → Print → Save as PDF. */
export function renderAssessmentReportHtml(
  report: AssessmentReport,
  meta: ReportMeta = {},
  growth: GrowthComparison | null = null,
): string {
  const date = new Date().toLocaleDateString("en-IN", {
    day: "numeric",
    month: "short",
    year: "numeric",
  });

  const strandRows = report.strands
    .map(
      (s) =>
        `<tr><td>${esc(s.label)}</td><td class="num">${s.percent}%</td><td class="num">${s.marksObtained} / ${s.marksPossible}</td></tr>`,
    )
    .join("");

  const weakList =
    report.weakItems.length > 0
      ? `<p class="weak"><strong>Focus next (reteach):</strong> ${report.weakItems.map(esc).join(", ")}</p>`
      : "";

  const misfitList =
    report.misfitItems?.length > 0
      ? `<p class="misfit"><strong>Review items (pilot):</strong> ${report.misfitItems.map(esc).join(", ")} — very easy or very hard; may need revision.</p>`
      : "";

  const growthBlock =
    growth?.baselinePercent != null && growth.endlinePercent != null
      ? `<div class="card growth">
    <h2 style="font-size:1.1rem;margin:0 0 12px;color:#496580">Baseline → Endline growth</h2>
    <table>
      <tr><td>Baseline</td><td class="num">${growth.baselinePercent}%</td><td class="num">${growth.baselineStudents ?? "—"} students</td></tr>
      <tr><td>Endline</td><td class="num">${growth.endlinePercent}%</td><td class="num">${growth.endlineStudents ?? "—"} students</td></tr>
      <tr><td><strong>Growth</strong></td><td class="num"><strong>${growth.growthPoints != null && growth.growthPoints >= 0 ? "+" : ""}${growth.growthPoints ?? "—"} pts</strong></td><td></td></tr>
    </table>
  </div>`
      : "";

  const itemRows = report.items
    .map((i) => {
      const flag =
        report.weakItems.includes(i.itemId) ? " weak" : report.misfitItems?.includes(i.itemId) ? " misfit" : "";
      return `<tr class="${flag.trim()}"><td>${esc(i.itemId)}</td><td>${esc(i.competency)}</td><td class="num">${i.percentCorrect}%</td><td class="num">${i.marksObtained}/${i.marksPossible}</td></tr>`;
    })
    .join("");

  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>${esc(report.title)} — Class Report</title>
  <style>
    * { box-sizing: border-box; }
    body { font-family: system-ui, "Segoe UI", sans-serif; color: #496580; margin: 0; padding: 24px; background: #f8fcfb; }
    .card { background: #fff; border: 1px solid #d0eae4; border-radius: 12px; padding: 20px; max-width: 720px; margin: 0 auto 16px; }
    h1 { font-size: 1.35rem; margin: 0 0 4px; color: #496580; }
    .sub { font-size: 0.85rem; color: #6b8a9a; margin-bottom: 16px; }
    .score { font-size: 2.5rem; font-weight: 700; color: #2a7a6a; margin: 8px 0; }
    table { width: 100%; border-collapse: collapse; font-size: 0.9rem; }
    th, td { text-align: left; padding: 8px 6px; border-bottom: 1px solid #e8f2ef; }
    th { color: #2a7a6a; font-weight: 600; }
    .num { text-align: right; }
    .weak { background: #ffdbbb; padding: 10px 12px; border-radius: 8px; font-size: 0.9rem; }
    .misfit { background: #f0faf8; padding: 10px 12px; border-radius: 8px; font-size: 0.85rem; margin-top: 8px; }
    tr.weak td { background: #fff8f0; }
    tr.misfit td { background: #f5f5f5; }
    .footer { font-size: 0.75rem; color: #8aa5b5; text-align: center; margin-top: 24px; }
    @media print { body { background: #fff; padding: 12px; } .card { border: none; box-shadow: none; } }
  </style>
</head>
<body>
  <div class="card">
    <h1>${esc(report.title)}</h1>
    <p class="sub">PedaStudio Class Assessment Report · Grade ${report.grade} · ${date}</p>
    ${meta.teacherName ? `<p class="sub">Teacher: ${esc(meta.teacherName)}</p>` : ""}
    ${meta.schoolName ? `<p class="sub">School: ${esc(meta.schoolName)}${meta.district ? ` · ${esc(meta.district)}` : ""}</p>` : ""}
    ${meta.classLabel ? `<p class="sub">Class: ${esc(meta.classLabel)}</p>` : ""}
    <p>Students assessed: <strong>${report.studentsAssessed}</strong></p>
    <div class="score">${report.scorePercent}%</div>
    <p>Total marks: ${report.totalMarksObtained} / ${report.totalMarksPossible}</p>
    ${meta.notes ? `<p class="sub">Notes: ${esc(meta.notes)}</p>` : ""}
  </div>

  ${growthBlock}

  <div class="card">
    <h2 style="font-size:1.1rem;margin:0 0 12px;color:#496580">FLN strand summary</h2>
    <table>
      <thead><tr><th>Strand</th><th class="num">%</th><th class="num">Marks</th></tr></thead>
      <tbody>${strandRows}</tbody>
    </table>
    ${weakList}
    ${misfitList}
  </div>

  <div class="card">
    <h2 style="font-size:1.1rem;margin:0 0 12px;color:#496580">Item breakdown</h2>
    <table>
      <thead><tr><th>Item</th><th>Competency</th><th class="num">Class %</th><th class="num">Marks</th></tr></thead>
      <tbody>${itemRows}</tbody>
    </table>
  </div>

  <p class="footer">Generated by PedaStudio · Maharashtra municipal schools · FLN-aligned English assessment · Print this page to save as PDF</p>
</body>
</html>`;
}
