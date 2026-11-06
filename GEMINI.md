# Gemini AI Agent Guidelines — Konteksto Project

This file serves as the system instruction and context guide for Gemini AI tools, Gemini CLI, and Gemini Code Assist working within the **Konteksto** repository.

---

## 📌 Context & Required Reading

Before generating or refactoring code in this repository, you **MUST** consult and follow the project specifications:

1. **`APP_SPECIFICATION.md`** — The functional and technical specification for Konteksto.
2. **`ANDROID_BEST_PRACTICES.md`** — The engineering rules, architectural conventions, and code guidelines.
3. **`AGENTS.md`** — AI Agent directives and rule enforcement summary.

---

## 🎯 Key Project Principles for Gemini

### 1. Google Products Only
- All backend services must use **Google Cloud / Firebase** infrastructure (Firebase Auth, Cloud Firestore, Cloud Storage, Cloud Functions, Cloud Run, Firebase Remote Config, Firebase App Check).
- Generative AI functionality uses **Firebase AI Logic** (`firebase-ai` library). Do not use deprecated `generativeai` or `firebase-vertexai` libraries.
- Model names must be retrieved dynamically via **Firebase Remote Config** (e.g., `gemini-3.8-flash` for slide analysis/Explain, `gemini-3.5-flash-lite` for live glossary).

### 2. Android Architecture & Jetpack Compose Rules
- Follow Unidirectional Data Flow (UDF) with Clean Architecture: `UI (Route/Screen) -> ViewModel -> Domain -> Data Repository`.
- **Layer Separation:** `data` and `domain` packages must never import any classes from `com.example.konteksto.ui.*`.
- **Screen Pattern:** Split composables into `<Name>Route` (collects `StateFlow` via `collectAsStateWithLifecycle()`) and `<Name>Screen` (stateless UI rendering plain state).
- **State Updates:** Mutate state atomically via `_uiState.update { ... }`.
- **Lists:** Render dynamic list data (transcripts, glossaries, slides) using `LazyColumn`/`LazyRow` with unique keys (`key = { item.id }`).
- **Material 3 & Material Design:** This is an Android app — build all UI with Material 3 components (`androidx.compose.material3`) and follow Material Design patterns (layout, navigation, component states, spacing, motion). Never use Material 2 (`androidx.compose.material`) or hand-build a component Material 3 already provides.
- **Action Feedback (Snackbar):** Every user action (save, bookmark, remove, delete, edit, copy, send, retry, plus failures) MUST confirm its result with a Material 3 `Snackbar` shown for **6 seconds**. Use `SnackbarHostState.showKontekstoSnackbar(...)` from `ui/components/KontekstoSnackbar.kt`, with the screen's `Scaffold(snackbarHost = { SnackbarHost(...) })`. Child composables receive an `onShowMessage: (String) -> Unit` callback instead of owning a host. Never use `android.widget.Toast`.
- **Theme First:** Style everything through `KontekstoTheme` (`MaterialTheme.colorScheme`, `.typography`, `.shapes`). No hardcoded colors, font sizes, or corner radii. Colors with no Material role live in `MaterialTheme.extendedColors`: cards and sections use `cardContainer` with no border (never `surface` or `surfaceContainerLowest`, which match or sink below the dark background), and success states use `success`/`successContainer`; every color must work in both light and dark mode.
- **Attached Designs:** When a design (Figma, screenshot, mockup) is provided, implement it using our existing theme in `ui/theme/` and Material 3 components, following Material Design patterns. Map design values to the closest theme token; add any genuinely new token to `ui/theme` instead of inlining raw values.

### 3. Asynchronous & Background Execution
- Keep suspend methods main-safe (`withContext(ioDispatcher)`).
- **Rethrow `CancellationException`** in try-catch blocks to preserve coroutine structured concurrency.
- Audio recording and live speech transcription run in `LiveBroadcastService` (Foreground Service, `microphone` type), controlled via `LiveBroadcastController`.

### 4. Code Quality & Error Reporting
- Never return fake or sample data in production logic. Return `Result.failure` on error and display user-friendly error states in the UI.
- Record uncaught/unexpected exceptions to `Firebase.crashlytics.recordException(e)`.
