# AGENTS.md

## Scope and current state
- This repo currently contains a single Android app module (`:app`) using Jetpack Compose (`app/src/main/java/app/slotnow/slotnowpro/MainActivity.kt`).
- Current UI is template-level only (`Greeting("Android")`), so most production behavior is defined in migration docs under `migration/`.
- Existing AI convention files were searched via one glob (`**/{.github/copilot-instructions.md,AGENT.md,AGENTS.md,CLAUDE.md,.cursorrules,.windsurfrules,.clinerules,.cursor/rules/**,.windsurf/rules/**,.clinerules/**,README.md}`) and none were found.

## Big-picture architecture to follow
- Treat `migration/BARBER_ANDROID_ARCHITECTURE.md` as the primary target blueprint: MVVM + Clean layers (`presentation -> domain -> data`), Hilt DI, Retrofit/OkHttp, optional Room.
- Keep backend as source of truth (no business-rule duplication in app); see `migration/BARBER_DASHBOARD_ANDROID_MIGRATION.md` section "Migration Philosophy".
- Planned navigation is graph-based with launch branching by persisted state (language/shop/token); see architecture doc section "App Launch Decision Tree".
- Planned data ownership: `TokenManager` + `ShopManager` (encrypted prefs) and `LanguageManager` (regular prefs); language persists across logout while token/shop are cleared.

## Developer workflows (project-specific)
- Use Gradle wrapper from repo root.
- Verified working command: `./gradlew :app:testDebugUnitTest --console=plain` (passes with template unit test).
- Common local checks (not all verified here): `./gradlew :app:assembleDebug --console=plain`, `./gradlew :app:lintDebug --console=plain`.
- Instrumentation tests live in `app/src/androidTest/...`; running them requires an emulator/device (`connectedDebugAndroidTest`).

## Conventions and patterns to preserve
- Use Kotlin + Compose only; current module config is in `app/build.gradle.kts` and versions are centralized in `gradle/libs.versions.toml`.
- Keep package root `app.slotnow.slotnowpro` unless a deliberate migration step changes namespace.
- Localisation target is strict: all UI strings in resources, EN/HI/MR parity, with one documented exception for onboarding language picker inline labels (from architecture doc section 16/17).
- API models should map from transport DTOs to domain models; repositories expose result wrappers (see `ApiResult` examples in migration docs).
- Time and money handling are domain-specific: UTC timestamps rendered in shop timezone; amounts are paise integers converted for UI.

## Coding instructions (clean code + Kotlin)
- Keep functions/classes small and single-purpose; do not mix UI, domain logic, and transport concerns in one file.
- Name by intent (`validateShopSlug`, `loadBookingsForDate`); avoid generic verbs (`handleData`, `process`).
- State must be immutable by default: expose read-only `StateFlow`/`Flow`, keep mutable backing properties private in ViewModels.
- Use Kotlin null-safety explicitly: avoid `!!`, model optional data with nullable types, and handle null at boundaries.
- Handle sealed types/enums (for example `ApiResult`) with exhaustive `when` branches.
- Async work must use coroutines: repository APIs should be `suspend`; presentation state should be driven from `viewModelScope` + `StateFlow`; avoid main-thread blocking calls.
- Composables should be state-light and hoisted: pass state + callbacks, keep business logic in ViewModels/repositories.
- Prefer `data class`, focused extension functions, and expression-style functions when they improve clarity; avoid clever abstractions that hide execution flow.

## Integration points and external contracts
- API base contract is `/api/v1/barber/` with standard success/error envelope; see `migration/BARBER_ANDROID_API_REFERENCE.md`.
- Auth model is Bearer token with refresh flow and retry guard (`TokenAuthenticator` + single-flight mutex pattern in architecture doc).
- Expected request headers include `Accept-Language`, `X-App-Version`, and `X-Platform` in addition to auth.
- Planned external services include Firebase Messaging/Analytics/Crashlytics (Phase 5), but they are not wired in current Gradle dependencies yet.

## Implementation guidance for AI agents
- Before coding features, align behavior with migration stories in `migration/BARBER_ANDROID_SCRUM_STORIES.md` (story IDs like `US-ONBOARD-*`, `US-AUTH-*`).
- When docs conflict, prefer `BARBER_ANDROID_ARCHITECTURE.md` + `BARBER_ANDROID_API_REFERENCE.md` for technical truth and call out ambiguity in PR notes.
- Keep changes incremental by phase (onboarding/auth first, then dashboard/workflow/profile) to avoid cross-layer churn.
- Add or update tests alongside feature code; current tests are placeholders, so new domain/viewmodel work should introduce the first meaningful test suites.

## PR checklist
- Scope maps to a story or migration phase (`US-...`) and does not introduce out-of-phase churn.
- Layer boundaries are preserved (`presentation -> domain -> data`); no business rules are duplicated from backend contracts.
- Kotlin clean-code rules are satisfied: no `!!`, immutable public state, and exhaustive `when` on sealed/enums (for example `ApiResult`).
- Compose UI stays state-hoisted and resource-driven (`stringResource`), except the documented onboarding language-picker inline-label exception.
- API integration follows current contract (`/api/v1/barber/`, expected headers, DTO-to-domain mapping) with no silent envelope deviations.
- Tests were added/updated for changed behavior (at minimum unit tests for domain/viewmodel logic) and local Gradle checks were run where applicable.



