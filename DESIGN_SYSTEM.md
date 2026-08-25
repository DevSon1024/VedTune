# VedTune Design System Specification

Welcome to the **VedTune Design System**, the definitive UI/UX guidelines and component library for the VedTune Android local music player application.

All future UI screens, components, and refactors MUST adhere strictly to the tokens, components, and rules detailed in this document.

---

## 1. Design Architecture & Philosophy

1. **Material 3 Foundation:** All components and themes are built directly on top of `androidx.compose.material3.*`.
2. **Mobile-First & Touch-Centric:** Designed for one-handed portrait music playback on mobile phones while scaling gracefully to foldables and tablets.
3. **Flawless Performance:** No heavy calculations, direct file scans, or unnecessary recompositions in UI layers.
4. **Subtle & Contrast-Safe:** Album art and dynamic colors must influence the UI subtly without overriding core readability or creating jarring color shifts.

---

## 2. Color System

Located at: `com.devson.vedtune.ui.theme.Color.kt` & `Theme.kt`

### A. Dark Theme (Primary Identity)
- **Background:** `Color(0xFF0B0F19)` (Deep Midnight)
- **Surface:** `Color(0xFF111827)` (Elevated Midnight Surface)
- **Surface Containers:**
  - Lowest: `Color(0xFF070A10)`
  - Low: `Color(0xFF0E1422)`
  - Container: `Color(0xFF131C2E)`
  - High: `Color(0xFF1E293B)`
  - Highest: `Color(0xFF334155)`
- **Primary:** `Color(0xFF818CF8)` (Vibrant Indigo / Violet)
- **Primary Container:** `Color(0xFF312E81)`
- **Secondary:** `Color(0xFFC084FC)` (Soft Electric Purple)
- **Tertiary:** `Color(0xFFF472B6)` (Rose Pink)
- **Error:** `Color(0xFFF87171)`
- **Outline / Dividers:** `OutlineDark` (`0xFF475569`), `OutlineVariantDark` (`0xFF334155`)

### B. Light Theme
- **Background:** `Color(0xFFF8FAFC)` (Crisp Ice Off-White)
- **Surface:** `Color(0xFFFFFFFF)` (Card White)
- **Surface Containers:**
  - Lowest: `Color(0xFFFFFFFF)`
  - Low: `Color(0xFFF8FAFC)`
  - Container: `Color(0xFFF1F5F9)`
  - High: `Color(0xFFE2E8F0)`
  - Highest: `Color(0xFFCBD5E1)`
- **Primary:** `Color(0xFF4338CA)` (Royal Indigo)
- **Secondary:** `Color(0xFF7E22CE)`
- **Tertiary:** `Color(0xFFBE185D)`
- **Error:** `Color(0xFFDC2626)`

### C. AMOLED Dark Mode
- Absolute Black `Color(0xFF000000)` on Background and Root Surfaces with stepped surface containers (`#080808`, `#101010`, `#181818`, `#222222`).

### D. Extended Semantic Tokens
Access via `MaterialTheme.extendedColors`:
- `success`, `onSuccess`, `successContainer`, `onSuccessContainer`
- `playerSurface`, `onPlayerSurface`
- `playingIndicatorBar`

### E. Dynamic Color (Material You)
Supported on Android 12+ (API 31+). When disabled by user preferences or on older Android versions, VedTune seamlessly falls back to the curated Indigo / Violet palette.

---

## 3. Typography Scale

Located at: `com.devson.vedtune.ui.theme.Type.kt`

| Style | Font Size | Line Height | Weight | Usage |
| :--- | :--- | :--- | :--- | :--- |
| `displayLarge` | 57sp | 64sp | Bold | Hero player counter / display |
| `displayMedium` | 45sp | 52sp | Bold | Large hero metrics |
| `displaySmall` | 36sp | 44sp | Bold | Prominent headers |
| `headlineLarge` | 32sp | 40sp | Bold | Major screen titles |
| `headlineMedium` | 26sp | 32sp | Bold | TopAppBar titles |
| `headlineSmall` | 22sp | 28sp | SemiBold | Category headers |
| `titleLarge` | 20sp | 26sp | SemiBold | Dialog & sheet titles |
| `titleMedium` | 16sp | 22sp | SemiBold | Section headers, card titles |
| `titleSmall` | 14sp | 20sp | SemiBold | Sub-headers |
| `bodyLarge` | 16sp | 24sp | Normal | Song title in list, primary text |
| `bodyMedium` | 14sp | 20sp | Normal | Artist / album secondary text |
| `bodySmall` | 12sp | 16sp | Normal | Secondary hints, descriptions |
| `labelLarge` | 14sp | 20sp | Medium | Button labels |
| `labelMedium` | 12sp | 16sp | Medium | Bottom bar labels, chips |
| `labelSmall` | 11sp | 14sp | Medium | Grid card titles, timestamps |

Specialized styles via `VedTuneTextStyles`:
- `VedTuneTextStyles.Metadata`: 11sp / 14sp for bitrates, codecs, sample rates.
- `VedTuneTextStyles.Badge`: 10sp / 12sp for sort chips and notification badges.

---

## 4. Spacing System

Located at: `com.devson.vedtune.ui.theme.Spacing.kt`

Access via `MaterialTheme.spacing.<token>`:

| Token | Dp Value | Intended Usage |
| :--- | :--- | :--- |
| `xxs` | `2.dp` | Micro-spacing between title and subtitle lines |
| `xs` | `4.dp` | Compact gaps between small chips and metadata tags |
| `s` | `8.dp` | Standard internal component gaps, list item vertical padding |
| `m` | `12.dp` | Spacing between artwork and text columns |
| `l` | `16.dp` | Standard screen horizontal margin and card padding |
| `xl` | `20.dp` | Section spacing |
| `xxl` | `24.dp` | Major section gap, dialog edge margins |
| `xxxl` | `32.dp` | Extended spacing between content groups |
| `huge` | `40.dp` | Screen header vertical offsets |
| `massive` | `48.dp` | Minimum touch target bounding dimension |

---

## 5. Shape System

Located at: `com.devson.vedtune.ui.theme.Shapes.kt`

Access via `VedTuneShapeTokens` or `MaterialTheme.shapes`:

| Token | Radius | Usage |
| :--- | :--- | :--- |
| `ExtraSmall` | `4.dp` | Mini badges, micro indicators, progress bars |
| `Small` | `8.dp` | Song list item artwork thumbnail, sort chips |
| `Medium` | `12.dp` | Standard cards, text fields, grid artwork |
| `Large` / `Card` | `16.dp` | Elevated cards, settings containers |
| `ExtraLarge` / `Dialog` | `24.dp` | Alert dialogs, modal bottom sheet top corners |
| `Pill` / `MiniPlayer` | `32.dp` | Floating mini-player container, pill buttons |
| `Full` | `CircleShape` | Play/pause hero buttons, artist avatars, action buttons |

---

## 6. Iconography & Touch Target Rules

Located at: `com.devson.vedtune.ui.theme.IconSizes.kt`

- **Visual Icon Sizes:**
  - `Small`: `16.dp` (Sort indicators, utility chip icons)
  - `Medium`: `20.dp` (Navigation icons, action strip icons, button icons)
  - `Standard`: `24.dp` (Top app bar icons, standard action icons)
  - `Large`: `28.dp` (Player secondary control icons)
  - `ExtraLarge`: `32.dp` (Player skip/prev track icons)
  - `Hero`: `40.dp` (Hero play/pause icon)
  - `EmptyState`: `64.dp` (Empty library and error placeholders)

- **Touch Target Rule:**
  - Interactive clickable areas MUST have at least **48dp** minimum bounding box (`VedTuneIconSizes.MinTouchTarget`).
  - Use `VedTuneIconButton` instead of raw unpadded `IconButton` with arbitrary sizes.

---

## 7. Reusable Component System

Located in: `com.devson.vedtune.ui.components.*`

### A. Buttons (`VedTuneButtons.kt`)
- `VedTuneIconButton(icon, contentDescription, onClick, ...)`: 48dp minimum touch target guaranteed.
- `VedTunePrimaryButton(text, onClick, icon, isLoading, ...)`: 48dp height primary M3 button.
- `VedTuneSecondaryButton(text, onClick, icon, ...)`: 48dp height filled tonal button.
- `VedTuneOutlinedButton(text, onClick, icon, ...)`: Outlined button.
- `VedTuneTextButton(text, onClick, icon, ...)`: Text button with 48dp touch target.

### B. List Items & Cards (`VedTuneCards.kt` & `SharedLibraryComponents.kt`)
- `VedTuneSongRow(song, onClick, isCurrentSong, isPlaying, showArtwork, showDuration, onOptionsClick)`: High-performance, memory-efficient song row.
- `VedTuneAlbumCard(album, onClick, showArtwork, showArtist, gridCount)`: Standard album grid card.
- `VedTuneArtistCard(artist, onClick, showArtwork, gridCount)`: Circular avatar artist card.
- `VedTunePlaylistCard(playlist, onClick, showArtwork, gridCount)`: Playlist card with track count.
- `VedTuneSectionHeader(title, count, actionText, onActionClick)`: Section divider and header.
- `VedTuneListItem(...)` & `VedTuneGridCard(...)`: Generalized library cards.

### C. Feedback & States (`VedTuneFeedback.kt`)
- `VedTuneEmptyState(title, description, icon, actionText, onActionClick)`: Consistent empty placeholders.
- `VedTuneLoadingState(message)`: Centralized progress indicator.

### D. Dialogs & Sheets (`VedTuneDialogs.kt`)
- `VedTuneInfoDialog(title, onDismiss, content)`: Standardized help / info dialog.
- `VedTuneConfirmDialog(title, message, onConfirm, onDismiss, isDestructive)`: Confirmation dialogs.
- `VedTuneBottomSheetHeader(title, onCloseClick, subtitle)`: Standard bottom sheet drag handle and title bar.

---

## 8. Motion & Animation Principles

Located at: `com.devson.vedtune.ui.theme.Motion.kt`

- **Standard Durations:**
  - `DurationShort` (150ms): Icon state changes, hover/press ripples.
  - `DurationMedium` (250ms): Tab changes, sheet slide in/out, list transitions.
  - `DurationLong` (350ms): Screen-level navigation transitions.
  - `DurationXLong` (500ms): Large layout morphing.
- **Easings:** `StandardEasing` (FastOutSlow), `EmphasizedEasing` (CubicBezier 0.2, 0.0, 0.0, 1.0).
- **Springs:** `bouncySpring()` for hero play/pause button press effects.

---

## 9. Responsive Layout System

Located at: `com.devson.vedtune.ui.theme.Responsive.kt`

- `rememberVedTuneAdaptiveInfo()`: Resolves `VedTuneWindowWidthSizeClass` (COMPACT, MEDIUM, EXPANDED).
- `calculateAdaptiveGridColumns(widthSizeClass, userSelectedSpan)`: Adjusts grid column counts intelligently on foldables and tablets so album art is never distorted.

---

## 10. Rules for Future Screen Redesigns (Phase 2+)

1. Never hardcode `Color(0x...)` in screen files. Use `MaterialTheme.colorScheme` or `MaterialTheme.extendedColors`.
2. Never hardcode arbitrary `.dp` margins. Use `MaterialTheme.spacing.*`.
3. Never use raw unpadded clickable `Icon` without 48dp minimum touch bounds. Use `VedTuneIconButton`.
4. Always provide meaningful, non-empty `contentDescription` for accessibility.
5. All artwork loading must pass through `SongArtwork` to respect user quality and caching preferences.
