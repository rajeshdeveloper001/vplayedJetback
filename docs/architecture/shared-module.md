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
| `AndroidRequestCredentials` | `Authorization`, `profile_id` and `x-bearer-token` for shared requests, read live from `SessionRepository` and `GeoRestrictionStore` (the stores `ApiHeadersInterceptor` reads), so nothing is mirrored |

The shared HTTP client runs on a copy of the app's `OkHttpClient` **without its interceptors**. It
shares the connection pool and dispatcher, and the shared layer adds the VPlayed headers and its own
redacting log itself.

## Current state (Phase 3: login)

**Login runs on the shared layer.** `AuthRepositoryModule` binds `AuthRepository` to
`feature/auth/data/SharedAuthRepository`, which calls `SharedGraph.authRepository` (email-login,
verifyEmailOtp, logout-device, verifyEmailLogin), maps its results to the app's domain types and saves
a verified session in `SessionRepository`. ViewModels, screens and the `TestModules.kt` fake are
unchanged. `DefaultAuthRepository` and `AuthApi` (Retrofit) stay until the Phase 7 cleanup; binding
`DefaultAuthRepository` again in `AuthRepositoryModule` is the rollback.

The shared layer reads auth answers by the rules both apps now follow (decisions in
`KMP_PLANNING/KMP_ARCHITECTURE_ANALYSIS.md` §13; contract in `docs/api/auth-api.md`):

- **D1**: a non-2xx HTTP status is always a failure. Within 2xx, the envelope's `statusCode` decides when present, otherwise the HTTP status.
- **D2–D4**: this app's existing behaviour, now shared with iOS.
- **D5**: the device-limit sign-out and `verifyEmailLogin` are used here only. iOS keeps them off until the backend confirms that signing in without a code is safe.

Home and geo still use Retrofit (Phase 4). Tests:
- `SharedAuthRepositoryTest` covers the wiring, the mapping and the saved session on the real shared client.
- The shared module's own `AuthRepositoryTest` covers the status-code rules.
- `SharedHeadersParityTest` proves the shared client sends the same backend headers as `ApiHeadersInterceptor`. It covers four cases: signed out, signed in with a profile, with a geo token, and with the geo token suppressed.
- `SharedConfigMappingTest` runs per build type.

Next, Home moves over by re-binding `HomeRepository` (Phase 4). The session moves into the shared layer in Phase 5.

## Rules

- No UI in the shared module, and no Android types in its common code. Android-only pieces (Keystore
  storage, WorkManager triggers, connectivity) stay in this app behind interfaces.
- Shared calls return `DataResult` or sealed results and never throw. Keep the app's own error-to-string
  mapping (`userMessageRes`) here.
- User-visible strings stay in `res/values/strings.xml`. The shared layer returns typed results and
  message keys.
