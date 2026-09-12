import fs from "fs";
import path from "path";
import { createClient } from "@supabase/supabase-js";
import { fileURLToPath } from "url";

const root = path.join(path.dirname(fileURLToPath(import.meta.url)), "..");
for (const name of [".env", ".env.local"]) {
  const p = path.join(root, name);
  if (!fs.existsSync(p)) continue;
  for (const line of fs.readFileSync(p, "utf8").split(/\r?\n/)) {
    const m = line.match(/^([A-Z0-9_]+)=(.*)$/);
    if (!m || process.env[m[1]]) continue;
    process.env[m[1]] = m[2].trim().replace(/^["']|["']$/g, "");
  }
}

const url = process.env.NEXT_PUBLIC_SUPABASE_URL || "";
const anon = process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY || "";
const service = process.env.SUPABASE_SERVICE_ROLE_KEY || "";

function mask(v) {
  if (!v) return "MISSING";
  if (v.length < 12) return `SET (short ${v.length})`;
  return `SET (${v.slice(0, 6)}…${v.slice(-4)}, ${v.length} chars)`;
}

console.log("URL:", url ? (url.includes("supabase.co") ? "OK looks like supabase" : "SET but unexpected host") : "MISSING");
console.log("ANON/publishable:", mask(anon));
console.log("SERVICE/secret:", mask(service));

if (!url || !service) {
  console.error("FAIL: need URL + secret key");
  process.exit(1);
}

const sb = createClient(url, service, { auth: { persistSession: false } });
const { data, error } = await sb.from("teachers").select("id").limit(1);

if (error) {
  console.error("FAIL:", error.message);
  process.exit(1);
}

console.log("CONNECT: OK — teachers table reachable");
console.log("teachers rows (sample):", data?.length ?? 0);
