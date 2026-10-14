# Testing strategy

| Level | Location | Runs on | What it covers |
|---|---|---|---|
| Unit | `app/src/test/` | JVM, every variant | ViewModels, domain rules, repositories (real DataStore on a temp file), design-system config, layout classification, variant config |
| Compose UI | `app/src/androidTest/` | Emulator/device, `dev` | Components, theme tokens, screens, adaptive grids, states |
| End-to-end | `app/src/androidTest/.../app/` | Emulator/device, `dev` | Real Hilt graph + DataStore: navigation, theme persistence, rotation |
| Variant | `BuildVariantConfigTest` + assemble tasks | JVM per variant | App IDs, server URLs, logging/debuggable, compilation of all variants |
| Static | Android Lint | Gradle | Lint rules on `dev` and `release` |

## Principles

- Test behavior through public APIs (ViewModel functions/state, composable semantics), not implementation details.
- Prefer fakes (`testing/FakeAppearanceRepository`) over mocks; prefer real DataStore over fakes for repository tests.
- Find UI nodes by text, role or semantics; use `testTag` only for containers (`HomeTestTags`). Never click coordinates.
- No placeholder tests. Each test fails for a real regression.
- Names: `givenCondition_whenAction_thenOutcome`, e.g. `givenInvalidInput_whenSubmitting_thenShowsValidationError`.

## Commands

Set `JAVA_HOME` first (JDK 25):

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
```

```bash
./gradlew testDevUnitTest testQaUnitTest testStageUnitTest testDemoUnitTest testReleaseUnitTest
```

```bash
./gradlew connectedDevAndroidTest
```

```bash
./gradlew lintDev lintRelease
```

```bash
./gradlew createDevUnitTestCoverageReport
```

Reports: `app/build/reports/tests/`, `app/build/reports/androidTests/connected/dev/`, `app/build/reports/lint-results-*.html`, `app/build/reports/coverage/`.

## Definition of done for a change

All variant unit tests, `connectedDevAndroidTest`, `lintDev lintRelease` and the five `assemble*` tasks pass — and the result was actually run, not assumed.

See also: [unit-testing.md](unit-testing.md), [compose-ui-testing.md](compose-ui-testing.md), [build-variant-testing.md](build-variant-testing.md).
