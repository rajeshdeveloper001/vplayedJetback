# Vplayed 3.0 — Android (Kotlin + Jetpack Compose)

OTT Android app. Single `:app` module, feature-oriented packages, MVVM + unidirectional data flow, Hilt, centralized design system. Built so far: splash screen, dashboard with bottom navigation (Home — API driven, Search / Promos / Subscription — placeholders) and a side menu, Login (email step, Fragment), app-wide geo-restriction refresh, Appearance settings, design-system reference. **Visual design follows the VPlayed prototype** (mobile Home); the previous app (`VplayedApp`, e.g. `ShocialFragment`) is a reference for logic only.

## Toolchain (do not change casually)

- Gradle 9.6 (daemon JDK 25), AGP 9.4.1 with **built-in Kotlin** — never apply `org.jetbrains.kotlin.android` to a module; it is declared `apply false` at the root only to pin the Kotlin version.
- Kotlin 2.4.21 — `kotlin`, `kotlin-compose`, `kotlin-serialization` plugins must share one version (`gradle/libs.versions.toml`).
- compileSdk/targetSdk 37, minSdk 24, Java 11 bytecode.
- All versions live in `gradle/libs.versions.toml`. Prefer stable releases; justify any new dependency in `docs/dependencies.md`.

Command-line builds need JDK 25: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`.

## Build types (no product flavors)

| Build type | Application ID | Server (`SERVER_URL`) | Images (`IMAGE_BASE_URL`) | Debuggable | R8 | HTTP logging |
|---|---|---|---|---|---|---|
| dev (default) | `com.vplayed.dev` | https://api-devgs.vplayed.com/ | gsvp-dev bucket | yes | no | yes |
| qa | `com.vplayed.qa` | https://api-qags.vplayed.com/ | gsvp-qa bucket | yes | no | yes |
| stage | `com.vplayed.stage` | https://api-stagings.vplayed.com/ | gsvp-qa bucket | no | yes | no |
| demo | `com.vplayed` | https://api-demogs.vplayed.com/ | gsvp-demo bucket | no | yes | no |
| release | `com.vplayed` | placeholder (TBD) | placeholder (TBD) | no | yes | no |

- `debug` is disabled (`androidComponents.beforeVariants`); `dev` is the instrumented-test build type.
- Environment values are defined once in `app/build.gradle.kts` and read **only** via `core/common/config/AppConfig` (injected). Never read `BuildConfig` elsewhere.
- Release signing comes from `keystore.properties` (git-ignored) or `VPLAYED_KEYSTORE_*` env vars. Never commit keys or passwords.
- The request-encryption key `SALT_KEY` comes from `secrets.properties` (`vplayed.saltKey`, git-ignored), `-Pvplayed.saltKey` or `VPLAYED_SALT_KEY`; it reaches code only as `AppConfig.transportKey`.
- Details: `docs/build/build-variants.md`.

## Package layout (`app/src/main/java/com/vplayed/`)

```
app/            Application (Coil loader, starts GeoRestrictionSync), MainActivity (BaseActivity + SplashScreen API),
                VPlayedApp (side menu + nav shell + tab bar), AppDrawer
core/common/    AppConfig, AppEnvironment, ConfigModule, validation/EmailValidator
core/error/     AppError, DataResult, resultOf { } (typed failures from the data layer)
core/designsystem/  color/ typography/ dimension/ shape/ theme/ component/ icon/
core/ui/        AppScreenScaffold, adaptive/ (AppLayoutInfo, ResponsiveContent), preview/ annotations,
                base/ (BaseActivity, BaseFragment, AppearanceViewModel)
core/repository/    interfaces (AppearanceRepository)
core/datastore/     DataStore implementations + Hilt modules
core/network/       OkHttp/Retrofit/Json, ApiHeadersInterceptor (device_id, x-bearer-token), SelectiveLoggingInterceptor,
                    @WithoutGeoToken / @RedactBody, connectivity/NetworkMonitor
core/geo/           GeoRestrictionStore (single source of IP / geo token / currency), GeoRestrictionRepository,
                    GeoRestrictionSync, work/GeoRefreshWorker (WorkManager)
core/security/      CryptoTransport (backend request encryption)
core/session/       UserSession, SessionRepository (encrypted DataStore + Keystore), sent as Authorization / profile_id
core/dispatcher/    @IoDispatcher / @DefaultDispatcher / @ApplicationScope
feature/<name>/     data/ domain/ presentation/{screen,viewmodel,components,navigation}
feature/splashscreen/  SplashScreen (start destination, ≥ 3 s), SplashViewModel (startup work)
feature/auth/          Login + OTP: LoginFragment / OtpFragment (BaseFragment, in the NavHost via AndroidFragment), LoginScreen /
                       OtpScreen (shared AuthLayout), Login/OtpViewModel, DefaultAuthRepository (email-login, verifyEmailOtp,
                       device limit), DeviceLimitHandoff
feature/dashboard/     navigation/ (DashboardTab: Home, Search, Promos, Subscription)
  home/                data/ (HomeApi, DTOs, HomeMapper, DefaultHomeRepository, di) · domain/ · presentation/
  search|promos|subscription/  placeholder screens until their APIs exist
feature/settings/      Appearance (theme mode, palette, text size, density)
reference/          not product UI: typography/OttTextStylesReference, showcase/ (design-system demo)
```

Features never depend on other features. `core` never depends on `feature`. Full description: `docs/architecture/project-structure.md`.

## Design-system rules (mandatory)

- Wrap every screen, preview and UI test in `AppTheme`. Read tokens through `AppDesignSystem`:
  `.colors` (Material roles), `.extendedColors` (status, disabled, divider, overlay, prototype neutrals: textSoft, textFaint, hairline, glass, artworkScrim, onImageScrim…), `.typography` (Material scale), `.textStyles` (OTT roles: heroTitle, heroSubtitle, screenHeader, header, subHeader, sectionLink, cardHeadline, cardTitle, cardBody, cardSubtitle, metadata, synopsis, badge, tag, action, actionMedium, actionSmall, monogram, caption, playerTitle, playerTimestamp, rank), `.dimens`, `.shapes`, `.elevations`.
- No `Color(0x…)`, literal `sp`, `FontFamily`, or ad-hoc `dp` in features or components. Raw colors exist only in `core/designsystem/color/BrandPalettes.kt`. A named private constant is acceptable for a one-off layout size.
- Colors follow the prototype: palette **Vplayed** (brand red `#FF0935` on black / `#E20831` on white) and **Black & white**. Yellow (`tertiary`) is the secondary accent (hero dots, chip dot).
- **The prototype is the source of truth for Home.** Each row type renders with its prototype component (`SectionStyle`): videos/series `.hcard`, micro series `.pcard`, top 10 `.t10`, live `.lcard`, promos `.pstat`, audio `.mcard`, channels `.chtile`, bulk purchase `.premium-in`; mapping and measurements in `docs/design-system/design-system-guide.md` › Home. The hero chip shows the content type (Video / Series / Micro Series), never a genre; title artwork replaces the title text unless it is missing or fails to load. Inspect the prototype (computed styles) before changing Home UI, verify on a 427dp-wide emulator, and record unavoidable differences in `docs/decisions.md`.
- Font: Inter only (`res/font/inter_*.ttf`, OFL license in `docs/licenses/`).
- Use existing components (`AppButton`, `AppTextField`, `AppCard`, `AppTopBar`, `AppScreenScaffold`, `AppLoadingIndicator`, `AppErrorView`, `AppEmptyState`, `AppDialog`, `AppSurface`) before writing new UI. Reproduce prototype CSS gradients / `color-mix()` with `cssLinearGradient`, `cssRadialGradient`, `Color.mixWith`. Add a component to `core/designsystem/component` only when it is reused or enforces consistency.
- Appearance preferences have one source of truth: `AppearanceRepository`. ViewModels observe it; never copy theme state.
- Guide: `docs/design-system/design-system-guide.md`.

## App launch

AndroidX SplashScreen (`installSplashScreen()` in `MainActivity`, theme `Theme.Vplayed30.Starting`) shows only `@color/splash_background` (empty icon, so the logo never jumps) until appearance preferences load. Then `feature/splashscreen/SplashScreen` — the nav start destination — fades in `@drawable/ic_vplayed_logo` (light/dark variants via `drawable-night`) on the same color and replaces itself with Home when `SplashViewModel` is ready — after at least 3 s (`SplashConfig.minDisplayMillis`, requested; tests use 0 ms) with startup work running in parallel. Add startup work (config, auth, first destination) to `SplashViewModel.runStartup()`.

## Responsive / orientation rules

- All orientations supported; `MainActivity` handles rotation/resize/uiMode in place (`android:configChanges`), so never lock orientation and never rely on activity recreation.
- Adapt to **window size**, never device type: `rememberAppLayoutInfo()` (bar vs rail, padding), `GridCells.Adaptive(AppDesignSystem.dimens.adaptiveGridMinCellWidth)`, `ResponsiveContent` for max widths.
- Screens use `AppScreenScaffold` and apply its `PaddingValues` (it handles status bar, cutouts and IME insets).
- Add `@ScreenPreviews` (phone, landscape, foldable, tablet × light/dark) to every screen and `@ComponentPreviews` to every component.

## Code conventions

- Screens: stateful `XRoute(viewModel = hiltViewModel())` + stateless `XScreen(uiState, callbacks)`. Collect with `collectAsStateWithLifecycle()`.
- UI state is immutable (`data class` / `sealed interface` with explicit Loading/Empty/Error/Content where relevant). ViewModels expose `StateFlow`, take events as functions.
- Business rules live in `domain/` (pure Kotlin), not in composables. Add a repository interface only when there is more than one implementation or tests need a fake.
- Stable `key`s for lazy items. User-visible strings in `res/values/strings.xml`.
- Type-safe navigation: `@Serializable` route objects in `feature/<x>/presentation/navigation`.
- Don't suppress warnings or opt into experimental APIs without a comment explaining why.

## Fragments

Compose destinations are the default. A screen that must be a Fragment extends `core/ui/base/BaseFragment` (override `ScreenContent()`, never `Content()`), and is placed in the NavHost with `AndroidFragment<T>()`; pass callbacks through `onUpdate`. The host activity extends `BaseActivity` (`FragmentActivity`).

## API

Home endpoints, pagination and response quirks: `docs/api/home-api.md`. Login and geo-restriction contracts (headers, encryption, status codes): `docs/api/auth-api.md`. Mark Retrofit methods `@RedactBody` when bodies hold credentials or personal data, and `@WithoutGeoToken` when the geo token must not be sent. Data layers return `DataResult` (never throw); map exceptions with `resultOf { }`. Parse with the app `Json` (lenient: unknown keys ignored, `coerceInputValues`). Use `JsonElement`/`JsonPrimitive` for fields whose JSON type varies.

## Testing

Test names: `givenX_whenY_thenZ`. See `docs/testing/` — `user-flows.md` lists every user flow with manual steps and its automated test. Instrumented tests run on `HiltTestApplication` (`com.vplayed.testing.HiltTestRunner`); `TestModules.kt` replaces the Home and auth repositories with `FakeHomeRepository` / `FakeAuthRepository` and the splash minimum with 0 ms. `GeoRestrictionSync` starts in `VPlayedApplication`, so it does not run under `HiltTestApplication` (no real network in tests). Unit-test API responses are synthetic fixtures in `src/test/resources/home` — never commit real API responses (they contain personal data).

```bash
./gradlew testDevUnitTest testQaUnitTest testStageUnitTest testDemoUnitTest testReleaseUnitTest   # unit tests, every variant
./gradlew connectedDevAndroidTest       # Compose UI tests (emulator/device required, Orchestrator)
./gradlew lintDev lintRelease
./gradlew assembleDev assembleQa assembleStage assembleDemo assembleRelease
./gradlew createDevUnitTestCoverageReport   # JaCoCo coverage for dev unit tests
```

Every change must keep all of the above green. Never report a check as passing without running it.

## Safely modifying the project

- Read a file fully before editing; keep diffs minimal and in the existing style.
- Don't upgrade working dependencies as a side effect. Version bumps are their own change, verified with the full command list above.
- Don't invent API endpoints, payloads, credentials or business rules — ask.
- New environment values → `BackendEnvironment` in `app/build.gradle.kts` + `AppConfig` + `BuildVariantConfigTest`.
- Keep `docs/` in sync when changing architecture, tokens, variants or tests.
