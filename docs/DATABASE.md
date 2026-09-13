# Database schema

PedaStudio stores teacher data in **Supabase Postgres**. The live tables are created
by `supabase/migrations/`. TypeScript types live in `lib/schema/` and
`lib/supabase/teachers.ts`.

| Table | Purpose |
|-------|---------|
| `teachers` | Profile + server-owned tier, usage, subscription |
| `plans` | Saved lesson plans (unique teacher + lesson + day) |
| `assessments` | Baseline / unit / endline scores |
| `classes` | Ability groups (Max) |
| `payments` | Razorpay receipts (server-only) |
| `tlm_resources` | Catalog artwork URLs |
| `flashcard_lessons` | Catalog flashcard image URLs |

The Android app never talks to the database. It calls `/api/profile`, `/api/plans`,
`/api/catalog`, `/api/me`, and the AI routes. Auth is Supabase Phone OTP.
