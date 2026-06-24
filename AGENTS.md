# AGENTS.md

# VedTune Development Rules

Project: VedTune

Package:

com.devson.vedtune

Purpose:

VedTune is a MediaStore-first local music player built with Kotlin, Jetpack Compose, Media3, Room, Hilt, Coroutines, and MVVM.

The primary objective is correctness, scalability, stability, and maintainability.

Visual polish is secondary until the playback engine and media library are production-ready.

---

# Agent Mission

Before writing code ask:

1. Does this improve reliability?
2. Does this improve performance?
3. Does this improve maintainability?
4. Does this improve scalability?

If the answer is no, do not implement it.

Never generate code solely for visual appeal.

---

# Core Philosophy

Playback Engine First.

Media Library Second.

Database Third.

User Interface Last.

A beautiful UI with unstable playback is considered a failed implementation.

A basic UI with a stable playback engine is considered successful.

---

# Development Priority Order

Always work in this order:

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

Never reverse this order.

---

# Forbidden Behaviors

Do NOT:

- Invent APIs.
- Invent Android classes.
- Invent Media3 methods.
- Invent MediaStore columns.
- Create fake implementations.
- Create placeholder business logic.
- Create TODO-based architecture.
- Ignore compiler errors.
- Ignore lint warnings.
- Ignore nullability.

If an API is unknown:

STOP

Explain uncertainty.

Request clarification.

Do not hallucinate.

---

# Storage Rules

VedTune is MediaStore-first.

Never implement:

File.walk()
Recursive folder scans
Periodic folder crawling
Manual filesystem indexing

Allowed:

MediaStore
ContentResolver
ContentObserver

MediaStore is the source of truth.

---

# Permissions Rules

Allowed:

READ_MEDIA_AUDIO
READ_EXTERNAL_STORAGE (legacy)

Forbidden:

MANAGE_EXTERNAL_STORAGE

Never request All Files Access.

Metadata editing must use:

MediaStore.createWriteRequest()

---

# Performance Rules

Target Library Size:

50,000+ songs

Every implementation must consider:

- startup speed
- memory usage
- database efficiency
- scrolling performance

Never:

Load entire libraries into Compose state.

Never:

Perform MediaInfo extraction during app startup.

Never:

Perform blocking disk operations on Main thread.

---

# MediaInfo Rules

Library:

com.github.marlboro-advance:mediainfoAndroid:v1.0.0-fix

MediaInfo must only run:

- on demand
- song details page
- metadata editor

Never run MediaInfo for every song during indexing.

Cache results when possible.

---

# Room Rules

Room stores:

- song metadata cache
- play counts
- favorites
- playlists
- statistics

Room is NOT the source of truth.

MediaStore remains source of truth.

Room mirrors MediaStore.

---

# Playback Rules

Playback logic belongs only inside:

player/
service/
repository/

Never inside Compose screens.

Never inside navigation graphs.

Never inside UI state classes.

Compose may observe playback state.

Compose must not own playback state.

---

# Compose Rules

Compose screens must remain lightweight.

Composable functions should:

- display state
- dispatch events

Composable functions should NOT:

- query MediaStore
- perform database writes
- control playback engine directly

Use ViewModels.

Use StateFlow.

Use immutable UI state.

---

# MVVM Rules

UI Layer:

Render state only.

ViewModel Layer:

Business logic.

Repository Layer:

Data access.

Data Layer:

MediaStore, Room, Preferences.

Never bypass layers.

---

# Navigation Rules

Use:

Navigation Compose

Do not use:

Fragments
Legacy Navigation
Multiple Activity architecture

Single Activity only.

---

# Dependency Injection Rules

Use Hilt.

Do not manually instantiate:

Repositories
DAOs
Player components

Inject everything.

---

# Code Quality Rules

Every new feature must:

Compile successfully.

No warnings.

No unused code.

No dead code.

No duplicated logic.

No magic numbers.

No hardcoded strings.

---

# Git Workflow Rules

After every completed milestone:

1. Verify build succeeds.
2. Verify app launches.
3. Verify feature works.
4. Generate commit message.

Format:

feat(scope): description

Examples:

feat(player): add media3 playback engine

feat(mediastore): implement audio synchronization

feat(room): add song metadata cache

fix(player): resolve playback state restoration

refactor(repository): simplify media synchronization

---

# Testing Rules

Every feature must include:

Manual verification checklist.

Expected behavior.

Edge cases.

Failure scenarios.

Never claim functionality is tested unless testing steps are provided.

---

# UI Development Restriction

Before Phase 5:

No advanced animations.

No shared element transitions.

No blur effects.

No custom shaders.

No visual experiments.

No artwork color extraction.

Focus entirely on:

- MediaStore
- Room
- Playback
- MediaSession
- Queue management

UI improvements become allowed only after:

Playback is stable.

Background playback works.

Queue restoration works.

Media synchronization works.

---

# Definition of Done

A task is complete only when:

- Code compiles
- No crashes
- No TODOs
- No fake implementations
- No placeholder logic
- Architecture respected
- Build passes
- Git commit generated

Anything else is incomplete.

---

# Final Rule

When forced to choose between:

1. Better architecture
2. Better UI

Always choose:

Better architecture.
