import type { AssessmentTool } from "@/lib/assessment-tools/types";
import type { PrintOptions } from "@/lib/assessment-tools/render";
import { renderAssessmentMarkdown } from "@/lib/assessment-tools/render";

function esc(s: string): string {
  return s
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

function mdToHtml(md: string): string {
  return esc(md)
    .replace(/\*\*(.+?)\*\*/g, "<strong>$1</strong>")
    .replace(/^# (.+)$/gm, "<h1>$1</h1>")
    .replace(/^## (.+)$/gm, "<h2>$1</h2>")
    .replace(/^### (.+)$/gm, "<h3>$1</h3>")
    .replace(/^_(.+)_$/gm, "<em>$1</em>")
    .replace(/\n/g, "<br/>");
}

/** Printable HTML assessment pack — Print → Save as PDF. */
export function renderAssessmentToolHtml(tool: AssessmentTool, opts: PrintOptions): string {
  const md = renderAssessmentMarkdown(tool, opts);
  const copyLabel = opts.copy === "student" ? "Student Copy" : "Assessor Copy";
  const body = mdToHtml(md);

  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <title>${esc(tool.title)} — ${copyLabel}</title>
  <style>
    body { font-family: Georgia, "Times New Roman", serif; color: #222; max-width: 720px; margin: 24px auto; padding: 0 16px; line-height: 1.5; font-size: 14px; }
    h1 { font-size: 1.4rem; color: #496580; border-bottom: 2px solid #2a7a6a; padding-bottom: 8px; }
    h2 { font-size: 1.1rem; color: #2a7a6a; margin-top: 20px; }
    h3 { font-size: 1rem; }
    @media print { body { margin: 12px; } }
  </style>
</head>
<body>
  ${body}
  <p style="margin-top:24px;font-size:11px;color:#888">PedaStudio · ${esc(copyLabel)} · Print this page to save as PDF</p>
</body>
</html>`;
}
