/**
 * Set a teacher subscription tier in Supabase (admin use).
 *
 *   node scripts/set-teacher-tier.mjs --uid=TEACHER_ID --tier=prime
 *   node scripts/set-teacher-tier.mjs --uid=TEACHER_ID --tier=prime --days=90
 *   node scripts/set-teacher-tier.mjs --all --tier=max
 *
 * Requires NEXT_PUBLIC_SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY in .env
 */

import { createClient } from "@supabase/supabase-js";
import { loadEnv } from "./load-env.mjs";

loadEnv();

const args = Object.fromEntries(
  process.argv.slice(2).map((arg) => {
    const [k, v] = arg.replace(/^--/, "").split("=");
    return [k, v ?? "true"];
  }),
);

const uid = args.uid;
const allUsers = args.all === "true";
const tier = args.tier || "basic";
const days = args.days ? Number(args.days) : null;

if (!uid && !allUsers) {
  console.error(
    "Usage: node scripts/set-teacher-tier.mjs --uid=USER_ID --tier=basic|prime|max [--days=90]",
  );
  console.error("   or: node scripts/set-teacher-tier.mjs --all --tier=basic|prime|max [--days=90]");
  process.exit(1);
}

if (!["basic", "prime", "max"].includes(tier)) {
  console.error("tier must be basic, prime, or max");
  process.exit(1);
}

const url = process.env.NEXT_PUBLIC_SUPABASE_URL?.trim();
const key = process.env.SUPABASE_SERVICE_ROLE_KEY?.trim();
if (!url || !key) {
  console.error("Set NEXT_PUBLIC_SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY in .env");
  process.exit(1);
}

const sb = createClient(url, key, { auth: { persistSession: false } });

function tierPatch() {
  if (tier === "basic" || !days) return { tier, tier_expires_at: null };
  const expires = new Date();
  expires.setUTCDate(expires.getUTCDate() + days);
  return { tier, tier_expires_at: expires.toISOString() };
}

if (allUsers) {
  const { data, error } = await sb.from("teachers").update(tierPatch()).neq("id", "").select("id");
  if (error) {
    console.error(error.message);
    process.exit(1);
  }
  console.log(`Set tier=${tier} on ${(data ?? []).length} teacher(s)${days ? ` for ${days} days` : ""}`);
} else {
  const { error } = await sb.from("teachers").upsert({ id: uid, ...tierPatch() }, { onConflict: "id" });
  if (error) {
    console.error(error.message);
    process.exit(1);
  }
  console.log(`Set teachers/${uid} tier=${tier}${days ? ` for ${days} days` : ""}`);
}
