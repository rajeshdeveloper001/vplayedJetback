# Project structure

Single Gradle module (`:app`) organized by feature and responsibility. Split into Gradle modules only when build time or ownership demands it — the package boundaries below are drawn so that split is mechanical.

```
app/src/main/java/com/vplayed/
├── app/
│   ├── VPlayedApplication.kt        @HiltAndroidApp
│   ├── MainActivity.kt              BaseActivity + SplashScreen API; handles config changes in place
│   ├── VPlayedApp.kt                side menu (ModalNavigationDrawer) + NavigationSuiteScaffold (bar ↔ rail) + NavHost
│   └── AppDrawer.kt                 side-menu sheet (Login)
├── core/
│   ├── common/config/               AppConfig, AppEnvironment, ConfigModule (BuildConfig → typed config)
│   ├── common/validation/           EmailValidator (shared form rule)
│   ├── designsystem/                tokens, theme, components, icons (see design-system guide)
│   ├── ui/                          AppScreenScaffold, adaptive/ (AppLayoutInfo, ResponsiveContent), preview/
│   ├── ui/base/                     BaseActivity, BaseFragment, AppearanceViewModel (theme for both)
│   ├── repository/                  repository interfaces shared across features
│   ├── datastore/                   DataStore-backed implementations + DataStoreModule
│   ├── network/                     NetworkModule (OkHttp, Retrofit, Json), ApiHeadersInterceptor, DeviceIdProvider,
│   │                                SelectiveLoggingInterceptor, @WithoutGeoToken/@RedactBody, connectivity/NetworkMonitor
│   ├── geo/                         GeoRestrictionSync, work/GeoRefreshWorker, di/GeoModule (provides the shared
│   │                                GeoStore and GeoRepository; the refresh itself runs in ../kmp)
│   ├── session/                     KeystoreSecureStore + KeystoreSessionCipher for the shared SessionManager, di/
│   ├── shared/                      bridge to the KMP shared layer (../kmp): SharedModule (Hilt, one SharedGraph),
│   │                                AppConfig.toSharedConfig(), AndroidSharedDeviceInfo — see shared-module.md
│   └── dispatcher/                  coroutine dispatcher qualifiers, @ApplicationScope
├── reference/
│   └── typography/OttTextStylesReference.kt   reference sheet of OTT text styles (+ phone/tablet previews)
├── core/error/                      AppError, DataResult, resultOf
└── feature/
    ├── splashscreen/                start destination (≥ 3 s), startup work; di/SplashModule
    ├── auth/                        Login + OTP on the KMP shared controllers (EmailLoginController, OtpController):
    │                                data/ (di provides the shared AuthRepository),
    │                                domain/ (DeviceLimitHandoff; the models are the shared ones),
    │                                presentation/ (LoginFragment, OtpFragment, viewmodel/Login|OtpViewModel,
    │                                screen/Login|OtpScreen, components/ (AuthLayout, OtpCodeField, DeviceLimitStep …),
    │                                navigation/LoginDestination, OtpDestination)
    ├── dashboard/
    │   ├── navigation/              DashboardTab (Home, Search, Promos, Subscription), dashboardGraph
    │   ├── home/
    │   │   ├── data/di/             HomeDataModule (provides the shared HomeRepository; endpoints, mapping
    │   │   │                        and models live in ../kmp `home/`)
    │   │   └── presentation/        viewmodel/HomeViewModel (runs the shared HomeFeedController), screen/HomeScreen,
    │   │                            components/ (HeroBannerCarousel, SectionRow, ContentCards, HomeShimmer, HomeTopBar)
    │   ├── search/  promos/  subscription/   placeholder screens
    └── settings/                    Appearance (full screen, from the Home top bar)
reference/
    ├── typography/                  OttTextStylesReference
    └── showcase/                    design-system showcase (from Appearance › Design system reference)

app/src/main/res/
├── font/          Inter (8 static weights)
├── drawable/      vector icons (AppIcons), ic_vplayed_logo (dark text), splash_icon_empty, launcher
├── drawable-night/ ic_vplayed_logo (white text)
├── values/        strings, splash_background, Theme.Vplayed30(.Starting)
└── values-night/  dark splash_background / window theme

app/src/test/        unit tests mirroring production packages; testing/ holds fakes and rules
app/src/androidTest/ Compose UI and end-to-end tests
```

## Layers and data flow

```
Composable (Route → stateless Screen)
   ↑ StateFlow<UiState>        ↓ events (functions)
ViewModel (@HiltViewModel)
   ↓
Repository interface (core/repository or feature/x/domain)
   ↓
Data source (DataStore, Retrofit service, Room DAO later)
```

- Unidirectional: state flows down, events flow up. UI state is immutable.
- `domain/` is plain Kotlin (no Android imports) and unit-testable on the JVM.
- Repository interfaces only where a fake is needed or there are multiple implementations.
- Dependency direction: `app → feature → core`. Features don't import each other; `core` never imports `feature`.

## Adding a feature

1. `feature/<name>/presentation/navigation/<Name>Navigation.kt`: `@Serializable data object <Name>Destination` + `NavGraphBuilder.<name>Screen()`.
2. `presentation/viewmodel/<Name>ViewModel.kt` with a `<Name>UiState` covering Loading/Empty/Error/Content as needed.
3. `presentation/screen/<Name>Screen.kt`: `<Name>Route` (hiltViewModel) + stateless `<Name>Screen` + `@ScreenPreviews`.
4. Data: Retrofit service + repository in `feature/<name>/data`, Hilt module beside it.
5. Register in `VPlayedApp` (and `DashboardTab` if it's a tab).
6. Unit tests for the ViewModel/domain, Compose tests for the screen.

## Activities and Fragments

- One activity: `MainActivity` extends `core/ui/base/BaseActivity` (a `FragmentActivity`): edge-to-edge, appearance-driven `AppTheme`, system-bar styling. No business logic.
- Screens are Compose destinations. A screen that must be a Fragment (Login) extends `BaseFragment` — a `ComposeView` disposed with the view lifecycle, themed from the activity's `AppearanceViewModel`, with `collectOnViewLifecycle` and `showToast` — and is placed in the NavHost with `AndroidFragment<T>()` (see `LoginNavigation`). Navigation Compose stays the only navigation framework; callbacks reach the fragment through `onUpdate`.

## App-wide geo-restriction state

```
ConnectivityManager default-network callback (NetworkMonitor)
   → GeoRestrictionSync: debounce 1 s, distinct; on app start online, offline → online, network switch
   → GeoRefreshScheduler: WorkManager unique work "geo-restriction-refresh" (REPLACE, CONNECTED, backoff)
   → GeoRefreshWorker → shared GeoRepository.refresh() (../kmp, serialized by a Mutex)
        GET api.ipify.org (shared third-party client, no app headers) → store.updateIp
        GET mediaview/api/v1/common/geo-data/{ip} (no geo token, body redacted) → token
        GeoTokenDecoder (Base64 → {"currency", "countryCode", …}) → store.updateGeo(token, info)
   → shared GeoStore.state (StateFlow) → ApiHeadersInterceptor and the SharedGraph add x-bearer-token
```

Failures keep the previous valid values; currency defaults to INR. Nothing is persisted (as in the previous app). Explicit refresh: `GeoRestrictionSync.refreshNow()`. The previous app's `KEY_NETWORK_CHANGED` broadcast had no receivers and is not reproduced; observers collect `GeoStore.state`.

## Dependency injection

Hilt modules sit next to what they provide: `ConfigModule` (AppConfig), `DispatchersModule` (+ `@ApplicationScope`), `DataStoreModule` + `RepositoryModule`, `NetworkModule` + `NetworkBindingsModule`, `GeoApiModule` + `GeoBindingsModule`, `AuthRepositoryModule` (shared `AuthRepository` from `SharedGraph`), `SessionStorageModule` + `SessionManagerModule` (shared `SessionManager` over `KeystoreSecureStore`). All are `SingletonComponent`. `GeoRefreshWorker` reads its dependencies through a Hilt `@EntryPoint`, so WorkManager's default initializer is kept.
