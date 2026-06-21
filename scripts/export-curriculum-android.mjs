import { readFileSync, writeFileSync, mkdirSync, readdirSync } from "fs";
import { join, dirname } from "path";
import { fileURLToPath } from "url";

const __dirname = dirname(fileURLToPath(import.meta.url));
const ROOT = join(__dirname, "..");
const OUT = join(ROOT, "android", "app", "src", "main", "assets");

function unescapeJsString(s) {
  return s.replace(/\\"/g, '"').replace(/\\\\/g, "\\");
}

/** Parse each lesson object block by id anchor — handles any field order. */
function parseLessons(filePath) {
  const text = readFileSync(join(ROOT, filePath), "utf8");
  const idRe = /id:\s*"([^"]+)"/g;
  const matches = [...text.matchAll(idRe)];
  const lessons = [];

  for (let i = 0; i < matches.length; i++) {
    const start = matches[i].index;
    const end = i + 1 < matches.length ? matches[i + 1].index : text.length;
    const block = text.slice(start, end);

    const pick = (re) => {
      const m = block.match(re);
      return m ? unescapeJsString(m[1]) : "";
    };
    const pickInt = (re) => {
      const m = block.match(re);
      return m ? Number(m[1]) : 0;
    };

    const vocabulary = [];
    const vocabBlock = block.match(/vocabulary:\s*\[([\s\S]*?)\]/);
    if (vocabBlock) {
      const wordRe = /"([^"]+)"/g;
      let wm;
      while ((wm = wordRe.exec(vocabBlock[1])) !== null) vocabulary.push(wm[1]);
    }

    const bloomsProgression = [];
    const bloomsRe = /\{\s*day:\s*(\d+)\s*,\s*level:\s*"([^"]+)"\s*,\s*focus:\s*"((?:\\.|[^"\\])*)"\s*\}/g;
    let bm;
    while ((bm = bloomsRe.exec(block)) !== null) {
      bloomsProgression.push({
        day: Number(bm[1]),
        level: bm[2],
        focus: unescapeJsString(bm[3]),
      });
    }

    lessons.push({
      id: matches[i][1],
      unit: pickInt(/unit:\s*(\d+)/),
      en: pick(/en:\s*"((?:\\.|[^"\\])*)"/),
      mr: pick(/mr:\s*"((?:\\.|[^"\\])*)"/) || pick(/en:\s*"((?:\\.|[^"\\])*)"/),
      hi: pick(/hi:\s*"((?:\\.|[^"\\])*)"/) || pick(/en:\s*"((?:\\.|[^"\\])*)"/),
      ur: pick(/ur:\s*"((?:\\.|[^"\\])*)"/) || pick(/en:\s*"((?:\\.|[^"\\])*)"/),
      type: pick(/type:\s*"([^"]+)"/),
      pages: pick(/pages:\s*"([^"]+)"/),
      days: pickInt(/days:\s*(\d+)/),
      vocabulary,
      bloomsProgression,
    });
  }
  return lessons;
}

function parseStringArray(filePath, constName) {
  const text = readFileSync(join(ROOT, filePath), "utf8");
  const block = text.match(new RegExp(`export const ${constName} = \\[([\\s\\S]*?)\\] as const`));
  if (!block) return [];
  const items = [];
  const re = /"([^"]+)"/g;
  let m;
  while ((m = re.exec(block[1])) !== null) items.push(m[1]);
  return items;
}

function parseAssessmentGroups(filePath) {
  const text = readFileSync(join(ROOT, filePath), "utf8");
  const block = text.match(/export const assessmentGroups[\s\S]*?=\s*\{([\s\S]*?)\};\s*(?:export|$)/);
  if (!block) return {};
  const groups = {};
  const groupRe = /(\w+):\s*\{\s*name:\s*"((?:\\.|[^"\\])*)"\s*,\s*lessons:\s*\[([\s\S]*?)\]\s*,\s*focus:\s*"((?:\\.|[^"\\])*)"\s*\}/g;
  let m;
  while ((m = groupRe.exec(block[1])) !== null) {
    const lessons = [];
    const lessonRe = /"([^"]+)"/g;
    let lm;
    while ((lm = lessonRe.exec(m[3])) !== null) lessons.push(lm[1]);
    groups[m[1]] = {
      name: unescapeJsString(m[2]),
      lessons,
      focus: unescapeJsString(m[4]),
    };
  }
  return groups;
}

function parseL1AssessmentGroups(filePath, constName) {
  const text = readFileSync(join(ROOT, filePath), "utf8");
  const block = text.match(
    new RegExp(`export const ${constName}[\\s\\S]*?=\\s*\\{([\\s\\S]*?)\\};\\s*(?:export|$)`),
  );
  if (!block) return {};
  const groups = {};
  const groupRe = /(\w+):\s*\{\s*name:\s*"((?:\\.|[^"\\])*)"\s*,\s*lessons:\s*\[([\s\S]*?)\]\s*,\s*focus:\s*"((?:\\.|[^"\\])*)"\s*\}/g;
  let m;
  while ((m = groupRe.exec(block[1])) !== null) {
    const lessons = [];
    const lessonRe = /"([^"]+)"/g;
    let lm;
    while ((lm = lessonRe.exec(m[3])) !== null) lessons.push(lm[1]);
    groups[m[1]] = {
      name: unescapeJsString(m[2]),
      lessons,
      focus: unescapeJsString(m[4]),
    };
  }
  return groups;
}

function parseLabelValues(filePath, constName) {
  const text = readFileSync(join(ROOT, filePath), "utf8");
  const block = text.match(new RegExp(`export const ${constName} = \\[([\\s\\S]*?)\\] as const`));
  if (!block) return [];
  const items = [];
  const re = /\{\s*value:\s*"([^"]+)",\s*label:\s*"([^"]+)"\s*\}/g;
  let m;
  while ((m = re.exec(block[1])) !== null) items.push({ value: m[1], label: m[2] });
  return items;
}

function parseEmojiMap() {
  const text = readFileSync(join(ROOT, "lib", "emoji-map.ts"), "utf8");
  const map = {};
  const re = /^\s*(\w[\w-]*):\s*"([^"]+)"/gm;
  let m;
  while ((m = re.exec(text)) !== null) map[m[1].toLowerCase()] = m[2];
  return map;
}

function parseWordMeanings() {
  const text = readFileSync(join(ROOT, "lib", "flashcards", "word-meanings.ts"), "utf8");
  const map = {};
  const entryRe = /(\w[\w-]*):\s*\{\s*mr:\s*"([^"]*)"(?:,\s*hi:\s*"([^"]*)")?(?:,\s*ur:\s*"([^"]*)")?\s*\}/g;
  let m;
  while ((m = entryRe.exec(text)) !== null) {
    map[m[1].toLowerCase()] = { mr: m[2], hi: m[3] || m[2], ur: m[4] || m[2], en: m[1] };
  }
  return map;
}

function exportFlashcards(curriculum, emojiMap, meanings) {
  const packsDir = join(ROOT, "lib", "flashcards", "lessons");
  const packFiles = readdirSync(packsDir).filter((f) => f.endsWith(".json"));
  const packs = {};
  for (const file of packFiles) {
    const pack = JSON.parse(readFileSync(join(packsDir, file), "utf8"));
    packs[pack.lessonId] = pack;
  }

  const allLessons = Object.values(curriculum).flat();
  const out = {};

  for (const lesson of allLessons) {
    const pack = packs[lesson.id];
    const words = pack?.cards?.map((c) => c.word.toLowerCase()) ||
      lesson.vocabulary?.map((w) => w.toLowerCase()) ||
      [];

    if (words.length === 0) continue;

    out[lesson.id] = {
      title: lesson.en,
      cards: words.map((word) => {
        const seed = pack?.cards?.find((c) => c.word.toLowerCase() === word);
        const meaning = meanings[word] || { en: word, mr: "", hi: "", ur: "" };
        return {
          word,
          emoji: emojiMap[word] || "📚",
          meaningMr: meaning.mr || word,
          meaningHi: meaning.hi || word,
          meaningUr: meaning.ur || word,
          type: seed?.type || "word",
        };
      }),
    };
  }
  return out;
}

mkdirSync(OUT, { recursive: true });

const curriculum = {
  "1_l2": parseLessons("lib/curriculum/grade1-english.ts"),
  "1_l1": parseLessons("lib/curriculum/grade1-english-l1.ts"),
  "2_l2": parseLessons("lib/curriculum/grade2-english-l2.ts"),
  "2_l1": parseLessons("lib/curriculum/grade2-english-l1.ts"),
  "3_l2": parseLessons("lib/curriculum/grade3-english-l2.ts"),
  "4_l2": parseLessons("lib/curriculum/grade4-english-l2.ts"),
  "5_l2": parseLessons("lib/curriculum/grade5-english-l2.ts"),
};

writeFileSync(join(OUT, "curriculum.json"), JSON.stringify(curriculum));

const assessments = {
  "1_l2": parseAssessmentGroups("lib/curriculum/grade1-english.ts"),
  "1_l1": parseL1AssessmentGroups("lib/curriculum/grade1-english-l1.ts", "grade1EnglishL1AssessmentGroups"),
  "2_l2": parseL1AssessmentGroups("lib/curriculum/grade2-english-l2.ts", "grade2EnglishL2AssessmentGroups"),
  "2_l1": parseL1AssessmentGroups("lib/curriculum/grade2-english-l1.ts", "grade2EnglishL1AssessmentGroups"),
  "3_l2": parseL1AssessmentGroups("lib/curriculum/grade3-english-l2.ts", "grade3EnglishL2AssessmentGroups"),
  "4_l2": parseL1AssessmentGroups("lib/curriculum/grade4-english-l2.ts", "grade4EnglishL2AssessmentGroups"),
  "5_l2": parseL1AssessmentGroups("lib/curriculum/grade5-english-l2.ts", "grade5EnglishL2AssessmentGroups"),
};
writeFileSync(join(OUT, "assessments.json"), JSON.stringify(assessments));

const tlmText = readFileSync(join(ROOT, "lib/tlm.ts"), "utf8");
const tlmResources = [];
const tlmRe = /\{\s*id:\s*"([^"]+)",\s*label:\s*"([^"]+)"/g;
let tm;
while ((tm = tlmRe.exec(tlmText)) !== null) tlmResources.push({ value: tm[1], label: tm[2] });

writeFileSync(
  join(OUT, "maharashtra.json"),
  JSON.stringify({
    districts: parseStringArray("lib/maharashtra-data.ts", "maharashtraDistricts"),
    zillaParishads: parseStringArray("lib/maharashtra-data.ts", "zillaParishads"),
    municipalCorporations: parseStringArray("lib/maharashtra-data.ts", "municipalCorporations"),
    administrationTypes: parseLabelValues("lib/maharashtra-data.ts", "administrationTypes"),
    mediums: parseLabelValues("lib/maharashtra-data.ts", "mediums"),
    internetAccess: parseLabelValues("lib/maharashtra-data.ts", "internetAccess"),
    printingAccess: parseLabelValues("lib/maharashtra-data.ts", "printingAccess"),
    tlmResources,
  }),
);

const emojiMap = parseEmojiMap();
const meanings = parseWordMeanings();
const flashcards = exportFlashcards(curriculum, emojiMap, meanings);
writeFileSync(join(OUT, "flashcards.json"), JSON.stringify(flashcards));

const counts = Object.fromEntries(Object.entries(curriculum).map(([k, v]) => [k, v.length]));
console.log("Curriculum:", counts);
console.log("Total lessons:", Object.values(curriculum).reduce((a, b) => a + b.length, 0));
console.log("Flashcard lessons:", Object.keys(flashcards).length);
