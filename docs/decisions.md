# Open decisions and assumptions

## Needs a decision

1. **Production URLs.** `release` uses placeholders for the server (`https://api.example.com/`) and image bucket (`https://images.example.com/`). Provide the real values (`production` in `app/build.gradle.kts`, or `-Pvplayed.serverUrl.production=…` / `-Pvplayed.imageBaseUrl.production=…`). Also confirm whether `release` should exist alongside `demo` — both use `com.vplayed`.
2. **Hero and card actions.** "Watch now", "+" (watchlist) and card taps are not wired yet — detail, player and watchlist screens and their APIs are still to come (`HomeActions.onWatchNow` / `onAddToList`).
3. **Search, Promos, Subscription tabs** show placeholders until their APIs and designs are shared. The prototype's mobile bar is Search | V | Profile with a Movies/Series/Live quick-nav capsule; the app uses the requested Home/Search/Promos/Subscription tabs styled like the prototype.
4. **"See all".** Row headers are not clickable yet (no list screen); the previous app opened a see-all page for rows with more than 2 items.
5. **Live status.** `streaming_status` is read as 1 = live (LIVE tag, "Started …", Watch Live), 0 = scheduled (UPCOMING, "Starts …" / "Tomorrow · …", Set reminder), anything else = finished (no tag, "Streamed <date>", Watch now). Confirm with the backend.
6. **Launcher display names.** Currently "Vplayed3.0", with " Dev" / " QA" / " Stage" / " Demo" suffixes. Confirm the product name.
7. **Stage logging.** Off, to mirror production. Switch `staging.loggingEnabled` if testers need HTTP logs.
8. **Dynamic color default.** Off (brand colors win). Change `BrandConfig.dynamicColorByDefault` if wanted.
9. **Static analysis.** Detekt not added (see `dependencies.md`); Android Lint is the gate for now.
10. **App icon.** Template launcher icon is unchanged.
11. **Geo token on public requests.** `x-bearer-token` is sent on every backend request once a geo token is known (product decision 2026-10-13). The previous app sent it only with `Authorization` (logged-in users), so login and Home calls never carried it — confirm the backend accepts it there.
12. **Email-login outcomes.** 200 / 203 (code sent) open the OTP screen; 202 (device limit) opens it on the device step when the answer lists devices (otherwise a Toast). A 2xx with `error: true`, an empty or malformed body, or any other status never opens it. Confirm the messages and the exact 202 device list (`docs/api/auth-api.md`).
14. **Device-limit "Continue" skips the code.** After signing devices out, the previous app logs in with `verifyEmailLogin` (email + `is_email_verified=1`, no code) and this app copies that, as decided. If the server does not tie it to an earlier verified code, it is an authentication bypass for accounts at their device limit — please have the backend confirm or require a code/token (`docs/api/auth-api.md`, open questions).
15. **Session scope.** The session (tokens, default profile) is stored encrypted and sent as `Authorization` / `profile_id`. There is no token refresh on 401 and no logout yet (the menu shows "Signed in as …" instead of Login), no registration screen after a 203 and no profile selection; new users and multi-profile accounts get a Toast and the default profile. Home is not reloaded after sign-in.
16. **Resend countdown** is 60 s (previous app); the prototype uses 30 s. Neither limits the number of resends; the server's answer (e.g. a rate limit) is shown as a Toast.
17. **203 at verification.** With tokens it signs the user in as a new user; without them it is "register first" (inline message). The previous app stored nothing at 203 and opened registration; confirm which one the server means.
18. **Status codes decide, as in the previous app.** Verified against the previous app's source: it branches on the HTTP status (200 / 202 / 203) and never reads the body's `error` flag for them. This app does the same for 202 and 203, and additionally refuses a **200** whose body says `error: true` (a wrong or expired code can never be "success"). If the backend sets `error: true` on a 200 that really means success, tell us.
13. **Request-encryption key.** `SALT_KEY` (`vplayed.saltKey`) is read from the git-ignored `secrets.properties`, a Gradle property or `VPLAYED_SALT_KEY`; set it on CI. It is compiled into the APK (as in the previous app), so it is extractable — the backend should not treat it as a secret. The previous app's `USE_SECURE_CRYPTO` (AES-GCM) path is not ported because the backend does not support it yet.

## Login vs prototype — remaining differences

Login follows the prototype page "VPlayed Sign Up" (email step; the Mobile page below 861dp), mapping in `docs/design-system/design-system-guide.md` › Login.

- **Fragment inside the Compose NavHost.** Login is a `LoginFragment` (`BaseFragment`) embedded with `AndroidFragment` (decision 2026-10-13), not a separate activity; Navigation Compose remains the only navigation library.
- **Social sign-in** (Google, Apple) is shown but disabled; labels use the prototype's "Sign in" mode. **Continue as guest** (wide layout) closes Login. "Already have an account? **Sign in**" (phone layout) and the **Terms of use** / **privacy policy** words are styled but not links (no targets yet).
- **Step label** "STEP 1 OF 3" is kept although steps 2–3 (OTP, success) are not implemented.
- **Empty email** shows "Email is required" (previous app); the prototype only has "Please enter a valid email address", used for malformed input. Validation uses the shared `EmailValidator` (previous app's rule), slightly stricter than the prototype's `x@y.z` pattern.
- **Platforms row** shows platform names as text; the prototype's SVG wordmarks (Android TV, Apple TV, Samsung, Fire TV, LG webOS) are not reproduced.
- **No `AppScreenScaffold`** on Login: the prototype has no top bar (close button inside the card); the screen applies `safeDrawing` insets itself.
- **Blur** (`filter: blur(34px)` on the card's ambient glow, `backdrop-filter`) is approximated with soft radial gradients (blur is API 31+ only).
- **Light theme**: the prototype keeps its dark overlay on the light backdrop (looks unintended); the app uses the light canvas with brand glows. The light focus ring uses neutral-950 at 8% because the prototype's ring is invisible on white; `formAccent` uses the primary red on light for contrast.
- **Landscape phones** (height < 480dp) keep the single card although wider than 860dp: the two-column modal needs 680dp of height.
- **Debug HTTP logs** (dev/qa only) show request URLs, so the geo-data URL reveals the device's public IP in logcat; bodies of login and geo-data calls and the `x-bearer-token` value are never logged.
- **Step label tracking** uses the `badge` role (0.14em) instead of the prototype's 0.12em; the feature "4k" icon is the outlined symbol (prototype: filled).

## OTP vs prototype — remaining differences

The OTP, device-limit and success steps follow the prototype page "VPlayed Sign Up" (steps 2–3 and `data-step="devices"`; the Mobile variant on compact windows), mapping in `docs/design-system/design-system-guide.md` › OTP. The prototype is a web page with no API calls, so these web → app adaptations were made:

- **One text field instead of six inputs.** The six boxes are drawn from one invisible field, so typing, Backspace, paste (digits only, first six), SMS autofill and the number keyboard work as on Android; focus "moves" by highlighting the next box. There is no per-box arrow-key handling. The keyboard opens when the step is shown (the prototype focuses the first box).
- **Verify only with six digits**, as the prototype; Enter/IME "Done" verifies a complete code.
- **A refused code is shown inline, never as a Toast** — the prototype's own pattern for an incorrect secret code (Manage Profiles "Incorrect PIN. Try again."; the Sign Up page has no error state): a red 12px semibold line under the boxes, one 0.4 s horizontal shake of the card (`wpShake`: 0, −8, +7, −4, +2, 0), the boxes emptied and focused again, no red box borders. The text is the server's message when it is short plain text (e.g. "OTP has expired"), else "Incorrect code. Try again." (the PIN text adapted to a code). The line goes away on the next edit or action. Other failures on this screen (offline, server error, a failed resend or device sign-out, "register first") use the same inline line without the shake and keep the typed digits; a successful resend shows no message (the restarted countdown and emptied boxes are the feedback). Error color is the screen's `formAccent` (prototype `danger-300` `#FF6B81` here; `#FF2E4E` for the Login field error), consistent with the Login card.
- **No Toasts on the OTP screen.** The "profile setup / choice is coming soon" hint for new users and multi-profile accounts is a line on the success step instead.
- **Back is a round icon button** (the prototype's ≤ 900px variant) on every width, with a 48dp touch target; system Back does the same as the in-screen Back, and "Change" is on its own line (also the ≤ 900px layout). No hover states, sheen or lift on buttons (touch).
- **Countdown** shows "Resend  in 1:00" (60 s, decision 16); Resend is dimmed until it ends, while a request is in flight and while verifying. It restarts only after a successful resend. It is stopped with the screen (it lives in the ViewModel).
- **Resend** also clears the boxes (a new code replaces the old one) — the previous app did the same.
- **Success step** shows the prototype's progress bar for 3 s, then opens the Dashboard (the Mobile page's target); the prototype's desktop pages go to "Complete Profile", which is not built. Step transitions are not animated (prototype: 0.4 s fade/slide).
- **Device rows** show icon, name and "This device · browser" (prototype: browser · "2h ago"; there is no timestamp in the data used). The primary button reads "Sign out from all devices" until a device was signed out, then "Continue" (previous app's flow); with no devices left the flow continues by itself (prototype).
- **Colors** reuse `extendedColors.auth`; the success green is the theme's `success` status color (prototype `#22C55E` on dark, `#15803D` on light for contrast). Box text is a fixed 24sp (prototype: 20–24px by width).
- **Boxes** are at most 56dp (46dp on windows up to 400dp wide, like the prototype) with 10dp / 7dp gaps; the prototype switches at the same 400px viewport width.

## Home vs prototype — remaining differences

Home follows the prototype's mobile Home component by component (`docs/design-system/design-system-guide.md` › Home), verified on a 427dp-wide emulator against captures of the prototype. These differences remain, because the API has no matching data or the prototype behaviour does not apply to touch:

- **Hero video, mute and pause controls** — the prototype plays a promo video in the first hero slide; the banner API returns images only, so the controls are omitted.
- **Hero title artwork has a black box** — the CMS title images for "Captain America: Brave New World" (`title_images` PNG/WebP) are opaque RGB with a black background; the prototype's artwork is transparent. Fix in the CMS (upload transparent PNGs). The prototype's drop shadow on title artwork is not drawn.
- **Rating badge, hover overlays, language chips, age/year meta** — hidden on mobile in the prototype or not in the API; not shown.
- **"Best Videos For You" portrait cards** — the prototype shows one editorial movie row as `.pcard`; the API has no per-row layout flag, so all video/series rows use `.hcard` (the prototype's default card). Micro series use `.pcard`.
- **Bulk-purchase card** — the API has no icon colour or variant; the variant comes from the stock banner file name (`blue.png` → blue, `yellow.png` → gold, otherwise violet; `dark-red.png` has no prototype variant). The icon is always the prototype's shopping bag (the API's `banner_badge` SVGs are not used). The prototype's "From …" prefix and "N titles" count do not apply to a single bundle; when there is no discount the sub line shows the validity ("Valid for 7 days").
- **Live card logos** — the API has no channel logo for live streams; the well shows the channel initials (as the prototype does for its sample channels).
- **Channel tile "Live now" pill** — no channel live flag in the API; the follower count pill is always shown.
- **Music play button** — revealed on hover in the prototype; not shown on touch.
- **Wide windows** — the prototype was measured at phone width; on tablets/landscape cards use the larger widths (`landscapeCardWidthLarge`, `portraitCardWidthLarge`), the bulk card is capped at 420dp and the hero at 72% of the window height.
- **Light theme** — the prototype light theme was not captured section by section; the light variants follow its CSS (`body.light`) where it exists (hero scrim, bulk-purchase card, top-10 stroke) and theme tokens elsewhere.
- **Top bar** — the app keeps its logo top bar (with the Appearance shortcut) above the hero; the prototype's mobile chrome (Search | V | Profile) is decision 3.

## Security issues found (please forward)

- **Public home API leaks personal data.** Channel items in `GET mediaview/api/v1/home` include studio owners' Aadhaar/PAN images and names, bank account and IFSC numbers, phone, email, address and `access_otp_token`. The app ignores them; the backend should stop returning them.
- **The previous app's repository contains secrets** in `app/build.gradle.kts` (e.g. a Google OAuth client secret `GOCSPX-…`, payment merchant id). None were copied here; rotate them and move them out of source control.

## Assumptions made

- Build types (not flavors) as requested; `debug` disabled; `dev` is the default and instrumented-test build type.
- `dev` and `qa` are signed with the local debug key; `stage`/`demo`/`release` use the release key when configured, otherwise are produced unsigned.
- Inter is the only font (bundled static weights, OFL licensed; the license must ship in the app's open-source notices).
- The activity handles configuration changes itself (rotation, resize, uiMode, density) so future video playback isn't interrupted; all state also survives recreation (ViewModels, `rememberSaveable`).
- Header text roles scale up on wider windows (×1.15 medium, ×1.3 expanded); body/card roles do not.
- Splash colors (`#DDDDDD` light / `#323232` dark) and the logo vectors are taken from the previous VplayedApp project. The splash follows the **system** light/dark setting (like the system splash); the in-app theme override applies from Home onward.
- The splash stays at least 3 seconds (requested); startup work runs in parallel. Tests use 0 ms via `TestSplashModule`.
- Item genre on cards: `default_genre` resolved against the item's `genre` list (the CMS sometimes stores it lowercase); counts and durations formatted like the prototype ("2h 08m", "2m 28s", "4.2M").
- Bundle price: `discounted_price` is the amount taken off (`2080 − 208`, 10%); a discount shows only when it is above 0.
- Visual design follows the prototype's mobile Home; logic follows the previous app's `ShocialFragment` (page-by-page rows, empty rows hidden, per-row paging), but rows load as the user scrolls instead of all pages at once.
- Stage images come from the QA bucket, as in the previous app.
- The two palettes are "Vplayed" (brand red on black/white) and "Black & white" (no red).
- Home APIs, email login with OTP verification, the device-limit step, the stored session and the geo-restriction refresh are integrated. Token refresh, logout, registration, profile selection, subscription, playback and offline storage (Room) await requirements.
- Geo-restriction state (IP, token, currency) is in memory only and refreshed on app start, reconnect and network switch, as the previous app kept it in memory. Its `KEY_NETWORK_CHANGED` broadcast had no receivers and is not reproduced.
- `device_id` = `Settings.Secure.ANDROID_ID` on every backend request, as in the previous app (needed for device limits).
