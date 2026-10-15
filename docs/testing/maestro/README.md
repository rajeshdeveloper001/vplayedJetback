# Maestro Mobile UI Automation Suite

Automated end-to-end and input validation test flows for VPlayed 3.0 Android using [Maestro](https://maestro.mobile.dev/).

---

## 1. Test Files Overview

| File | Purpose |
|---|---|
| [`vplayed-e2e-suite.yaml`](vplayed-e2e-suite.yaml) | Complete all-in-one test suite covering splash, email input validation, OTP lifecycle, tabs navigation, and appearance settings. |
| [`login-input-validation-flow.yaml`](login-input-validation-flow.yaml) | Tests email input validation boundaries: empty submission, malformed patterns (`user@`, `user@example`, `user@example.c`), and valid input clearing. |
| [`otp-verification-flow.yaml`](otp-verification-flow.yaml) | Tests OTP 6-digit input length constraint, incomplete input, resend timer, and navigation back via Change. |
| [`dashboard-navigation-flow.yaml`](dashboard-navigation-flow.yaml) | Tests bottom bar tabs (`Home`, `Search`, `Promos`, `Subscription`), Appearance settings, and vertical scrolling. |

---

## 2. Prerequisites

1. Install Maestro CLI:
   ```bash
   curl -FsSL "https://get.maestro.mobile.dev" | bash
   ```
2. Start your Android Emulator or connect a physical device via ADB:
   ```bash
   adb devices
   ```
3. Assemble and install the `dev` variant:
   ```bash
   ./gradlew installDevDebug
   # Or using Android Studio Run 'app'
   ```

---

## 3. Running with Maestro Studio

Open Maestro Studio in your browser:
```bash
maestro studio
```
Then select any flow from `docs/testing/maestro/` to run interactively.

---

## 4. Running via Maestro CLI

```bash
# Run the complete end-to-end suite
maestro test docs/testing/maestro/vplayed-e2e-suite.yaml

# Run individual flows
maestro test docs/testing/maestro/login-input-validation-flow.yaml
maestro test docs/testing/maestro/otp-verification-flow.yaml
maestro test docs/testing/maestro/dashboard-navigation-flow.yaml
```
