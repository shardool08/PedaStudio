# Tier gating checklist

Source of truth: `lib/tier-config.ts` (server) and `android/.../data/TierConfig.kt` (client UI).

**Usage resets every Monday UTC** (`usage.week` = ISO date of that Monday).

Server enforcement is required for any feature that costs money or must not be bypassed.

## Enforced today

| Feature | API / store | Basic | Prime | Max |
|---------|-------------|-------|-------|-----|
| Lesson plan generation | `POST /api/generate-plan` + `usage.plans` | 2/wk | 6/wk | 6/wk |
| Plan modes (reteach/practice/continue) | `POST /api/generate-plan` `mode` | Always | Always | Always |
| Grade access | `POST /api/generate-plan` | 1–5 | 1–5 | 1–5 |
| Max free trial | `GET /api/me` → `ensureMaxTrial` | — | — | 7 days once |
| Textbook scan | `POST /api/scan` + `usage.scans` | No | No | 2/wk |
| Bulk scan + AI marking | `POST /api/assessment/scan-mark` + `usage.ocrScans` | No | 6/wk | 12/wk |
| Report export | `GET /api/assessment/report` + `reportPdfExport` | No | No | Yes |
| Account snapshot | `GET /api/me` | — | — | — |

## Client UI only (wire when feature ships)

| Feature | Gate with | Basic | Prime | Max |
|---------|-----------|-------|-------|-----|
| Baseline / endline / unit tests | `features.*Assessment*` | Yes | Yes | Yes |
| Manual assessment entry | `features.manualAssessmentEntry` | Yes | Yes | Yes |
| Short vs full action plan | `shortActionPlan` / `fullActionPlan` | Short | Full | Full |
| Unit TLM kit | `features.unitTlmKit` | Yes | Yes | Yes |
| Year TLM full + PDF | `yearTlmListFull`, `yearTlmPdfShare` | View only | Full | Full |
| Worksheets | `features.worksheets` + 1 per plan/week | No | 1 per plan | 1 per plan |
| Ability groups | `features.abilityGroups` | No | No | Yes |
| Cluster export | `features.clusterExport` | No | No | Yes |
| Hindi / Urdu UI | `features.hindiUrduUi` | Yes | Yes | Yes |
| Max classes / students | `limits.maxClasses`, `maxStudentsPerClass` | 1 × 45 | 1 × 45 | 2 × 60 |

## Setting a teacher tier (admin)

Tier is stored on `users/{uid}`:

```json
{
  "tier": "prime",
  "tierExpiresAt": "2026-09-01T00:00:00.000Z"
}
```

Only the **Admin SDK** (API routes / Firebase console / admin script) may write `tier`, `tierExpiresAt`, and `usage`. Client writes are blocked in `firestore.rules`.

Usage document shape:

```json
{
  "usage": {
    "week": "2026-06-23",
    "plans": 1,
    "worksheets": 0,
    "scans": 0,
    "ocrScans": 0
  }
}
```

## Adding a new gated feature

1. Add limit + feature flags to `lib/tier-config.ts`
2. Mirror flags in `TierConfig.kt`
3. Enforce in API route with `assertFeatureAllowed` / `assertWeeklyUsageAllowed` / `incrementUsage`
4. Gate Android entry point with `TeacherAccount.features`
5. Update this checklist
