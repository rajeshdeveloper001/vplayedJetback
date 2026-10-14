# Compose UI testing

Location: `app/src/androidTest/java/com/vplayed/…`. Runs on the `dev` build type (`testBuildType = "dev"`) with AndroidX Test Orchestrator, which clears app data (DataStore) before every test.

Use the **v2** rules (`androidx.compose.ui.test.junit4.v2.createComposeRule` / `createAndroidComposeRule`); the v1 rules are deprecated.

## Current suites

| Test | Covers |
|---|---|
| `core/designsystem/AppComponentsTest` | Button click/disabled/loading semantics, text-field error semantics, error retry, empty state, loading description, dialog |
| `core/designsystem/AppThemeTest` | Light/dark schemes, forced modes, palette switch, Inter font, text scale, OTT headers on expanded windows, compact density |
| `feature/home/HomeScreenTest` | Initial render, validation error, Loading/Empty/Error/Content states, 2-column grid on tablet width vs stacked on phone width |
| `feature/splashscreen/SplashScreenTest` | Logo shown, stays until ready, finishes exactly once |
| `feature/dashboard/home/HomeScreenTest` | Shimmer, full-screen error + Retry, empty, hero (chip, Watch now, +), top-10 semantics, auto-pagination, paging retry, paging shimmer |
| `app/DashboardFlowTest` | **User flows** on the real app with fake Home and auth data: launch → Home, scroll pagination, all tabs, offline → Retry, Appearance → Light → back, rotation, side menu → Login, close, invalid / valid email (see `user-flows.md`) |
| `feature/auth/OtpScreenTest` | Phone / tablet OTP layouts, countdown text and dimmed Resend, typing and paste through the real ViewModel (digits only, six max), Verify enabled only with six digits, device step rows and actions, success step |
| `feature/auth/LoginScreenTest` | Phone / landscape-phone / tablet layouts (`DeviceConfigurationOverride.ForcedSize`), empty and invalid email errors, loading ignores taps, Continue / close / guest callbacks, disabled social buttons |

## Patterns

**Component / screen in isolation** — always wrap in `AppTheme`; drive stateless screens with a real ViewModel when it has no Android dependencies:

```kotlin
@get:Rule val composeRule = createComposeRule()

composeRule.setContent { AppTheme { AppButton(text = "Play", onClick = { clicks++ }) } }
composeRule.onNodeWithText("Play").performClick()
```

**Adaptive layouts** — force a window size instead of relying on the device:

```kotlin
composeRule.setContent {
    DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(1280.dp, 800.dp))) { Home() }
}
```

**Lazy grids** — scroll before asserting: `onNodeWithTag(HomeTestTags.GRID).performScrollToNode(hasText("Submit"))`.

**Infinite animations** (progress indicators) — `composeRule.mainClock.autoAdvance = false` then `advanceTimeBy(…)`.

**Full app** — `@HiltAndroidTest` + `createEmptyComposeRule()` + `ActivityScenario.launch(MainActivity::class.java)`, so the fake can be configured (`@Inject FakeHomeRepository`) before the app starts. `TestModules.kt` (`@TestInstallIn`) swaps the Home repository and the splash duration for every instrumented test. Navigation items: `onNode(hasText("Appearance") and hasClickAction())`. Rotation: set `activity.requestedOrientation`, reset it in `@After`.

## Commands

```bash
./gradlew connectedDevAndroidTest
```

```bash
./gradlew connectedDevAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.vplayed.feature.home.HomeScreenTest
```

With several devices attached, select one with `ANDROID_SERIAL=emulator-5554`. Recommended matrix: a phone (Pixel 10 Pro AVD) and a tablet (Medium_Tablet AVD).
