#!/usr/bin/env node
/**
 * Push Razorpay keys from .env to Firebase App Hosting secrets.
 * Requires: firebase CLI logged in, RAZORPAY_KEY_ID + RAZORPAY_KEY_SECRET in .env
 *
 * Usage: node scripts/set-razorpay-secrets.mjs
 * Then:  npm run firebase:deploy-api
 */
import { readFileSync, existsSync } from "fs";
import { spawnSync } from "child_process";
import { resolve, dirname } from "path";
import { fileURLToPath } from "url";

const root = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const envPath = resolve(root, ".env");

function loadEnv() {
  if (!existsSync(envPath)) {
    console.error("Missing .env — add RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET first.");
    process.exit(1);
  }
  const vars = {};
  for (const line of readFileSync(envPath, "utf8").split("\n")) {
    const m = line.match(/^([A-Z0-9_]+)=(.*)$/);
    if (m) vars[m[1]] = m[2].trim().replace(/^["']|["']$/g, "");
  }
  return vars;
}

function setSecret(name, value) {
  if (!value) {
    console.error(`Missing ${name} in .env`);
    process.exit(1);
  }
  console.log(`Setting secret ${name}…`);
  const r = spawnSync(
    "firebase",
    ["apphosting:secrets:set", name, "--project", "pedastudio-d6b2a", "--data-file", "-"],
    { input: value, stdio: ["pipe", "inherit", "inherit"], shell: true },
  );
  if (r.status !== 0) process.exit(r.status ?? 1);
}

const env = loadEnv();
setSecret("RAZORPAY_KEY_ID", env.RAZORPAY_KEY_ID);
setSecret("RAZORPAY_KEY_SECRET", env.RAZORPAY_KEY_SECRET);
console.log("\nDone. Redeploy API: npm run firebase:deploy-api");
