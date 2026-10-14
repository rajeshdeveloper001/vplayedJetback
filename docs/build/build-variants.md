# Build variants

The app uses **build types only** (no product flavors). Each build type is bound to one backend environment.

| Build type | Application ID | `SERVER_URL` | `ENVIRONMENT` | Launcher name | Debuggable | R8 | HTTP logging | Signing |
|---|---|---|---|---|---|---|---|---|
| `dev` (default in Studio) | `com.vplayed.dev` | `https://api-devgs.vplayed.com/` | `dev` | Vplayed3.0 Dev | yes | no | BODY | debug key |
| `qa` | `com.vplayed.qa` | `https://api-qags.vplayed.com/` | `qa` | Vplayed3.0 QA | yes | no | BODY | debug key |
| `stage` | `com.vplayed.stage` | `https://api-stagings.vplayed.com/` | `stage` | Vplayed3.0 Stage | no | yes | off | release key* |
| `demo` | `com.vplayed` | `https://api-demogs.vplayed.com/` | `demo` | Vplayed3.0 Demo | no | yes | off | release key* |
| `release` | `com.vplayed` | `https://api.example.com/` (placeholder) | `production` | Vplayed3.0 | no | yes | off | release key* |

\* Unsigned when no release keystore is configured.

`debug` is created by AGP and cannot be deleted, so its variant is **disabled** (`androidComponents.beforeVariants`): no `assembleDebug` task, and it doesn't appear in Android Studio's Build Variants panel. `demo` and `release` share the base application ID and therefore replace each other on a device.

## Where configuration lives

- `app/build.gradle.kts` → `BackendEnvironment` definitions (`development`, `qa`, `staging`, `demo`, `production`) → `buildConfigField`s `ENVIRONMENT`, `SERVER_URL`, `IMAGE_BASE_URL`, `LOGGING_ENABLED` and the `app_name` resValue.
- Image buckets: dev `https://gsvp-dev.s3.ap-south-1.amazonaws.com/`, QA and stage `https://gsvp-qa.s3.ap-south-1.amazonaws.com/` (as in the previous app), demo `https://gsvp-demo.s3.ap-south-1.amazonaws.com/`, release placeholder. Override with `-Pvplayed.imageBaseUrl.<id>=…`.
- `core/common/config/AppConfig` is the only reader of `BuildConfig`; inject `AppConfig` everywhere else.
- Override a URL without editing code: `-Pvplayed.serverUrl.<id>=https://…/` (ids: `dev`, `qa`, `stage`, `demo`, `production`) or set it in `~/.gradle/gradle.properties`.

## Safety guards

- Configuration fails if any server URL is not `https://…/`.
- Configuration fails if the production URL equals any non-production URL.
- `BuildVariantConfigTest` runs in **every** variant's unit tests and asserts the application ID, environment, server URL, logging and debuggable flags of that variant, plus "no non-production host and no logging in production".
- `release` currently uses a placeholder URL — set the real one before shipping (see `docs/decisions.md`).

## Release signing

Create `keystore.properties` at the project root (git-ignored):

```properties
storeFile=../keys/vplayed-release.jks
storePassword=…
keyAlias=…
keyPassword=…
```

or on CI set `VPLAYED_KEYSTORE_FILE`, `VPLAYED_KEYSTORE_PASSWORD`, `VPLAYED_KEY_ALIAS`, `VPLAYED_KEY_PASSWORD`. `stage`, `demo` and `release` use it when present.

## Commands

All commands need JDK 25:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
```

```bash
./gradlew assembleDev assembleQa assembleStage assembleDemo assembleRelease
```

```bash
./gradlew installDev
```

```bash
./gradlew bundleRelease
```

Outputs: `app/build/outputs/apk/<buildType>/`. The application ID of each built APK is in `app/build/outputs/apk/<buildType>/output-metadata.json`.

## Adding an environment

1. Add a `BackendEnvironment` and a `create("<name>") { … environment(<env>) }` block in `app/build.gradle.kts` (use `matchingFallbacks` for future library modules).
2. Add the id to `AppEnvironment`.
3. Add expectations to `BuildVariantConfigTest`.
4. Add `"<name>Implementation"(libs.androidx.compose.ui.tooling)` if it is debuggable (previews).
5. Update this document and `CLAUDE.md`.
