# Home API

Implemented in the KMP shared layer (`../kmp`, `home/DefaultHomeRepository`, `HomeMapper`, `HomeFeedController`), used by this app and VPlayed (Phase 4). Base URL = `AppConfig.serverUrl` (per build type). Every request carries the shared client's headers, the same as `core/network/ApiHeadersInterceptor` (`Accept`, `X-REQUEST-TYPE: mobile`, `app_version`, `device_type: android`, `X-LANGUAGE-CODE`, plus `device_id`, `x-bearer-token` and the session when known), matching the previous app's guest requests.

A page is read when the HTTP status is 2xx and the envelope's `statusCode` is 200 (decision D6; `status` is advisory).

## Endpoints

| Call | Endpoint | Used for |
|---|---|---|
| Banners | `GET mediaview/api/v1/homebanners` | Hero carousel |
| Rows | `GET mediaview/api/v1/home?page={n}&limit=3&inside_limit=10` | Home rows, 3 rows per page, 10 items per row |
| Row items | `GET mediaview/api/v1/home/all/{sectionId}?page={n}&inside_limit=10` | More items of one row (horizontal paging) |

All responses use the envelope `{ statusCode, status, message, response }`. A `status` other than `success` (or `statusCode` ≠ 200) is treated as an error even when HTTP is 200.

## Pagination

- **Rows (vertical):** `response.next` is the next page number, `null` on the last page (dev: 19 rows → 7 pages). The app loads page 1 with the banners, then requests the next page when the user is within 2 rows of the end (`HomeViewModel.loadNextPage`). The previous app chained every page immediately; scroll-driven loading saves data and memory.
- **Items (horizontal):** each row has its own `next`. When the user scrolls a row within 3 items of its end, `home/all/{id}` is called with that page (`HomeViewModel.loadMoreItems`).
- Rows without items are dropped; a row id already shown is never added twice; items are de-duplicated by id.

## Section types

| `type` | Content | Prototype component (`SectionStyle`) | Fields used besides title/images |
|---|---|---|---|
| 1 | Videos / series | `.hcard` (`LANDSCAPE`) | `default_genre` / `genre`, `video_duration`, `episodes_count`, `sho_type`, `prices` |
| 2 | Promos | `.hcard.pstat` (`PROMO`) | `video_duration`, `view_count`, `guds_count` (likes) |
| 3 | Audio | `.mcard` (`MUSIC`) | `artist_name`, `audio_duration` |
| 4 | Channels | `.chtile` (`CHANNEL`) | `bg_card_color`, `description`, `follow_count`, `profile_picture` |
| 5 | Bulk purchase / offers | `.premium-in` (`OFFER`), no row title (as in the previous app) | `label`, `description`, `days`, `prices` (+ `discounted_price`, `discounted_percentage`), `banner_img` (colour variant) |
| 6 | Live streaming | `.lcard` (`LIVE`) | `streaming_status`, `stream_start_time`, `schedule_at`, `studio_name` |
| 7 | Micro series | `.pcard` (`PORTRAIT`) | as type 1 |
| any, `is_top_ten` | Ranked | `.t10` (`TOP_TEN`) | portrait `thumbnail_image` |

Banners: `master_details.sho_type` (1 video, 2 series, 6 micro series) is shown in the hero chip; `title_images.default.mobile` (PNG) is preferred over `title_image` (may be AVIF, decodable only from Android 12).

`row_count: 2` shows the row as two stacked lines scrolling together.

## Response quirks handled

| Field | Observed shapes | Handling |
|---|---|---|
| `video_duration` | string `"267.72"` or number `125` | `JsonPrimitive`, parsed as seconds |
| banner `genre` | objects `{title}` **or** bare genre ids when app headers are sent | ids are ignored; `default_genre.title` is used first |
| `default_genre.title`, row `title` (`home/all`), bundle `label` | string or localized object `{"en": "…"}` | `localizedText()` |
| `new_thumbnail_images.default` | object or `null` | nullable |
| `prices` | several currencies | INR (`currency_type` 1) preferred |
| item `default_genre` | object with a title that may be lowercase (`"action"`), a slug string, or an id | resolved to the display title of the matching `genre` entry |
| bundle `discounted_price` | amount taken **off** `amount` (2080 / 208 / 10%); 0 = no discount even if `discounted_percentage` > 0 | price shown = `amount − discounted_price`, original struck |
| counts (`view_count`, `follow_count`, `days`, `episodes_count`) | number or numeric string | `JsonPrimitive`, parsed leniently |
| `streaming_status` | 0 scheduled, 1 live, 2 finished (assumed, see decisions) | LIVE / UPCOMING tag; finished streams show no tag |

## Images

Paths are relative to the environment's image bucket (`BuildConfig.IMAGE_BASE_URL`): dev `gsvp-dev`, QA and stage `gsvp-qa`, demo `gsvp-demo` (S3, ap-south-1). Absolute URLs (e.g. `banner_img`) are used as is.

| Use | Preference |
|---|---|
| 16:9 cards | `new_thumbnail_images.default` `tv` (889×500) → `tc` → `tl` → `tp` → `tm` → `thumbnail_image` |
| 2:3 posters (micro series, top 10) | `thumbnail_image` (290×435 in these rows) |
| Hero | `video_banner_images.default.mobile` (840×385) → `web` → thumbnails |
| Channel | `profile_picture` (logo well; initials while loading or missing) |
| Offer | not shown: the prototype card has no artwork; `banner_img`'s file name (blue / yellow) picks the colour variant |
| Hero title artwork | `title_images.default.mobile` → `web` → `title_image` |

## Security note for the backend team

The public `home` response for channel rows (type 4) includes studio KYC and account data — Aadhaar/PAN image paths and names, bank account and IFSC numbers, `access_otp_token`, owner contact details. The app ignores these fields, but they should be removed from the public API.
