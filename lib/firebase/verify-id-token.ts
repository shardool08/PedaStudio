import { createVerify } from "crypto";

/**
 * Verifies a Firebase ID token using Google's public signing certificates.
 *
 * The Admin SDK does the same thing, but it needs a service-account key that the
 * Railway deployment does not have. This path needs nothing but a network call to
 * Google, so it works on any host and on the Firebase Spark (no-billing) plan.
 */

const CERT_URL =
  "https://www.googleapis.com/robot/v1/metadata/x509/securetoken@system.gserviceaccount.com";

/** Tolerance for clock drift between this server and Google, in seconds. */
const CLOCK_SKEW_SECONDS = 60;

let certCache: { certs: Record<string, string>; expiresAt: number } | null = null;

export function firebaseProjectId(): string {
  return (
    process.env.FIREBASE_PROJECT_ID?.trim() ||
    process.env.NEXT_PUBLIC_FIREBASE_PROJECT_ID?.trim() ||
    // Matches android/app/google-services.json; not a secret (it ships inside the APK).
    "pedastudio-d6b2a"
  );
}

async function getSigningCerts(): Promise<Record<string, string>> {
  if (certCache && certCache.expiresAt > Date.now()) return certCache.certs;

  const response = await fetch(CERT_URL);
  if (!response.ok) throw new Error(`Could not fetch Google signing certs (${response.status})`);
  const certs = (await response.json()) as Record<string, string>;

  const maxAge = Number(response.headers.get("cache-control")?.match(/max-age=(\d+)/)?.[1] ?? 3600);
  certCache = { certs, expiresAt: Date.now() + maxAge * 1000 };
  return certs;
}

function decodeSegment<T>(segment: string): T | null {
  try {
    return JSON.parse(Buffer.from(segment, "base64url").toString("utf8")) as T;
  } catch {
    return null;
  }
}

interface TokenHeader {
  alg?: string;
  kid?: string;
}

interface TokenPayload {
  aud?: string;
  iss?: string;
  sub?: string;
  user_id?: string;
  exp?: number;
  iat?: number;
  auth_time?: number;
}

/** Returns the uid when the token is genuinely signed by Google for this project, else null. */
export async function verifyIdTokenWithGoogleCerts(token: string): Promise<string | null> {
  const [headerPart, payloadPart, signaturePart] = token.split(".");
  if (!headerPart || !payloadPart || !signaturePart) return null;

  const header = decodeSegment<TokenHeader>(headerPart);
  const payload = decodeSegment<TokenPayload>(payloadPart);
  if (!header || !payload) return null;
  if (header.alg !== "RS256" || !header.kid) return null;

  const projectId = firebaseProjectId();
  if (payload.aud !== projectId) return null;
  if (payload.iss !== `https://securetoken.google.com/${projectId}`) return null;

  const uid = payload.sub || payload.user_id;
  if (!uid) return null;

  const now = Math.floor(Date.now() / 1000);
  if (!payload.exp || payload.exp + CLOCK_SKEW_SECONDS < now) return null;
  if (payload.iat && payload.iat - CLOCK_SKEW_SECONDS > now) return null;

  let certs: Record<string, string>;
  try {
    certs = await getSigningCerts();
  } catch (error) {
    console.error("Firebase token verify: cert fetch failed", error);
    return null;
  }

  const cert = certs[header.kid];
  if (!cert) return null;

  try {
    const verifier = createVerify("RSA-SHA256");
    verifier.update(`${headerPart}.${payloadPart}`);
    verifier.end();
    if (!verifier.verify(cert, Buffer.from(signaturePart, "base64url"))) return null;
  } catch {
    return null;
  }

  return uid;
}
