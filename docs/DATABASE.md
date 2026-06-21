# Firestore Database Schema

PedaStudio uses **Firebase Firestore** as the cloud database. TypeScript types live in `lib/schema/`.

## Collections

```
catalog/
  tlmResources              — public TLM resource catalog
  flashcards/lessons/{id}   — flashcard content per lesson

users/{uid}                 — teacher profile + account fields
  plans/{lessonId_dayN}     — saved lesson plans
  assessments/{id}          — baseline / unit / endline scores
  classes/{classId}         — (scaffold) ability groups for Max tier
  payments/{paymentId}      — Razorpay payment records (server-only write)
```

## Protected fields (server-only)

Clients cannot write these on `users/{uid}`:

- `tier`, `tierExpiresAt`
- `usage` — `{ month, plans, worksheets, scans, ocrScans }`
- `subscription`

## Setup commands

```bash
# Seed TLM + flashcard catalog
npm run firebase:seed-catalog

# Add tier/usage defaults to existing users
npm run firebase:init-db

# Deploy rules + indexes
npm run firebase:deploy-rules

# Set a teacher tier (CLI)
npm run tier:set -- --uid=USER_ID --tier=prime --days=90
```

## Admin panel

Set `ADMIN_SECRET` in `.env.local`, then visit `/admin/login`.

Features: dashboard stats, teacher list, tier management, usage reset, catalog status.

## Indexes

Defined in `firestore.indexes.json` — deploy with `firebase deploy --only firestore:indexes`.
