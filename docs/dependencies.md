# Dependencies and rationale

Versions: `gradle/libs.versions.toml`. All are stable releases (verified against Maven metadata on 2026-10-12; fragment and work on 2026-10-13).

## Build

| Item | Version | Why |
|---|---|---|
| Gradle | 9.6.0 | From the project template; daemon JDK 25 |
| Android Gradle Plugin | 9.4.1 | From the template; built-in Kotlin support |
| Kotlin (KGP pin, Compose compiler, Serialization plugins) | 2.4.21 | Latest stable; all three must match |
| KSP | 2.3.12 | Annotation processing for Hilt (KSP2, Kotlin-version independent) |
| Hilt plugin | 2.60.1 | Dependency injection code generation |

## Runtime

| Library | Version | Purpose |
|---|---|---|
| androidx.core:core-ktx | 1.19.1 | Kotlin extensions (template) |
| androidx.core:core-splashscreen | 1.2.0 | Android 12+ splash API with backport to API 24 |
| activity-compose | 1.13.0 | `setContent`, `enableEdgeToEdge` |
| lifecycle-runtime-compose / viewmodel-compose | 2.11.0 | `collectAsStateWithLifecycle`, ViewModel in Compose |
| navigation-compose | 2.10.2 | Type-safe navigation with `@Serializable` routes |
| Compose BOM | 2026.09.00 | Aligns Compose UI 1.12.1, Material3 1.4.0 |
| material3 | (BOM) | Material 3 components and theming |
| material3-adaptive-navigation-suite | (BOM) | Bottom bar ↔ navigation rail |
| ui-tooling-preview / ui-tooling | (BOM) | Previews (tooling only in dev/qa) |
| datastore-preferences | 1.2.1 | Persisted appearance preferences |
| androidx.fragment:fragment-ktx + fragment-compose | 1.9.1 | Login is a Fragment (`BaseFragment`); `AndroidFragment` embeds it in the Compose NavHost, so no second navigation library |
| androidx.work:work-runtime-ktx | 2.12.0 | Geo-restriction refresh (`GeoRefreshWorker`): unique work, network constraint, retry with backoff |
| hilt-android + hilt-compiler | 2.60.1 | Dependency injection |
| androidx.hilt:hilt-lifecycle-viewmodel-compose | 1.4.0 | `hiltViewModel()` |
| kotlinx-coroutines-android | 1.11.0 | Coroutines / Flow |
| kotlinx-serialization-json | 1.11.0 | Type-safe navigation routes |
| okhttp | 5.5.0 | The HTTP engine under the shared layer's Ktor client (no interceptors; logging is the shared layer's, dev/qa only) |
| io.coil-kt.coil3:coil-compose + coil-network-okhttp | 3.6.3 | Image loading with memory/disk cache (Home artwork) |
| com.vplayed:shared | 0.1.0 (source) | Kotlin Multiplatform business logic shared with the iOS app; built from `../kmp` by the composite build, not downloaded. See `docs/architecture/shared-module.md` |
| org.jetbrains.compose.components:components-resources | 1.12.1 | Runtime of the shared string catalog (Compose Multiplatform Resources: `Res.string.*`, `stringResource`, plurals). Comes with `com.vplayed:shared`; listed in the catalog only for the local-AAR route (`app/libs/shared-release.aar`), which brings no transitive dependencies. Its androidx Compose artifacts resolve to the BOM's versions (1.12.1) |
| com.google.firebase:firebase-bom + firebase-auth | 35.0.2 | Phone sign-in: Firebase texts and checks the login code, as in the previous apps (the backend never sees it). Started from `AppConfig.firebase` (secrets), no google-services plugin |
| androidx.credentials:credentials + credentials-play-services-auth | 1.6.0 | Google sign-in sheet (Credential Manager) |
| com.google.android.libraries.identity.googleid:googleid | 1.2.1 | "Sign in with Google" option and ID-token credential for Credential Manager |
| ↳ io.ktor:ktor-client-core + ktor-client-okhttp | 3.6.0 (transitive) | HTTP client of the shared layer; runs on the app's OkHttp 5.5.0 (same version Ktor 3.6.0 depends on), sharing its connection pool |

## Testing

| Library | Version | Purpose |
|---|---|---|
| junit | 4.13.2 | Unit and instrumented test runner API |
| kotlinx-coroutines-test | 1.11.0 | `runTest`, test dispatchers |
| compose ui-test-junit4 / ui-test-manifest | (BOM) | Compose UI tests (`junit4.v2` rules) |
| androidx.test core / runner / ext-junit | 1.7.0 / 1.7.0 / 1.3.0 | Instrumentation |
| androidx.test.espresso:espresso-core | 3.7.0 | Pinned: Compose ui-test pulls 3.5.0, which crashes on API 35+ |
| androidx.test:orchestrator | 1.6.1 | Isolated instrumented tests, cleared app data per test |
| com.google.dagger:hilt-android-testing | 2.60.1 | User-flow tests with fake data (`@HiltAndroidTest`, `@TestInstallIn`) |

Coverage uses AGP's built-in JaCoCo (`enableUnitTestCoverage` on `dev`), no extra plugin.

## Removed from the template

`androidx.appcompat`, `com.google.android.material` (View system, unused in a Compose app; the window theme now uses the platform `Theme.Material` parent). Fragments need only `FragmentActivity` (`BaseActivity`), not AppCompat.

Gson (used by the previous app) is not added: kotlinx.serialization with `@SerialName` produces the same request JSON.

## Deliberately not added yet

| Library | Reason | Add when |
|---|---|---|
| Room 2.8.5 | No local schema exists; a placeholder entity would invent one | Offline catalog / downloads / watch history is specified |
| MockK | Hand-written fakes cover current tests | Interaction-heavy code needs mocks |
| Turbine | Flow tests use `runTest` + StateFlow values | Complex multi-emission flows |
| Detekt | Latest stable (1.23.8) is built against Kotlin 2.0 and is not verified with Kotlin 2.4 / JDK 25; 2.0 is still alpha. Android Lint runs instead | Detekt 2.0 is stable, or 1.23.x is verified on this toolchain |
| material3-adaptive (direct) | Window size is read from `LocalWindowInfo` | List-detail / supporting-pane layouts are needed |
| Media3 / ExoPlayer | Playback requirements not provided | Player feature work starts |

Removed in Phase 7 (no caller left): Retrofit 3.0.0 + converter-kotlinx-serialization, okhttp logging-interceptor, mockwebserver3.
