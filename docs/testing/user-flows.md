# User flows — manual checks and their automated tests

Each flow below is written as the steps a tester follows on a device, followed by the automated test that performs the same steps. Automated flows run on the real app (navigation, ViewModels, DataStore) with Home data from `FakeHomeRepository` and login answers from `FakeAuthRepository` (no network) and the splash minimum set to 0 ms (`TestSplashModule`).

Run all automated flows:

```bash
./gradlew connectedDevAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.vplayed.app.DashboardFlowTest
```

## 1. Launch → splash → Home

1. Install the dev build and open **Vplayed3.0 Dev**.
2. The splash shows the VPlayed logo on grey (light) / dark grey (dark) for **3 seconds**.
3. Home opens: hero banner (content-type chip such as **SERIES** — never a genre —, title artwork or title, description, **Watch now** and **+**), rows below, each drawn with its prototype card (live, channel, bulk purchase, videos, micro series, top 10, promos, music), bottom bar with **Home** selected (red icon).
4. Press Back: the app closes; the splash never reappears.

Automated: `DashboardFlowTest.givenAppLaunch_whenSplashFinishes_thenUserSeesHeroRowsAndHomeTab`, `SplashViewModelTest` (3 s minimum), `SplashScreenTest`.

## 2. First load shows a shimmer

1. Clear app data, enable a slow network (Developer options › Network speed, or emulator "Network: EDGE").
2. Open the app: after the splash, grey shimmering placeholders (hero block + card rows) show until data arrives.

Automated: `HomeScreenTest.givenFirstLoad_whenShown_thenShimmerPlaceholderIsVisible`, `HomeViewModelTest.givenFirstPageInFlight_whenOpened_thenShowsLoadingShimmerState`.

## 3. Scrolling loads more rows (pagination)

1. On Home, scroll down steadily.
2. Near the end, a shimmer row appears and new rows are added (dev has 19 rows over 7 pages: Live streaming, channel, Bulk purchase, Videos / Series, …, Promo Titles).
3. At the very end, no more shimmer appears.
4. Scroll a long row (e.g. **Videos / Series**) sideways to its end: a shimmer card appears and more titles are appended.

Automated: `DashboardFlowTest.givenHome_whenUserScrollsToTheBottom_thenNextPagesOfRowsLoad`, `HomeScreenTest.givenShortList_whenShown_thenNextPageIsRequestedAutomatically`, `HomeViewModelTest` (append, de-duplicate, end reached, one request at a time, row paging).

## 4. Offline and retry

1. Turn on airplane mode and clear app data. Open the app.
2. Home shows "You're offline. Check your connection and try again." with **Retry**.
3. Turn airplane mode off, tap **Retry**: Home loads.
4. With content shown, go offline and scroll to the bottom: the footer shows an error with **Retry**; tapping it after reconnecting loads the next rows.

Automated: `DashboardFlowTest.givenNoConnection_whenUserTapsRetry_thenHomeLoads`, `HomeScreenTest.givenNothingLoaded_whenUserTapsRetry_thenRetryIsRequested`, `HomeScreenTest.givenNextPageFailed_whenUserTapsRetry_thenNextPageIsRequestedAgain`, `DefaultHomeRepositoryTest` (network, 401, 500, failed envelope, malformed JSON).

## 5. Bottom navigation

1. Tap **Search**, **Promos**, **Subscription**: each tab opens (placeholder screens for now) and its icon turns red and filled.
2. Tap **Home**: Home returns at the same scroll position.

Automated: `DashboardFlowTest.givenHome_whenUserVisitsEveryTab_thenEachOpensAndHomeComesBack`.

## 6. Theme and colors

1. Tap the settings icon (top right) on Home → **Appearance**.
2. Choose **Dark**: black surfaces, white text. Choose **Light**: white surfaces, dark text. **System** follows the phone setting live.
3. Brand palette **Black & white** replaces the red accent with white (dark) / black (light).
4. Press back: Home uses the chosen theme; restart the app — the choice is kept.

Automated: `DashboardFlowTest.givenHome_whenUserChoosesLightThemeInAppearance_thenSelectionIsKeptAndBackReturnsHome`, `AppThemeTest`, `DataStoreAppearanceRepositoryTest`, `AppearanceSettingsViewModelTest`.

## 7. Rotation and tablets

1. Rotate the phone on Home: the screen does not reload, scroll position and data stay, the bottom bar becomes a side rail.
2. On a tablet (Medium_Tablet AVD) the rail is shown in both orientations and cards are larger.

Automated: `DashboardFlowTest.givenHome_whenUserRotatesToLandscape_thenSameScreenKeepsContent`, `AppLayoutInfoTest`.

## 8. Environments

Install `dev`, `qa`, `stage` and `demo` side by side (demo replaces release). Each shows its environment in the launcher name and loads from its server (`docs/build/build-variants.md`).

Automated: `BuildVariantConfigTest` in every variant's unit tests.

## 9. Side menu → Login

1. On Home, tap the menu button (top left), or swipe from the left edge on any tab. The side menu shows the logo and **Login**.
2. Tap **Login**: the menu closes and the sign-in screen opens (a Fragment inside the app's navigation). Phone: one card ("STEP 1 OF 3", "Sign in or create your account", disabled Google/Apple buttons, **Email ID**, **Continue**, terms, "Already have an account? Sign in"). Tablet or window ≥ 861dp wide (not a landscape phone): the marketing column ("Enjoy the Ultimate Streaming Experience", features, platforms) beside the card and **Continue as guest** instead of the sign-in line.
3. Tap **Continue** with the field empty: "Email is required". Type `user@example` and tap **Continue** (or leave the field): "Please enter a valid email address". Typing clears the error.
4. Enter a valid email and tap **Continue**: the button shows a spinner and ignores further taps; a Toast shows the server's message (e.g. code sent, device limit) or an error ("You're offline…"). The screen stays open — there is no OTP step yet.
5. Rotate, open the keyboard, switch light/dark: the card stays centered and scrolls above the keyboard.
6. Tap ✕, **Continue as guest** (tablet) or Back: the dashboard returns.

Automated: `DashboardFlowTest.givenHome_whenUserChoosesLoginInSideMenu_thenLoginScreenOpens`, `…givenLogin_whenUserTapsClose_thenDashboardIsBack`, `…givenLogin_whenUserSubmitsInvalidEmail_thenErrorShownAndNothingSent`, `…givenLogin_whenUserSubmitsValidEmail_thenOneLoginRequestIsSent`, `LoginScreenTest` (phone / landscape phone / tablet layouts, errors, loading, callbacks, disabled social buttons), `LoginViewModelTest` (validation, Toast messages, duplicate submits), `../kmp` `AuthRepositoryTest` and `EmailLoginControllerTest` (encrypted body, 200/202/203, API error, 4xx/5xx, malformed, offline, missing key, validation), `../kmp` `TransportCipherTest` (OpenSSL-compatible output). Toast rendering itself is checked manually.

## 11. Login → OTP → Dashboard

1. Menu → **Login**, enter a valid email, **Continue**. The OTP screen opens ("STEP 2 OF 3", "Verify it's you", "Enter the 6-digit code we sent to <email>"); a Login answer that is an error, an empty answer or HTTP 200 with an API error stays on Login with a Toast.
2. Six empty boxes, the number keyboard opens, **Verify & continue** is dimmed, "Resend" is dimmed with "in 1:00" counting down. Type digits: the next box is highlighted; Backspace deletes; paste "12-34 56" keeps the digits (max six); five digits keep Verify dimmed.
3. Enter the correct code and tap **Verify & continue** (the keyboard's Done key does the same): spinner, then "You're all set" with a progress bar for 3 s, then the Dashboard; the menu shows "Signed in as <email>" instead of Login. Later API calls carry the session.
4. Wrong or expired code: **no Toast** — the card shakes once, the boxes empty and focus returns to the first one, and a red line under them shows the server's message ("OTP has expired" …) or "Incorrect code. Try again."; typing removes the line. Offline or a server error shows the same inline line without the shake and keeps the digits.
5. After 60 s **Resend** turns red: tap it, the boxes clear and the countdown restarts (no message). A failed resend shows the inline error, keeps the digits and the countdown stays at zero.
6. **Back**, **Change** or the system Back return to Login with the email still typed; ✕ goes to the Dashboard.
7. Account at its device limit: the OTP screen opens on "Maximum devices reached" with the devices; **Sign out** on one device (or **Sign out from all devices**), then **Continue** (automatic when none is left): the code step (limit found on login) or the success step (limit found while verifying). Back on the device step returns to the code step (boxes cleared) or to Login.
8. Rotate / tablet: the same screen in the shared layout (tablet: marketing column beside the card).
9. Signed in, open the menu and tap **Sign out**: the entry reads "Signing out…" and ignores taps; when the server confirms (or no longer knows the token) the menu shows **Login** again and later API calls carry no session. Offline or a server error: a Toast ("You're offline…" / "We couldn't sign you out. Please try again.") and the account stays signed in.
10. Kill and relaunch while signed in: still signed in (the session is restored before the first request). A request answered with 401 signs the user out (the menu shows **Login**).

Automated: `DashboardFlowTest` (`…OneRequestIsSentAndOtpScreenOpens`, `…LoginRejectedByBackend…`, `…EntersCorrectCode_thenSuccessThenDashboardWithSignedInMenu`, `…WrongCode…`, `…TapsChange…`, `…TapsClose…`, `…AccountAtDeviceLimit…`), `OtpScreenTest` (phone / tablet layouts, countdown, resend, typing, paste, inline error, device and success steps), `OtpViewModelTest` (verify, wrong/expired codes, offline, duplicate taps, resend, 60 s countdown in virtual time, device sign-out and continue paths, success → Dashboard), `LoginViewModelTest`, `../kmp` `OtpControllerTest` and `AuthRepositoryTest` (every status mapping and flow rule, encrypted bodies), `KeystoreSecureStoreTest`, `DashboardFlowTest.givenSignedIn_whenUserTapsSignOutInSideMenu_thenServerIsToldAndMenuShowsLogin`, `AppShellViewModelTest` (sign-out outcomes, duplicate taps), `../kmp` `SessionManagerTest` / `SessionGraphTest` (restore, 401 rule, logout). Real-backend sign-in needs a test email and its code: not automated.

## 10. Geo-restriction refresh (IP and geo token)

1. Install a dev build (HTTP logging on) and open it online. Logcat (tag `API`) shows the IP lookup and one `GET <redacted> -> 200` (the geo-data request, whose path holds the IP) once. Bodies and headers are never logged.
2. Later backend calls (Home pages, login) carry `x-bearer-token` and `device_id`.
3. Turn airplane mode on, then off: one new pair of requests after reconnecting. Toggle Wi-Fi ↔ mobile data: one new pair. Leaving the connection unchanged triggers nothing.
4. With airplane mode on at launch, the app still opens normally (no crash, Home shows the offline state).

Automated: `GeoRestrictionSyncTest` (start online, offline → online, network switch, repeated status, flapping, single start), `GeoWorkDecisionTest`, `SharedHeadersTest` (no null/empty geo header, latest token, `@WithoutGeoToken`, `device_id`), `SharedHeadersParityTest`; in `../kmp`, `GeoRepositoryTest` (success, IP failure, invalid IP, failed geo request keeps state, missing token, 404 not retried, undecodable token, concurrent refreshes never overlap, geo-data path, no app headers to ipify, no old token on geo-data) and `GeoTokenDecoderTest`.
