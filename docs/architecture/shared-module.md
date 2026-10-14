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

The shared HTTP client runs on a copy of the app's `OkHttpClient` **without its interceptors**. It
shares the connection pool and dispatcher, and the shared layer adds the VPlayed headers and its own
redacting log itself.

## Current state (Phase 2)

Nothing in the app calls `SharedGraph` yet, so behaviour is unchanged. `SharedHeadersParityTest`
proves that the shared client sends the same backend headers as `ApiHeadersInterceptor`: signed out,
signed in with a profile, with a geo token, and with the geo token suppressed. `SharedConfigMappingTest`
runs per build type.

Features move over in later phases by re-binding their repository interfaces (`AuthRepository`,
`HomeRepository`, `SessionRepository`) to shared implementations. ViewModels, screens and
`TestModules.kt` fakes keep their current interfaces. Until session and geo state move into the shared
layer (Phase 3/5), anything that calls the shared client must mirror them into
`SharedGraph.requestCredentials`.

## Rules

- No UI in the shared module, and no Android types in its common code. Android-only pieces (Keystore
  storage, WorkManager triggers, connectivity) stay in this app behind interfaces.
- Shared calls return `DataResult` or sealed results and never throw. Keep the app's own error-to-string
  mapping (`userMessageRes`) here.
- User-visible strings stay in `res/values/strings.xml`. The shared layer returns typed results and
  message keys.
