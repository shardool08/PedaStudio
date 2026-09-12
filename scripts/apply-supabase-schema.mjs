/**
 * Apply supabase/migrations/001_initial_schema.sql
 *
 * Requires in .env (or .env.local):
 *   DATABASE_URL=postgresql://postgres.[ref]:[password]@aws-0-....pooler.supabase.com:6543/postgres
 *
 * Get it from: Supabase → Project Settings → Database → Connection string (URI)
 * Prefer "Session mode" (port 5432) or direct connection for DDL.
 */
import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";
import { createRequire } from "module";

const require = createRequire(import.meta.url);
const __dirname = path.dirname(fileURLToPath(import.meta.url));
const root = path.join(__dirname, "..");

function loadEnv() {
  for (const name of [".env.local", ".env"]) {
    const p = path.join(root, name);
    if (!fs.existsSync(p)) continue;
    for (const line of fs.readFileSync(p, "utf8").split(/\r?\n/)) {
      const m = line.match(/^([A-Z0-9_]+)=(.*)$/);
      if (!m || process.env[m[1]]) continue;
      process.env[m[1]] = m[2].trim().replace(/^["']|["']$/g, "");
    }
  }
}

loadEnv();

const databaseUrl =
  process.env.DATABASE_URL ||
  process.env.SUPABASE_DB_URL ||
  process.env.POSTGRES_URL;

if (!databaseUrl) {
  console.error(`
Missing DATABASE_URL.

1. Supabase Dashboard → Project Settings → Database
2. Copy "Connection string" → URI (use the postgres password you set)
3. Add to .env:

DATABASE_URL=postgresql://postgres.[PROJECT_REF]:YOUR_PASSWORD@db.[PROJECT_REF].supabase.co:5432/postgres

Then run: node scripts/apply-supabase-schema.mjs
`);
  process.exit(1);
}

async function main() {
  let pg;
  try {
    pg = require("pg");
  } catch {
    console.error('Missing dependency. Run: npm install pg --no-save');
    process.exit(1);
  }

  const sqlPath = path.join(root, "supabase", "migrations", "001_initial_schema.sql");
  const sql = fs.readFileSync(sqlPath, "utf8");

  const client = new pg.Client({
    connectionString: databaseUrl,
    ssl: { rejectUnauthorized: false },
  });

  console.log("Connecting to Supabase Postgres…");
  await client.connect();
  try {
    console.log("Applying 001_initial_schema.sql…");
    await client.query(sql);
    const tables = await client.query(`
      select table_name from information_schema.tables
      where table_schema = 'public'
        and table_name in (
          'teachers','plans','assessments','classes','payments',
          'tlm_resources','flashcard_lessons'
        )
      order by table_name
    `);
    console.log("Done. Tables present:");
    for (const row of tables.rows) console.log(" -", row.table_name);
  } finally {
    await client.end();
  }
}

main().catch((err) => {
  console.error("Migration failed:", err.message);
  process.exit(1);
});
