# Konteksto — Live Seminar & Conference Companion App Specification

## 1. Overview & Core Vision

**Konteksto** is a mobile companion app designed for live seminars, conferences, academic lectures, and keynote presentations. 

### The Problem
During live talks, audience members often struggle to keep up when speakers use unfamiliar industry jargon, specialized acronyms, or complex technical concepts. Additionally, attendees miss key points or struggle to take clear notes while trying to listen and analyze slide content simultaneously.

### The Solution
**Konteksto** bridges the comprehension gap by providing a real-time live transcription stream straight from the speaker's microphone to attendees' mobile screens. The app auto-detects technical terms in real-time, generates an instant contextual glossary, allows attendees to snap photos of slides for AI analysis, and saves the full transcript record for post-session review.

---

## 2. Actors & User Roles

### Host (Speaker / Presenter)
- **Role:** The person giving the talk or seminar.
- **Key Capabilities:**
  - Create a new live session.
  - Generate a unique 6-digit Join Code and QR Code.
  - Record live voice audio directly via mobile microphone.
  - Stream real-time Speech-to-Text transcripts to attendees.
  - Pause, resume, and terminate live sessions.

### Participant (User / Attendee)
- **Role:** Audience members attending the seminar.
- **Key Capabilities:**
  - Scan QR Code or enter Join Code to join a live session.
  - View live real-time transcription synchronized with speaker audio.
  - Explore auto-generated AI terminology glossaries.
  - Take slide photos for instant AI analysis and explanation.
  - Bookmark and review past session transcript archives.

---

## 3. Core Features & Capabilities

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Konteksto Application Flow                      │
└────────────────────────────────────────────────────────────────────────┘
                                   │
               ┌───────────────────┴───────────────────┐
               ▼                                       ▼
     ┌───────────────────┐                   ┌───────────────────┐
     │    Host (Speaker) │                   │ User (Participant)│
     └─────────┬─────────┘                   └─────────┬─────────┘
               │                                       │
     1. Create Live Session                  1. Scan QR / Enter Code
     2. Generate QR & Code                   2. Join Live Room
     3. Speak (Audio Stream)                 3. View Live Transcript Stream
               │                                       │
               ▼                                       ▼
    ┌──────────────────────┐               ┌───────────────────────┐
    │  Real-Time STT Engine│               │   AI Terminology &    │
    │  (Speech-to-Text)    ├──────────────►│   Glossary Engine     │
    └──────────┬───────────┘               └───────────┬───────────┘
               │                                       │
               ▼                                       ▼
    ┌──────────────────────┐               ┌───────────────────────┐
    │ Firestore Live Stream│               │ Slide Photo Capture & │
    │   (Transcript Segments)              │  Vision AI Explanation│
    └──────────────────────┘               └───────────────────────┘
```

### Feature A: Session Creation & Room Entry
- **Host Flow:**
  1. Tap "Host Session", enter Title, Speaker Name, Topic, and Description.
  2. System generates a unique 6-digit alphanumeric Join Code (e.g., `KTK-892`) and a scannable QR Code.
  3. Displays Host control dashboard with Audio Recording controls (Mic Toggle, Pause, Stop Session).
- **Participant Flow:**
  1. Tap "Join Session".
  2. Option to scan QR Code via built-in CameraX scanner or type the 6-digit Join Code.
  3. Enters the live room and connects to the session stream.

### Feature B: Live Voice Capture & Real-Time Transcription
- Host's mobile microphone captures speech audio continuously.
- Speech-to-Text engine streams live transcribed text segments into Cloud Firestore.
- Participants receive real-time UI updates (via Firestore `StateFlow` / `callbackFlow`) displaying speaker words with minimal latency.

### Feature C: Auto-Detected AI Terminology & Glossary
- As transcript chunks are generated, an AI service analyzes text for specialized jargon, technical terms, or acronyms.
- Automatically generates an inline **Glossary Card** with simplified definitions, context, and examples.
- Participants can tap on highlighted terms in the transcript to view instant explanations without leaving the live screen.

### Feature D: Slide Photo Capture & AI Vision Analysis
- Participants can tap a Camera button to capture a photo of a presentation slide.
- The photo is processed using Vision AI (OCR + Multimodal LLM).
- Generates a concise summary, bullet-point explanation, and translation of complex diagrams or code on the slide.
- Analyzed slides are attached to the participant's session record.

### Feature E: Session History & Transcript Archive
- All completed sessions are stored in Firebase.
- Participants and Hosts can revisit past sessions anytime.
- Includes full transcript search, bookmarked terms, slide photo explanations, and export capabilities (Text / PDF).

---

## 4. Technical Architecture & Tech Stack

### Client Architecture (Android)
- **UI Framework:** Jetpack Compose (Material Design 3).
- **Architecture Pattern:** Layered Unidirectional Data Flow (UI Layer with ViewModels + StateFlow, Domain Layer with UseCases, Data Layer with Repositories).
- **Asynchronous Flow:** Kotlin Coroutines & `StateFlow` collected via `collectAsStateWithLifecycle()`.
- **Dependency Injection:** Hilt.
- **Camera & Scanner:** CameraX + Google ML Kit Barcode Scanning.

### Backend & Cloud Services (Firebase & AI)
- **Firebase Auth:** Host & Anonymous Participant Authentication.
- **Cloud Firestore:** Real-time sync for live sessions, transcripts, glossary items, and slide entries.
- **Firebase Crashlytics & Analytics:** Session health monitoring and crash reports.
- **Speech-to-Text Service:** Android SpeechRecognizer / Firebase Cloud Speech-to-Text.
- **Generative AI Engine:** Gemini AI API (Multimodal Vision for slide analysis & NLP for terminology detection).

---

## 5. Firestore Database Schema

### Collection: `sessions`
```json
{
  "sessionId": "KTK-892",
  "hostUid": "host_user_123",
  "title": "Introduction to Quantum Computing",
  "speakerName": "Dr. Sarah Chen",
  "topic": "Physics & Computing",
  "status": "LIVE",
  "createdAt": 1710000000000,
  "endedAt": null
}
```

### Sub-collection: `sessions/{sessionId}/transcripts`
```json
{
  "segmentId": "seg_001",
  "timestamp": 1710000015000,
  "speakerUid": "host_user_123",
  "text": "Today we are discussing qubits and quantum superposition.",
  "sequenceNumber": 1
}
```

### Sub-collection: `sessions/{sessionId}/glossary`
```json
{
  "termId": "term_qubit",
  "term": "Qubit",
  "definition": "A quantum bit, the basic unit of quantum information capable of existing in superposition.",
  "timestamp": 1710000020000
}
```

### Sub-collection: `sessions/{sessionId}/slides`
```json
{
  "slideId": "slide_101",
  "participantUid": "user_456",
  "imageUrl": "https://storage.googleapis.com/...",
  "explanation": "This diagram illustrates the Bloch Sphere representing qubit state transformations.",
  "timestamp": 1710000050000
}
```

---

## 6. Recommended Project Package Structure

```
com.example.konteksto/
├── data/
│   ├── firebase/             # Firestore, Auth & Storage data sources
│   ├── speech/               # Speech-to-Text audio capture service
│   ├── vision/               # CameraX & Slide AI analysis service
│   └── repository/           # Session, Transcript & Glossary repositories
├── di/                       # Hilt Modules (Firebase, Services, Repositories)
├── domain/
│   ├── model/                # Session, TranscriptSegment, TerminologyItem, SlideAnalysis
│   └── usecase/              # CreateSessionUseCase, StreamTranscriptUseCase, AnalyzeSlideUseCase
├── ui/
│   ├── components/           # QR Scanner, Transcript Card, Glossary Sheet, Mic Controls
│   ├── login/                # LoginRoute.kt, LoginScreen.kt, GoogleSignInButton.kt, FeatureItem.kt
│   ├── profile/              # ProfileScreen.kt, ProfileScreenContent.kt, SignedInProfileCard.kt
│   ├── history/              # SessionHistoryScreen & ViewModel
│   ├── host/                 # HostLiveSessionScreen & ViewModel
│   ├── join/                 # JoinSessionScreen (QR + Code input)
│   ├── participant/          # ParticipantLiveSessionScreen & ViewModel
│   └── theme/                # Material Design 3 theme definitions
└── util/                     # QR Generator, Date Formatters & Extensions
```

---

## 7. Google Products & Cloud Services Matrix

### Key Products & Architectural Mapping

1. **Real-Time Voice Capture & Speech-to-Text:**
   - **Google Cloud Speech-to-Text API** / **Android SpeechRecognizer**: Converts host microphone speech audio into continuous text segments.
2. **Real-Time Data Streaming:**
   - **Firebase Cloud Firestore**: Broadcasts live transcripts, glossaries, and slide analyses instantly to all connected participants.
3. **Instant QR Code Entry:**
   - **Google ML Kit Barcode Scanning** + **Android CameraX**: On-device QR code reader for sub-second room entry.
4. **AI Terminology Auto-Detection:**
   - **Google Gemini API (Gemini 2.0 Flash / NLP)**: Real-time transcript processing to extract technical terms and output definitions.
5. **Slide Photo AI Explanation:**
   - **Google Gemini Multimodal Vision API**: OCR + image understanding to generate summaries of slides and diagrams.
6. **User Authentication:**
   - **Firebase Authentication**: Host accounts (Google Sign-In / Email) and Anonymous Participant entry.
7. **Slide Image Hosting:**
   - **Firebase Cloud Storage**: Secure storage bucket for slide photos.
8. **Backend Security & Anti-Abuse:**
   - **Firebase App Check** + **Google Play Integrity API**: Protects APIs from unauthorized calls and bots.
9. **Push Notifications & Session Alerts:**
   - **Firebase Cloud Messaging (FCM)**: Host alerts and session status updates.
10. **Session Telemetry & Health:**
    - **Firebase Crashlytics** & **Firebase Analytics**: Real-time crash monitoring and application metrics.

