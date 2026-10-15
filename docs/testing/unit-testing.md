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
| `feature/dashboard/home/HomeViewModelTest` | The ViewModel on the shared `HomeFeedController`: first load starts on creation, loading state, paging, row paging, retry, refresh. The feed rules, mapping and requests are tested in `../kmp` (`HomeFeedControllerTest`, `HomeMapperTest`, `HomeRepositoryTest`) |
| `core/shared/SharedConfigMappingTest` | Per build type: the KMP shared layer gets the same environment, URLs, logging, version and transport key as `AppConfig`; key never printed |
| `core/shared/SharedHeadersTest` | The headers backend requests carry, built as the app builds them (`toSharedConfig`, `AndroidSharedDeviceInfo`, shared `SessionManager` and `GeoStore`, OkHttp engine): signed out, session + profile + geo token, a request opting out of the geo token. The rest of the HTTP contract is tested in `../kmp` (`EndpointContractTest`, `OkHttpEngineTest`) |
| `core/geo/GeoRestrictionSyncTest` | Refresh on start online, reconnect and network switch; none while unchanged; flapping debounced (virtual time) |
| `core/geo/GeoWorkDecisionTest` | Worker success / retry / give-up mapping of the shared refresh outcome (the refresh itself is tested in `../kmp`: `GeoRepositoryTest`, `GeoTokenDecoderTest`) |
| `feature/auth/LoginViewModelTest` | The ViewModel on the shared `EmailLoginController`: validation (empty, malformed), focus-loss check, OTP opens only for a real "code sent" (200/203) or a 202 with devices (handed over once), Toasts otherwise (unsafe server text → app string), duplicate submits ignored, email cleared on close / sign-in |
| `feature/auth/OtpViewModelTest` | The ViewModel on the shared `OtpController`: six-digit rule, verify success → session stored → success step → Dashboard after 3 s, wrong / expired code, network failure, duplicate verify, resend, 60 s countdown (virtual time), device step (sign out one / all, continue, back, handoff), no session on a refused code |
| `core/session/KeystoreSecureStoreTest` | The store under the shared `SessionManager`: a session stored by the previous app version is restored (upgrade), round trip, ciphertext only in storage, corrupt / foreign data → signed out and cleared, Keystore unavailable → memory only, sign-out clears storage, masked `toString` |
| `app/AppShellViewModelTest` | Side menu account: signed-in email; Sign out through the shared `SessionManager.logout` (accepted or 401 → signed out; offline / refusal → stays signed in with a Toast message, the server's text never shown), one request per tap burst |
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

**API** — Home and geo requests are made by the shared layer, so their response tests live in `../kmp` (synthetic fixtures in `home/HomeFixtures.kt`, served by a Ktor `MockEngine`). Never commit real API responses.

**Home ViewModel** — `FakeHomeRepository` (test) implements the shared `HomeRepository`: it scripts each page/row result, records calls, and can hold a page (`holdSections`) to assert loading states.

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
