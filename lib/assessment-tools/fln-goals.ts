import type { FlnStrand } from "./types";

/** NIPUN-aligned English FLN strands — labels for teacher reports. */
export const FLN_STRAND_LABELS: Record<FlnStrand, string> = {
  OL: "Oral Language",
  PA: "Phonological Awareness",
  DEC: "Decoding & Letter-Sound",
  RF: "Reading Fluency",
  RC: "Reading Comprehension",
  WR: "Writing",
  VOC: "Vocabulary",
};

/** Grade-band expectations referenced in baseline/endline design. */
export interface GradeFlnProfile {
  grade: number;
  baselineFocus: string[];
  endlineFocus: string[];
  /** Minimum strands to cover in every unit test. */
  unitTestStrands: FlnStrand[];
}

export const GRADE_FLN_PROFILES: GradeFlnProfile[] = [
  {
    grade: 1,
    baselineFocus: [
      "Listen and follow 1-step instruction in English",
      "Recognise 5+ common letters by shape/name",
      "Name familiar objects (body/home) with support",
      "Count 1–5 objects; respond to How many?",
      "Identify rhyming pairs in oral items",
    ],
    endlineFocus: [
      "Introduce self; use greetings and can/can't",
      "All 26 letter sounds; read/write CVC words",
      "Read simple sentences; retell story events",
      "Numbers 0–10; days of week; basic prepositions",
      "Vocabulary across all 4 units of My English Book",
    ],
    unitTestStrands: ["OL", "VOC", "DEC", "RC"],
  },
  {
    grade: 2,
    baselineFocus: [
      "Read Grade 1 CVC words at sight",
      "Follow 2-step instructions",
      "Use past tense in simple sentences (oral)",
      "Read short passage (30–40 words) with support",
    ],
    endlineFocus: [
      "Read unseen passage independently",
      "Write 3–4 sentence description / letter",
      "Use plurals, prepositions, present continuous",
      "Comprehension: main idea + inference (simple)",
    ],
    unitTestStrands: ["OL", "VOC", "DEC", "RF", "RC", "WR"],
  },
  {
    grade: 3,
    baselineFocus: [
      "Read Grade 2 level passage fluently",
      "Spell familiar words; punctuation basics",
      "Answer WH questions from short text",
    ],
    endlineFocus: [
      "Paragraph writing; dialogue punctuation",
      "Grammar: tenses, articles, conjunctions (intro)",
      "Comprehension of 80–100 word unseen passage",
    ],
    unitTestStrands: ["VOC", "RF", "RC", "WR", "DEC"],
  },
  {
    grade: 4,
    baselineFocus: [
      "Read multi-paragraph text; identify main idea",
      "Use adjectives, prepositions, pronouns correctly",
      "Write 5–6 sentence composition",
    ],
    endlineFocus: [
      "Digraphs, magic-e, vowel combinations in reading",
      "Story writing with sequence; formal letter draft",
      "Comprehension including vocabulary in context",
    ],
    unitTestStrands: ["VOC", "RF", "RC", "WR", "DEC"],
  },
  {
    grade: 5,
    baselineFocus: [
      "Silent letters; prefix/suffix recognition",
      "Read biography-style passage",
      "Write notice / paragraph with structure",
    ],
    endlineFocus: [
      "Tenses, conjunctions, comparison degrees",
      "Essay / story / formal letter",
      "Inferential comprehension; charts and tables",
    ],
    unitTestStrands: ["VOC", "RF", "RC", "WR", "DEC", "OL"],
  },
];

export function getGradeFlnProfile(grade: number): GradeFlnProfile | undefined {
  return GRADE_FLN_PROFILES.find((p) => p.grade === grade);
}
