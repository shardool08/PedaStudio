/**
 * Initialize Firestore defaults for all existing users and verify catalog.
 *
 * Usage:
 *   node scripts/init-database.mjs
 *   node scripts/init-database.mjs --dry-run
 */

import admin from "firebase-admin";

const dryRun = process.argv.includes("--dry-run");

function initAdmin() {
  if (admin.apps.length) return;
  const raw = process.env.FIREBASE_SERVICE_ACCOUNT_JSON;
  if (raw) {
    admin.initializeApp({ credential: admin.credential.cert(JSON.parse(raw)) });
    return;
  }
  admin.initializeApp();
}

initAdmin();
const db = admin.firestore();

function currentUsageMonth() {
  const now = new Date();
  return `${now.getUTCFullYear()}-${String(now.getUTCMonth() + 1).padStart(2, "0")}`;
}

function emptyUsage() {
  const month = currentUsageMonth();
  return { month, plans: 0, worksheets: 0, scans: 0, ocrScans: 0 };
}

async function ensureUserDefaults(uid, data) {
  const patch = {};
  if (!data.tier) patch.tier = "basic";
  if (!data.usage) patch.usage = emptyUsage();
  if (!data.createdAt) patch.createdAt = admin.firestore.FieldValue.serverTimestamp();
  if (Object.keys(patch).length === 0) return false;
  patch.updatedAt = admin.firestore.FieldValue.serverTimestamp();
  if (!dryRun) await db.collection("users").doc(uid).set(patch, { merge: true });
  return true;
}

console.log(dryRun ? "DRY RUN — no writes" : "Initializing database…");

const usersSnap = await db.collection("users").get();
let patched = 0;
for (const doc of usersSnap.docs) {
  if (await ensureUserDefaults(doc.id, doc.data())) {
    patched++;
    console.log(`  users/${doc.id} — added defaults`);
  }
}

const tlmSnap = await db.doc("catalog/tlmResources").get();
const fcSnap = await db.collection("catalog/flashcards/lessons").get();

console.log(`\nSummary:`);
console.log(`  Teachers: ${usersSnap.size} (${patched} patched)`);
console.log(`  TLM catalog: ${tlmSnap.exists ? "seeded" : "MISSING — run npm run firebase:seed-catalog"}`);
console.log(`  Flashcard lessons: ${fcSnap.size}`);

if (!tlmSnap.exists) {
  console.log(`\nRun: npm run firebase:seed-catalog`);
}

console.log(`\nDeploy indexes: npm run firebase:deploy-rules`);
console.log(`Admin panel: set ADMIN_SECRET and visit /admin/login`);
