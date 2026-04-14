# Barber Dashboard — Web → Native Android Migration Plan

> Source: Next.js web app (commits `fdc97d4`, `47e011f`, `72aa1d4`)  
> Target: Native Android (Kotlin) application  
> Date: 2026-04-13  
> Last Updated: 2026-04-13 (added onboarding flow, shop profile display, localisation)

---

## 1. Migration Philosophy

The existing **business logic and database rules stay in the backend** (Next.js API routes / Supabase). The Android app is a **pure consumer** of REST APIs. We do not port server-side logic to Android; instead we:

1. Expose clean, versioned REST endpoints from the existing Next.js backend
2. Build a native Android app that calls those endpoints
3. Reuse existing Supabase RLS, session validation, state machine, and payment logic unchanged

This keeps a single source of truth and avoids logic duplication.

---

## 2. Migration Phases

### Phase 0 — Backend API Hardening (Pre-Android Work)
> Done by backend team. Android work cannot start until Phase 0 is complete.

| # | Task | Output |
|---|------|--------|
| 0.1 | Agree on **mobile API contract** (versioned base path `/api/v1/barber/`) | API spec document |
| 0.2 | Define **standard JSON envelope** for all responses | See Section 5 |
| 0.3 | Replace cookie-based session with **Bearer token** auth for mobile | `Authorization: Bearer <token>` header |
| 0.4 | Add `/api/v1/barber/auth/refresh` endpoint for token renewal | Prevents 8-hour logout loop |
| 0.5 | Expose barber **dashboard summary** endpoint (today's stats) | New endpoint |
| 0.6 | Expose **bookings list** endpoint with date + status filters | Extend existing query |
| 0.7 | Expose **booking detail** endpoint | New endpoint |
| 0.8 | CORS headers for Android API calls (not needed if same-host, but needed for dev) | Middleware config |
| 0.9 | Add **rate limiting** on OTP endpoints for mobile clients | Security |
| 0.10 | Write OpenAPI / Postman collection for all barber endpoints | Dev reference |
| 0.11 | Build `GET /api/v1/barber/shop/{slug}/validate` — validates slug, returns shop name/timezone/logo | New endpoint for onboarding |

**Estimated effort**: 6–8 engineering days (backend)

---

### Phase 1 — Project Setup, Onboarding & Auth (Sprint 1–2)

| # | Task | Notes |
|---|------|-------|
| **1.0** | **Add localisation infrastructure: `strings.xml` for EN/HI/MR, `LanguageManager`, lint rule** | **Must be first task — blocks all UI work. See Architecture Section 17.** |
| 1.1 | Create Android project with recommended package structure | See Architecture doc |
| 1.2 | Configure Gradle dependencies (Retrofit, Hilt, Room, Compose, AppCompat, etc.) | See Architecture Section 3 |
| 1.3 | Implement `ApiClient` with Retrofit + OkHttp + Bearer token interceptor + `Accept-Language` header | |
| **1.4a** | **Build `LanguageSelectionScreen` (onboarding Step 1) — inline hardcoded labels** | **One-time, skipped on subsequent launches** |
| **1.4b** | **Build `ShopSetupScreen` (onboarding Step 2) — slug input + validation** | **Calls `GET /api/v1/barber/shop/{slug}/validate`** |
| **1.4c** | **Implement `ShopManager`, `OnboardingViewModel`, `OnboardingRepositoryImpl`** | |
| **1.4d** | **Update `AppNavGraph` with onboarding graph + 4-branch start destination logic** | **See Architecture Section 7** |
| 1.5 | Build Login screen — phone entry step, shows shop name from `ShopManager` | Matches web Step 1 |
| 1.6 | Build OTP verification screen — 6-digit input | Matches web Step 2 |
| 1.7 | Implement `AuthRepository`: request OTP, verify OTP, store token in EncryptedSharedPreferences | |
| 1.8 | Implement `AuthViewModel` with login state machine | |
| 1.9 | Token persistence + auto-login on app restart | |
| 1.10 | Token refresh flow (before expiry) | |
| 1.11 | Logout flow: call revoke API + clear token + clear shop slug (language preserved) | |

**Definition of Done**: Barber completes one-time onboarding (language → shop → login), token persists, auto-login works on restart, logout clears token + shop slug but preserves language.

---

### Phase 2 — Dashboard Screen (Sprint 3–4)

| # | Task | Notes |
|---|------|-------|
| 2.1 | Build Dashboard screen scaffold (TopAppBar, bottom nav / tabs) | |
| 2.2 | Implement date navigator component (back/forward arrows, date picker) | |
| 2.3 | Implement tab bar: Upcoming / Cancelled / No Show with count badges | |
| 2.4 | Build `AppointmentCard` composable (time, status badge, customer, service, amount) | |
| 2.5 | Implement appointment list with `LazyColumn` | |
| 2.6 | Pull-to-refresh with `SwipeRefresh` | |
| 2.7 | Implement `DashboardViewModel` + `BookingsRepository` | |
| 2.8 | Status badge colour mapping | Matches web colour scheme |
| 2.9 | "Current appointment" and "Missed start time" visual indicators | Timezone-aware |
| 2.10 | Empty state UI per tab | |

**Definition of Done**: Barber sees their day's appointments, can navigate dates, tabs filter correctly.

---

### Phase 3 — Booking Detail & Workflow Actions (Sprint 5–6)

| # | Task | Notes |
|---|------|-------|
| 3.1 | Build `BookingDetailBottomSheet` composable | Replaces web drawer |
| 3.2 | Customer info section (name, phone with click-to-call) | `Intent.ACTION_DIAL` |
| 3.3 | Payment summary section (total / paid / due / method) | |
| 3.4 | Status progress indicator (Confirmed → In Chair → Completed) | |
| 3.5 | Primary CTA button with context-sensitive label + enabled state | |
| 3.6 | Secondary actions: Mark No Show, Cancel & Refund | |
| 3.7 | Implement `WorkflowRepository.executeAction()` | |
| 3.8 | Implement `WorkflowViewModel` with action states (loading/success/error) | |
| 3.9 | Optimistic UI update on workflow success | |
| 3.10 | Error dialog for invalid transitions or network failures | |
| 3.11 | Confirmation dialog for destructive actions (No Show, Cancel) | |

**Definition of Done**: Barber can execute all workflow transitions from the app.

---

### Phase 4 — Profile Screen (Sprint 7)

| # | Task | Notes |
|---|------|-------|
| 4.1 | Build Profile screen (avatar, name, phone edit form) | |
| 4.2 | Avatar composable with barber initials + designation badge | |
| 4.3 | Form validation (name non-empty, phone E.164) | Client-side pre-validation |
| 4.4 | "Save Changes" button: disabled when unchanged, loading while saving | |
| 4.5 | Implement `ProfileRepository.updateProfile()` | |
| 4.6 | Success / error snackbar feedback | |
| **4.7** | **Add read-only Shop Association card to profile (name, @slug, timezone from `ShopManager`)** | **No API call needed — data from local cache** |
| **4.8** | **Add Language preference row to profile → navigates to `LanguageSelectionScreen` reused from onboarding** | **See Architecture Section 7** |

**Definition of Done**: Barber can update name and phone; profile shows associated shop (read-only); barber can change language preference from profile.

---

### Phase 5 — Polish & Production Readiness (Sprint 8)

| # | Task | Notes |
|---|------|-------|
| 5.1 | FCM push notifications for new bookings / status changes | Requires backend webhook |
| 5.2 | Offline detection + graceful degradation messaging | |
| 5.3 | Loading skeletons for all screens | |
| 5.4 | Deep link support (`clipper://barber/shop/{slug}/dashboard`) | |
| 5.5 | Crashlytics integration | Firebase |
| 5.6 | Analytics: screen views, workflow action events | Firebase Analytics |
| 5.7 | ProGuard / R8 rules for Retrofit + Hilt | |
| 5.8 | Accessibility: content descriptions, minimum touch targets (48dp) | |
| 5.9 | Night mode / dynamic colours | Material You |
| 5.10 | End-to-end test with real backend (staging) | |

---

## 3. Key Differences: Web vs Android

| Concern | Web (Current) | Android (Target) |
|---------|--------------|-----------------|
| Auth token transport | httpOnly cookie | `Authorization: Bearer` header |
| Token storage | Browser cookie | `EncryptedSharedPreferences` |
| Token refresh | Browser resends cookie automatically | Manual refresh interceptor in OkHttp |
| **Shop entry** | **Auto from URL slug in browser address bar** | **One-time onboarding screen → `ShopManager` (EncryptedPrefs)** |
| **Localisation** | **Browser/OS locale only, English UI** | **In-app language selection (EN/HI/MR), stored in `LanguageManager`, applied via `AppCompatDelegate`** |
| Navigation | Next.js router | Jetpack Navigation Component |
| UI framework | React + Tailwind CSS | Jetpack Compose + Material 3 |
| Bottom sheet | CSS transform animation | `ModalBottomSheet` (Compose) |
| Date formatting | `date-fns` in shop timezone | `java.time` with shop timezone |
| Pull-to-refresh | Custom gesture (72px threshold) | `PullToRefreshBox` composable |
| Phone call | Browser `tel:` link | `Intent.ACTION_DIAL` |
| State management | React `useState` / Server Components | `ViewModel` + `StateFlow` |
| Offline | Browser cache | Room DB cache (optional Phase 5) |
| OTP delivery | WhatsApp (Twilio) | Same — no Android change |

---

## 4. Backend Changes Required

### 4.1 Session Token — Cookie → Bearer Token

The current session system uses httpOnly cookies, which work fine for web but are awkward for native apps. Two options:

**Option A (Recommended)**: Add a `mobile_token` field to `barber_sessions` table alongside the existing cookie mechanism. Mobile clients pass `Authorization: Bearer <plain_token>` in headers; the API route hashes it and validates the same way.

**Option B**: Keep existing architecture unchanged. Mobile calls the same cookie-based endpoints and reads the `Set-Cookie` response header, storing the value manually. Works but is fragile.

→ **Choose Option A** to keep cookie and mobile auth paths clean and independent.

### 4.2 New Endpoint: Dashboard Summary
The web dashboard page fetches bookings via a server component. Android needs a REST endpoint that returns today's appointment count per status category (for header badges and stats widget).

### 4.3 Booking List Endpoint with Pagination
The current implementation fetches all bookings for a date in one query. Android should request paginated results for large barber schedules.

### 4.4 Token Refresh Endpoint
Web sessions expire after 8 hours and users are redirected to login. Mobile apps need a refresh flow to avoid interrupting the barber mid-shift.

### 4.5 Push Notification Webhook
For Phase 5 FCM support, the backend needs to store FCM device tokens per barber session and trigger push notifications on booking state changes.

### 4.6 Shop Slug Validation Endpoint
`GET /api/v1/barber/shop/{slug}/validate` — returns shop info for the onboarding screen; no authentication required. Must return `SHOP_NOT_FOUND` (404) if slug doesn't exist and `SHOP_INACTIVE` (403) if the shop is deactivated. Response must include `shopName`, `shopTimezone`, and optional `logoUrl`.

---

## 5. Standard API Response Envelope (Proposed)

All mobile endpoints should return:

```json
// Success
{
  "success": true,
  "data": { ... },
  "meta": {
    "timestamp": "2026-04-13T10:30:00Z",
    "timezone": "Asia/Kolkata"
  }
}

// Error
{
  "success": false,
  "error": {
    "code": "BARBER_NOT_FOUND",
    "message": "No barber found with this phone number",
    "hint": "Check the number is registered for this shop"
  }
}
```

### Standard Error Codes
| Code | HTTP | Meaning |
|------|------|---------|
| `INVALID_PHONE` | 400 | Phone format invalid |
| `BARBER_NOT_FOUND` | 404 | No barber matches phone + shop |
| `SHOP_NOT_FOUND` | 404 | Shop slug invalid |
| `SHOP_INACTIVE` | 403 | Shop exists but deactivated |
| `OTP_INVALID` | 400 | Wrong or expired OTP |
| `OTP_LOCKED` | 429 | Too many OTP attempts |
| `SESSION_EXPIRED` | 401 | Token past TTL |
| `SESSION_REVOKED` | 401 | Token was explicitly revoked |
| `BARBER_INACTIVE` | 403 | Barber account deactivated |
| `INVALID_TRANSITION` | 409 | State machine transition not allowed |
| `BOOKING_NOT_FOUND` | 404 | bookingId doesn't exist or not owned by barber |
| `PAYMENT_ALREADY_COLLECTED` | 409 | Duplicate collect_payment attempt |

---

## 6. Migration Risks & Mitigations

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|-----------|
| API breaking changes affecting web while mobile is being built | Medium | High | Version all mobile endpoints under `/api/v1/barber/` — keep existing web routes unchanged |
| Session token format change breaks existing web sessions | Low | High | Option A adds new column; existing cookie path unchanged |
| Timezone bugs in Android date handling | Medium | Medium | Mirror `getCurrentShopTime()` logic using `java.time.ZonedDateTime`; write unit tests |
| OTP delivery delay / failure on mobile network | Medium | Medium | Implement OTP resend button with 30s cooldown; backend has rate limiting |
| FCM token staleness | Medium | Low | Re-register FCM token on each login; backend overwrites stored token |
| Booking state conflicts (web + Android simultaneously open) | Low | Medium | Server enforces state machine; Android should refresh state after every action |
| **Translation completeness (Hindi/Marathi)** | **Medium** | **Medium** | **Provide translation template with all string keys to translators before Sprint 1; require all 3 locales complete and reviewed before first release** |
| **Locale switching UX regression** | **Low** | **Low** | **`AppCompatDelegate.setApplicationLocales()` handles activity recreation on API 33+; test manually on API 26–32 via emulator; add to integration test suite** |
| **Barber locked out if wrong slug entered in onboarding** | **Low** | **High** | **Onboarding has no "back" from shop step to avoid partial state; barber can clear app data; show support contact on `SHOP_INACTIVE` error** |

---

## 7. Timeline Summary

| Phase | Sprint | Effort | Dependencies |
|-------|--------|--------|-------------|
| Phase 0: Backend API Hardening | Pre-work | 6–8d (BE) | None |
| Phase 1: Setup, Onboarding & Auth | Sprint 1–2 | 11d (Android) | Phase 0 complete |
| Phase 2: Dashboard Screen | Sprint 3–4 | 8d (Android) | Phase 1 |
| Phase 3: Workflow Actions | Sprint 5–6 | 10d (Android) | Phase 2 |
| Phase 4: Profile Screen | Sprint 7 | 5d (Android) | Phase 1 |
| Phase 5: Polish + Production | Sprint 8 | 6d (Android) | Phases 2–4 |
| **Total** | **8 sprints** | **~46d** | |

2-week sprints → approximately **16–18 weeks** from Phase 0 kickoff to production.

> Phase 1 expanded by ~3 days (localisation infra + two onboarding screens).  
> Phase 4 expanded by ~1 day (shop association card + language picker in profile).  
> Backend Phase 0 expanded by ~1 day (shop slug validation endpoint).
