# AI Agent Guidelines & Project Directives — Konteksto

This document contains mandatory guidelines for all AI agents (Gemini, Claude, GitHub Copilot, Cursor, etc.) working on the **Konteksto** codebase.

---

## 🚨 MANDATORY COMPLIANCE DIRECTIVE

Every AI Agent interacting with this project **MUST ALWAYS READ AND ADHERE TO**:
1. **`APP_SPECIFICATION.md`** — Defines **WHAT** we build (Feature specs, Firestore schema, Roles, Google-only rule, Background hosting, AI models).
2. **`ANDROID_BEST_PRACTICES.md`** — Defines **HOW** we build it (Architecture layers, Compose performance, Coroutine safety, Error handling, Dependency injection, Testing).

When proposing, generating, or modifying code, verify your changes against these two files.

---

## 🔑 Core Engineering Rules

### 1. Platform Policy (Google Ecosystem Only)
- Every cloud service, database, AI backend, and authentication mechanism MUST be a Google product (Firebase, Google Cloud, Gemini via `firebase-ai`, App Check, Remote Config).
- **No Third-Party APIs:** Do not send audio, transcripts, join codes, or session data to any non-Google service.
- **On-Device Open Source Libraries Allowed:** Coil (image loading), ZXing (on-device QR generation).

### 2. Architecture & Layer Boundaries
```
UI Layer (Route, Screen, ViewModel) ──► Domain Layer (Models, UseCases) ──► Data Layer (Repositories, Firebase)
```
- **Strict Layer Dependency Direction:** `data` and `domain` layers MUST NEVER import UI classes (`com.example.konteksto.ui.*`). Domain models shared across layers belong in `domain/model/`.
- **Repository Abstractions:** ViewModel and Composables must interact with data only through repository interfaces (e.g., `SessionRepository`), never directly with Firebase, DataStore, or Gemini SDKs.

### 3. UI Layer & Compose Standards
- **Route & Screen Separation:** Always split screens into:
  - `<Name>Route`: Collects state from ViewModel with `collectAsStateWithLifecycle()`, handles navigation/callbacks.
  - `<Name>Screen`: Stateless composable accepting plain values and callbacks (enables previews and testing without ViewModels).
- **State Ownership & Atomic Updates:** ViewModel owns UI state (`StateFlow`). State updates must be atomic using `_uiState.update { it.copy(...) }` (never direct `value =` assignments).
- **State Persistence:** Use `rememberSaveable` for text inputs, dialog flags, and scroll states that must survive screen rotation.
- **Compose Performance:** Use `LazyColumn`/`LazyRow` with unique, stable keys (`key = { item.id }`) for dynamic lists. Never use `Column` with `forEach` for list data like transcripts or glossaries.
- **Material 3 & Material Design:** This is an Android app — build all UI with Material 3 components (`androidx.compose.material3`) and follow Material Design patterns (layout, navigation, component states, spacing, motion). Never use Material 2 (`androidx.compose.material`) or hand-build a component Material 3 already provides.
- **Theme First:** Style everything through `KontekstoTheme` (`MaterialTheme.colorScheme`, `.typography`, `.shapes`). No hardcoded colors, font sizes, or corner radii.
- **Attached Designs:** When a design (Figma, screenshot, mockup) is provided, implement it using our existing theme in `ui/theme/` and Material 3 components, following Material Design patterns. Map design values to the closest theme token; add any genuinely new token to `ui/theme` instead of inlining raw values.

### 4. Background Work & Foreground Services
- Long-running work (live transcription, audio recording) MUST live in `LiveBroadcastService` (a Foreground Service of type `microphone`), NOT in Composables or ViewModels.
- Composable UI components observe `LiveBroadcastController` state and issue commands to the service.

### 5. Coroutines & Flow Rules
- **Main Safety:** Keep suspend functions main-safe by switching dispatchers (`withContext(ioDispatcher)`). Inject dispatchers into repositories and services.
- **Cancellation Protection:** NEVER swallow `CancellationException` in `catch (e: Exception)` or `runCatching`. Always rethrow `CancellationException`.
- **No GlobalScope:** Never use `GlobalScope`. Inject an application-level `CoroutineScope` for background work outliving screen lifecycles.
- **Flow Error Handling:** Catch flow errors using `.catch { ... }` in ViewModels to prevent uncaught exceptions from crashing `viewModelScope`.

### 6. Error Handling & Security
- **No Fake Data Fallbacks:** Never fall back to made-up or sample data in production paths. Report errors to the user and return `Result.failure(e)`.
- **Security & Secrets:** No Gemini API keys in `BuildConfig` or app binaries. Model names MUST be fetched from **Firebase Remote Config** (not hardcoded).
- **Crashlytics:** Record unexpected errors with `Firebase.crashlytics.recordException(e)`. Never log transcripts, user notes, questions, or credentials.

---

## 🛠️ Quick Reference for Code Inspections & Refactoring

When editing code, check for and resolve existing gaps listed in `ANDROID_BEST_PRACTICES.md` Section 13:
- Fix data layer files importing `ui.*` classes.
- Convert `_uiState.value = ...` to `_uiState.update { ... }`.
- Ensure cancellation exceptions are rethrown in `try-catch` blocks.
- Verify `LazyColumn` has `key = { ... }`.
- Ensure hardcoded strings are moved to `strings.xml`.
