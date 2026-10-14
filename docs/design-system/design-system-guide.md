# Design system guide

All visual decisions live in `app/src/main/java/com/vplayed/core/designsystem/`. Feature code reads tokens through `AppDesignSystem` inside `AppTheme`, so changing a token here restyles the whole app.

```
designsystem/
  color/       BrandPalette, StatusColors, AppExtendedColors, BrandPalettes (raw values)
  typography/  AppFonts (Inter), TypeScaleTokens (Material), OttTypeTokens (OTT roles), TextScale
  dimension/   AppDimensions (+ Spacing, IconSizes), UiDensity, WindowWidthClass
  shape/       AppShapes, AppElevations, AppMotion
  theme/       AppTheme, AppDesignSystem, AppearanceConfig, ThemeMode, BrandConfig
  component/   AppButton, AppTextField, AppCard, AppTopBar, AppSurface,
               AppLoadingIndicator, AppErrorView, AppEmptyState, AppDialog
  icon/        AppIcons → res/drawable vectors
```

## Three layers of configuration

| Layer | Where | Who changes it |
|---|---|---|
| Brand defaults (white-label) | `BrandConfig`, `BrandPalettes`, `AppFonts`, token objects | Developers, per brand |
| User preferences | `AppearanceConfig`, persisted by `AppearanceRepository` (DataStore) | The user, in Appearance settings |
| Platform settings | System dark mode, font scale, wallpaper colors, window size | The OS; read live by `AppTheme` |

`AppTheme(appearance, darkTheme, windowWidthClass)` combines them. At runtime `MainActivity` passes the repository's config; previews and tests use defaults or explicit values.

## Light and dark theme

- `ThemeMode.SYSTEM` (default) follows the device and updates live — the activity is not recreated on `uiMode` changes.
- `LIGHT` / `DARK` force a mode. `MainActivity` re-applies `enableEdgeToEdge` whenever the resolved mode changes so status/navigation bar icons keep contrast.
- Every palette defines both `lightScheme` and `darkScheme`. `res/values(-night)/colors.xml` holds only `splash_background` (system splash, window and splash screen; follows the system light/dark setting).
- Dynamic color (Android 12+) is opt-in (`BrandConfig.dynamicColorByDefault = false`). When on, Material roles come from the wallpaper; status colors still come from the palette.

## Palettes (from the prototype)

| Palette | Dark mode | Light mode |
|---|---|---|
| **Vplayed** (default) | Black surfaces (`#0A0A0B` → `#2E3037` ladder), white text, muted `#8B919C`, brand red `#FF0935` | White surfaces (`#FFFFFF`, `#F4F5F7`), ink `#111216`, red `#E20831` (4.5:1 on white) |
| **Black & white** | Same surfaces, white accent | Same surfaces, black accent |

Yellow (`tertiary`: `#FFD119` dark / `#876900` light) is the prototype's secondary accent: hero dash fill, hero chip dot, music note and spindle ring (grey in Black & white).

Prototype neutrals without a Material role live in `PrototypeNeutrals` and are read through `AppDesignSystem.extendedColors`: `textSoft` (`#B7BCC4` / `#3F444D`), `textFaint` (`#646A75`), `hairline` (white 8% / ink 10%), `glassFill` / `glassRing`, `artworkScrim` (`#0A0A0B`, chips and fades on artwork), `onImageScrim` / `onImageScrimSoft` (text on artwork, both themes), `darkTileHigh` / `darkTileLow` (channel tile base), `vinylGroove` / `vinylRidge` (music disc), plus `skeletonBase` / `skeletonHighlight`. Bulk-purchase colour variants are `PromoAccents.Blue / Gold / Violet / Green`.

## Change colors

1. Edit values in `color/BrandPalettes.kt` (`Vplayed` is the default). Fill every role for both schemes; generating them with the Material Theme Builder from one seed color is easiest.
2. New brand: add another `BrandPalette(id, nameRes, lightScheme, darkScheme)` and list it in `BrandConfig.palettes`; set `BrandConfig.defaultPalette` to switch the default.
3. Success/warning/info: `DefaultStatusColors`, or pass `lightStatus` / `darkStatus` to one palette.
4. Use in UI: `AppDesignSystem.colors.primary`, `AppDesignSystem.extendedColors.success`, `.divider`, `.overlay`, `.disabled`.

## Fonts

Inter 4.1 static instances (Light → Black + Italic) are bundled in `res/font/inter_*.ttf` (SIL OFL 1.1 — `docs/licenses/Inter-OFL-1.1.txt`; include it in the app's open-source notices). `AppFonts.InterFamily` maps weights to files. Static files are used (not the variable font) so weights render correctly on API 24–25.

To change or add a font: copy `.ttf` files into `res/font/` (lowercase names), declare a `FontFamily` like `InterFamily`, and add an `AppFont` to `AppFonts.all`. With more than one font the Appearance screen shows a font picker automatically. Changing `BrandConfig.defaultFont` restyles every text style.

## Typography

Two scales, both built from tokens, both scaled by the user's in-app text size (`TextScale` 0.85–1.30, on top of the system font scale):

**Material scale** (`AppDesignSystem.typography`, tokens in `TypeScaleTokens`) — used by Material components and general UI: `displayLarge … labelSmall`.

**OTT text styles** (`AppDesignSystem.textStyles`, tokens in `OttTypeTokens`):

Values are the prototype's computed styles on a 427dp phone.

| Role | Size / line (sp) | Weight | Use for |
|---|---|---|---|
| `heroTitle` | 40 / 39.2, −0.035em | Bold | Hero title (the hero clamps it to 30–40sp by width, as `clamp(30px, 9.5vw, 40px)`) |
| `heroSubtitle` | 14 / 22.4 | Regular | Hero synopsis |
| `screenHeader` | 28 / 34 | Bold | Page title ("Movies", "My List") |
| `header` | 21 / 26, −0.01em | Bold | Row title (rows clamp it to 18–22sp by width on phones) |
| `subHeader` | 16 / 22 | SemiBold | Secondary heading, "Episodes", "Cast" |
| `sectionLink` | 14 / 20 | SemiBold | "See all" next to a rail header |
| `cardHeadline` | 20 / 24, −0.01em | Bold | Channel name, bulk-purchase title, price |
| `cardTitle` | 14 / 17.5 | SemiBold | Title under a card or poster |
| `cardBody` | 13 / 19.5 | Regular | Copy inside promo cards |
| `cardSubtitle` | 12 / 15 | Regular | "Action · 2h 08m" under a card |
| `metadata` | 13 / 18 | Medium | "2026 · U/A 13+ · 2h 14m · Tamil" |
| `synopsis` | 15 / 22 | Regular | Descriptions and long text |
| `badge` | 11 / 14, +0.14em | Bold | Hero chip, overlines (uppercase text) |
| `tag` | 12 / 15, +0.06em | Bold | LIVE / UPCOMING tags on artwork |
| `action` | 16 / 16 | Bold | Watch now / Subscribe |
| `actionMedium` | 15 / 18 | SemiBold | Glass/outlined buttons ("Follow") |
| `actionSmall` | 14 / 14 | Bold | Card buttons ("Watch Live", "Explore bundles") |
| `monogram` | 17 / 17 | ExtraBold | Initials in logo wells |
| `caption` | 12 / 16 | Regular | Credits, legal, helper text |
| `playerTitle` | 18 / 24 | SemiBold | Title in the player overlay |
| `playerTimestamp` | 12 / 16 | Medium, tabular digits | 01:02:45 / 02:14:00 |
| `rank` | 224 / 224 | ExtraBold, outlined | Top-10 numerals (drawn in dp by `TopTenCard`) |
| `formTitle` | 24 / 27.6, −0.03em | Bold | Form heading ("Sign in or create your account"); grows on wide windows |
| `formLabel` | 12 / 16 | SemiBold | Field labels ("Email ID") |
| `formInput` | 15 / 20 | SemiBold | Text typed into fields (placeholder: Medium) |
| `formSupporting` | 12 / 18.6 | Medium | Help, error (SemiBold) and terms lines under a form |
| `formAction` | 15 / 20 | Bold | Full-width form buttons, social sign-in buttons |
| `formDivider` | 11 / 14, +0.08em | SemiBold | "or continue with email" |
| `promoTitle` | 28 / 30.24, −0.035em | Bold | Marketing headline beside a form; grows on wide windows (≈36 on tablets) |
| `promoBody` | 15 / 23.25 | Medium | Marketing copy and feature list |
| `formCode` | 24 / 28 | Bold | One digit of a verification code |

Header roles (`heroTitle`, `heroSubtitle`, `screenHeader`, `header`, `subHeader`, `formTitle`, `promoTitle`) grow on wider windows — ×1.15 on medium (≥600dp) and ×1.3 on expanded (≥840dp: tablets, phone landscape) — via `headerScaleFor`. Card, body and metadata roles keep their size so rails stay dense.

A live reference sheet with every role, sample OTT text and its resolved size/weight is in `com.vplayed.reference.typography.OttTextStylesReference` — open its previews to compare phone and tablet sizes.

```kotlin
Text("Trending Now", style = AppDesignSystem.textStyles.header)
Text("See all", style = AppDesignSystem.textStyles.sectionLink, color = AppDesignSystem.colors.primary)
```

## Dimensions and spacing

`AppDesignSystem.dimens`:

- `spacing.none/xxs/xs/sm/md/lg/xl/xxl/xxxl/huge` = 0/4/8/12/16/20/24/32/40/48 dp
- Screen: `screenHorizontalPadding`, `screenVerticalPadding` (base values; `AppLayoutInfo` widens horizontal padding to 24/32dp on medium/expanded windows)
- Components: `buttonHeight`, `inputFieldHeight`, `toolbarHeight`, `bottomNavigationHeight`, `navigationRailWidth`, `listItemHeight`, `minTouchTarget` (48dp, never reduced), `cardCornerRadius`, `cardContentPadding`, `cardSpacing`, `dialogPadding`, `bottomSheetPadding`, `icon.small/medium/large/illustration`, `borderThin/Thick`, `dividerThickness`
- Layout: `readableContentMaxWidth` (640dp), `contentMaxWidth` (1200dp), `adaptiveGridMinCellWidth` (320dp)
- Sign-in: `authCardMaxWidth` (460), `authModalMaxWidth` (1280), `authTwoColumnMinWidth` (861), `authCornerRadius` (24), `formFieldMinHeight` (52), `formActionMinHeight` (54), `socialButtonMinHeight` (50), `authIconButtonSize` (38), `authLogoHeight` (46), `authBackButtonSize` (40)
- Verification: `otpBoxMaxSize` (56) / `otpBoxMaxSizeNarrow` (46), `otpBoxGap` (10) / `otpBoxGapNarrow` (7), `authSuccessIconSize` (84), `authSuccessBarWidth` (160) / `authSuccessBarHeight` (3), `deviceSignOutHeight` (36, inside a 48dp touch target)

`UiDensity.COMPACT` swaps in `AppDimensions.Compact` (smaller component heights/padding). Change defaults in `AppDimensions`.

## Shapes, elevation, motion

`AppShapes`: `extraSmall…extraLarge` (feed Material `Shapes`) plus `card` and `button` (both 8dp, as in the prototype), `tile` (12dp: live cards, channel tiles), `panel` (16dp: logo wells, icon tiles), `promo` (24dp: bulk-purchase card), `input`, `dialog`, `bottomSheet`, `chip`. `AppElevations`: Material levels 0–5 plus `card`, `dialog`, `toolbar`, `navigation`. `AppMotion.standard()` for animation specs.

## Component styles

Components read only tokens, so restyle them by changing tokens (e.g. `AppShapes.button` changes every button). Change a component file only for behavioral changes. Conventions:

- `AppButton(style = PRIMARY | SECONDARY | TEXT, loading, leadingIcon)` — loading disables clicks and announces "Loading".
- `AppTextField(errorMessage = …)` replaces supporting text and sets error semantics.
- `AppCard(onClick = …)` makes the whole card one accessible target.
- `AppErrorView` / `AppEmptyState` / `AppLoadingIndicator` are full-area states with live-region announcements.

## Home (prototype mapping)

Every Home row renders with the prototype component for its API type (`SectionStyle`, chosen in `HomeMapper`). Measurements are the prototype's computed styles at 427dp (`#rows > .row` etc.); the emulator used for checks (1280×2856 @ 480dpi) is 427dp wide.

| API row | Prototype component | Implementation |
|---|---|---|
| Banners | `#hero` | `HeroBannerCarousel`: height clamp(460, 72% of window, 600); artwork focus 18% from top; mobile `.hero-scrim` (page colour from the bottom + left shade; the hero stays dark in the light theme); `.badge-new` chip with the **content type** (Video / Series / Micro Series from `sho_type`, never a genre) and glowing yellow dot; **title artwork** (PNG variant, natural size within min(560, 68vw) × clamp(96, 12vw, 150)) or the title text (clamp(30, 9.5vw, 40)) when there is none or it fails to load; 2-line synopsis; red **Watch now** (50dp, glow) and glass **+**, the primary 48dp wider; dashes 18dp from the bottom (7dp dots, active 20dp track filling yellow over the 6 s auto-advance) |
| Row header | `.row-head` | `SectionRow`: 22dp above the row, 44dp header, title clamp(18, 5vw, 22) bold, 28dp chevron (view all, when > 2 items, not top 10 / bundles) |
| Videos / series (1) | `.hcard` | `LandscapeCard`: 158dp, 16:9, 8dp corners, white-8% hairline, bottom fade, title (2 lines) and "Genre · duration" (series/micro: "N Episodes" when there is no duration) |
| Micro series (7) | `.pcard` | `PortraitCard`: 112dp, 221:295 poster, meta in soft grey (wraps) |
| `is_top_ten` | `.t10` | `TopTenCard`: outlined Inter ExtraBold numeral in a 120×168dp box (two digits: 176dp, more overlap), 128dp 3:4 poster overlapping by 34dp, bold title hanging under the poster |
| Live (6) | `.lcard` | `LiveCard`: 300dp, 12dp corners; LIVE (red) / UPCOMING (dark glass) tag, title and "Started / Starts / Tomorrow · time" over the fade; footer with channel initials well and name, **Watch Live** (red) or **Set reminder** (white) |
| Promos (2) | `.hcard.pstat` | `PromoCard`: hcard + "▶ 2m 28s" pill + views · likes |
| Audio (3) | `.mcard` | `MusicCard`: grooved vinyl disc with the cover as its label, yellow spindle ring, centred title and ♪ artist |
| Channels (4) | `.chtile` | `ChannelTile`: 300×256dp plate mixed from the brand colour (`bg_card_color`), white logo well, name, description, followers pill, glass **Follow** |
| Bulk purchase (5) | `.premium-in` | `OfferCard`: width = window − 72dp, 24dp corners, accent variant (blue / gold / violet / green, `PromoAccents`) from the banner artwork, icon tile, overline, title, description, price with struck original and "Save up to N%" (or validity), **Explore bundles**; no row header |
| Skeletons | | `Modifier.shimmer()` / `ShimmerBox` / `HomeShimmer` / `RowShimmer` |
| Mobile tab bar | | Material 3 `NavigationBar` via `NavigationSuiteScaffold`: background colour, no pill, selected icon filled red, label white; muted grey otherwise. Rail on landscape/tablets |

CSS gradients and `color-mix()` are reproduced with `cssLinearGradient`, `cssRadialGradient` and `Color.mixWith` (`core/designsystem/component/CssGradients.kt`). Remaining differences from the prototype are listed in `docs/decisions.md`.

## Login (prototype mapping)

Source: prototype page "VPlayed Sign Up" (email step), its Mobile variant on compact windows. Colors: `extendedColors.auth` (`AuthSurfaces.dark` / `.light` in `BrandPalettes.kt`: canvas, glass modal/card fills and borders, field fill/border/focus ring/error, social and icon-tile fills, text ladder `textStrong` / `textPrimary` / `textSecondary` / `textMuted`) and `extendedColors.formAccent` (`--brand-2`: brand red mixed with 15% white on dark, the primary color on light).

| Prototype | Implementation |
|---|---|
| `.bg-layer` / `.bg-glow` | `Modifier.loginBackdrop()`: auth canvas + three radial glows (brand 12% / 8%, yellow 4%) |
| ≤ 860px, Mobile page | `LoginScreen` single card: max 460dp, 24dp corners, padding 40/32 (30/20 at ≤ 400dp), close button top-right, "Already have an account? Sign in" |
| > 860px | Two-column modal (max 1280dp, min 680dp tall): `LoginShowcasePanel` (logo, headline with sweeping accent, subtitle, 5 features, platforms) + card (18dp corners, padding 40/28, 56/64 above 1100dp), **Continue as guest**. Landscape phones (height < 480dp) keep the single card |
| `.modal` / `.right-card` | `modalSurface` (135° gradient, 1dp border, deep shadow) / `cardSurface` (glass fill, border, top highlight, red ambient glow) |
| `.social-btn` | `LoginSocialButton`: min 50dp, 8dp corners, glass fill + border; Google/Apple disabled (50% opacity) until social login exists |
| `.field input` | `LoginEmailField` on `BasicTextField`: min 52dp, 14/18 padding, neutral focus border + 4dp halo (never red), error border `#EF4444`, error text in `formAccent`, email keyboard, IME Done submits, autofill hint |
| `.btn-primary` | `LoginPrimaryButton`: min 54dp, brand fill, red glow shadow, trailing arrow, spinner while submitting |
| `.terms` | `formSupporting` with "Terms of use" / "privacy policy" in the brand color (not links yet) |

Icons are Material Symbols Rounded vectors (`AppIcons.Menu`, `Login`, `Close`, `ArrowForward`, `PersonOutline`, `Devices`, `Download`, `FourK`, `Redeem`) plus the prototype's Google and Apple marks. Differences are listed in `docs/decisions.md`.

## OTP (prototype mapping)

Source: the same prototype page "VPlayed Sign Up", steps 2–3 and `data-step="devices"`. The shell (backdrop, single card / two-column modal, close button, paddings) is shared with Login in `components/AuthLayout.kt`; colors are `extendedColors.auth`.

| Prototype | Implementation |
|---|---|
| `.astep-back` (≤ 900px disc) | `AuthBackButton`: 40dp round glass button, chevron, 48dp touch target |
| `.step-indicator`, `h1`, `.otp-note` | "STEP 2 OF 3" (`badge`, `formAccent`, announced as a live region), `formTitle` "Verify it's you", note in `bodyMedium` with the email in bold, "Change" on its own line |
| `.otp-row input` ×6 | `OtpCodeField`: six `OtpBox`es (square, max 56dp, 8dp corners, fill `fieldFill`; empty `socialBorder`, filled `fieldBorder`, next box `fieldBorderFocused` + 4dp halo) over one invisible text field; digits in `formCode` |
| `.wpm-err` + `.wpm-card.shake` (Manage Profiles, incorrect PIN) | `AuthInlineError` (12sp semibold, `formAccent`, live region) under the boxes / device list; `Modifier.shakeOnChange(shakeCount)` shakes the card once (0, −8, +7, −4, +2, 0 dp over 400 ms; off when animations are off); no red box borders |
| `.resend` | "Didn't get the code? **Resend** in 1:00": `formSupporting`, Resend brand-colored when usable, muted otherwise |
| `#verifyBtn` | `LoginPrimaryButton` (disabled at 50% until six digits, spinner while verifying) |
| `data-step="devices"` | `DeviceLimitStep`: `formTitle`, note, rows (`fieldFill`, 10dp corners, 38dp icon tile, `cardTitle` name, `cardSubtitle` detail, outlined brand "Sign out"), primary button without arrow ("Sign out from all devices") → "Continue" |
| `data-step="success"` | `AuthSuccessStep`: 84dp circle (theme `success` at 16%) with check, `formTitle`, note, 160×3dp progress bar filling over 3 s (full when animations are off), "Taking you in…" |

New icons: `ChevronLeft`, `DesktopWindows`, `PhoneIphone`, `Tv` (Material Symbols Rounded). Differences are listed in `docs/decisions.md`.

## Responsive layout

- `rememberAppLayoutInfo()` → `widthClass`, `isHeightCompact`, `isLandscape`, `useNavigationRail`, `screenHorizontalPadding`. Derived from window size only (works for split-screen, freeform, foldables).
- `VPlayedApp` shows a bottom bar on compact portrait windows and a navigation rail in landscape / medium / expanded windows.
- Grids: `LazyVerticalGrid(GridCells.Adaptive(dimens.adaptiveGridMinCellWidth))` — 1 column on phones, 2–3 in landscape/tablets.
- `AppScreenScaffold` applies `WindowInsets.safeDrawing` (system bars, cutouts, keyboard).

## Previews

`@ScreenPreviews` — phone, phone landscape, foldable, tablet in light/dark. `@ComponentPreviews` — light, dark, font scale 1.5. Always wrap in `AppTheme { }`.
