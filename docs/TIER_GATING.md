# Tier gating checklist

Source of truth: `lib/tier-config.ts` (server) and `android/.../data/TierConfig.kt` (client UI).

Server enforcement is required for any feature that costs money or must not be bypassed.

## Enforced today

| Feature | API / store | Basic | Prime | Max |
|---------|-------------|-------|-------|-----|
| Lesson plan generation | `POST /api/generate-plan` + Firestore `usage.plans` | 20/mo | Unlimited | Unlimited |
| Plan modes (reteach/practice/continue) | `POST /api/generate-plan` `mode` | After unit test only | Always | Always |
| Grade access | `POST /api/generate-plan` | 1–3 | 1–5 | 1–8 |
| Account snapshot | `GET /api/me` | — | — | — |

## Client UI only (wire when feature ships)

| Feature | Gate with | Basic | Prime | Max |
|---------|-----------|-------|-------|-----|
| Baseline / endline / unit tests | `features.*Assessment*` | Yes | Yes | Yes |
| Manual assessment entry | `features.manualAssessmentEntry` | Yes | Yes | Yes |
| Short vs full action plan | `shortActionPlan` / `fullActionPlan` | Short | Full | Full |
| Unit TLM kit | `features.unitTlmKit` | Yes | Yes | Yes |
| Year TLM full + PDF | `yearTlmListFull`, `yearTlmPdfShare` | View only | Full | Full |
| Worksheets | `features.worksheets` + `limits.worksheetsPerMonth` | No | 10/mo | Unlimited |
| Textbook scan | `features.textbookScan` + `limits.scansPerMonth` | No | 15/mo | 60/mo |
| Bulk scan + OCR | `bulkPaperScan`, `aiAutoMark`, `ocrScansPerMonth` | No | Yes | Priority |
| Report PDF export | `features.reportPdfExport` | No | Yes | Yes |
| Ability groups | `features.abilityGroups` | No | No | Yes |
| Cluster export | `features.clusterExport` | No | No | Yes |
| Hindi / Urdu UI | `features.hindiUrduUi` | No | Yes | Yes |
| Max classes / students | `limits.maxClasses`, `maxStudentsPerClass` | 1 × 45 | 2 × 45 | Unlimited × 60 |

## Setting a teacher tier (admin)

Tier is stored on `users/{uid}`:

```json
{
  "tier": "prime",
  "tierExpiresAt": "2026-09-01T00:00:00.000Z"
}
```

Only the **Admin SDK** (API routes / Firebase console / admin script) may write `tier`, `tierExpiresAt`, and `usage`. Client writes are blocked in `firestore.rules`.

Pilot: set first 50 teachers to `prime` with a 90-day `tierExpiresAt`.

## Adding a new gated feature

1. Add limit + feature flags to `lib/tier-config.ts`
2. Mirror flags in `TierConfig.kt`
3. Enforce in API route with `assertFeatureAllowed` / `incrementUsage`
4. Gate Android entry point with `TeacherAccount.features`
5. Update this checklist
