import type { BalbharatiLesson } from "@/lib/curriculum";

export type PlanMode = "practice" | "reteach" | "continue" | null;

export interface PlanSelections {
  goal?: string;
  hook?: string;
  tlms?: string;
  teaching?: string;
  practice?: string;
  assessment?: string;
  notes?: string;
  // legacy chat-plan keys
  p1?: string;
  p2?: string;
  p3?: string;
  p4?: string;
  p5?: string;
  p6?: string;
}

export interface TeacherProfile {
  language?: string;
  medium?: string;
  studentCount?: string;
  seating?: string;
  resources?: string;
  name?: string;
  comfort?: string;
}

function langName(code?: string): string {
  if (code === "hi") return "Hindi";
  if (code === "ur") return "Urdu";
  if (code === "en") return "English";
  return "Marathi";
}

function modeInstruction(mode: PlanMode): string {
  if (mode === "practice") {
    return "FEEDBACK MODE — PRACTICE DAY: Students struggled yesterday. Keep the same objective but use a NEW, simpler activity and extra guided practice. Do not introduce new vocabulary.";
  }
  if (mode === "reteach") {
    return "FEEDBACK MODE — RE-TEACH: Most students did not understand. Simplify everything — shorter steps, more repetition, concrete examples. Same day's focus, easier approach.";
  }
  if (mode === "continue") {
    return "FEEDBACK MODE — CONTINUE: Teacher ran out of time yesterday. Plan only the remaining parts of this day's focus. Start with a quick recap, then finish what was left.";
  }
  return "";
}

export function buildPlanPrompt(
  lesson: BalbharatiLesson,
  day: number,
  selections: PlanSelections,
  teacherProfile: TeacherProfile,
  mode: PlanMode = null,
  assessmentReteachNotes?: string,
): string {
  const dayInfo = lesson.bloomsProgression.find((b) => b.day === day);
  const lang = langName(teacherProfile.language);
  const grade = parseInt(lesson.id.split(".")[0], 10) || 1;

  const objective = selections.goal || selections.p1 || dayInfo?.focus || "";
  const hook = selections.hook || selections.p2 || "not set";
  const tlms = selections.tlms || selections.p3 || teacherProfile.resources || "Blackboard, Textbook";
  const teaching = selections.teaching || selections.p4 || "not set";
  const practice = selections.practice || selections.p5 || "Whole class together";
  const assessment = selections.assessment || selections.p6 || "Oral questions";
  const notes = selections.notes || "";
  const modeLine = modeInstruction(mode);
  const assessmentLine =
    assessmentReteachNotes?.trim()
      ? `\nASSESSMENT DATA — target these weak learning outcomes in this lesson:\n${assessmentReteachNotes.trim()}\n`
      : "";

  return `You are creating a COMPLETE, READY-TO-USE lesson plan for a Grade ${grade} English teacher in a ${lang}-medium municipal school in Maharashtra, India.

LESSON: ${lesson.id} — ${lesson.en} (Balbharati 2025-26)
Type: ${lesson.type} | Pages: ${lesson.pages} | Unit: ${lesson.unit}
Day ${day} of ${lesson.days}
Bloom's Level: ${dayInfo?.level || "Remember"}
Focus: ${dayInfo?.focus || objective}

VOCABULARY: ${lesson.vocabulary.join(", ")}
STRUCTURES: ${lesson.structures.join(" | ") || "none"}
COMPETENCIES: ${lesson.competencies.join("; ")}

TEACHER'S CHOICES:
- Objective/Goal: ${objective}
- Hook style: ${hook}
- Available TLMs: ${tlms}
- Teaching approach: ${teaching}
- Practice style: ${practice}
- Assessment method: ${assessment}
${notes ? `- Special notes: ${notes}` : ""}
${assessmentLine}

TEACHER PROFILE:
- Name: ${teacherProfile.name || "Teacher"}
- Class size: ${teacherProfile.studentCount || "35"} students
- Medium: ${teacherProfile.medium || "Marathi"}
- Seating: ${teacherProfile.seating || "Rows"}
- English comfort: ${teacherProfile.comfort || "moderate"}

${modeLine ? modeLine + "\n" : ""}
Return ONLY a JSON object with this EXACT structure:

{
  "objective": {
    "blooms_level": "${dayInfo?.level || "Remember"}",
    "text": "By the end of this session, students will be able to [Bloom's verb] [specific content]",
    "success_criteria": "Each child can [measurable action]"
  },
  "hook": {
    "duration": "2-3 min",
    "steps": ["Step 1: [exact instruction in ${lang}]", "Step 2: [next step]", "Step 3: [next step]"],
    "teacher_says": "[Opening line in ${lang}]",
    "youtube_query": ""
  },
  "tlm": {
    "items": [
      {"name": "[TLM item from teacher's list]", "is_printable": false, "description": "[How to use it in class]"}
    ]
  },
  "activity": {
    "name": "[Activity name matching ${teaching} approach]",
    "duration": "10-12 min",
    "steps": [
      {"step": 1, "instruction": "[Exact instruction in ${lang}]", "duration": "3 min"},
      {"step": 2, "instruction": "[Next step]", "duration": "3 min"},
      {"step": 3, "instruction": "[Next step]", "duration": "4 min"}
    ],
    "tip": "[Classroom management tip]"
  },
  "practice": {
    "mode": "${practice}",
    "duration": "5-7 min",
    "steps": ["[What teacher does]", "[What students do]", "[How to check understanding]"],
    "student_action": "[Exact task for each student]"
  },
  "assessment": {
    "type": "[oral/written/both based on ${assessment}]",
    "duration": "3-5 min",
    "questions": ["[Q1 in ${lang}]", "[Q2]", "[Q3]", "[Q4]", "[Q5]"],
    "exit_token": "[Quick check before students leave]",
    "needs_worksheet": ${assessment.toLowerCase().includes("worksheet") ? "true" : "false"}
  },
  "closure": {
    "duration": "2 min",
    "instruction": "[How to end: recap key words, praise, preview tomorrow]"
  },
  "vocabulary_focus": ["word1", "word2", "word3"],
  "board_plan": "[What to write on the blackboard — words, structures, diagrams]"
}

RULES:
- ALL instructions in ${lang} with English vocabulary in brackets
- The hook MUST use the "${hook}" approach
- Practice MUST use "${practice}" format
- Assessment MUST use "${assessment}" method
- Only use TLMs the teacher has: ${tlms}
- NEVER suggest printing worksheets or materials — use blackboard, oral, physical TLM only
- Be VERY specific with teacher actions and exact sentences
- youtube_query must be empty string
- Return ONLY valid JSON, no markdown`;
}

export function normalizePlan(plan: Record<string, unknown>, selections?: PlanSelections): Record<string, unknown> {
  const out = { ...plan };

  const activity = out.activity as Record<string, unknown> | undefined;
  if (activity) {
    if (!activity.tip && activity.management_tip) activity.tip = activity.management_tip;
    if (Array.isArray(activity.steps)) {
      activity.steps = activity.steps.map((s: Record<string, unknown>) => ({
        ...s,
        duration: s.duration || s.time || "2 min",
      }));
    }
    out.activity = activity;
  }

  const practice = out.practice as Record<string, unknown> | undefined;
  if (practice) {
    const steps = practice.steps;
    if ((!steps || (Array.isArray(steps) && steps.length === 0)) && practice.instruction) {
      practice.steps = String(practice.instruction)
        .split(/\n|(?<=[.!?])\s+/)
        .map((s) => s.trim())
        .filter(Boolean);
    }
    out.practice = practice;
  }

  if (!out.tlm && selections?.tlms) {
    out.tlm = {
      items: selections.tlms.split(",").map((t) => ({
        name: t.trim(),
        is_printable: t.trim().toLowerCase().includes("flashcard"),
        description: "",
      })),
    };
  }

  const assessment = out.assessment as Record<string, unknown> | undefined;
  if (assessment && !assessment.type) {
    assessment.type = selections?.assessment || "oral";
    out.assessment = assessment;
  }

  return out;
}
