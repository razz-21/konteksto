# Konteksto 🎙️✨

**Live Seminar & Conference Companion App for Android**

Konteksto is an AI-powered real-time transcript and contextual companion app for live talks, keynotes, lectures, and seminars. It bridges the understanding gap between speakers and audiences by providing live transcription, instant AI-grounded explanations, and dynamically generated session glossaries.

---

## 💡 About Konteksto

### The Problem
During complex talks, audience members often encounter unfamiliar jargon, acronyms, or technical concepts. Stopping a live speaker to ask for clarification is impossible, and searching online during a presentation causes attendees to lose focus and miss key points.

### The Solution
With Konteksto:
- **Speakers (Hosts)** present while streaming audio transcription. Before the talk, hosts can upload slide decks and add context notes to ground the AI in the exact subject matter.
- **Attendees (Participants)** follow a real-time transcript on their phones, tap highlighted terms for instant definitions, select complex sentences for private AI-driven explanations, and ask questions anchored in the talk's context.

---

## 🚀 Key Features

### 🎙️ For Hosts (Speakers & Presenters)
- **Session Setup & Context Grounding:**
  - Create talks with custom titles, categories, venues, cover photos, and target languages.
  - Upload **slide decks (PDF)** and context notes (audience level, definitions, background).
  - Pre-talk AI analysis extracts slide-by-slide summaries and populates a **starter glossary**.
  - Edit or refine slide summaries and terms before going live.
- **Background Live Streaming:**
  - Powered by an Android **Microphone Foreground Service** (`LiveBroadcastService`) with ongoing status bar notifications.
  - Live transcription and AI term extraction continue seamlessly even when the app is minimized, the phone is locked, or the screen rotates.
- **Seamless Room Sharing:**
  - On-device **QR code generation** (ZXing) and 6-character human-friendly join codes.
  - Universal deep links (`https://<project>.web.app/join/{code}`) for camera app scanning.

### 👥 For Participants (Audience)
- **Instant Frictionless Entry:**
  - Join rooms in seconds via QR scanning (Google Code Scanner) or typing a code.
  - Anonymous Firebase authentication requires no mandatory initial signup.
- **Live Auto-Scrolling Transcript:**
  - Real-time low-latency transcript streaming from Firestore.
  - Auto-scrolls as new speech segments arrive, with a manual scroll-lock and "Jump to Live" indicator.
- **Explain Mode & Private AI Q&A:**
  - Toggle Explain mode to pause scrolling and select specific transcript lines or phrases.
  - Get instant, private AI explanations grounded in the host's slide summaries and context notes.
  - Ask follow-up questions or free-form queries about the presentation.
- **Dynamic AI Glossaries:**
  - AI engine continually processes speech in the background to detect jargon, abbreviations, and technical terms.
  - Terms are automatically highlighted directly within the live transcript.
  - Tapping a highlighted term opens an inline definition overlay without leaving the live screen.
  - Bookmark terms to build a personal dictionary across sessions.
- **Session History & Export:**
  - Review past talks, search full transcripts, and export content to Text or PDF.

---

## 🛠️ Technology Stack

### **Android Client**
| Layer / Feature | Technology Used |
|---|---|
| **Language** | Kotlin |
| **UI Framework** | Jetpack Compose, Material 3 (Single Activity Architecture) |
| **Min / Target SDK** | Android 13 (API 33) / Android 16 (API 36) |
| **Concurrency & Streams** | Kotlin Coroutines, Flow, `StateFlow`, `collectAsStateWithLifecycle()` |
| **Architecture** | Unidirectional Data Flow (UDF), Layered Architecture (UI ➔ Domain ➔ Data) |
| **Dependency Injection** | Hilt / Factory pattern |
| **Background Services** | Android Foreground Service (`FOREGROUND_SERVICE_MICROPHONE`), `PARTIAL_WAKE_LOCK` |
| **Image & QR Utilities** | Coil (Compose Image Loading), ZXing (QR Code Generation), Google Code Scanner |
| **Preferences** | Jetpack DataStore |

### **Backend & AI Infrastructure (Strictly Google Cloud Ecosystem)**
| Service | Role |
|---|---|
| **Firebase Authentication** | Anonymous and Google Sign-In identity management |
| **Cloud Firestore** | Low-latency real-time database with snapshot listeners and offline cache |
| **Cloud Storage for Firebase** | Secure hosting for host slide deck PDFs and cover photos |
| **Cloud Functions for Firebase** | Serverless triggers for batch glossary generation & callable functions for Explain Q&A |
| **Firebase AI Logic & Gemini** | Grounded AI models (`gemini-3.8-flash`, `gemini-3.5-flash-lite`) for deck analysis, explanations, and term extractions |
| **Firebase Remote Config** | Dynamic model identifier management without app re-releases |
| **Firebase App Check** | Play Integrity verification for API security |
| **Cloud Run** | WebSocket relay for continuous streaming Speech-to-Text |

---

## 🏗️ Architecture & Data Flow

```
┌────────────────────────────────────────────────────────────────────────┐
│                              HOST APP                                  │
│  Creates session ──► Uploads PDF/Notes ──► LiveBroadcastService (Mic) │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │ Speech Stream
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│                             BACKEND SERVICES                           │
│  Speech-to-Text Relay ──► Firestore (transcripts & live/current)      │
│                                   │                                    │
│  Cloud Functions + Gemini ◄──────┴──────► AI Glossary Engine          │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │ Real-time Snapshot Listeners
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│                           PARTICIPANT APP                              │
│  Live Transcript ──► Highlighted Terms ──► Explain Mode / Private Q&A  │
└────────────────────────────────────────────────────────────────────────┘
```

### Key Technical Principles
1. **Google Products Only Policy:** Every cloud service, database, AI model, and auth provider runs on Firebase or Google Cloud.
2. **Security & Privacy First:** No API keys are embedded in client binaries (`BuildConfig`). All Gemini requests pass through App Check and serverless functions to prevent parameter tampering and credential exposure.
3. **Zero Audio Persistence:** Live speech audio is processed in-memory for continuous transcription and is never stored on servers or local devices.

---

## 📂 Project Structure

```
com.example.konteksto/
├── data/               # Repositories, Data Sources, Firebase & Gemini integrations
├── domain/             # Models and business logic use cases
├── service/            # LiveBroadcastService foreground service & mic controllers
└── ui/                 # Jetpack Compose UI Screens & ViewModels
    ├── home/           # Participant & Host home screens
    ├── host/           # Session creation, slide upload, studio & live broadcasting
    ├── participant/    # Live room, Explain mode, Q&A, and live transcript view
    ├── history/        # Session history and export
    ├── navigation/     # KontekstoNavHost & Navigation graph
    └── theme/          # Material Design 3 color schemes and typography
```

---

## 📋 Requirements & Setup

### Prerequisites
- **Android Studio:** Ladybug (2024.2.1) or newer
- **JDK:** Java 17+
- **Android Device / Emulator:** API Level 33 (Android 13) or higher

### Building the Application
1. Clone the repository:
   ```bash
   git clone https://github.com/your-org/m-konteksto.git
   cd m-konteksto
   ```
2. Place your `google-services.json` file into the `app/` directory.
3. Build the project using Gradle:
   ```bash
   ./gradlew assembleDebug
   ```
4. Run tests:
   ```bash
   ./gradlew test
   ```

---

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.
