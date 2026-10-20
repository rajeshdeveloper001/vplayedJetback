# Shared KMP module

Business logic shared with the iOS app (VPlayed, SwiftUI) lives in a Kotlin Multiplatform library,
`com.vplayed:shared`, in the separate `kmp` repository. UI stays here, in Compose. The full plan is
in `../KMP_PLANNING/` next to the repositories (`KMP_MIGRATION_PLAN.md`, `KMP_IMPLEMENTATION_BACKLOG.md`).

## How it is built

The shared library can be consumed in two modes:
1. **Local AAR (Default & Automated):** Built via `./refresh_shared_aar.sh`, which compiles `:shared`, generates Compose Multiplatform resources, and publishes a release `.aar` + `.pom` to `vplayedKMP/repo/localMaven/`. The Android app resolves `com.vplayed:shared:0.1.0` through its configured `KmpLocalMaven` repository.
2. **Composite Build:** Pass `-Pvplayed.useComposite=true` (or set `vplayed.useComposite=true` in `gradle.properties`) to substitute source directly from `../vplayedKMP` or `../kmp`.

- `kmp` uses the same toolchain as this project (Gradle 9.6, AGP 9.4.1, Kotlin 2.4.21, JDK 25). Change both together.
- The library brings Ktor 3.6.0 on the OkHttp engine (OkHttp 5.5.0, the version this app already uses) and kotlinx coroutines/serialization 1.11.0 (also already used here).

## Automated AAR Generation & Refresh

Whenever shared code, error messages, or localization resources (`composeResources/`) change in `vplayedKMP`:

```bash
# From the vplayedUpdate project root:
./refresh_shared_aar.sh
```

This script:
1. Locates `vplayedKMP` and JDK 25.
2. Runs `:shared:publishAllPublicationsToLocalRepoRepository`, generating the release AAR with compiled resources and POM metadata at `repo/localMaven/`.
3. Refreshes dependencies and verifies the Android application builds cleanly.

## Local setup (first-time)

Run the setup script **once** after cloning both repositories:

```bash
# From the vplayedUpdate project root:
chmod +x setup_kmp.sh
./setup_kmp.sh                             # auto-discovers vplayedKMP / kmp sibling
./setup_kmp.sh /path/to/your/vplayedKMP   # or provide the path explicitly
```

The script:
1. Finds the KMP repo (checks `../kmp`, `../vplayedKMP`, `../vplayed-kmp`, common macOS paths).
2. If the repo isn't at a default sibling path, writes `vplayed.kmpDir=/path` to
   `~/.gradle/gradle.properties` so every Gradle invocation resolves it automatically.
3. Sets `JAVA_HOME` to the Android Studio JBR (JDK 25 — required by Gradle 9.6).
4. Updates `local.properties` with the Android SDK path.
5. Runs `./gradlew help` to verify the composite build resolves with no errors.

**KMP path resolution order** (first directory containing `settings.gradle.kts` wins):

| Priority | Source | Value |
|---|---|---|
| 1 | Gradle property | `-Pvplayed.kmpDir=…` or `vplayed.kmpDir=…` in `~/.gradle/gradle.properties` |
| 2 | Sibling `../kmp` | canonical name, used by CI |
| 3 | Sibling `../vplayedKMP` | common clone name |
| 4 | Sibling `../vplayed-kmp` | hyphenated variant |

If none of the above resolves, `settings.gradle.kts` throws a descriptive error with instructions.

## Bridge (`core/shared`)

| File | Role |
|---|---|
| `SharedModule` | Hilt `@Provides @Singleton SharedGraph`, the single shared dependency graph for the process |
| `SharedConfigMapping` | `AppConfig.toSharedConfig()`: environment, server/image URLs, logging, version, transport key. `AppConfig` stays the only reader of `BuildConfig` |
| `AndroidSharedDeviceInfo` | `device_id`, `deviceDetail` and `X-Language-Code` from `DeviceIdProvider`, `DeviceNameProvider` and the default locale (read per request) |
| (graph arguments) | The shared `SessionManager` (`core/session/di/SessionManagerModule`, over `KeystoreSecureStore`) and `GeoStore` (`GeoModule`). The graph sends their `Authorization`, `profile_id` and `x-bearer-token` |

The shared HTTP client runs on the app's `OkHttpClient` (`core/network/NetworkModule`), which has
no interceptors: the shared layer adds the VPlayed headers and its own redacting log itself.

## Current state (Phase 3 done: login · Phase 4 done: Home, geo restriction · Phase 5 done: session)

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
- start the session the controller hands over with the shared `SessionManager.signIn`.

**The session is shared (Phase 5).** The shared `SessionManager` (`../kmp`, `session/`) owns it:
- `KeystoreSecureStore` stores it in the same DataStore file, key (`session`) and Keystore alias as
  before, and the shared `SessionCodec` reads the old `UserSession` JSON, so signed-in users stay
  signed in after the upgrade;
- every shared request waits until the stored session has been read; `SplashViewModel` also waits,
  so the menu shows the right account;
- a 401 on a request that carried the session's token ends it (decision D21); `AppShellViewModel`
  then shows Login again;
- the side menu's **Sign out** (`AppShellViewModel.signOut`) uses the shared `SessionManager.logout`: the session ends when the server confirms or no longer knows the token; other failures keep it and show a Toast.

`LoginUiState` and `OtpUiState` are aliases of the shared `EmailLoginState` and `OtpState`. Messages
are shared `AuthMessage`s: the server's own words when safe, else an `AuthMessageKey` whose text
comes from the shared string catalog (`AuthMessageKey.resource`; `AuthMessage.kt` resolves it).

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
- `SharedHeadersTest` checks the headers the app's backend requests carry, built the way the app builds the graph.
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
`GeoRepositoryTest`, `GeoTokenDecoderTest`).

**Cleanup (Phase 7).** The rest of the old network stack is gone: Retrofit and its converter,
`ApiHeadersInterceptor`, `SelectiveLoggingInterceptor` and the OkHttp logging interceptor,
`@WithoutGeoToken` / `@RedactBody`, the unused third-party client, the app's own `AppError` /
`DataResult` (`core/error`), the email-validator alias and their tests. `core/network` keeps the
`OkHttpClient` the shared engine runs on, the device id / name providers and the network monitor.

## Rules

- No UI in the shared module, and no Android types in its common code. Android-only pieces (Keystore
  storage, WorkManager triggers, connectivity) stay in this app behind interfaces.
- Shared calls return `DataResult` or sealed results and never throw. Error and message keys map to
  catalog strings in the shared layer (`com.vplayed.shared.l10n`: `AppError.messageResource`,
  `AuthMessageKey.resource`, validation `message` / `signInMessage` / `registrationMessage`);
  `core/ui/ErrorMessages.kt` and `AuthMessage.kt` only resolve them.
- **User-visible text comes from the shared string catalog** (`../kmp/shared/src/commonMain/composeResources`,
  Compose Multiplatform Resources), the same strings and translations VPlayed shows (iOS wording is the
  reference). Composables use `org.jetbrains.compose.resources.stringResource(Res.string.…)` /
  `pluralStringResource(Res.plurals.…)` with `com.vplayed.shared.l10n.Res` and the generated accessor
  imports; code outside composition uses `core/ui/SharedText.kt` (`sharedString`). Catalog placeholders
  are all `%1$s`: pass numbers through `localizedNumber`, and the brand (`brandName()`) to messages that
  name it. `res/values/strings.xml` keeps only text that is never translated. To add or change text, edit
  the catalog in `../kmp` (and run its `scripts/generate-swift-l10n-keys.py` for VPlayed).
- **Language.** Menu → Language (`feature/settings`, `LanguageScreen`) lists the backend's languages
  (`SharedGraph.languageRepository`, loaded by `AppLaunchTasks`) and applies the choice as the per-app
  locale (`core/locale/AppLocaleManager`; Settings → Apps → Language on Android 13+, `locales_config.xml`).
  The activity is recreated in the new language. A language without translations (Telugu, Hindi) shows
  English text; server content follows `X-Language-Code`.
