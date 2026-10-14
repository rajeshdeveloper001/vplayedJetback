# Email login, OTP, session and geo-restriction APIs

Contracts taken from the previous app (`VplayedApp`: `GudShoServices`, `LoginNewBottomSheet`, `PhoneOTPBaseView`, `LogoutUsersFragment`, `NetworkChangeWorker`, `SessionRequestInterceptor`). The HTML prototype ("VPlayed Sign Up") defines the **screens** only — it contains no network calls. Nothing here was invented; open questions are listed at the end.

All paths are relative to the environment's server URL (`AppConfig.serverUrl`). Login calls are made by the KMP shared layer (`../kmp`, `auth/DefaultAuthRepository`) with `RequestOptions.Sensitive` (request/response bodies are never logged); the geo calls are Retrofit methods marked `@RedactBody` / `@WithoutGeoToken`.

## Flow

```
Login (email) ── email-login ──► 200/203 ──► OTP screen, code step
                              └► 202 (device limit) ──► OTP screen, device step
OTP code step ── verifyEmailOtp ──► 200 ──► session stored ──► success step (3 s) ──► Dashboard
                  │ resend: email-login again (countdown 60 s)
                  └► 202 ──► device step ── logout-device (one / all) ──► Continue ──► verifyEmailLogin ──► 200 ──► success
OTP device step (started from email-login's 202) ── logout-device ──► Continue ──► email-login ──► code step
```

## Common headers (`core/network/ApiHeadersInterceptor`)

Every request to the VPlayed backend carries `Accept: application/json`, `X-REQUEST-TYPE: mobile`, `app_version`, `device_type: android`, `X-LANGUAGE-CODE` and:

| Header | Value | When |
|---|---|---|
| `device_id` | `Settings.Secure.ANDROID_ID` | Whenever the platform provides it (previous app: always) |
| `x-bearer-token` | Geo token from `GeoRestrictionStore` | Whenever a token is known, except on requests marked `@WithoutGeoToken` (the geo-data request itself). Never sent empty or null. |
| `Authorization` | `Bearer <access_token>` | When a session is stored |
| `profile_id` | Default profile id | When a session is stored and it has a profile id |

The previous app sent `x-bearer-token` only together with `Authorization`; this app sends it whenever available (product decision, 2026-10-13). Third-party calls (ipify) use a separate client with none of these headers. `Authorization` and `x-bearer-token` are redacted in HTTP logs.

## Request encryption

`email-login` and `verifyEmailOtp` bodies are `{"data": "<cipher>"}` where `<cipher>` = the shared `TransportCipher.encrypt(SALT_KEY, <plain JSON>)`: OpenSSL/CryptoJS passphrase AES (EVP_BytesToKey MD5, AES-256-CBC, `Base64("Salted__" + salt + ciphertext)`), the previous app's legacy `AESEncryption` path, encrypted **once**. `SALT_KEY` comes from `secrets.properties` (`vplayed.saltKey`, git-ignored), `-Pvplayed.saltKey` or `VPLAYED_SALT_KEY`; without it no request is sent and the user sees a generic error. `verifyEmailLogin` and `logout-device` are plain form posts (as in the previous app).

## Public IP — `GET https://api.ipify.org/?format=json`

Response `{"ip": "203.0.113.10"}`. Accepted only when the value is an IPv4/IPv6 literal.

## Geo access key — `GET mediaview/api/v1/common/geo-data/{ip}`

`{ip}` is a path segment (encoded = true). Response envelope `{"error", "message", "response": {"geo_token": "…"}}`; only `response.geo_token` is read. The token is Base64 of JSON such as `{"currency": "USD"}`; only `currency` is read. Missing token or bad Base64/JSON never crashes and never clears the last valid values.

## Send / resend the code — `POST users/api/v1/auth/email-login`

Plain JSON `{"email": "<trimmed email>"}`. Resend is the same call. Response envelope (`UserResponse`/`ResponseStatus`): `error: Boolean`, `statusCode: Int`, `status: String`, `message: String`, `response: User` (read only for `get_devices` / `device_limit` on 202).

The status in these tables is the one that decides (decision D1, shared with iOS since the login moved
to the KMP shared layer): a non-2xx HTTP status always fails; within 2xx the envelope's `statusCode`
decides when present, else the HTTP status. The previous app read the HTTP status only.

| Status | Meaning (previous app) | This app |
|---|---|---|
| 200 | Existing user, code sent | `CodeSent(isNewUser = false)` → OTP screen |
| 203 | New user, code sent | `CodeSent(isNewUser = true)` → OTP screen. Backend (2026-10-14): 203 means a new user who must register; registration is out of scope for now, so the current handling stays |
| 202 | Device limit reached (`response.get_devices`, `response.device_limit`) | `DeviceLimitReached(devices)`: OTP screen on the device step (no devices in the answer → Toast only) |
| 200 with `error: true` | — | `Rejected` → Toast on Login, no navigation (a 202/203 is decided by its status alone, as in the previous app) |
| other 2xx, empty or malformed body | — | Failure (never "OTP sent") |
| 4xx/5xx | Snackbar with `message` from the error body | `Rejected(message)` when the error body has a message, else generic error |
| Network / timeout | "No internet" | "You're offline…" |

## Verify the code — `POST users/api/v1/auth/verifyEmailOtp`

Plain JSON `{"email": "<trimmed>", "otp": "<6 digits>", "deviceDetail": "<device name>"}` (encrypted as above). `deviceDetail` = `Settings.Global.DEVICE_NAME`, else `"<BRAND> <DEVICE>"`. Response: `UserResponse` whose `response` is the signed-in `User`.

| HTTP | This app |
|---|---|
| 200 with `error != true` and `response.access_token` | **Signed in**: `UserSession` stored (see below) → success step → Dashboard |
| 200 / 203 without a usable `access_token` | 200: failure (not a login). 203: `RegistrationRequired` → Toast (registration screen not built) |
| 203 with `access_token` | Signed in as a new user (assumption: the previous app stored nothing at 203 and went to registration; confirm) |
| 202 | `DeviceLimitReached` → device step |
| 200 with `error: true` | `Rejected(message)` → inline error, shake, boxes emptied (no Toast) |
| 203 with `error: true` and no session | `RegistrationRequired` (status decides, as in the previous app) |
| 4xx/5xx with a `message` | `Rejected(message)` → the server's text inline (invalid vs expired codes are only distinguishable by that text) |
| 401 without a message | `Unauthorized`; other 4xx/5xx without a message → server error |
| Network / timeout / malformed JSON | Failure → inline "You're offline…" / generic error; the typed code is kept |

Signed-in flags: `isNewUser` = 203 or `isNewUsers` or empty `first_name`; `hasMultipleProfiles` = `profile_count > 1`. Both only add a Toast ("profile setup / selection is coming soon"): the previous app's registration and profile-selection screens do not exist yet, and the default profile is used.

## Device limit

Rows come from `response.get_devices[]` (`device_name`, `device_type`, `browser_name`, `current_device`, plus the credentials `username` / `access_token` of that device). Credentials stay in memory inside `SignedInDevice.signOutKey` (masked in logs), and travel from Login to the OTP screen through `DeviceLimitHandoff`, never through navigation arguments.

- **Sign out** — `POST users/api/v1/auth/logout-device` (form): `username`, `access_token` (that device's), `preference` = `"1"` (one device) or `"all"` (every device; the previous app sends the first row's credentials). Success only when HTTP 2xx and body `error == false` (the previous app's rule); otherwise `Rejected(message)`.
- **Continue after a sign-out** (limit found while verifying) — `POST users/api/v1/auth/verifyEmailLogin` (form, not encrypted): `is_email_verified=1`, `email`, `deviceDetail`, `browsername=""`. Same response handling as verify. **This call carries no code**, exactly as in the previous app; see the open questions.
- **Continue after a sign-out** (limit found by `email-login`) — the email-login request is sent again; its answer decides (code sent → code step).
- The prototype's behaviours are kept: with no devices left the flow continues by itself; Back on the device step returns to the code step (boxes cleared) when it came from verify, or to Login when it came from `email-login`.

## Session (`core/session`)

On a verified login the repository stores `UserSession(userId = _id, email, displayName = first_name, accessToken = access_token, refreshToken = refresh_token, profileId = default_profile._id)`:

- one JSON value, encrypted with an Android Keystore AES-256-GCM key, in its own DataStore file (`session.preferences_pb`), excluded from cloud backup and device transfer;
- unreadable or foreign data means "signed out" and is cleared; if the Keystore is unavailable the session lives in memory for that run only (never stored in plain text);
- read before the first request (the splash waits for it), then sent as `Authorization` / `profile_id`.

Not implemented yet (the previous app has them): refreshing an expired token (`POST users/api/v1/auth/refresh-token`, form `refresh_token`), logout (`POST users/api/v1/auth/logout`), registration after a 203, multi-profile selection, FCM device-token registration (`POST mediadetails/api/v1/device-tokens`).

## Open questions for the backend

- Should `x-bearer-token` be sent on public (pre-login) endpoints? The previous app never did.
- `verifyEmailLogin` signs a user in with only an email and `is_email_verified=1`, after the device-limit step. Does the server verify that an OTP was entered for this email (a server-side session)? If not, anyone knowing an email could skip the code once the account is at its device limit (202 on `email-login`). Recommend the backend require the code or a short-lived token.
- The `logout-device` responses and the 202 answer include other devices' `access_token`s. Is it intended that a client that has not authenticated receives them?
- Exact `message` texts for 200/202/203, whether 203 at verify returns tokens, the JSON types of `device_limit` / `profile_count` / `current_device` (read leniently here), the `device_type` values, and the expiry time of a code.
