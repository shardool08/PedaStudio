// Flashcard Generation Pipeline for PedaStudio
// Run this script ONCE to generate flashcard content for all lessons
// Output: JSON files per grade with pre-built flashcard sets
//
// Usage: node scripts/generate-flashcards.mjs
//
// Prerequisites:
// - Set ANTHROPIC_API_KEY environment variable
// - npm install (project dependencies)

import fs from "fs";
import path from "path";

const API_KEY = process.env.ANTHROPIC_API_KEY;
if (!API_KEY) {
  console.error("ERROR: Set ANTHROPIC_API_KEY environment variable");
  process.exit(1);
}

// Import curriculum data by reading the TS files and extracting lesson arrays
// Since we can't import TS directly, we'll read and eval the exported arrays
function loadLessons(filepath) {
  const content = fs.readFileSync(filepath, "utf8");
  // Extract the array between the first [ and its matching ]
  const match = content.match(/export const \w+ = (\[[\s\S]*?\n\];)/);
  if (!match) return [];
  try {
    // Strip TypeScript type annotations
    let clean = match[1]
      .replace(/as const/g, "")
      .replace(/: \w+\[\]/g, "")
      .replace(/\/\/.*/g, ""); // remove comments
    return eval(clean);
  } catch (e) {
    console.error("Failed to parse", filepath, e.message);
    return [];
  }
}

// All curriculum files to process
const CURRICULUM_FILES = [
  { grade: 1, type: "L2", file: "lib/curriculum/grade1-english.ts", varName: "balbharatiLessons" },
  { grade: 1, type: "L1", file: "lib/curriculum/grade1-english-l1.ts", varName: "grade1EnglishL1Lessons" },
  { grade: 2, type: "L1", file: "lib/curriculum/grade2-english-l1.ts", varName: "grade2EnglishL1Lessons" },
  { grade: 2, type: "L2", file: "lib/curriculum/grade2-english-l2.ts", varName: "grade2EnglishL2Lessons" },
  { grade: 3, type: "L2", file: "lib/curriculum/grade3-english-l2.ts", varName: "grade3EnglishL2Lessons" },
  { grade: 4, type: "L2", file: "lib/curriculum/grade4-english-l2.ts", varName: "grade4EnglishL2Lessons" },
  { grade: 5, type: "L2", file: "lib/curriculum/grade5-english-l2.ts", varName: "grade5EnglishL2Lessons" },
];

async function generateFlashcardsForLesson(lesson, grade) {
  const prompt = `You are creating classroom flashcards for Grade ${grade} English in an Indian municipal school.

LESSON: ${lesson.id} — ${lesson.en}
TYPE: ${lesson.type}
VOCABULARY: ${(lesson.vocabulary || []).join(", ")}
STRUCTURES: ${(lesson.structures || []).join("; ")}
COMPETENCIES: ${(lesson.competencies || []).join("; ")}

Generate flashcards appropriate for Grade ${grade}:

${grade <= 2 ? `GRADE ${grade} FORMAT (beginner):
- Simple word flashcards: just the word + a clear image description
- For nouns: "cat" → image of a cat
- For verbs: "run" → image of a child running  
- For adjectives: "big" → image showing something big vs small
- 8-10 flashcards per lesson` : ""}

${grade === 3 ? `GRADE 3 FORMAT (early intermediate):
- Word flashcards: word + image description
- Phrase flashcards: "the big cat" (article + adjective + noun)
- Simple sentence patterns: "I can ___" with blank to fill
- 8-12 flashcards per lesson, mix of words and phrases` : ""}

${grade >= 4 ? `GRADE ${grade} FORMAT (intermediate):
- Word flashcards for new vocabulary
- Sentence pattern flashcards: "Subject + Verb + Object" with example
- Grammar structure flashcards: "She is ___ing" (present continuous)
- Comparison flashcards: "___ is bigger than ___"
- 10-12 flashcards per lesson, mix of vocabulary and structures` : ""}

For EACH flashcard provide:
1. "text" — the word, phrase, or sentence pattern shown on the card
2. "type" — one of: "word", "phrase", "pattern", "structure"  
3. "image_prompt" — a detailed prompt for AI image generation. Be very specific:
   - For children aged ${grade + 5}-${grade + 6} years old
   - Simple, colorful, cartoon/illustration style
   - White or light background
   - No text in the image
   - Clear, recognizable subject
   - Example: "A cute cartoon brown cat sitting, simple illustration style, white background, child-friendly, colorful"
4. "blank" — if it's a pattern/structure card, what goes in the blank (null for word cards)
5. "category" — "noun", "verb", "adjective", "preposition", "structure", "pattern"

Respond with ONLY a JSON array, no other text:
[
  { "text": "cat", "type": "word", "image_prompt": "A cute cartoon cat...", "blank": null, "category": "noun" },
  ...
]`;

  try {
    const response = await fetch("https://api.anthropic.com/v1/messages", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "x-api-key": API_KEY,
        "anthropic-version": "2023-06-01",
      },
      body: JSON.stringify({
        model: process.env.ANTHROPIC_MODEL || "claude-sonnet-4-5-20250929",
        max_tokens: 2000,
        messages: [{ role: "user", content: prompt }],
      }),
    });

    const data = await response.json();
    if (data.error) {
      console.error(`  ERROR for ${lesson.id}: ${data.error.message}`);
      return null;
    }

    const text = data.content?.[0]?.text || "[]";
    const clean = text.replace(/```json|```/g, "").trim();
    return JSON.parse(clean);
  } catch (e) {
    console.error(`  PARSE ERROR for ${lesson.id}: ${e.message}`);
    return null;
  }
}

async function main() {
  const outputDir = "lib/flashcards";
  if (!fs.existsSync(outputDir)) fs.mkdirSync(outputDir, { recursive: true });

  let totalLessons = 0;
  let totalCards = 0;
  const allImagePrompts = [];

  for (const curr of CURRICULUM_FILES) {
    console.log(`\n=== Processing Grade ${curr.grade} ${curr.type} ===`);
    
    // Read lessons from file
    const content = fs.readFileSync(curr.file, "utf8");
    // Extract vocabulary and other fields for each lesson using regex
    const lessonMatches = content.matchAll(/\{\s*id:\s*"([^"]+)".*?en:\s*"([^"]+)".*?type:\s*"([^"]+)".*?vocabulary:\s*\[(.*?)\].*?structures:\s*\[(.*?)\].*?competencies:\s*\[(.*?)\]/gs);
    
    const lessons = [];
    for (const m of lessonMatches) {
      lessons.push({
        id: m[1],
        en: m[2],
        type: m[3],
        vocabulary: m[4].match(/"([^"]+)"/g)?.map(s => s.replace(/"/g, "")) || [],
        structures: m[5].match(/"([^"]+)"/g)?.map(s => s.replace(/"/g, "")) || [],
        competencies: m[6].match(/"([^"]+)"/g)?.map(s => s.replace(/"/g, "")) || [],
      });
    }

    console.log(`  Found ${lessons.length} lessons`);
    const gradeCards = {};

    for (let i = 0; i < lessons.length; i++) {
      const lesson = lessons[i];
      console.log(`  [${i + 1}/${lessons.length}] ${lesson.id}: ${lesson.en}...`);
      
      const cards = await generateFlashcardsForLesson(lesson, curr.grade);
      if (cards && cards.length > 0) {
        gradeCards[lesson.id] = cards;
        totalCards += cards.length;

        // Collect image prompts for Phase 2
        cards.forEach((card, j) => {
          allImagePrompts.push({
            id: `${lesson.id}_${j}`,
            lesson: lesson.id,
            text: card.text,
            prompt: card.image_prompt,
          });
        });

        console.log(`    → ${cards.length} cards generated`);
      } else {
        console.log(`    → FAILED, will retry later`);
      }

      // Rate limiting — wait 500ms between calls
      await new Promise(r => setTimeout(r, 500));
      totalLessons++;
    }

    // Save grade file
    const filename = `grade${curr.grade}-english-${curr.type.toLowerCase()}-flashcards.json`;
    fs.writeFileSync(
      path.join(outputDir, filename),
      JSON.stringify(gradeCards, null, 2)
    );
    console.log(`  Saved: ${filename}`);
  }

  // Save all image prompts for Phase 2 (Flux/DALL-E generation)
  fs.writeFileSync(
    path.join(outputDir, "image-prompts.json"),
    JSON.stringify(allImagePrompts, null, 2)
  );

  console.log(`\n========================================`);
  console.log(`DONE!`);
  console.log(`Total lessons processed: ${totalLessons}`);
  console.log(`Total flashcards generated: ${totalCards}`);
  console.log(`Image prompts saved: ${allImagePrompts.length}`);
  console.log(`\nNext step: Use image-prompts.json with Flux/DALL-E to generate images`);
  console.log(`========================================`);
}

main().catch(console.error);
