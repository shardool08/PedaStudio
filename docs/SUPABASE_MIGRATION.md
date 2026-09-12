# Migrate off Firebase → Supabase

Firebase Blaze is cancelled. Target stack:

| Job | Old | New |
|-----|-----|-----|
| Database | Firestore | **Supabase Postgres** |
| Auth (phone OTP) | Firebase Auth | **Supabase Auth** (Phone) |
| Files | Firebase Storage | **Supabase Storage** |
| Next.js API | Firebase App Hosting | **Railway** or small VPS (recommended) / Vercel Pro |
| Payments | Razorpay | Unchanged |
| AI | Anthropic | Unchanged |

> Why not free Vercel alone? Plan/scan routes need up to ~120s. Hobby serverless limits are too short. Pair Supabase with **Railway** (~$5/mo) or a **$5 VPS** running `npm start`.

## 1. Create Supabase project

1. [supabase.com](https://supabase.com) → New project (Free tier is fine for pilot).
2. Project Settings → API → copy:
   - Project URL → `NEXT_PUBLIC_SUPABASE_URL`
   - `anon` `public` key → `NEXT_PUBLIC_SUPABASE_ANON_KEY`
   - `service_role` key → `SUPABASE_SERVICE_ROLE_KEY` (**server only, never in Android**)
3. SQL Editor → paste and run `supabase/migrations/001_initial_schema.sql`.

## 2. Enable Phone Auth

1. Authentication → Providers → **Phone** → Enable.
2. For India cost control, use **MSG91** or Twilio under Auth → Phone → SMS provider (Supabase docs).
3. Local/dev: set `ALLOW_DEV_OTP=true` and use a fixed test flow (see `lib/supabase/auth.ts`) — never enable in production.

## 3. Env files

Copy `.env.example` → `.env.local` and fill Supabase + Anthropic + Razorpay.  
Remove reliance on `FIREBASE_*` / `FIREBASE_SERVICE_ACCOUNT_JSON` once cutover is done.

## 4. Host the Next.js API

**Option A — Railway (simple)**  
- New Web Service from this repo  
- Start: `npm run build && npm start`  
- Set all env vars from `.env.example`  
- Point Android `pedastudio.api.url.production` at the Railway URL  

**Option B — VPS**  
- Docker or Node 22 + `npm start` on Hetzner/DigitalOcean (~$5–6/mo)

## 5. Android cutover (next engineering phase)

1. Replace Firebase Auth with Supabase phone OTP (or call your `/api/auth/*` helpers).
2. Send `Authorization: Bearer <supabase_access_token>` to existing `/api/*` routes.
3. Remove direct Firestore reads/writes (`FirestoreRepository`) — sync via API only.
4. Drop `google-services` / Firebase BOM when Auth+Firestore are gone.

## 6. Data from old Firebase

If you still have Firestore data and the project is readable:

```bash
# Export manually from Firebase console, or use a one-off script later.
```

Clean pilot restart is OK if no real teacher data must be kept.

## 7. Cost (pilot)

| Item | Typical |
|------|---------|
| Supabase Free | $0 (500MB DB, Auth included; SMS billed by provider) |
| API host (Railway/VPS) | ~$5–6/mo |
| MSG91/Twilio OTP | Pay per SMS |
| Anthropic | Usage-based (main variable) |
| Razorpay | % of payments |

## Status in repo

- [x] Postgres schema (`supabase/migrations/001_initial_schema.sql`)
- [x] Supabase server client (`lib/supabase/`)
- [ ] Wire `lib/tier-service.ts` / admin / subscription off Firestore → Supabase
- [ ] Android Auth + remove Firestore SDK
- [ ] Deploy API to Railway/VPS and update Android production URL
