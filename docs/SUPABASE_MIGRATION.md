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
3. Local/dev: with no Supabase or Firebase env vars set, `lib/api-auth.ts` treats unauthenticated
   requests as a fixed dev uid so routes stay callable. This only applies when
   `NODE_ENV=development`. For a real OTP round trip, add a test number under
   Auth → Phone instead.

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

## 5. Web sign-in (done)

`/login` signs teachers in with Supabase phone OTP (`app/(teacher)/login/page.tsx`).
Session state comes from `lib/auth-context.tsx`, `apiFetch` in `lib/api-client.ts` attaches
the access token, and `RequireAuth` guards teacher pages.

`requireApiUser` accepts **Supabase** access tokens. Older Android builds that still send
Firebase ID tokens are verified against Google's public certificates (no Admin SDK).

## 6. Android cutover (done)

1. Phone OTP goes to Supabase Auth (`PhoneAuthController`).
2. The app sends `Authorization: Bearer <supabase_access_token>` to `/api/*`.
3. Profile, plans and catalog sync through `/api/profile`, `/api/plans`, `/api/catalog`.
4. The Firebase BOM / `google-services` plugin are gone. Put the public Supabase URL and
   anon key in `android/gradle.properties` (`pedastudio.supabase.url` / `.anon`).

## 7. Data from old Firebase

If you still have Firestore data and the project is readable:

```bash
# Export manually from Firebase console, or use a one-off script later.
```

Clean pilot restart is OK if no real teacher data must be kept.

## 8. Cost (pilot)

| Item | Typical |
|------|---------|
| Supabase Free | $0 (500MB DB, Auth included; SMS billed by provider) |
| API host (Railway/VPS) | ~$5–6/mo |
| MSG91/Twilio OTP | Pay per SMS |
| Anthropic | Usage-based (main variable) |
| Razorpay | % of payments |

## Status in repo

- [x] Postgres schema (`supabase/migrations/001_initial_schema.sql` + `002` + `003`)
- [x] Supabase server client (`lib/supabase/`)
- [x] Web phone OTP at `/login`
- [x] Android phone OTP + profile/plan/catalog sync via the API
- [x] Tiers, usage, assessments, admin panel on Supabase
- [x] API on Railway (`pedastudio-production.up.railway.app`)
- [ ] Enable **Phone** under Supabase → Authentication → Providers
- [ ] Run `003_auth_user_trigger.sql` in the SQL Editor
- [ ] Rebuild the Android app with `pedastudio.supabase.url` / `.anon` set
