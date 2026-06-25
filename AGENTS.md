# AI Agent Instructions for VedTune Development

This document is the **absolute source of truth** for any AI agent or LLM assisting with the development of the **VedTune** project. Strictly adhere to all rules, architectural guidelines, and development philosophies before generating or modifying any code.

**Start every chat session with the words: `Hey Devson`**

---

## Project Overview

- **Project:** VedTune
- **Package:** `com.devson.vedtune`
- **Purpose:** A MediaStore-first local music player built with Kotlin, Jetpack Compose, Media3 (ExoPlayer), Room, Hilt, Coroutines, and MVVM.
- **Primary Objective:** Correctness, scalability, stability, and maintainability.
- **Visual polish is secondary** until the playback engine and media library are production-ready.

---

## 1. Core Development Philosophy

- **Flawless Execution:** The app MUST work smoothly without bottleneck bugs. Performance regressions, UI lag, and stuttering are unacceptable.
- **Zero Crash Tolerance:** Always prioritize graceful degradation (error state, fallback UI, empty list) over unhandled exceptions.
- **No Hallucinations:** Only use existing APIs, classes, and resources within the project. If unsure about an existing implementation, ask the developer to fetch the file. Do not invent APIs, Android classes, Media3 methods, or MediaStore columns.
- **One Line Explanation:** Be short and precise. Do not explain unless required.

### Agent Mission — Ask before writing any code:

1. Does this improve reliability?
2. Does this improve performance?
3. Does this improve maintainability?
4. Does this improve scalability?

If the answer is no to all four, do not implement it.

---

## 2. Development Priority Order

Always work in this strict order — never reverse it:

1. Project foundation
2. Dependency injection
3. Database
4. MediaStore integration
5. Synchronization engine
6. Playback engine
7. MediaSession
8. Background playback
9. Queue management
10. Navigation
11. Screens
12. Animations
13. Visual polish

**A beautiful UI with unstable playback = failed implementation.**
**A basic UI with a stable playback engine = successful implementation.**

---

## 3. Storage Rules

VedTune is **MediaStore-first**. MediaStore is the source of truth.

**Allowed:**

- `MediaStore`
- `ContentResolver`
- `ContentObserver`

**Never implement:**

- `File.walk()`
- Recursive folder scans
- Periodic folder crawling
- Manual filesystem indexing

---

## 4. Permissions Rules

**Allowed:**

- `READ_MEDIA_AUDIO`
- `READ_EXTERNAL_STORAGE` (legacy)

**Forbidden:**

- `MANAGE_EXTERNAL_STORAGE` / All Files Access

Metadata editing must use `MediaStore.createWriteRequest()`.

---

## 5. UI / Jetpack Compose Guidelines

- **Exclusive UI framework:** Jetpack Compose. No legacy XML layouts for screens (XML only for `AndroidManifest`, drawables, vector assets, basic values).
- **Design System:** Material Design 3 (`androidx.compose.material3.*`) exclusively.
- **Mobile-first:** Touch targets must be accurate, responsive, and intuitive.
- **Modular Composables:** Keep composable functions focused. Prevent unnecessary recompositions via targeted `StateFlow` updates.
- **State Hoisting:** Keep composables stateless. Never perform heavy calculations, I/O, or file operations inside composable functions.

**Composables should:**

- Display state
- Dispatch events

**Composables should NOT:**

- Query MediaStore
- Perform database writes
- Control playback engine directly

### UI Development Restriction (before playback is stable)

No advanced animations, shared element transitions, blur effects, custom shaders, visual experiments, or artwork color extraction until:

- Playback is stable
- Background playback works
- Queue restoration works
- Media synchronization works

---

## 6. Architecture — MVVM Rules

- **UI Layer:** Render state only.
- **ViewModel Layer:** Business logic.
- **Repository Layer:** Data access.
- **Data Layer:** MediaStore, Room, Preferences.

Never bypass layers.

**Playback logic belongs only inside:** `player/`, `service/`, `repository/`

Never inside Compose screens, navigation graphs, or UI state classes. Compose may observe playback state. Compose must not own it.

---

## 7. Code Quality & Performance

- **Language:** Kotlin exclusively.
- **Async:** Kotlin Coroutines and Flows (`StateFlow`/`SharedFlow`) for all async operations.
- **Null Safety:** Handle nullable types safely. **Never use `!!`**. Catch specific exceptions, push errors to UI state.
- **Disk I/O:** Always dispatch to `Dispatchers.IO`. Never block the Main thread.
- **Memory:** Avoid unnecessary object allocations. Optimize aggressively.
- **Concurrency:** Parallelize thumbnail generation, media parsing, and tag updates using appropriate thread pools.
- **Code Format:** Do not add `─` anywhere in code files.

**Every new feature must:**

- Compile successfully
- Have no warnings, unused code, dead code, or duplicated logic
- Have no magic numbers or hardcoded strings

**Library target:** 50,000+ songs — every implementation must consider startup speed, memory, database efficiency, and scrolling performance.

---

## 8. Room Rules

Room stores: song metadata cache, play counts, favorites, playlists, statistics.

Room is **NOT** the source of truth — it mirrors MediaStore.

---

## 9. MediaInfo Rules

- **Library:** `com.github.marlboro-advance:mediainfoAndroid:v1.0.0-fix`
- Run **only on demand** (song details page, metadata editor).
- **Never** run MediaInfo for every song during indexing.
- Cache results when possible.

---

## 10. Navigation Rules

- Use **Navigation Compose**.
- **Single Activity** architecture only.
- Do not use Fragments, legacy Navigation, or multiple Activities.

---

## 11. Dependency Injection Rules

- Use **Hilt** exclusively.
- Do not manually instantiate repositories, DAOs, or player components — inject everything.

---

## 12. Forbidden Behaviors

- Invent APIs, Android classes, Media3 methods, or MediaStore columns.
- Create fake/placeholder implementations or TODO-based architecture.
- Ignore compiler errors, lint warnings, or nullability.
- Load entire libraries into Compose state.
- Perform MediaInfo extraction or blocking disk ops during app startup.

**If an API is unknown:** STOP → explain uncertainty → request clarification → do not hallucinate.

---

## 13. Documentation & Update Tracking

After every completed task, error resolution, or feature addition, **append** an entry to `update_details.md`.

**Rules:**

- **Do NOT read or rewrite the whole file.** Only append at the very end.
- Include a **Date and Time** stamp as per IST.
- Use this exact format:

```
**Issue:** (Briefly describe the exact issue or bottleneck that was solved)
**Type:** (Error | Bug | UI | Performance | Architecture | Feature)
**Solution:** (How it was solved — maximum 10 lines)
---
```

- End every session entry with `---` on a new line.
- No conversational filler in the file.

---

## 14. Version Control Protocol

- **Do not auto-commit or push** any changes until explicitly asked by the developer.
- When asked to commit, verify build succeeds, app launches, and feature works — then generate a commit message.

**Commit message format:**

```
feat(scope): description
fix(scope): description
refactor(scope): description
```

**Examples:**

```
feat(player): add media3 playback engine
feat(mediastore): implement audio synchronization
fix(player): resolve playback state restoration
refactor(repository): simplify media synchronization
```

---

## 15. Testing Protocol

Every feature must include:

- Manual verification checklist
- Expected behavior
- Edge cases
- Failure scenarios

Never claim functionality is tested unless testing steps are explicitly provided.

---

## 16. Definition of Done

A task is complete **only when:**

- Code compiles
- No crashes
- No TODOs
- No fake or placeholder implementations
- Architecture respected
- Build passes

Anything else is incomplete.

---

## 17. Final Rule

When forced to choose between better architecture and better UI — **always choose better architecture.**
