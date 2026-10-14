# Shared KMP module

Business logic shared with the iOS app (VPlayed, SwiftUI) lives in a Kotlin Multiplatform library,
`com.vplayed:shared`, in the separate `kmp` repository. UI stays here, in Compose. The full plan is
in `../KMP_PLANNING/` next to the repositories (`KMP_MIGRATION_PLAN.md`, `KMP_IMPLEMENTATION_BACKLOG.md`).

## How it is built

- `settings.gradle.kts` includes `../kmp` as a **composite build** and substitutes
  `com.vplayed:shared` with its `:shared` project. Gradle builds it from source with this build, and
  edits in `../kmp` apply on the next build. CI needs both repositories checked out side by side, or a
  published artifact in place of the composite build.
- `kmp` uses the same toolchain as this project (Gradle 9.6, AGP 9.4.1, Kotlin 2.4.21, JDK 25). Change
  both together.
- The library brings Ktor 3.6.0 on the OkHttp engine (OkHttp 5.5.0, the version this app already uses)
  and kotlinx coroutines/serialization 1.11.0 (also already used here).

## Bridge (`core/shared`)

| File | Role |
|---|---|
| `SharedModule` | Hilt `@Provides @Singleton SharedGraph`, the single shared dependency graph for the process |
| `SharedConfigMapping` | `AppConfig.toSharedConfig()`: environment, server/image URLs, logging, version, transport key. `AppConfig` stays the only reader of `BuildConfig` |
| `AndroidSharedDeviceInfo` | `device_id`, `deviceDetail` and `X-Language-Code` from `DeviceIdProvider`, `DeviceNameProvider` and the default locale, the same sources `ApiHeadersInterceptor` uses |
| `AndroidRequestCredentials` | `Authorization`, `profile_id` and `x-bearer-token` for shared requests, read live from `SessionRepository` and the shared `GeoStore` (the stores `ApiHeadersInterceptor` reads), so nothing is mirrored |

The shared HTTP client runs on a copy of the app's `OkHttpClient` **without its interceptors**. It
shares the connection pool and dispatcher, and the shared layer adds the VPlayed headers and its own
redacting log itself.

## Current state (Phase 3 done: login · Phase 4 done: Home, geo restriction)

**Login runs on the shared layer.** `LoginViewModel` and `OtpViewModel` delegate to the shared
`EmailLoginController` and `OtpController` (`../kmp`, `auth/`), which hold every rule of the flow:
- email validation (decision D16: TLD of 2–64 letters, at most 50 characters)
- one request at a time
- reading the answers (D1–D5)
- the 60 s resend countdown
- the device-limit steps
- the 3 s success step

The ViewModels keep their public API (`uiState`, `events`, the same functions). They:
- turn the controllers' effects into navigation and Toasts,
- hand the device list to the OTP screen through `DeviceLimitHandoff`,
- store the session the controller hands over (`AuthSession.toUserSession`) in `SessionRepository`.

`LoginUiState` and `OtpUiState` are aliases of the shared `EmailLoginState` and `OtpState`. Messages
are shared `AuthMessage`s: the server's own words when safe, else an `AuthMessageKey` that
`AuthMessage.kt` maps to `strings.xml`.

`AuthRepositoryModule` provides the shared `AuthRepository` (`SharedGraph.authRepository`); the
`TestModules.kt` fake implements the same interface. `core/common/validation/EmailValidator` is an alias
of the shared validator, so every form uses the same rule.

The previous Android login code (Retrofit `AuthApi` and DTOs, `DefaultAuthRepository`, the app-side
domain models, the Phase 3 bridge `SharedAuthRepository` and `core/security/CryptoTransport`) was
deleted: requests, encryption and models now come from the shared layer.

The rules (decisions in `KMP_PLANNING/KMP_ARCHITECTURE_ANALYSIS.md` §5; contract in `docs/api/auth-api.md`):

- **D1**: a non-2xx HTTP status is always a failure. Within 2xx, the envelope's `statusCode` decides when present, otherwise the HTTP status.
- **D2–D4**: this app's existing behaviour, now shared with iOS.
- **D5**: this app offers the device-limit step (`AuthFlowConfig(deviceSignOutEnabled = true)`), including `verifyEmailLogin`. iOS keeps it off until the backend confirms that signing in without a code is safe.
- **D16**: the shared email rule.

Tests:
- The flow rules are tested in the shared module: `EmailLoginControllerTest`, `OtpControllerTest` (ported from this app's `OtpViewModelTest`), `AuthRepositoryTest` and `AuthMessageTest`.
- Here, `LoginViewModelTest` and `OtpViewModelTest` run the real controllers with a fake repository, including the stored session.
- `SharedHeadersParityTest` proves the shared client sends the same backend headers as `ApiHeadersInterceptor`.
- `SharedConfigMappingTest` runs per build type.

**Home runs on the shared layer (Phase 4).** `HomeRepositoryModule` provides the shared
`HomeRepository` (`SharedGraph.homeRepository`: `homebanners`, `home`, `home/all/{id}`,
`follow_studio`), and `HomeViewModel` runs the shared `HomeFeedController` on `viewModelScope`:
parallel first load, scroll paging with de-duplication, row paging, refresh that keeps content, and
(decision D9) skipping up to five pages that add no row. `HomeUiState` is an alias of the shared
`HomeFeedState`; the UI uses the shared models (`com.vplayed.shared.home`: `HomeBanner`, `HomeSection`,
`ContentItem`, `ItemDetails`, `SectionStyle`, `PagingStatus` …) directly. Shared errors get their text
from `core/ui/ErrorMessages.kt` (`SharedAppError.userMessage()`).

The shared mapper keeps this app's image choices (decision D12, `HomeImagePolicy.ANDROID`: `tv`
first, `default` renders only); the other Home rules are now common to both apps:
- **D6**: a page is read when the envelope's `statusCode` is 200 (`status` is advisory).
- **D7**: row types 8 and 9 are promos and channels; rows of unknown types are dropped.
- **D10**: live items read `streaming_status`/`schedule_at` (this app's keys, confirmed in the dev API) and fall back to iOS's `publish_status`/`scheduled_at`.
- **D11**: bundles also carry `background_color`; this app keeps the file-name accent.
- **D13**: the price is the one whose `currency_type` is the item's `default_currency_type` (1 = INR when absent).
- **D14**: items carry iOS's `AccessTag` (not shown here yet).

**Geo restriction runs on the shared layer (Phase 4).** `GeoModule` provides the shared `GeoStore`
(one per process, created before the `SharedGraph` because both header stacks read it) and
`GeoRepository` (`SharedGraph.geoRepository`: ipify → `common/geo-data/{ip}` → token → currency).
This app still decides when to refresh (decision D18): `GeoRestrictionSync` and `GeoRefreshWorker`
are unchanged and call the shared repository.

The previous Android code was deleted: Retrofit `HomeApi`, `HomeDtos`, `HomeMapper`,
`DefaultHomeRepository`, the app-side Home models and repository interface, `GeoRestrictionStore`,
`GeoRestrictionRepository`, `GeoTokenDecoder` and the Retrofit geo APIs, with their tests and fixtures
(ported to `../kmp`: `HomeMapperTest`, `HomeRepositoryTest`, `HomeFeedControllerTest`,
`GeoRepositoryTest`, `GeoTokenDecoderTest`). No Retrofit API is left; the Retrofit/OkHttp setup in
`core/network` stays until the cleanup phase, since the shared client's engine reuses the app's
`OkHttpClient`.

The session moves into the shared layer in Phase 5.

## Rules

- No UI in the shared module, and no Android types in its common code. Android-only pieces (Keystore
  storage, WorkManager triggers, connectivity) stay in this app behind interfaces.
- Shared calls return `DataResult` or sealed results and never throw. Keep the app's own error-to-string
  mapping (`userMessageRes`) here.
- User-visible strings stay in `res/values/strings.xml`. The shared layer returns typed results and
  message keys.
