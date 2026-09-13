import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";
import { loadEnv } from "./load-env.mjs";

loadEnv();

const root = path.join(path.dirname(fileURLToPath(import.meta.url)), "..");
const url = process.env.NEXT_PUBLIC_SUPABASE_URL?.trim() || "";
const anon = process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY?.trim() || "";
const lp = path.join(root, "android", "local.properties");
let text = fs.existsSync(lp) ? fs.readFileSync(lp, "utf8") : "";

function setKey(src, key, value) {
  if (!value) return src;
  const line = `${key}=${value}`;
  const re = new RegExp(`^${key.replace(/\./g, "\\.")}=.*$`, "m");
  if (re.test(src)) return src.replace(re, line);
  return `${src.trimEnd()}\n${line}\n`;
}

text = setKey(text, "pedastudio.supabase.url", url);
text = setKey(text, "pedastudio.supabase.anon", anon);
fs.writeFileSync(lp, text);
console.log("local.properties supabase url:", url ? "SET" : "MISSING");
console.log("local.properties supabase anon:", anon ? `SET len=${anon.length}` : "MISSING");
