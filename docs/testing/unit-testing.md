# Unit testing

Location: `app/src/test/java/com/vplayed/…`, mirroring production packages. Shared helpers live in `com.vplayed.testing`.

Unit tests are enabled for **every** build type (`android.onlyEnableUnitTestForTheTestedBuildType=false` in `gradle.properties`), so `testQaUnitTest` compiles against the QA `BuildConfig`, and so on.

## Current suites

| Test | Covers |
|---|---|
| `core/common/config/BuildVariantConfigTest` | Per-variant app ID, environment, server URL, logging, debuggable, `SALT_KEY` wiring (never in `toString`) |
| `core/common/config/AppEnvironmentTest` | Environment id parsing |
| `core/datastore/DataStoreAppearanceRepositoryTest` | Defaults, persistence, text-scale clamping, unknown values, reset |
| `core/designsystem/DesignSystemConfigTest` | Brand fallbacks, Inter default, light/dark schemes, typography scaling, OTT header hierarchy, density |
| `core/ui/adaptive/AppLayoutInfoTest` | Window classes, bar vs rail, padding per window size |
| `feature/home/HomeViewModelTest` | Email validation flow, demo state |
| `feature/splashscreen/SplashViewModelTest` | 3-second minimum (virtual time), waits for startup work |
| `feature/dashboard/home/HomeMapperTest` | Card styles per type, image URLs, portrait posters, subtitles, prices, live flag, genre ids, localized titles |
| `feature/dashboard/home/DefaultHomeRepositoryTest` | Real Retrofit + JSON against MockWebServer: paths/params, paging, 401/500/failed envelope/malformed/offline |
| `feature/dashboard/home/HomeViewModelTest` | Loading, content, pagination, de-duplication, retry, banners-only failure, empty, row paging, refresh |
| `core/network/ApiHeadersInterceptorTest` | Mobile app headers, `device_id`, geo token header (never null/empty, latest value, skipped on `@WithoutGeoToken`), `Authorization` / `profile_id` only with a session |
| `core/common/validation/EmailValidatorTest` | Empty, malformed and valid (trimmed) addresses |
| `core/security/CryptoTransportTest` | OpenSSL-compatible output (fixed-salt vector), random salt, no empty payload |
| `core/geo/GeoTokenDecoderTest` | Currency from Base64 JSON; invalid Base64 / malformed JSON / missing currency return null |
| `core/geo/GeoRestrictionRepositoryTest` | IP → geo token → currency; failures keep the last valid state; serialized refreshes; geo-data path |
| `core/geo/GeoRestrictionSyncTest` | Refresh on start online, reconnect and network switch; none while unchanged; flapping debounced (virtual time) |
| `core/geo/GeoWorkDecisionTest` | Worker success / retry / give-up mapping |
| `feature/auth/DefaultAuthRepositoryTest` | Real Retrofit + JSON + encryption against MockWebServer: email-login and verifyEmailOtp bodies decrypt to the expected JSON, 200/203/202, HTTP 200 with API error, missing token, invalid / expired code, 4xx/5xx, malformed, offline, missing key, session saved only on a real login, device list parsing, logout-device and verifyEmailLogin forms |
| `feature/auth/LoginViewModelTest` | Validation, focus-loss check, OTP opens only for a real "code sent" (200/203) or a 202 with devices, Toasts otherwise, duplicate submits ignored, email cleared on close / sign-in |
| `feature/auth/OtpViewModelTest` | Six-digit rule, verify success → success step → Dashboard after 3 s, wrong / expired code, network failure, duplicate verify, resend (success, failure, overlap), 60 s countdown (virtual time), device step (sign out one / all, continue, back, handoff) |
| `core/session/DataStoreSessionRepositoryTest` | Round trip, ciphertext only in storage, corrupt / foreign data → signed out, Keystore unavailable → memory only, masked `toString` |
| `feature/settings/AppearanceSettingsViewModelTest` | Loading→Ready, actions write to the repository, reset, AppearanceViewModel observes changes |

## Patterns

**ViewModels using `viewModelScope`** — install `MainDispatcherRule` and create the ViewModel in `@Before` (property initializers run before rules):

```kotlin
@get:Rule val mainDispatcherRule = MainDispatcherRule()
private val repository = FakeAppearanceRepository()
private lateinit var viewModel: AppearanceSettingsViewModel

@Before fun setUp() { viewModel = AppearanceSettingsViewModel(repository) }

@Test
fun givenUserSelections_whenApplied_thenRepositoryIsSingleSourceOfTruth() = runTest {
    backgroundScope.launch(mainDispatcherRule.testDispatcher) { viewModel.uiState.collect() } // WhileSubscribed
    viewModel.onThemeModeSelected(ThemeMode.DARK)
    assertEquals(ThemeMode.DARK, repository.state.value.themeMode)
}
```

**DataStore** — use a real store on a `TemporaryFolder` with `TestScope.backgroundScope`:

```kotlin
private val testScope = TestScope(UnconfinedTestDispatcher())
private val dataStore = PreferenceDataStoreFactory.create(
    scope = testScope.backgroundScope,
    produceFile = { File(tmp.root, "test.preferences_pb") },
)
```

**Pure logic** (validators, mappers, layout classification) — plain JUnit, no rules.

**API** — synthetic JSON fixtures in `src/test/resources/home/` parsed with `Fixtures.json` (the app's own `NetworkModule.provideJson()`), served by `MockWebServer` for repository tests. Never commit real API responses.

**Home ViewModel** — `FakeHomeRepository` (test) scripts each page/row result, records calls, and can hold a page (`holdSections`) to assert loading states.

## Commands

```bash
./gradlew testDevUnitTest
```

```bash
./gradlew testDevUnitTest --tests "com.vplayed.feature.home.HomeViewModelTest"
```

```bash
./gradlew testDevUnitTest testQaUnitTest testStageUnitTest testDemoUnitTest testReleaseUnitTest
```
