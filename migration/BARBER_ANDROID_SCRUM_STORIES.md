# Barber Android App — Technical Scrum User Stories

> Feature: Barber Dashboard Native Android App  
> Date: 2026-04-13  
> Last Updated: 2026-04-13 (added onboarding, shop profile, localisation)  
> Estimation: Story points (Fibonacci). 1 SP ≈ half a day of focused dev.

---

## Epic 0: Onboarding

> One-time flow on fresh install. Runs before auth. Skipped on subsequent launches unless app data is cleared.

---

### US-ONBOARD-01 · Language Selection

**As a** barber,  
**I want to** choose my preferred language when I first open the app,  
**so that** every screen — including login and the shop setup step — is shown in my language.

**Acceptance Criteria**
- [ ] Language selection screen shown on very first launch (before any other screen)
- [ ] Three options presented as tappable cards: **English**, **हिंदी (Hindi)**, **मराठी (Marathi)**
- [ ] Each card shows the language name in both that language and English (e.g., "हिंदी · Hindi")
- [ ] Selected language persisted in `LanguageManager` (regular SharedPreferences — not encrypted)
- [ ] `AppCompatDelegate.setApplicationLocales()` applied immediately on selection
- [ ] On subsequent launches: this screen is skipped entirely if language is already stored
- [ ] Language choice survives logout — it is a **device preference, not a session preference**
- [ ] Language can also be changed later from the Profile screen (see US-PROFILE-03)

**Technical Notes**
- `LanguageManager.save(code)` where code ∈ `{ "en", "hi", "mr" }`
- `AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(code))` works back to API 21 via AppCompat
- This screen must render **before** any `stringResource()` call — use hardcoded strings only on this one screen (or embed all three labels inline)
- Navigation: on language selected → check if shop slug stored → `onboarding_shop` or `auth`

**Story Points: 3**

---

### US-ONBOARD-02 · Shop Setup (Slug Entry & Validation)

**As a** barber,  
**I want to** enter my shop's identifier once,  
**so that** the app permanently knows which shop I belong to without me entering it every login.

**Acceptance Criteria**
- [ ] Screen shown after language selection (or directly on launch if language is set but slug is not)
- [ ] Single text input: "Enter your shop ID" (e.g., `cuts-by-raj`)
- [ ] Input auto-lowercases and strips spaces
- [ ] "Continue" button calls `GET /api/v1/barber/shop/{slug}/validate`
- [ ] On success: shop name shown as confirmation ("Found: **Cuts by Raj**") + "Looks good" button navigates to Login
- [ ] On `SHOP_NOT_FOUND`: inline error "No shop found with this ID. Check with your manager."
- [ ] On `SHOP_INACTIVE`: inline error "This shop is currently inactive. Contact support."
- [ ] On network error: show retry option
- [ ] Shop info (name, slug, timezone, logoUrl) saved to `ShopManager` on success
- [ ] On subsequent launches: this screen is **skipped** if slug already stored in `ShopManager`
- [ ] No way to change shop slug from this screen — barber must clear app data (this is intentional)

**Technical Notes**
- Endpoint: `GET /api/v1/barber/shop/{slug}/validate` (no auth required)
- `ShopManager.save(shopInfo)` — persists to EncryptedSharedPreferences
- After this screen, `shopSlug` is always available silently for all subsequent API calls
- The shop logo (if returned) can be shown in the Login screen header

**Story Points: 5**

---

## Epic 1: Authentication

---

### US-AUTH-01 · Request OTP

**As a** barber,  
**I want to** enter my phone number and receive a WhatsApp OTP,  
**so that** I can authenticate without a password.

**Acceptance Criteria**
- [ ] Phone input field accepts 10-digit Indian numbers and E.164 format
- [ ] Client-side validation rejects non-numeric / too-short input before API call
- [ ] "Send OTP" button is disabled while request is in-flight
- [ ] On success: navigate to OTP entry screen, showing masked phone (e.g. `+91 98xxx x3210`)
- [ ] On `BARBER_NOT_FOUND`: show "This number is not registered with this shop"
- [ ] On `INVALID_PHONE`: show inline field error "Enter a valid 10-digit number"
- [ ] On network error: show retry option
- [ ] Loading indicator during API call

**Technical Notes**
- Endpoint: `POST /api/v1/barber/auth/request-otp`
- Request: `{ phone, shopSlug }`
- `shopSlug` is read silently from `ShopManager.getSlug()` — **never entered by the user on this screen** (set during onboarding, see US-ONBOARD-02)
- Store masked phone in `AuthViewModel` for display on next screen

**Story Points: 3**

---

### US-AUTH-02 · Verify OTP

**As a** barber,  
**I want to** enter the 6-digit OTP I received,  
**so that** I get a session token and access the dashboard.

**Acceptance Criteria**
- [ ] 6-cell OTP input with auto-advance and auto-submit on 6th digit
- [ ] Backspace navigates to previous cell
- [ ] "Verify" button disabled until all 6 digits entered
- [ ] On success: store Bearer token in `EncryptedSharedPreferences`, navigate to Dashboard
- [ ] On `OTP_INVALID`: shake animation + error "Incorrect OTP. X attempts remaining"
- [ ] On `OTP_LOCKED`: disable input, show "Too many attempts. Request a new OTP"
- [ ] Resend OTP link visible after 30-second countdown
- [ ] Keyboard auto-shows on screen open

**Technical Notes**
- Endpoint: `POST /api/v1/barber/auth/verify-otp`
- Response contains `token` (plain) and `expiresAt` (ISO timestamp)
- Store `token`, `expiresAt`, `barberId`, `shopSlug` in `EncryptedSharedPreferences`
- Token passed as `Authorization: Bearer <token>` on all subsequent requests

**Story Points: 5**

---

### US-AUTH-03 · Auto-Login on App Restart

**As a** barber,  
**I want to** stay logged in when I reopen the app,  
**so that** I don't have to authenticate multiple times per shift.

**Acceptance Criteria**
- [ ] App reads stored token on launch
- [ ] If token exists and is not expired: navigate directly to Dashboard (skip login)
- [ ] If token is expired or missing: navigate to Login screen
- [ ] Splash screen shown during token validation (no white flash)
- [ ] If backend returns `SESSION_EXPIRED` or `SESSION_REVOKED` on any API call: clear token + redirect to Login

**Technical Notes**
- Check `expiresAt` locally before hitting network (early bail-out)
- Validate token with `GET /api/v1/barber/auth/me` on launch for freshness
- Handle race: token valid locally but revoked server-side

**Story Points: 3**

---

### US-AUTH-04 · Token Refresh

**As a** barber,  
**I want** my session to be automatically renewed before expiry,  
**so that** I am not logged out mid-shift.

**Acceptance Criteria**
- [ ] OkHttp authenticator: if API returns 401, attempt one refresh before failing
- [ ] Refresh endpoint called with existing token → returns new token + new `expiresAt`
- [ ] New token stored, original request retried transparently
- [ ] If refresh fails (token revoked, barber inactive): navigate to Login
- [ ] No duplicate refresh calls in-flight (single-flight pattern)

**Technical Notes**
- Endpoint: `POST /api/v1/barber/auth/refresh`
- Request: `{ token }` (or via `Authorization` header)
- Implement as OkHttp `Authenticator`, not a manual timer
- Use `Mutex` in `TokenManager` to prevent concurrent refreshes

**Story Points: 5**

---

### US-AUTH-05 · Logout

**As a** barber,  
**I want to** log out explicitly,  
**so that** my session is revoked and no one else can use my account.

**Acceptance Criteria**
- [ ] Logout button in dashboard header / profile screen
- [ ] Confirmation dialog before logout ("Are you sure?")
- [ ] Calls revoke API (fire-and-forget — proceed even if network is offline)
- [ ] Clears token from `EncryptedSharedPreferences` (`TokenManager.clearToken()`)
- [ ] Clears shop session/context from `ShopManager`, but preserves the persisted shop slug needed for the next login
- [ ] **Does NOT clear language** — `LanguageManager` is untouched on logout
- [ ] Navigates to Login screen (not onboarding), back stack cleared (no back-navigation to dashboard)
- [ ] On next launch: onboarding skipped because language + shop slug are still stored; app goes straight to Login

**Technical Notes**
- Endpoint: `POST /api/v1/barber/auth/logout`
- Header: `Authorization: Bearer <token>`
- Back stack cleared with `NavOptions.popUpTo(graph) { inclusive = true }`
- Navigate to `auth` graph (not `onboarding`) — shop slug is preserved for next login

**Story Points: 2**

---

### US-AUTH-06 · Login Screen Shows Shop Context

**As a** barber,  
**I want** the login screen to show which shop I am logging into,  
**so that** I have confidence I am authenticating to the correct shop.

**Acceptance Criteria**
- [ ] Login screen header shows shop name (from `ShopManager.getName()`) — no API call needed
- [ ] Shop logo shown if available (from `ShopManager.getLogoUrl()`)
- [ ] If for any reason `ShopManager` has no slug (edge case: data partially cleared), redirect to `onboarding_shop`
- [ ] `shopSlug` is passed to OTP request silently from `ShopManager` — barber never types it on login

**Technical Notes**
- All shop context data comes from `ShopManager` (populated during onboarding, see US-ONBOARD-02)
- No additional API call needed on the login screen
- This replaces the previous deep-link-based shop resolution approach

**Story Points: 2**

---

## Epic 2: Dashboard

---

### US-DASH-01 · View Today's Appointments

**As a** barber,  
**I want to** see all my appointments for today,  
**so that** I can plan and manage my workload.

**Acceptance Criteria**
- [ ] Default view shows current date's appointments
- [ ] Appointments sorted by `start_time` ascending
- [ ] "Upcoming" tab active by default
- [ ] Each card shows: time range, status badge, customer name, service name + duration, total amount
- [ ] Empty state: "No appointments for this day" with illustration
- [ ] Loading skeleton shown on first load

**Technical Notes**
- Endpoint: `GET /api/v1/barber/dashboard/bookings?date=YYYY-MM-DD&status=upcoming`
- Date format: ISO date in shop's timezone (backend converts to UTC range)
- Use `LazyColumn` with `items(bookings, key = { it.id })`

**Story Points: 5**

---

### US-DASH-02 · Date Navigation

**As a** barber,  
**I want to** navigate to past and future dates,  
**so that** I can check upcoming appointments or review past ones.

**Acceptance Criteria**
- [ ] Back arrow (←) navigates to previous day
- [ ] Forward arrow (→) navigates to next day
- [ ] Current date shown in centre (`Mon, 13 Apr 2026`)
- [ ] "Today" chip shortcut: jumps to current date and highlights if not already on today
- [ ] Tap on date label: opens `DatePickerDialog`
- [ ] List reloads when date changes

**Technical Notes**
- Date state in `DashboardViewModel`, triggers `bookingsFlow` collection on change
- Use `LocalDate` (java.time) for manipulation, convert to UTC range for API query

**Story Points: 3**

---

### US-DASH-03 · Status Filter Tabs

**As a** barber,  
**I want to** filter appointments by status (Upcoming / Cancelled / No Show),  
**so that** I can focus on actionable items.

**Acceptance Criteria**
- [ ] Three tabs: **Upcoming** (pending_payment + confirmed + seated + completed), **Cancelled**, **No Show**
- [ ] Each tab shows a count badge
- [ ] Tab switch does not make a new API call — filters locally from fetched list
- [ ] Counts update after workflow actions (e.g., marking no-show moves card to No Show tab)
- [ ] Selected tab persists during date navigation

**Technical Notes**
- Fetch all bookings for the day in one call
- Filter in `DashboardViewModel.filteredBookings` using a `combine` on `bookings` + `selectedTab`

**Story Points: 3**

---

### US-DASH-04 · Current Appointment Indicator

**As a** barber,  
**I want** my current appointment highlighted,  
**so that** I can immediately see who I should be serving right now.

**Acceptance Criteria**
- [ ] Appointment whose `start_time ≤ now < end_time` shown with emerald/green border + ring
- [ ] Appointment whose `start_time` is in the past and status is still `confirmed`: shown with red border + "Missed start time" label
- [ ] Indicators update in real-time (or on pull-to-refresh)
- [ ] Indicators respect shop timezone (not device timezone)

**Technical Notes**
- Pass `shopTimezone` from session/shop info
- Use `java.time.ZonedDateTime.now(ZoneId.of(shopTimezone))` for comparisons
- Consider 1-minute `tickerFlow` in ViewModel to update current appointment highlighting

**Story Points: 3**

---

### US-DASH-05 · Pull-to-Refresh

**As a** barber,  
**I want to** pull down to refresh the appointment list,  
**so that** I can see newly booked or updated appointments.

**Acceptance Criteria**
- [ ] Swipe-down gesture triggers refresh
- [ ] Loading indicator shown during refresh
- [ ] List updates with fresh data
- [ ] Error toast shown if refresh fails (network error)

**Technical Notes**
- Use `PullToRefreshBox` (Material3 experimental) or `accompanist-swiperefresh`
- Trigger `viewModel.refresh()` which calls repository bypassing cache

**Story Points: 2**

---

## Epic 3: Booking Detail & Workflow

---

### US-WORKFLOW-01 · View Booking Details

**As a** barber,  
**I want to** tap a booking card and see full details,  
**so that** I know who I'm serving and how much they owe.

**Acceptance Criteria**
- [ ] Tap on card opens a bottom sheet
- [ ] Shows: date + time range, customer name, customer phone (tappable)
- [ ] Shows: service name + duration
- [ ] Shows: total amount, paid amount, due amount, payment method (Cash / Online)
- [ ] Shows: status progress bar (Confirmed → In Chair → Completed) — current step highlighted
- [ ] Bottom sheet dismissible by swipe-down or backdrop tap

**Technical Notes**
- Data already loaded in dashboard list — no extra API call for detail view
- Bottom sheet: `ModalBottomSheet` with `SheetState`
- Amount display: convert paise to `₹X.XX` format

**Story Points: 5**

---

### US-WORKFLOW-02 · Start Service

**As a** barber,  
**I want to** mark a confirmed appointment as started,  
**so that** the system knows the customer is in the chair.

**Acceptance Criteria**
- [ ] "Start Service" button shown when status is `confirmed` or `pending_payment`
- [ ] Loading state while API call is in-flight (button disabled + spinner)
- [ ] On success: booking card updates to "In Chair" status (amber badge)
- [ ] On `INVALID_TRANSITION`: show error snackbar "Cannot start this appointment"
- [ ] On network error: show retry option

**Technical Notes**
- Endpoint: `POST /api/v1/barber/dashboard/bookings/{bookingId}/workflow`
- Body: `{ "action": "start" }`
- Update `DashboardViewModel` state with returned `WorkflowResponse`

**Story Points: 3**

---

### US-WORKFLOW-03 · Complete Service

**As a** barber,  
**I want to** mark a service as completed,  
**so that** the customer can be checked out.

**Acceptance Criteria**
- [ ] "Complete Service" button shown when status is `seated`
- [ ] On success: booking updates to "Completed" status (green badge)
- [ ] If `due > 0`: primary CTA changes to "Collect ₹X.XX"

**Technical Notes**
- Same workflow endpoint with `{ "action": "complete" }`
- Response includes updated `payment_status` and `paid_amount`

**Story Points: 2**

---

### US-WORKFLOW-04 · Collect Cash Payment

**As a** barber,  
**I want to** record a cash payment from the customer,  
**so that** the booking is marked as fully paid.

**Acceptance Criteria**
- [ ] "Collect ₹X.XX" button shown when status is `completed` and `payment_status` is `pending` or `partial`
- [ ] Confirmation dialog: "Confirm collection of ₹X.XX cash from [customer name]?"
- [ ] On success: due amount becomes ₹0, payment method shows "Cash", button changes to "Done"
- [ ] Cannot collect payment twice (button disabled after first collection)

**Technical Notes**
- Endpoint: `POST /api/v1/barber/dashboard/bookings/{bookingId}/workflow`
- Body: `{ "action": "collect_payment" }`
- Response returns `payment_method: "Cash"` and updated `paid_amount`

**Story Points: 3**

---

### US-WORKFLOW-05 · Mark No Show

**As a** barber,  
**I want to** mark an appointment as no-show,  
**so that** the slot is recorded as unserved.

**Acceptance Criteria**
- [ ] "Mark No Show" secondary action available for non-finalized appointments
- [ ] Confirmation dialog: "Mark [customer name] as no-show for [time]?"
- [ ] On success: booking moves to No Show tab, card shows "No Show" slate badge
- [ ] Action not available for already-completed or already-cancelled bookings

**Technical Notes**
- Body: `{ "action": "mark_no_show" }`
- After success: `DashboardViewModel` moves booking to noShow list

**Story Points: 3**

---

### US-WORKFLOW-06 · Cancel & Refund

**As a** barber,  
**I want to** cancel a confirmed appointment and trigger a refund,  
**so that** the customer gets their money back.

**Acceptance Criteria**
- [ ] "Cancel & Refund" secondary action available for `confirmed` appointments only
- [ ] Warning dialog: "This will cancel the booking and initiate a refund. Continue?"
- [ ] Loading state while refund is being processed (may take a few seconds)
- [ ] On success: booking moves to Cancelled tab, shows refund status message
- [ ] On failure: show error with refund failure reason

**Technical Notes**
- Body: `{ "action": "cancel_refund" }`
- Refund is processed asynchronously by the backend (Razorpay + QStash)
- Show "Refund initiated" success message (not "Refund completed")

**Story Points: 5**

---

### US-WORKFLOW-07 · Call Customer

**As a** barber,  
**I want to** call the customer directly from the booking detail,  
**so that** I can confirm their arrival without switching apps.

**Acceptance Criteria**
- [ ] Customer phone number shown as a tappable element in booking detail
- [ ] Tap opens Android phone dialler with number pre-filled
- [ ] Works even if app does not have CALL_PHONE permission (uses dialler intent)

**Technical Notes**
- `Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))`
- No permission required for ACTION_DIAL (user confirms the call)

**Story Points: 1**

---

## Epic 4: Profile Management

---

### US-PROFILE-01 · View Profile

**As a** barber,  
**I want to** see my profile information including my associated shop,  
**so that** I know what name, phone number, and shop are on record.

**Acceptance Criteria**
- [ ] Profile screen accessible from dashboard header icon
- [ ] Shows avatar with barber initials + designation badge
- [ ] Shows current name and phone number (read-only by default)
- [ ] "Edit" button to enter edit mode for name/phone (see US-PROFILE-02)
- [ ] **Shop Association section** (always read-only):
  - [ ] Section label: "Shop" (localised)
  - [ ] Shop name displayed (e.g., "Cuts by Raj")
  - [ ] Shop slug displayed as `@cuts-by-raj`
  - [ ] Shop timezone displayed (e.g., "Asia/Kolkata")
  - [ ] No edit button on this section — intentionally read-only
- [ ] **Language preference row** showing current language with a chevron → navigates to US-PROFILE-03

**Technical Notes**
- Barber info (name, phone, designation) from `GET /api/v1/barber/auth/me` or login cache
- Shop info (name, slug, timezone) from `ShopManager` — **no additional API call**
- Designation from `barbers.designation` field

**Story Points: 3**

---

### US-PROFILE-02 · Edit Name and Phone

**As a** barber,  
**I want to** update my name and phone number,  
**so that** my profile stays accurate.

**Acceptance Criteria**
- [ ] Name field: non-empty, max 100 chars
- [ ] Phone field: validated as 10-digit or E.164 format
- [ ] "Save Changes" disabled when no changes detected (compared to original values)
- [ ] "Save Changes" disabled while API call is in-flight
- [ ] On success: profile values updated, success snackbar shown, edit mode exits
- [ ] On validation error from backend: inline field error shown
- [ ] "Discard Changes" option exits edit mode without saving

**Technical Notes**
- Endpoint: `POST /api/v1/barber/auth/update-profile`
- Body: `{ name, phone }`
- Backend normalizes phone to E.164 before storing

**Story Points: 3**

---

### US-PROFILE-03 · Change Language

**As a** barber,  
**I want to** change the app language from my profile,  
**so that** I can switch to my preferred language at any time after onboarding.

**Acceptance Criteria**
- [ ] Language picker accessible from Profile screen (see US-PROFILE-01)
- [ ] Shows same three options as onboarding: English, हिंदी, मराठी
- [ ] Current language shown with a checkmark
- [ ] On selection: language saved via `LanguageManager.save(code)` and applied immediately via `AppCompatDelegate.setApplicationLocales()`
- [ ] UI updates to selected language without requiring app restart
- [ ] Back navigation returns to updated Profile screen (now in new language)

**Technical Notes**
- No API call — purely client-side preference
- `AppCompatDelegate.setApplicationLocales()` triggers `onConfigurationChanged` → Compose recomposes with new strings
- Language code stored: `"en"`, `"hi"`, `"mr"`

**Story Points: 2**

---

## Epic 5: Infrastructure & Non-Functional

---

### US-INFRA-01 · API Error Handling & Retry

**As a** developer,  
**I want** a centralised error-handling layer in the network client,  
**so that** every screen handles errors consistently without duplicate code.

**Acceptance Criteria**
- [ ] All API calls return `Result<T>` (sealed class: `Success`, `ApiError`, `NetworkError`)
- [ ] `ApiError` includes `code` (machine-readable), `message`, `httpStatus`
- [ ] `NetworkError` wraps `IOException` for offline / timeout cases
- [ ] 401 responses trigger token refresh before propagating error
- [ ] ViewModels expose `UiState` sealed class (Loading / Success / Error)

**Story Points: 5**

---

### US-INFRA-02 · Timezone-Aware Date Formatting

**As a** barber,  
**I want** all times shown in the shop's timezone,  
**so that** appointment times match what the customer sees.

**Acceptance Criteria**
- [ ] Shop timezone (e.g., `Asia/Kolkata`) stored locally from session/login response
- [ ] All time formatting uses `ZonedDateTime` with shop timezone
- [ ] Device timezone has no effect on displayed times
- [ ] Time format: `hh:mm a` (e.g., `10:30 AM`)
- [ ] Date format: `EEE, dd MMM yyyy` (e.g., `Mon, 13 Apr 2026`)
- [ ] Unit tests covering timezone edge cases (DST, UTC+5:30)

**Story Points: 3**

---

### US-INFRA-03 · Dependency Injection Setup

**As a** developer,  
**I want** all dependencies injected via Hilt,  
**so that** the codebase is testable and loosely coupled.

**Acceptance Criteria**
- [ ] `NetworkModule`: provides `OkHttpClient`, `Retrofit`, `ApiService`
- [ ] `AuthModule`: provides `TokenManager`, `AuthRepository`
- [ ] `BookingModule`: provides `BookingsRepository`, `WorkflowRepository`
- [ ] All ViewModels use `@HiltViewModel`
- [ ] Unit tests can inject fake repositories

**Story Points: 3**

---

### US-INFRA-04 · Navigation Graph

**As a** developer,  
**I want** a Jetpack Navigation graph,  
**so that** screen transitions are predictable and back-stack management is correct.

**Acceptance Criteria**
- [ ] Four graphs: `onboarding`, `auth`, `main`
- [ ] App launch computes `startDestination` from stored state:
  - No language stored → `onboarding_language`
  - Language set, no shop slug → `onboarding_shop`
  - Slug set, no valid token → `auth` (Login)
  - All stored + valid token → `main` (Dashboard)
- [ ] On successful onboarding completion: navigate to `auth`, pop `onboarding` from back stack
- [ ] On successful auth: navigate to `main`, pop `auth` from back stack
- [ ] On logout: navigate to `auth` (not onboarding — shop slug is preserved), pop `main`
- [ ] On session expired (mid-use): `SessionEventBus` fires → navigate to `auth`, pop `main`
- [ ] Transition animations: fade for graph switches, slide for within-graph

**Story Points: 3**

---

### US-INFRA-05 · FCM Push Notifications

**As a** barber,  
**I want to** receive a push notification when a new booking is made for me,  
**so that** I am aware of new customers without refreshing the app.

**Acceptance Criteria**
- [ ] FCM token registered on login and stored on backend
- [ ] Notification received when a new booking is created for this barber
- [ ] Notification received when an existing booking is cancelled
- [ ] Tapping notification opens app to the relevant booking detail
- [ ] FCM token refreshed if it changes (via `FirebaseMessagingService.onNewToken`)

**Technical Notes**
- Backend work required: store FCM token per barber session, send notification on booking events
- Android: `FirebaseMessagingService`, `NotificationManager`, deep-link from notification

**Story Points: 8** (includes backend coordination)

---

### US-INFRA-06 · Localisation Infrastructure

**As a** developer,  
**I want** all UI strings externalised into locale-specific resource files from Day 1,  
**so that** English, Hindi, and Marathi are fully supported without retrofitting later.

**Acceptance Criteria**
- [ ] `res/values/strings.xml` — English (canonical baseline, all string keys defined here)
- [ ] `res/values-hi/strings.xml` — Hindi (complete translation, no fallback to English)
- [ ] `res/values-mr/strings.xml` — Marathi (complete translation, no fallback to English)
- [ ] **Zero hardcoded UI strings** anywhere in the codebase — enforced via lint rule
- [ ] String key naming convention: `screen_element_description` (e.g., `login_phone_hint`, `dashboard_tab_upcoming`, `profile_shop_section_title`)
- [ ] `LanguageManager` class with `save(code)` / `get()` using regular SharedPreferences
- [ ] `AppCompatDelegate.setApplicationLocales()` used for locale switching (API 21+ compatible)
- [ ] Unit test: `LanguageManagerTest` — save/retrieve/default language
- [ ] All 3 locale files present and complete before first release (no partial translations)

**Technical Notes**
- String resource folders: `res/values/` (en), `res/values-hi/` (hi), `res/values-mr/` (mr)
- `LanguageManager` uses regular (not encrypted) `SharedPreferences` — language is not sensitive data
- `LanguageManager` key: `"language"`, values: `"en"` | `"hi"` | `"mr"`, default: `"en"`
- Do NOT use locale auto-detection from device — always use explicitly stored preference
- The onboarding language selection screen (US-ONBOARD-01) is the only exception — it uses inline hardcoded labels so it renders before any locale is applied

**Story Points: 8**

---

## Story Summary

| Epic | Stories | Total SP |
|------|---------|---------|
| Epic 0: Onboarding | 2 stories | 8 SP |
| Epic 1: Authentication | 6 stories | 20 SP |
| Epic 2: Dashboard | 5 stories | 16 SP |
| Epic 3: Workflow | 7 stories | 22 SP |
| Epic 4: Profile | 3 stories | 8 SP |
| Epic 5: Infrastructure | 6 stories | 30 SP |
| **Total** | **29 stories** | **104 SP** |

At 10 SP per 2-week sprint → **~11 sprints / 22 weeks** from kickoff to production-ready app.
