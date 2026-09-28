---
trigger: always_on
---

name: m3-expressive-android
description: Design and build spectacular, production-grade Android UI with Jetpack Compose and the Material 3 Expressive design system, phone-first and fully adaptive to tablets, foldables, landscape, ChromeOS and resizable windows. Use this skill whenever the user wants to create, redesign, polish or review ANY Android screen or component in Compose/Material 3 - including "make this UI better", "modern Material", "Material You", "expressive", shape morphing, wavy progress, button groups, FAB menu, floating toolbar, flexible app bars, navigation rail, list-detail or supporting-pane layouts, tablet/foldable/large-screen support, dynamic color, dark theme, spring motion - or when they share a screenshot or Compose file of an Android UI, even if they never say "Material 3 Expressive". Also use it for questions about which Expressive APIs are stable, MaterialExpressiveTheme setup, or how to make an existing app look great on every screen size.

---

# Material 3 Expressive for Android: adaptive-first Compose UI

This skill turns "build me a nice screen" into UI that feels designed: bold hierarchy, purposeful shape, spring motion, and layouts that reflow correctly from a 360dp phone to a 1600dp desktop window. It is Android-first (Jetpack Compose, Material 3, Kotlin) and assumes an MVVM/unidirectional-data-flow codebase.

Two ideas drive everything:

1. **Expressive is a dial, not a coat of paint.** Every screen gets exactly one hero moment; the rest stays calm. Uniformly bouncy, uniformly colorful UI looks worse than plain M3.
2. **Adaptive is the skeleton, not a later pass.** Decide layout from the _window size class_ before styling anything, otherwise you end up stretching a phone screen across a tablet.

Dated API knowledge lives in `references/` (last verified 2026-09-28). Read only what the task needs.

## Workflow

### Step 0: Ground yourself in the project (never skip)

Expressive is tied to specific library versions, and they change fast. As of 2026-09-28:

- Stable `androidx.compose.material3` is **1.4.0** (baseline M3).
- Expressive components and theming live in the **1.5.0-alpha** line (latest seen: alpha29, 2026-09-23). Many Expressive APIs have graduated inside those alphas and no longer need the opt-in, but the alphas still make source-breaking changes (Slider, ToggleButton, TimePicker dialog, SplitButton renames in alpha25 to alpha29).

So before writing code:

1. Look at the project's `libs.versions.toml` / `build.gradle(.kts)` for `material3`, the `material3-adaptive*` artifacts, Compose BOM, Kotlin, `compileSdk`. If files are not provided, ask for them or state your assumption in one line.
2. If already on `1.5.0-alpha*`: use Expressive APIs directly.
3. If on stable 1.4.0: tell the user the trade-off in two sentences and offer both paths. (A) move to the alpha, keeping `material3`, `material3-window-size-class` and `material3-adaptive-navigation-suite` on the same version. (B) "Expressive in spirit on stable": theme tokens, shape scale, spring motion, emphasized type and adaptive layouts all work today, and Expressive components can be swapped in later. If the user explicitly asked for Expressive components, default to A and flag that it is an alpha.
4. If you are about to use a symbol marked "verify" in the references and you can search, check the release notes (`https://developer.android.com/jetpack/androidx/releases/compose-material3`) or API reference first. **Never invent an API.** If you cannot verify, say so, use the stable equivalent, and name what to swap in later.

### Step 1: Brief the screen (five lines, in your head or in the reply)

- **Purpose** in one sentence and the **primary action**.
- **Dial**: _utilitarian_ (settings, forms, tables), _content_ (feeds, galleries, lists), or _moment_ (onboarding, empty state, now-playing, success). Utilitarian screens stay subtle: expressive theme, calm motion, little shape play. Moment screens can go big.
- **Hero moment**: the one element that gets oversized type, a bold container, a morphing shape or a signature animation.
- **Adaptive plan**: what changes at compact, medium and expanded widths (see `references/adaptive.md`, archetype table).
- **States**: loading, empty, error, populated. Design all four.

### Step 2: Build the adaptive skeleton first

1. Read window size from `currentWindowAdaptiveInfo(supportLargeAndXLargeWidth = true)` (or the newer variant in the project's adaptive version). Never branch on device type, orientation, or `Configuration.screenWidthDp` guesses.
2. Navigation comes from `NavigationSuiteScaffold`: short bar on compact, wide rail on larger windows. Do not hand-roll bar-vs-rail switching.
3. Multi-pane content uses the canonical layouts: `ListDetailPaneScaffold`, `SupportingPaneScaffold`, or a feed grid. Text-heavy single-pane screens get a content max width.
4. Edge-to-edge and insets from the start.

Templates: `templates/AdaptiveAppShell.kt`, `templates/AdaptiveListDetail.kt`.

### Step 3: Apply the six Expressive tactics (see `references/design-language.md`)

| Tactic          | Rule of thumb                                                                                                                                                                                                 |
| --------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Color**       | Use roles, never hex. Let `primaryContainer` / `tertiaryContainer` carry the hero, `surfaceContainer*` tiers carry depth. Dynamic color on by default, with a brand fallback.                                 |
| **Typography**  | Emphasized styles for headlines and key numbers, regular for body. One family, ideally variable (weight/width axes).                                                                                          |
| **Shape**       | Use the shape scale consistently, morph shape on press/selection, one decorative `MaterialShapes` motif at most. Inner radius = outer radius minus padding.                                                   |
| **Size**        | Contrast creates hierarchy: one oversized element per screen. Bigger touch targets in compact windows.                                                                                                        |
| **Motion**      | Spatial changes (position, size, shape, rotation) use `MaterialTheme.motionScheme` spatial springs, which may overshoot. Effects (color, alpha) use effects specs, which never overshoot. One scheme per app. |
| **Containment** | Group related content in containers and segmented lists. Cards only when they group something. No card-in-card.                                                                                               |

### Step 4: Write the code (the contract)

- **Complete, compilable files**: package line, all imports, no `TODO` placeholders, no "add your own code here".
- **Route/Screen split**: `FooRoute(viewModel = hiltViewModel())` collects `uiState` with `collectAsStateWithLifecycle()` and passes plain state and lambdas into a stateless `FooScreen(uiState, onEvent, modifier)`. Screens must be previewable without Hilt.
- **Previews** for each screen: `@PreviewScreenSizes`, `@PreviewLightDark`, `@PreviewFontScale` (and `@PreviewDynamicColors` for themed surfaces), with fake state.
- **Theme only**: colors from `MaterialTheme.colorScheme`, type from `MaterialTheme.typography`, shapes from `MaterialTheme.shapes`, motion from `MaterialTheme.motionScheme`. Hard-coded values are allowed only for spacing constants and the brand fallback color scheme.
- **Strings** in `strings.xml` (provide the resource block), `contentDescription` on every meaningful icon (`null` only for decorative ones), semantics on custom controls.
- **Lists**: stable `key`, `contentType`, no allocation in item lambdas; read animated values inside `graphicsLayer {}` or draw lambdas, not in composition.
- **Opt-in** with `@OptIn(ExperimentalMaterial3ExpressiveApi::class)` at the narrowest scope that compiles. Do not blanket-suppress.
- Prefer `Icons.Rounded.*` (softer, matches Expressive shapes).

### Step 5: Verify before you hand it over

Walk `references/quality-checklist.md` (accessibility, font scale 200%, RTL, dark, dynamic color on/off, predictive back, insets, performance, adaptive sizes). In the reply, end with:

- which APIs are alpha or still experimental and need opt-in,
- anything you could not verify and how you handled it,
- 3 to 5 concrete things to test on a device (at least one tablet or resizable window, one font-scale check).

## When the user shares a screenshot or existing code

Audit before redesigning. Name 5 to 8 specific gaps, for example: flat hierarchy (everything `bodyMedium`), uniform corners, hard-coded colors, phone-only layout stretched on tablets, static transitions, cards nested in cards, tiny touch targets, missing loading/empty states. Give the plan in at most 8 bullets, then the code. Keep the app's existing architecture and naming; change the UI layer, not the data layer, unless asked.

## Non-negotiables (and why)

1. **No hard-coded colors, type or shapes.** Breaks dynamic color, dark theme, high-contrast and the expressive scheme swap.
2. **Layout from window size class, never device type.** Foldables, split-screen, freeform and ChromeOS windows all change width at runtime. Do not lock orientation; on recent Android versions large screens ignore such locks anyway.
3. **Stateless screens, hoisted state.** Makes previews, tests and pane reuse trivial.
4. **Spatial vs effects motion.** Bouncing a color fade looks broken; a stiff position move looks dated.
5. **48dp minimum touch targets, real semantics, 200% font scale survives.**
6. **Content max width or panes on wide windows.** A 1200dp-wide paragraph or full-width button is a bug.
7. **Do not invent APIs.** Unverified symbols must be flagged.
8. **Restraint.** If more than one thing on a screen is shouting, nothing is.

## Reference map (read what the task needs)

| File                              | Read when                                                                                                                    |
| --------------------------------- | ---------------------------------------------------------------------------------------------------------------------------- |
| `references/design-language.md`   | Designing or critiquing look and feel: color, type, shape, size, motion, containment, screen "moment" recipes, anti-patterns |
| `references/components.md`        | Choosing components, checking stable vs experimental status, avoiding renamed or removed APIs, code snippets                 |
| `references/adaptive.md`          | Anything about tablets, foldables, landscape, rails, panes, grids, input devices, insets, archetype layouts                  |
| `references/theme-and-setup.md`   | Gradle versions, `MaterialExpressiveTheme`, color/type/shape/motion setup, variable fonts                                    |
| `references/quality-checklist.md` | Final verification                                                                                                           |
| `templates/*.kt`                  | Starting points to copy and adapt (not compiled in the authoring environment; treat as reviewed drafts)                      |

## Small requests

For a single component ("make this button expressive"), skip the ceremony: do Step 0's version check, give the component code with correct sizes/shapes/motion, and mention the opt-in. Do not produce a whole app shell nobody asked for.
