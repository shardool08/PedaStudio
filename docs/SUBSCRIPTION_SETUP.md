# Subscription & payments setup

## Tiers (Grades 1–5 English)

| Tier | Monthly | Yearly | Who it's for |
|------|---------|--------|--------------|
| Basic | Free | Free | One grade, 8 plans/mo |
| **Prime** | **₹129** | **₹999** | **All Grades 1–5 — best value** |
| Max | ₹279 | ₹2,299 | Two grades + scan & cluster export |

## Firebase secrets (App Hosting)

```bash
firebase apphosting:secrets:set ANTHROPIC_API_KEY --project pedastudio-d6b2a
firebase apphosting:secrets:grantaccess ANTHROPIC_API_KEY --backend pedastudio-api --project pedastudio-d6b2a
```

Optional Razorpay test keys (`rzp_test_…` from [Razorpay Dashboard](https://dashboard.razorpay.com)) — already wired in `apphosting.yaml`:

```bash
firebase apphosting:secrets:set RAZORPAY_KEY_ID --project pedastudio-d6b2a
firebase apphosting:secrets:set RAZORPAY_KEY_SECRET --project pedastudio-d6b2a
firebase apphosting:secrets:set RAZORPAY_WEBHOOK_SECRET --project pedastudio-d6b2a
firebase apphosting:secrets:grantaccess RAZORPAY_KEY_ID,RAZORPAY_KEY_SECRET,RAZORPAY_WEBHOOK_SECRET --backend pedastudio-api --project pedastudio-d6b2a
npm run firebase:deploy-api
```

In [Razorpay Dashboard → Webhooks](https://dashboard.razorpay.com/app/webhooks), add:

- **URL:** `https://<your-apphosting-url>/api/subscription/webhook`
- **Event:** `payment.captured`
- Copy the webhook secret into `RAZORPAY_WEBHOOK_SECRET`

Local dev: add the same keys to `.env`, run `npm run dev`, point Android at your PC IP.

**Important:** Do not set `PILOT_TIER=max` in production — it gives everyone Max and disables purchase buttons.

**Max trial:** Every new teacher gets **7 days of Max** automatically on first `GET /api/me` (one-time; tracked via `maxTrialUsed` in Firestore). After trial, they drop to Basic unless they subscribe.

Optional:

- `PEDASTUDIO_SUPPORT_WHATSAPP` — e.g. `919876543210`
- `PEDASTUDIO_SUPPORT_EMAIL` — e.g. `support@pedastudio.in`

## Pilot without Razorpay

If secrets are not set, the app shows **Contact us to upgrade** and opens WhatsApp.  
Grant tiers manually:

```bash
npm run tier:set -- --uid=FIREBASE_UID --tier=prime --days=90
```

## API routes

| Route | Purpose |
|-------|---------|
| `GET /api/me` | Account + tier + usage + subscription |
| `GET /api/subscription/plans` | Plan catalog + marketing copy |
| `POST /api/subscription/create-order` | Start Razorpay checkout |
| `POST /api/subscription/verify` | Verify payment → activate tier |
| `POST /api/subscription/webhook` | Razorpay `payment.captured` backup activation |

## Android flow

1. Profile → **Manage plan** → compare Basic / Prime / Max  
2. Choose monthly or yearly → **Upgrade**  
3. Razorpay checkout → server verifies signature → tier active in Firestore  
4. Usage limits enforced on `POST /api/generate-plan`

## Deploy

```bash
npm run firebase:deploy-rules
npm run firebase:deploy-api
```

Rebuild the Android app after pulling changes.
