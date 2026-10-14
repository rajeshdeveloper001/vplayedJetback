# Build-variant testing

Variants: `dev`, `qa`, `stage`, `demo`, `release` (`debug` is disabled). See [../build/build-variants.md](../build/build-variants.md).

## What is verified and how

| Check | Mechanism |
|---|---|
| Every variant compiles and packages | `assemble<Variant>` for all five |
| Application ID and suffix | `BuildVariantConfigTest` (per variant) + `output-metadata.json` |
| Server URL per environment | `BuildVariantConfigTest` |
| No dev/QA/stage/demo host, logging or debuggable in production | `BuildVariantConfigTest.givenProductionEnvironment_…` |
| HTTPS + trailing slash for every URL; production URL distinct | Configuration-time `require` in `app/build.gradle.kts` |
| Lint for debuggable and minified builds | `lintDev`, `lintRelease` |
| R8 does not break release-like builds | `assembleStage/Demo/Release` (R8 runs) |

## Commands

```bash
./gradlew testDevUnitTest testQaUnitTest testStageUnitTest testDemoUnitTest testReleaseUnitTest
```

```bash
./gradlew assembleDev assembleQa assembleStage assembleDemo assembleRelease
```

```bash
grep -h '"applicationId"' app/build/outputs/apk/*/output-metadata.json
```

Expected:

```
"applicationId": "com.vplayed.dev",
"applicationId": "com.vplayed.qa",
"applicationId": "com.vplayed.stage",
"applicationId": "com.vplayed",      (demo)
"applicationId": "com.vplayed",      (release)
```

## CI notes

- Use JDK 25 (Gradle daemon toolchain).
- Pass real URLs with `-Pvplayed.serverUrl.<id>=…` if they differ from the defaults; pass signing via `VPLAYED_KEYSTORE_*` env vars.
- Run unit tests for all variants — a test passing in `dev` says nothing about `release`'s `BuildConfig`.
