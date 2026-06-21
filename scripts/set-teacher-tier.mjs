/**
 * Set a teacher subscription tier in Firestore (admin use).
 *
 * Usage:
 *   node scripts/set-teacher-tier.mjs --uid=FIREBASE_UID --tier=prime
 *   node scripts/set-teacher-tier.mjs --uid=FIREBASE_UID --tier=prime --days=90
 *
 * Requires GOOGLE_APPLICATION_CREDENTIALS or FIREBASE_SERVICE_ACCOUNT_JSON.
 */

import admin from "firebase-admin";

const args = Object.fromEntries(
  process.argv.slice(2).map((arg) => {
    const [k, v] = arg.replace(/^--/, "").split("=");
    return [k, v ?? "true"];
  }),
);

const uid = args.uid;
const tier = args.tier || "basic";
const days = args.days ? Number(args.days) : null;

if (!uid) {
  console.error("Usage: node scripts/set-teacher-tier.mjs --uid=USER_ID --tier=basic|prime|max [--days=90]");
  process.exit(1);
}

if (!["basic", "prime", "max"].includes(tier)) {
  console.error("tier must be basic, prime, or max");
  process.exit(1);
}

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

const payload = {
  tier,
  updatedAt: admin.firestore.FieldValue.serverTimestamp(),
};

if (days && tier !== "basic") {
  const expires = new Date();
  expires.setUTCDate(expires.getUTCDate() + days);
  payload.tierExpiresAt = expires;
}

await db.collection("users").doc(uid).set(payload, { merge: true });
console.log(`Set users/${uid} tier=${tier}${days ? ` for ${days} days` : ""}`);
