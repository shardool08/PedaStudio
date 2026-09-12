# PedaStudio — Project Status

Last updated: June 2026

## Architecture

- **Android app** (primary) — Kotlin/Compose (migrating Auth/DB off Firebase)
- **API** — Next.js (moving from Firebase App Hosting → Railway/VPS)
- **Database / Auth** — **Supabase** (Postgres + Phone Auth) — see `docs/SUPABASE_MIGRATION.md`
- **Admin** — `/admin` (dashboard, teachers, tiers, catalog)
- **Payments** — Razorpay (unchanged)

## Completed

| Area | Status |
|------|--------|
| Registration, profile, home, roadmap | Done |
| Quick plan, plan view, lesson detail | Done |
| Scan, worksheet, TLM kit, flashcards (emoji) | Done |
| Structured assessments G1–5 English | Done (G1 L2 hand-crafted; others auto-generated) |
| Assessment tally, scan/mark, HTML reports | Done |
| Baseline → endline growth in endline report | Done |
| Weak items → re-teach lesson plan | Done (Android) |
| `afterUnitTest` for Basic tier plan modes | Done |
| Admin assessment drill-down (strands, weak items) | Done |
| Printable HTML tool packs (`format=html`) | Done |
| MCQ answer rotation in auto-generator | Done |
| Psychometric misfit flags (p&lt;0.2, p&gt;0.95) | Done (in reports) |

## Deploy checklist

```powershell
cd c:\Users\techl\PedaStudio
npm run firebase:deploy-rules   # if rules changed
npm run firebase:deploy-api     # ship API changes
```

Rebuild Android app in Android Studio after pulling.

## Still open (by priority)

### P1 — Content quality
- Hand-crafted assessment packs for G1 L1, G2–G5 L1/L2 (Phase B in `docs/ASSESSMENT_DESIGN.md`)
- Human review of auto-generated items before wide pilot

### P2 — Product
- Grade 4–5 textbook verification
- Razorpay live checkout (needs Firebase secrets)
- Pilot tier automation in-app (script exists: `npm run tier:set`)
- Bundled offline assessment item content in Android assets

### P3 — Future
- Web teacher UI for assessments
- Per-student longitudinal records
- Cluster / block officer dashboards
- BigQuery export for district analytics

## Dropped from plan

- Grades 6–8, non-English subjects (for now)
- Flashcard AI pipeline, voice input
