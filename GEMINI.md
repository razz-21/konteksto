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

### 3. Asynchronous & Background Execution
- Keep suspend methods main-safe (`withContext(ioDispatcher)`).
- **Rethrow `CancellationException`** in try-catch blocks to preserve coroutine structured concurrency.
- Audio recording and live speech transcription run in `LiveBroadcastService` (Foreground Service, `microphone` type), controlled via `LiveBroadcastController`.

### 4. Code Quality & Error Reporting
- Never return fake or sample data in production logic. Return `Result.failure` on error and display user-friendly error states in the UI.
- Record uncaught/unexpected exceptions to `Firebase.crashlytics.recordException(e)`.
