/**
 * Seed TLM + flashcard catalog into Supabase from the Android bundled assets.
 *
 *   npm run supabase:seed-catalog
 *
 * Requires NEXT_PUBLIC_SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY.
 */
import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";
import { createClient } from "@supabase/supabase-js";
import { loadEnv } from "./load-env.mjs";

loadEnv();

const root = path.join(path.dirname(fileURLToPath(import.meta.url)), "..");
const url = process.env.NEXT_PUBLIC_SUPABASE_URL?.trim();
const key = process.env.SUPABASE_SERVICE_ROLE_KEY?.trim();
if (!url || !key) {
  console.error("Set NEXT_PUBLIC_SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY in .env");
  process.exit(1);
}

const sb = createClient(url, key, { auth: { persistSession: false } });

const tlmPath = path.join(root, "android/app/src/main/assets/tlm-catalog.json");
const tlm = JSON.parse(fs.readFileSync(tlmPath, "utf8"));
const tlmRows = tlm.map((item, index) => ({
  id: item.id,
  label: item.label,
  emoji: item.emoji || "",
  image_url: item.imageUrl || null,
  sort_order: index,
}));

const { error: tlmError } = await sb.from("tlm_resources").upsert(tlmRows, { onConflict: "id" });
if (tlmError) {
  console.error("TLM seed failed:", tlmError.message);
  process.exit(1);
}
console.log(`TLM resources: ${tlmRows.length} rows`);

const fcPath = path.join(root, "android/app/src/main/assets/flashcards.json");
const lessons = JSON.parse(fs.readFileSync(fcPath, "utf8"));
const fcRows = Object.entries(lessons).map(([lessonId, lesson]) => ({
  lesson_id: lessonId,
  title: lesson.title || "",
  cards: lesson.cards || [],
}));

const chunk = 50;
for (let i = 0; i < fcRows.length; i += chunk) {
  const slice = fcRows.slice(i, i + chunk);
  const { error } = await sb.from("flashcard_lessons").upsert(slice, { onConflict: "lesson_id" });
  if (error) {
    console.error("Flashcard seed failed:", error.message);
    process.exit(1);
  }
}
console.log(`Flashcard lessons: ${fcRows.length} rows`);
console.log("Catalog seeded.");
