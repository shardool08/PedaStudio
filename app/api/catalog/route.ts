import { NextRequest, NextResponse } from "next/server";
import { requireApiUser } from "@/lib/api-auth";
import { getSupabaseAdmin, isSupabaseConfigured } from "@/lib/supabase/server";

export const dynamic = "force-dynamic";

/**
 * Image URLs for TLM resources and, when `lessonId` is given, that lesson's flashcards.
 * Replaces the app's direct reads of Firestore `catalog/*`. The app ships labels and
 * emoji locally, so an empty response here just means no artwork yet.
 */
export async function GET(req: NextRequest) {
  const auth = await requireApiUser(req);
  if (auth instanceof NextResponse) return auth;

  const lessonId = new URL(req.url).searchParams.get("lessonId")?.trim();
  const empty = { tlmImages: {}, flashcardImages: {} };
  if (!isSupabaseConfigured()) return NextResponse.json(empty);

  const tlm = await getSupabaseAdmin().from("tlm_resources").select("id,image_url");
  if (tlm.error) console.error("TLM catalog read failed:", tlm.error.message);

  const tlmImages: Record<string, string> = {};
  for (const row of tlm.data ?? []) {
    if (row.image_url) tlmImages[String(row.id)] = String(row.image_url);
  }

  const flashcardImages: Record<string, string> = {};
  if (lessonId) {
    const cards = await getSupabaseAdmin()
      .from("flashcard_lessons")
      .select("cards")
      .eq("lesson_id", lessonId)
      .maybeSingle();
    if (cards.error) console.error("Flashcard catalog read failed:", cards.error.message);

    const list = (cards.data?.cards as Array<{ word?: string; imageUrl?: string }> | null) ?? [];
    for (const card of list) {
      if (card.word && card.imageUrl) flashcardImages[card.word] = card.imageUrl;
    }
  }

  return NextResponse.json({ tlmImages, flashcardImages });
}
