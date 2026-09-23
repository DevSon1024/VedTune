# AI Agent Instructions for VedTune Development

Absolute source of truth for AI agents working on **VedTune**. Strictly adhere to all rules, architecture, and development philosophies before writing or modifying code.

**Start every chat session with: `Hey Devson`**

---

## 1. Project Overview & Priority

- **Project:** VedTune (`com.devson.vedtune`)
- **Tech Stack:** Kotlin, Jetpack Compose (M3), Media3 (ExoPlayer), Room, Hilt, Coroutines/Flows, MVVM.
- **Source of Truth:** `MediaStore` (Room only caches/mirrors metadata, favorites, playlists, and stats).
- **Core Mission:** Ask before writing code: Does this improve (1) reliability, (2) performance, (3) maintainability, (4) scalability? If not, do not implement.
- **Priority Order:** 1. Foundation → 2. DI → 3. DB → 4. MediaStore → 5. Sync → 6. Playback → 7. MediaSession → 8. Background Playback → 9. Queue → 10. Navigation → 11. Screens → 12. Animations → 13. Visual Polish.
- **Golden Rule:** When choosing between architecture and UI — **always choose better architecture.** Stable playback > visual polish.

---

## 2. Architecture & Layering (Strict MVVM)

- **UI Layer (`ui/`):** Stateless Composables only. Render state, dispatch user events.
- **ViewModel Layer (`ui/`):** UI business logic, state emission via `StateFlow`.
- **Repository Layer (`domain/`, `data/`):** Data mediation and abstract contracts.
- **Data Layer (`data/`):** MediaStore queries, Room DB, Preferences DataStore. Never bypass layers.
- **Playback Layer (`player/`, `service/`):** All player controls and MediaSession logic belong strictly here. Never control playback directly from Compose or navigation graphs.
- **Navigation:** Single Activity (`MainActivity`) with **Navigation Compose** exclusively. No Fragments or legacy Navigation.
- **Dependency Injection:** **Hilt** exclusively. Inject all repositories, DAOs, dispatchers, and player components — no manual instantiation.

---

## 3. Storage, Permissions & MediaStore Rules

- **MediaStore-First:** `MediaStore`, `ContentResolver`, and `ContentObserver` are the only allowed indexing APIs.
- **Forbidden Storage Actions:** `File.walk()`, recursive directory crawling, periodic folder scans, manual filesystem indexing.
- **Permissions:** Only `READ_MEDIA_AUDIO` (API 33+) and `READ_EXTERNAL_STORAGE` (legacy). `MANAGE_EXTERNAL_STORAGE` is strictly forbidden.
- **Metadata Writes:** Must use `MediaStore.createWriteRequest()`.

---

## 4. UI & Jetpack Compose Guidelines

- **Framework:** Jetpack Compose with Material Design 3 (`androidx.compose.material3.*`) exclusively. No XML screen layouts (XML allowed only for Manifest, drawables, and basic values).
- **Performance:** State hoisting, focused modular composables, targeted `StateFlow` subscriptions (`collectAsStateWithLifecycle`), mobile-first touch targets (≥ 48dp).
- **Restrictions:** Composables must NEVER query MediaStore, write to database, or control playback directly.
- **Pre-requisite Gate:** No advanced animations, shared element transitions, blur effects, custom shaders, or color extraction until playback, background service, queue restoration, and sync engine are 100% stable.

---

## 5. Code Quality, Concurrency & Performance

- **Language:** Kotlin exclusively.
- **Async/Threading:** Coroutines & Flows (`StateFlow`/`SharedFlow`). Dispatch all Disk I/O and DB operations to `Dispatchers.IO`. Never block Main.
- **Null Safety:** Safe calls only. **Never use `!!`**. Catch specific exceptions and expose errors through UI state.
- **Memory & Scale:** Target 50,000+ tracks. Avoid allocations in hot paths, parallelize thumbnails/tag parsing, use composite LazyColumn keys for queue duplicate safety.
- **Code Cleanliness:** Zero warnings, zero unused/dead/duplicated code, no magic numbers, no hardcoded strings. Do NOT use `─` characters anywhere in code.
- **Zero Hallucination:** Use only existing APIs, classes, and Media3/MediaStore methods. If uncertain, STOP and ask the developer.

---

## 6. Documentation & Update Tracking (`update_details.md`)

After every completed task, error fix, or feature, **append** to the very end of `update_details.md`.
- **Do NOT read or rewrite the whole file.** Append at the very end only.
- Include IST timestamp. Use this exact format:

```markdown
Date: DD-MM-YYYY HH:mm IST

- **Issue:** (Briefly describe the exact issue or bottleneck solved)
- **Type:** (Error | Bug | UI | Performance | Architecture | Feature)
- **Solution:** (How it was solved)
---
```
- End every entry with `---` on a new line. No conversational filler.

---

## 7. Git & Version Control Protocol

- **Do NOT auto-commit or push** changes without explicit developer instruction.
- **Commit Message Format:** `feat(scope): description`, `fix(scope): description`, `refactor(scope): description`, `perf(scope): description`.

---

## 8. Definition of Done & Testing

A task is complete **only when:**
1. Code compiles without errors or warnings (`./gradlew assembleDebug`).
2. Unit tests pass (`./gradlew testDebugUnitTest`).
3. Zero crashes, zero TODOs, zero placeholder/fake implementations.
4. Architectural boundaries are fully respected.
5. Verification includes manual checklist, expected behavior, edge cases, and failure scenarios.
