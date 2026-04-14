# Barber Android App — API Reference

> **Base prefix**: `/api/v1/barber/`  
> **Auth**: `Authorization: Bearer <token>` (all endpoints except Section 0 and 1.1–1.2)  
> **Content-Type**: `application/json`  
> **Status**: All endpoints implemented — `feature/barber-dashboard` branch

---

## Standard Response Envelope

### Success
```json
{
  "success": true,
  "data": { ... },
  "meta": {
    "timestamp": "2026-04-13T10:30:00Z",
    "timezone": "Asia/Kolkata"
  }
}
```
> `timezone` is only present in responses where date/time context matters (e.g., bookings list).

### Error
```json
{
  "success": false,
  "error": {
    "code": "MACHINE_READABLE_CODE",
    "message": "Human readable message",
    "hint": "Optional additional context"
  }
}
```

---

## 0. Onboarding API

### 0.1 Validate Shop Slug
**`GET /api/v1/barber/shop/{slug}/validate`**  
No authentication required.

Called once during onboarding before login. Android caches the response in `ShopManager` (EncryptedSharedPreferences).

**Path Parameter**: `slug` — shop identifier (e.g., `cuts-by-raj`)

**Response 200**
```json
{
  "success": true,
  "data": {
    "shopSlug": "cuts-by-raj",
    "shopName": "Cuts by Raj",
    "shopTimezone": "Asia/Kolkata",
    "logoUrl": "https://cdn.example.com/logo.png"
  },
  "meta": {
    "timestamp": "2026-04-13T10:30:00Z"
  }
}
```
> `logoUrl` is nullable (`null` if no logo configured).

**Error Codes**
| Code | HTTP | Condition |
|------|------|-----------|
| `SHOP_NOT_FOUND` | 404 | No shop matches this slug |
| `SHOP_INACTIVE` | 403 | Shop exists but subscription access is inactive/expired |
| `INTERNAL_ERROR` | 500/503 | Database error during lookup, or subscription verification service unavailable |

---

## 1. Authentication APIs

### 1.1 Request OTP
**`POST /api/v1/barber/auth/request-otp`**  
No authentication required.

**Request Body**
```json
{
  "phone": "9876543210",
  "shopSlug": "cuts-by-raj"
}
```

| Field | Type | Constraints |
|-------|------|-------------|
| `phone` | string | 8–20 chars; E.164, 10-digit, or 91-prefix formats accepted |
| `shopSlug` | string | 1–100 chars |

**Response 200**
```json
{
  "success": true,
  "data": {
    "maskedPhone": "+91 98xxx x3210",
    "otpExpiresInSeconds": 300
  },
  "meta": {
    "timestamp": "2026-04-13T10:30:00Z"
  }
}
```

**Error Codes**
| Code | HTTP | Condition |
|------|------|-----------|
| `VALIDATION_ERROR` | 400 | Missing or malformed fields |
| `INVALID_PHONE` | 400 | Phone string cannot be parsed to E.164 |
| `SHOP_NOT_FOUND` | 404 | `shopSlug` not found |
| `SHOP_INACTIVE` | 403 | Shop exists but subscription access is inactive/expired |
| `BARBER_NOT_FOUND` | 404 | Phone not registered in this shop |
| `OTP_SEND_FAILED` | 502 | WhatsApp OTP delivery failed |
| `INTERNAL_ERROR` | 500/503 | Database error, or subscription verification service unavailable |

---

### 1.2 Verify OTP
**`POST /api/v1/barber/auth/verify-otp`**  
No authentication required.

**Request Body**
```json
{
  "phone": "9876543210",
  "code": "123456",
  "shopSlug": "cuts-by-raj"
}
```

| Field | Type | Constraints |
|-------|------|-------------|
| `phone` | string | 8–20 chars |
| `code` | string | 4–10 chars |
| `shopSlug` | string | 1–100 chars |

**Response 200**
```json
{
  "success": true,
  "data": {
    "token": "a3f1c8...64hexchars",
    "expiresAt": "2026-04-13T18:30:00Z",
    "barber": {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "name": "Raj Kumar",
      "phone": "+919876543210",
      "designation": "Senior Barber",
      "shopId": "550e8400-e29b-41d4-a716-446655440001",
      "shopName": "Cuts by Raj",
      "shopSlug": "cuts-by-raj",
      "shopTimezone": "Asia/Kolkata",
      "logoUrl": null,
      "brandColor": "#3b82f6"
    }
  },
  "meta": {
    "timestamp": "2026-04-13T10:30:00Z"
  }
}
```

> `designation`, `logoUrl`, and `brandColor` are nullable.  
> Token is a 64-char hex string. The backend stores only its SHA-256 hash — the plain token is never stored.  
> Cache the entire `barber` object in `ShopManager` / `TokenManager` — no extra call needed for profile screen.

**Error Codes**
| Code | HTTP | Condition |
|------|------|-----------|
| `VALIDATION_ERROR` | 400 | Missing or malformed fields |
| `INVALID_PHONE` | 400 | Phone string cannot be parsed to E.164 |
| `OTP_EXPIRED` | 400 | OTP past 5-minute TTL or not found |
| `OTP_INVALID` | 400 | Wrong code |
| `OTP_LOCKED` | 429 | Too many incorrect attempts |
| `OTP_VERIFICATION_FAILED` | 500 | Internal verification error |
| `SHOP_NOT_FOUND` | 404 | Shop not found for given slug |
| `SHOP_INACTIVE` | 403 | Shop exists but subscription access is inactive/expired |
| `BARBER_NOT_FOUND` | 404 | Barber lookup failed after OTP success |
| `INTERNAL_ERROR` | 503 | Subscription verification service unavailable |

---

### 1.3 Refresh Token
**`POST /api/v1/barber/auth/refresh`**  
`Authorization: Bearer <token>`

Creates a new 8-hour session and revokes the current one. Call before session expiry (recommended: when < 1 hour remains).

**No request body required.**

**Response 200**
```json
{
  "success": true,
  "data": {
    "token": "b7e2d1...64hexchars",
    "expiresAt": "2026-04-14T02:30:00Z"
  },
  "meta": {
    "timestamp": "2026-04-13T18:00:00Z"
  }
}
```

**Error Codes**
| Code | HTTP | Condition |
|------|------|-----------|
| `UNAUTHORIZED` | 401 | Token invalid, expired, or revoked |
| `SHOP_INACTIVE` | 403 | Shop exists but subscription access is inactive/expired |
| `INTERNAL_ERROR` | 503 | Subscription verification service unavailable |

> Old token is revoked asynchronously after new token is created — the swap is safe to retry.

---

### 1.4 Logout
**`POST /api/v1/barber/auth/logout`**  
`Authorization: Bearer <token>`

Revokes the session server-side. Android must also clear the locally stored token regardless of response status.

Android logout contract: clear auth/session token and shop onboarding context (`shopSlug`/shop metadata), while keeping language preference.

**No request body required.**

**Response 200**
```json
{
  "success": true,
  "data": {
    "message": "Logged out successfully"
  },
  "meta": {
    "timestamp": "2026-04-13T10:30:00Z"
  }
}
```

**Error Codes**
| Code | HTTP | Condition |
|------|------|-----------|
| `UNAUTHORIZED` | 401 | No valid Bearer token in header |
| `SHOP_INACTIVE` | 403 | Shop exists but subscription access is inactive/expired |
| `INTERNAL_ERROR` | 503 | Subscription verification service unavailable |

---

### 1.5 Get Current Barber (Session Validation)
**`GET /api/v1/barber/auth/me`**  
`Authorization: Bearer <token>`

Call on app launch to confirm the stored token is still valid and refresh cached barber/shop data.

**Response 200**
```json
{
  "success": true,
  "data": {
    "barber": {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "name": "Raj Kumar",
      "phone": "+919876543210",
      "designation": "Senior Barber",
      "shopId": "550e8400-e29b-41d4-a716-446655440001",
      "shopName": "Cuts by Raj",
      "shopSlug": "cuts-by-raj",
      "shopTimezone": "Asia/Kolkata",
      "logoUrl": null,
      "brandColor": "#3b82f6"
    },
    "session": {
      "expiresAt": "2026-04-13T18:30:00Z"
    }
  },
  "meta": {
    "timestamp": "2026-04-13T10:30:00Z"
  }
}
```

> Use `session.expiresAt` to schedule proactive token refresh.  
> Update `ShopManager` with latest `shopName`/`shopTimezone`/`logoUrl` on every app launch in case shop config changed.

**Error Codes**
| Code | HTTP | Condition |
|------|------|-----------|
| `UNAUTHORIZED` | 401 | Token invalid, expired, or revoked |
| `SHOP_INACTIVE` | 403 | Shop exists but subscription access is inactive/expired |
| `INTERNAL_ERROR` | 503 | Subscription verification service unavailable |
| `BARBER_NOT_FOUND` | 404 | Barber record deleted after session was created |
| `SHOP_NOT_FOUND` | 404 | Shop record deleted after session was created |

---

### 1.6 Update Profile
**`POST /api/v1/barber/auth/update-profile`**  
`Authorization: Bearer <token>`

**Request Body**
```json
{
  "name": "Raj Kumar",
  "phone": "9876543210"
}
```

| Field | Type | Constraints |
|-------|------|-------------|
| `name` | string | 1–100 chars |
| `phone` | string | 8–20 chars |

**Response 200**
```json
{
  "success": true,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "Raj Kumar",
    "phone": "+919876543210",
    "designation": "Senior Barber"
  },
  "meta": {
    "timestamp": "2026-04-13T10:30:00Z"
  }
}
```

> Phone is returned normalized to E.164 format. Update local cache with the returned values.

**Error Codes**
| Code | HTTP | Condition |
|------|------|-----------|
| `UNAUTHORIZED` | 401 | Token invalid or expired |
| `SHOP_INACTIVE` | 403 | Shop exists but subscription access is inactive/expired |
| `INTERNAL_ERROR` | 503 | Subscription verification service unavailable |
| `VALIDATION_ERROR` | 400 | Missing or invalid fields |
| `INVALID_PHONE` | 400 | Phone string cannot be parsed to E.164 |
| `UPDATE_FAILED` | 500 | Database update error |

---

## 2. Dashboard APIs

### 2.1 Get Bookings for Date
**`GET /api/v1/barber/dashboard/bookings?date=YYYY-MM-DD`**  
`Authorization: Bearer <token>`

Fetch all bookings assigned to the authenticated barber for a given date.

**Query Parameters**
| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `date` | string | No | `YYYY-MM-DD` in shop timezone. Defaults to today. |

**Response 200**
```json
{
  "success": true,
  "data": {
    "selectedDate": "2026-04-13",
    "appointments": [
      {
        "id": "550e8400-e29b-41d4-a716-446655440002",
        "customer_name": "Arun Sharma",
        "customer_phone": "+919123456789",
        "start_time": "2026-04-13T05:00:00Z",
        "end_time": "2026-04-13T05:30:00Z",
        "status": "confirmed",
        "total_amount": 40000,
        "payment_status": "pending",
        "services": {
          "name": "Haircut + Beard Trim"
        },
        "payments": [
          { "amount": 20000, "status": "paid" }
        ]
      }
    ],
    "summary": {
      "total": 8,
      "pending": 3,
      "seated": 1,
      "completed": 3,
      "no_show": 1,
      "canceled": 0
    }
  },
  "meta": {
    "timestamp": "2026-04-13T10:30:00Z",
    "timezone": "Asia/Kolkata"
  }
}
```

**Field Notes**
- `total_amount` and payment `amount` are in **paise** (₹1 = 100 paise). Display: `amount / 100.0` → `"₹400.00"`
- `start_time` / `end_time` are UTC ISO strings. Convert to shop timezone (`meta.timezone`) for display.
- `services` is an object `{ name }`. Can be `null` for walk-in bookings without a linked service.
- `payments` is an array of payment records. Sum `amount` where `status = "paid"` to compute amount already collected.
- `summary.pending` counts bookings with status `confirmed` or `pending_payment`.

**Booking Status Values**
| Value | Meaning |
|-------|---------|
| `pending_payment` | Booking held, payment not yet received |
| `confirmed` | Booking confirmed |
| `seated` | Customer is in the chair |
| `completed` | Service done |
| `canceled` | Canceled (may have refund in progress) |
| `no_show` | Customer did not arrive |

**Error Codes**
| Code | HTTP | Condition |
|------|------|-----------|
| `UNAUTHORIZED` | 401 | Token invalid or expired |
| `SHOP_INACTIVE` | 403 | Shop exists but subscription access is inactive/expired |
| `INTERNAL_ERROR` | 503 | Subscription verification service unavailable |
| `SHOP_NOT_FOUND` | 404 | Shop record not found |
| `QUERY_FAILED` | 500 | Database error |

---

## 3. Workflow API

### 3.1 Execute Workflow Action
**`POST /api/v1/barber/dashboard/bookings/{bookingId}/workflow`**  
`Authorization: Bearer <token>`

Drive a booking through its state machine. Each action is idempotent within its transition — calling `start` on an already-`seated` booking returns `INVALID_TRANSITION` (409), not a silent success.

**Request Body**
```json
{
  "action": "start"
}
```

**Valid Actions**
| Action | Allowed From | Transitions To |
|--------|-------------|----------------|
| `start` | `confirmed`, `pending_payment` | `seated` |
| `complete` | `seated` | `completed` |
| `collect_payment` | `completed` (due > 0) | `completed` (payment_status updated) |
| `mark_no_show` | any non-final | `no_show` |
| `cancel_refund` | `confirmed` only | `canceled` |

**Response 200**
```json
{
  "success": true,
  "data": {
    "bookingId": "550e8400-e29b-41d4-a716-446655440002",
    "status": "seated",
    "paymentStatus": "pending",
    "paidAmount": 0,
    "paymentMethod": null
  },
  "meta": {
    "timestamp": "2026-04-13T10:30:00Z"
  }
}
```

> `paymentMethod` is `"cash"` when `action = collect_payment`, `null` otherwise.  
> `paidAmount` is in paise.

**Error Codes**
| Code | HTTP | Condition |
|------|------|-----------|
| `UNAUTHORIZED` | 401 | Token invalid or expired |
| `SHOP_INACTIVE` | 403 | Shop exists but subscription access is inactive/expired |
| `INTERNAL_ERROR` | 503 | Subscription verification service unavailable |
| `VALIDATION_ERROR` | 400 | Invalid or missing `action` field |
| `BOOKING_NOT_FOUND` | 404 | Booking doesn't exist or not owned by this barber/shop |
| `INVALID_TRANSITION` | 409 | Action not valid for current booking status |
| `ALREADY_PAID` | 409 | `collect_payment` called when balance is already zero |
| `PAYMENT_FAILED` | 500 | Failed to write cash payment record |
| `UPDATE_FAILED` | 500 | Failed to update booking status |
| `CANCEL_FAILED` | varies | Cancel/refund pipeline failed (proxied status code from cancel route) |

---

## 4. Data Types Reference

### Monetary Amounts
All `amount` fields are **integer paise** (1 INR = 100 paise).

```kotlin
fun formatAmount(paise: Int): String = "₹%.2f".format(paise / 100.0)
```

### Booking Status Enum
```kotlin
enum class BookingStatus(val value: String) {
    PENDING_PAYMENT("pending_payment"),
    CONFIRMED("confirmed"),
    SEATED("seated"),
    COMPLETED("completed"),
    CANCELED("canceled"),
    NO_SHOW("no_show")
}
```

### Payment Status Enum
```kotlin
enum class PaymentStatus(val value: String) {
    PENDING("pending"),
    PARTIAL("partial"),
    PAID("paid")
}
```

### Workflow Action Enum
```kotlin
enum class WorkflowAction(val value: String) {
    START("start"),
    COMPLETE("complete"),
    COLLECT_PAYMENT("collect_payment"),
    MARK_NO_SHOW("mark_no_show"),
    CANCEL_REFUND("cancel_refund")
}
```

### ApiResult Sealed Class
```kotlin
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class ApiError(
        val code: String,
        val message: String,
        val httpStatus: Int
    ) : ApiResult<Nothing>()
    data class NetworkError(val cause: IOException) : ApiResult<Nothing>()
}
```

---

## 5. Request Headers

### Authenticated requests
```
Authorization: Bearer <token>
Content-Type: application/json
Accept: application/json
Accept-Language: en
X-App-Version: 1.0.0
X-Platform: android
```

### Pre-auth requests (onboarding, OTP)
```
Content-Type: application/json
Accept: application/json
Accept-Language: en
X-App-Version: 1.0.0
X-Platform: android
```

> `Accept-Language` is `"en"`, `"hi"`, or `"mr"` from `LanguageManager.get()`. The backend does not act on it today, but sends it consistently so future server-side error localisation is possible without a client update.

---

## 6. Error Handling — Android Actions

| Error Code | Android UX Action |
|-----------|-------------------|
| `UNAUTHORIZED` (401) | Clear stored token → navigate to Login screen |
| `BARBER_INACTIVE` (403) | Clear token → show "Account deactivated. Contact your manager." |
| `SHOP_NOT_FOUND` (404 on onboarding) | Show inline error on ShopSetupScreen |
| `SHOP_INACTIVE` (403 on onboarding) | Show inline error "Shop is currently inactive" on ShopSetupScreen |
| `OTP_EXPIRED` | Show "OTP expired — tap Resend" |
| `OTP_LOCKED` (429) | Disable input, show countdown to resend |
| `OTP_INVALID` | Shake input field, show "Incorrect code" |
| `INVALID_TRANSITION` (409) | Show snackbar with `error.message`; refresh booking state |
| `ALREADY_PAID` (409) | Show snackbar "Payment already collected" |
| `CANCEL_FAILED` | Show error dialog with retry |
| Network error / timeout | Show offline banner; auto-retry on connectivity restored |

---

## 7. Session Lifecycle

```
App launch:
  token present?
    → GET /auth/me
        ok  → cache barber+shop, proceed to Dashboard
        401 → navigate to Login

Login flow:
  POST /auth/request-otp → POST /auth/verify-otp
    → store token + expiresAt in EncryptedSharedPreferences
    → cache barber object in ShopManager

Background refresh (WorkManager):
  when (expiresAt - now) < 1 hour:
    → POST /auth/refresh
        ok  → replace stored token + expiresAt
        401 → notify user, navigate to Login on next foreground

Logout:
  POST /auth/logout
  → clear token, barber cache, shop slug/shop metadata (keep language)
  → navigate to onboarding (shop setup)
```
