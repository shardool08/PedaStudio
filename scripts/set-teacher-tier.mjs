/**
 * Set a teacher subscription tier in Firestore (admin use).
 *
 * Usage:
 *   node scripts/set-teacher-tier.mjs --uid=FIREBASE_UID --tier=prime
 *   node scripts/set-teacher-tier.mjs --uid=FIREBASE_UID --tier=prime --days=90
 *   node scripts/set-teacher-tier.mjs --all --tier=max
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

function tierPayload() {
  const data = {
    tier,
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  };
  if (days && tier !== "basic") {
    const expires = new Date();
    expires.setUTCDate(expires.getUTCDate() + days);
    data.tierExpiresAt = expires;
  } else if (tier === "basic") {
    data.tierExpiresAt = admin.firestore.FieldValue.delete();
  } else {
    data.tierExpiresAt = admin.firestore.FieldValue.delete();
  }
  return data;
}

if (allUsers) {
  const snap = await db.collection("users").get();
  if (snap.empty) {
    console.log("No users found.");
    process.exit(0);
  }
  const batchSize = 400;
  let updated = 0;
  let batch = db.batch();
  let batchCount = 0;

  for (const doc of snap.docs) {
    batch.set(doc.ref, tierPayload(), { merge: true });
    batchCount++;
    updated++;
    if (batchCount >= batchSize) {
      await batch.commit();
      batch = db.batch();
      batchCount = 0;
    }
  }
  if (batchCount > 0) await batch.commit();
  console.log(`Set tier=${tier} on ${updated} user(s)${days ? ` for ${days} days` : ""}`);
} else {
  await db.collection("users").doc(uid).set(tierPayload(), { merge: true });
  console.log(`Set users/${uid} tier=${tier}${days ? ` for ${days} days` : ""}`);
}
