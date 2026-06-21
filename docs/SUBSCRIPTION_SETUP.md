# Subscription & payments setup

## Tiers

| Tier | Monthly | Yearly |
|------|---------|--------|
| Basic | Free | Free |
| Prime | ₹149 | ₹1,299 |
| Max | ₹399 | ₹3,999 |

## Firebase secrets (App Hosting)

```bash
firebase functions:secrets:set RAZORPAY_KEY_ID --project pedastudio-d6b2a
firebase functions:secrets:set RAZORPAY_KEY_SECRET --project pedastudio-d6b2a
```

Grant secrets to the `pedastudio-api` backend in Firebase Console → App Hosting → Environment.

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
