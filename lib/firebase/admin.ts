import { cert, getApps, initializeApp, type App } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";

let adminApp: App | null = null;

export function getAdminApp(): App | null {
  if (adminApp) return adminApp;
  if (getApps().length) {
    adminApp = getApps()[0]!;
    return adminApp;
  }
  try {
    // Firebase App Hosting sets FIREBASE_CONFIG — Admin SDK initializes with no args.
    if (process.env.FIREBASE_CONFIG) {
      adminApp = initializeApp();
      return adminApp;
    }
    const raw = process.env.FIREBASE_SERVICE_ACCOUNT_JSON;
    if (raw) {
      const serviceAccount = JSON.parse(raw);
      adminApp = initializeApp({
        credential: cert(serviceAccount),
        projectId: serviceAccount.project_id || process.env.NEXT_PUBLIC_FIREBASE_PROJECT_ID,
      });
      return adminApp;
    }
    return null;
  } catch {
    return null;
  }
}

export async function verifyFirebaseIdToken(token: string): Promise<string | null> {
  const app = getAdminApp();
  if (!app) return null;
  try {
    const decoded = await getAuth(app).verifyIdToken(token);
    return decoded.uid;
  } catch {
    return null;
  }
}

export function isApiAuthConfigured(): boolean {
  return !!(
    process.env.FIREBASE_CONFIG ||
    process.env.FIREBASE_SERVICE_ACCOUNT_JSON ||
    process.env.GOOGLE_CLOUD_PROJECT
  );
}
