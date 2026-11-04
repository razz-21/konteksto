# Konteksto — Live Seminar & Conference Companion App Specification

## 1. Overview

**Konteksto** is an Android companion app for live seminars, conferences, lectures, and keynotes.

### The Problem
Audience members lose the thread when a speaker uses unfamiliar jargon, acronyms, or technical concepts. They can't stop the talk to ask, and looking a term up means missing what comes next.

### The Solution
The host speaks and the audience reads a live transcript on their own phones. Before the talk, the host uploads a slide deck and context notes, which ground the AI in what this talk is actually about. During the talk, attendees can select any part of the transcript and get an explanation of it, ask a question, and browse a glossary that builds itself as the speaker talks.

### Platform Rule: Google Products Only
- Every hosted service and API is a Google product (Firebase, Google Cloud, Gemini, Google Play).
- No session data, audio, or join code is sent to a non-Google service.
- Open-source client libraries that run only on the device (for example Coil and ZXing) are allowed.

---

## 2. Scope

| In scope | Out of scope |
|---|---|
| Host session creation with slide deck and context notes | Attendee slide photo capture |
| QR code and join code room entry | Vision AI explanation of captured photos |
| Live transcript from host to audience | Storing or replaying the host's audio |
| Hosting that continues in the background | Public Q&A visible to the whole room |
| Explain mode and private AI questions | iOS and web clients |
| AI terminology and glossary engine | |
| Session history and transcript review | |

> The host's **slide deck upload stays in scope**. Gemini reads the host's PDF once, before the talk, to produce text context. What is excluded is attendees photographing slides during the talk.

---

## 3. Roles

### Host (Speaker / Presenter)
Signs in with Google or email. Can:
- Create a session and share its QR code and join code.
- Upload a slide deck, add context notes, and review or edit the AI's slide summaries and terms.
- Start, pause, resume, and end the live stream, including from the notification while the app is in the background.

### Participant (Audience)
Joins with no account required (anonymous Firebase Auth), and can link a Google account later to keep their history. Can:
- Scan a QR code or enter a join code to enter the room.
- Read the live transcript.
- Turn on Explain mode, select transcript text, and get an explanation.
- Ask their own questions about the talk.
- Tap highlighted terms and browse the session glossary.
- Review and bookmark past sessions.

---

## 4. Application Flow

```
        HOST                                            PARTICIPANT
          │                                                  │
 1. Create session                                 1. Scan QR / enter code
 2. Upload deck + context notes                    2. Enter room
 3. AI prepares slide summaries + starter terms    3. Read live transcript
 4. Share QR + code                                          │
 5. Start stream                                             │
          │                                                  │
          ▼                                                  │
 ┌──────────────────────────┐                                │
 │ LiveBroadcastService     │  keeps running when the app    │
 │ (microphone foreground   │  is backgrounded or the        │
 │  service + notification) │  screen is locked              │
 └────────────┬─────────────┘                                │
              │ speech                                       │
              ▼                                              │
 ┌──────────────────────────┐   ┌──────────────────────┐     │
 │ Speech-to-Text           ├──►│ Firestore            ├────►│ live transcript
 └──────────────────────────┘   │ transcripts/glossary │     │ + highlighted terms
                                └───┬──────────────▲───┘     │
                       new segments │              │ terms   │
                                    ▼              │         │
                                ┌──────────────────┴───┐     │
                                │ Glossary Engine      │     │
                                │ (Cloud Function +    │     │
                                │  Gemini)             │     │
                                └──────────────────────┘     │
                                                             │
                                ┌──────────────────────┐     │
                                │ Explain / Ask        │◄────┤ select text + Explain
                                │ (Cloud Function +    ├────►│ private answer sheet
                                │  Gemini + host       │     │
                                │  context)            │     │
                                └──────────────────────┘     │
```

---

## 5. Features

### Feature A: Session Setup & Context (Host)
1. The host enters title, speaker name, category, venue, time, spoken language, and a cover photo.
2. The host optionally uploads a **slide deck (PDF)** to Cloud Storage and adds **context notes**: background, audience level, definitions, and intended meanings.
3. Gemini reads the deck and notes, then produces a title, summary, explanation, and key takeaway for each slide, plus a **starter glossary**.
4. The host reviews and can edit or delete any slide summary, term, or note.
5. If notes change after analysis, the studio prompts the host to analyze again.

The deck and notes are optional. Without them, Explain and the glossary still work from the transcript alone, with less specific results.

### Feature B: Room Entry
**Join code**
- 6 characters from an unambiguous alphabet that leaves out `0`, `O`, `1`, and `I` (for example `MWJSML`).
- Reserved in a Firestore transaction through `joinCodes/{code}`, so two sessions can never share a code.
- Released when the session has ended and its history retention no longer needs entry by code.

**QR code**
- Generated **on the device** with ZXing and never through an online QR service.
- Encodes an Android App Link: `https://<project>.web.app/join/{code}`.
- Firebase Hosting serves the `assetlinks.json` file and a fallback page with the Play Store link, so a QR scanned with the phone's normal camera also works.

**Participant flow**
1. Tap **Join Session**.
2. Scan with the Google code scanner, which needs no camera permission in Konteksto, or type the code.
3. Sign in anonymously if not already signed in, and enter the room.

| Session state | What the participant sees |
|---|---|
| `not_live` | Waiting room with session details |
| `live` | Live transcript |
| `paused` | Transcript so far, with a "Host paused" banner |
| `interrupted` | Transcript so far, with a "Reconnecting to host…" banner |
| `ended` | Full transcript in read-only review |

### Feature C: Live Transcription
- Speech-to-text output is split into **segments** at natural pauses. Each final segment becomes a Firestore document.
- The in-progress sentence is written to one `live/current` document, at most once per second, so attendees see words forming without creating a document for each update.
- Attendees receive updates through Firestore snapshot listeners (`callbackFlow` into `StateFlow`).
- A late joiner loads the most recent segments first and pages backward through earlier ones.
- The transcript auto-scrolls unless the attendee has scrolled up or is selecting text, and a "Jump to live" button returns them to the latest line.
- The session's spoken language is chosen by the host instead of being inferred from the device locale.

**Speech engine** (behind a `TranscriptionEngine` interface so the two can be swapped):

| | Android `SpeechRecognizer` (current) | Cloud Speech-to-Text V2, Chirp 3 (target) |
|---|---|---|
| Fit | Android's reference says it is **not intended for continuous recognition** | Built for streaming transcription |
| Continuity | Restart loop, which can miss words in each gap | Continuous audio, with stream rotation before the 5-minute limit |
| Language | Device-dependent | Supports `fil-PH` (Preview at time of writing) and multilingual recognition |
| Backend | None | Cloud Run relay, because credentials must stay off the device |
| Cost | No per-minute API charge | Billed by audio processed |

Target flow: `AudioRecord` (16 kHz, 16-bit PCM) → secure WebSocket to Cloud Run (Firebase ID token and App Check token) → `StreamingRecognize` → final segments written to Firestore by the relay. The relay rotates streams before the limit without a visible break, and the client reconnects automatically if its connection is closed.

### Feature D: Background Hosting
**Once the host starts the stream, it keeps running when they leave the app, switch apps, or lock the screen.** Transcripts keep reaching the audience and the glossary keeps growing.

**How it works**
- `LiveBroadcastService`, a foreground service of type `microphone`, owns the microphone, the speech engine, and transcript publishing.
- Today this work lives inside a composable and stops when the host leaves the screen. It must move out of the UI. `HostStudioViewModel` observes a shared `LiveBroadcastController` and sends it commands.
- The service must start **while the app is visible**, because Android does not allow a microphone foreground service to be started from the background. The natural moment is when the host taps **Start**.
- It runs its own `CoroutineScope`, so it is unaffected by the destruction of the Activity or ViewModel.
- It holds a `PARTIAL_WAKE_LOCK` only while live, with a safety timeout.

**Permissions**
```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.WAKE_LOCK" />

<!-- Required for SpeechRecognizer while that engine is in use -->
<queries>
    <intent>
        <action android:name="android.speech.RecognitionService" />
    </intent>
</queries>

<service
    android:name=".service.LiveBroadcastService"
    android:exported="false"
    android:foregroundServiceType="microphone" />
```

Before starting, the host is asked for microphone and notification permission, with a plain explanation that the microphone stays on until they end the session. If notification permission is denied, streaming still works, and the studio warns that the host won't have lock-screen controls.

**Notification (ongoing, for as long as the session is live)**
- Shows the session title, status (Live or Paused), and elapsed time.
- Actions: **Pause / Resume** and **End session**. **End session** opens a confirmation in the app so it can't be triggered by accident.
- Tapping the notification returns to the Host Studio.

**State and interruptions**

| Event | Behavior |
|---|---|
| Host pauses | Microphone is **actually released**, status becomes `paused`, and attendees see a banner |
| App backgrounded or screen locked | Stream continues with no change |
| Host swipes the app from recents | Stream continues, because the service keeps running |
| Phone call or another app takes the microphone | Status shows "Mic unavailable" and capture resumes when the mic returns |
| Network lost | Status shows "Reconnecting", final segments queue in Firestore's offline cache, and the gap is logged as a system event |
| No speech for 15 minutes | Notification asks the host whether to keep streaming, so a forgotten session doesn't record indefinitely |
| Process killed | The service does not restart itself, because it can't restart the mic from the background. The heartbeat lapses, attendees see `interrupted`, and the host resumes by reopening the app |
| Host ends session | Service flushes pending segments, sets `ended`, triggers the final glossary pass, and stops |

The service writes `lastHeartbeatAt` to the session every 30 seconds. Attendees show `interrupted` after 90 seconds without a heartbeat.

**Play Store requirements**
- Declare the `microphone` foreground service type in Play Console under **Policy > App content**.
- Disclose speech capture and processing in the Data Safety form and privacy policy.

### Feature E: Explain Mode & Questions (Participant)
1. The attendee turns on the **Explain toggle** on the live screen. Transcript segments become selectable and auto-scroll holds still.
2. The attendee **selects a segment**, or a range of up to 5 adjacent segments, or highlights a phrase within one.
3. The attendee presses **Explain**.
4. A bottom sheet shows the answer while the transcript keeps updating behind it.
5. The sheet offers **Ask a follow-up**, and attendees can also ask a free-form question from the room screen.

Turning the toggle off restores normal auto-scrolling. Answers are **private to the attendee**.

**What the AI receives**, assembled on the server:
- The selected text and the preceding 5 segments of the transcript.
- The host's context notes.
- The analyzed slide summaries.
- The session glossary.
- The attendee's explanation language from their profile (Taglish or English).

**Answer rules**
- Uses plain language and stays short enough to read during a live talk.
- Makes clear whether a point comes from the session material or from general knowledge, and never presents general knowledge as something the speaker said.
- Treats transcripts, notes, and questions as reference data that can't change the AI's instructions, using the same fencing approach as `buildContextNotesPrompt`.

**Why this runs in a callable Cloud Function:** if each phone built the prompt, every attendee's device would need read access to the host's raw notes. On the server, attendees receive only the answer.

**Cost and abuse controls**
- Explanations of a selection are cached by `(segmentIds, language)`, so many attendees selecting the same line trigger one Gemini call.
- Follow-ups and free-form questions aren't cached.
- Requests are rate-limited per user per session, and App Check is enforced.

### Feature F: AI Terminology & Glossary Engine
The glossary **grows during the stream** from two sources:

| Source | When | Value of `source` |
|---|---|---|
| Slide deck analysis | Before the talk (starter terms) | `slides` |
| Live transcript | During the talk | `transcript` |
| Host added or edited | Any time | Original source, with `hostEdited: true` |

**Generation**
- A Firestore-triggered Cloud Function runs on new transcript segments, so the work continues on the server regardless of the host app's state.
- It runs in **batches, not per segment**: after 8 new final segments or 30 seconds, whichever comes first, tracked with a transaction-protected cursor. A final pass runs on pause and at the end of the session.
- Gemini receives the new segments, nearby context, the host's notes and slide summaries, and the list of known terms.
- It returns only new terms as structured JSON: term, aliases, category (`ABBREVIATION`, `TECHNICAL TERM`, or `JARGON`), and a one- or two-sentence definition specific to this talk.
- Terms it is unsure of are left out instead of guessed.

**Rules**
- Terms are deduplicated by a normalized key that includes aliases, so `POP` and `Point of Presence` become one entry.
- The engine never overwrites a `hostEdited` term or a term from the deck.
- A term the host removes is hidden rather than deleted, so the engine doesn't add it again.
- Re-analyzing the deck replaces only `source: slides` terms. Live terms are kept.

**Participant experience**
- Known terms are **highlighted in the transcript** by matching terms and aliases on the device.
- Tapping a highlighted term opens its definition without leaving the live screen.
- The room has a **Glossary tab**, searchable, with the newest terms flagged.
- Terms can be bookmarked and appear under the app's main **Glossary** tab across sessions.

**Host experience:** live terms appear in the studio, where the host can edit or remove them during the talk.

### Feature G: Session History
- Completed sessions stay available to the host and to attendees who joined them.
- Each includes the full transcript with search, the glossary, and the attendee's bookmarks and own explanations.
- Export to Text or PDF.

---

## 6. Technical Architecture

### Android Client
- **UI:** Jetpack Compose, Material 3, single-activity navigation.
- **Pattern:** Unidirectional data flow with ViewModels, `StateFlow`, and repositories, following `ANDROID_BEST_PRACTICES.md`.
- **Async:** Kotlin Coroutines, collected with `collectAsStateWithLifecycle()`.
- **Dependency injection:** Hilt is the target. The code uses manual factories today, and adopting Hilt is planned work (Section 11).
- **Minimum / target SDK:** 33 / 36.

### Where Each AI Task Runs

| Task | Runs on | Why |
|---|---|---|
| Slide deck analysis | Host device, Firebase AI Logic with App Check | Only the host uses it, and they own the data |
| Glossary engine | Cloud Functions (Firestore trigger) | Runs independently of the host's phone, and clients can't forge terms |
| Explain and questions | Cloud Functions (callable) | Keeps host notes off attendee devices, and centralizes caching and rate limits |
| Speech-to-text relay | Cloud Run | Needs a long-lived streaming connection, which Cloud Functions isn't suited to |

### AI Models
- Model names are **never hard-coded**. They are read from **Firebase Remote Config**, so a retired model can be replaced without an app release.
- Use explicit stable names and not `-latest` aliases.
- Recommended at time of writing: `gemini-3.8-flash` for deck analysis and Explain, and `gemini-3.5-flash-lite` for the glossary engine. Check the Firebase AI Logic models page before release.

### Security
- **No Gemini API key in the app.** Anything in `BuildConfig` can be extracted from the APK.
- **App Check with Play Integrity** is enforced on Firestore, Cloud Storage, Cloud Functions, Firebase AI Logic, and the Cloud Run relay.
- **Audio is streamed for transcription only** and never stored.
- **Firestore and Storage rules live in the repository** and are tested with the Firebase Emulator Suite.

---

## 7. Google Products Matrix

| Need | Product |
|---|---|
| Host and participant identity | Firebase Authentication (Google, email, anonymous) |
| Google sign-in UI | Android Credential Manager |
| Real-time data | Cloud Firestore |
| Slide deck files and cover photos | Cloud Storage for Firebase |
| Speech-to-text | Cloud Speech-to-Text V2 (Chirp 3), with Android `SpeechRecognizer` as the interim engine |
| Speech relay | Cloud Run |
| Glossary and Explain backend | Cloud Functions for Firebase (2nd gen) |
| Generative AI | Gemini API, through Firebase AI Logic on the device and the Google Gen AI SDK on the server |
| AI model configuration | Firebase Remote Config |
| QR scanning | Google code scanner (ML Kit through Google Play services) |
| Join links and fallback page | Android App Links with Firebase Hosting |
| Abuse protection | Firebase App Check with Play Integrity |
| Session alerts | Firebase Cloud Messaging |
| Crash and usage monitoring | Firebase Crashlytics and Google Analytics for Firebase |
| Backend logs | Cloud Logging |
| Distribution | Google Play |

Firebase Dynamic Links is not an option: the service was shut down on August 25, 2025.

**Open-source libraries on the device (no third-party service):** ZXing core for QR generation (`com.google.zxing:core`) and Coil for image loading.

---

## 8. Firestore Data Model

### `sessions/{sessionId}`
```json
{
  "sessionId": "session_1710000000000",
  "hostUid": "host_user_123",
  "title": "Introduction to Quantum Computing",
  "speakerName": "Dr. Sarah Chen",
  "category": "Physics & Computing",
  "venue": "Main Hall",
  "startTime": "10:00 AM",
  "endTime": "11:00 AM",
  "bannerImageUrl": "https://...",
  "talkCode": "MWJSML",
  "spokenLanguage": "en-US",
  "status": "live",
  "lastHeartbeatAt": 1710000030000,
  "attendeesCount": 120,
  "slideDeckFileName": "quantum.pdf",
  "slideDeckSizeMb": 14.2,
  "slideDeckPageCount": 32,
  "slideDeckUploadTime": "Uploaded at 8:45 AM",
  "isSlideDeckAnalyzed": true,
  "analyzedContextNotesSignature": "...",
  "createdAt": 1710000000000,
  "endedAt": null
}
```
`status` is one of `not_live`, `live`, `paused`, `interrupted`, or `ended`.

### `joinCodes/{code}`
```json
{ "sessionId": "session_1710000000000", "createdAt": 1710000000000 }
```

### `sessions/{sessionId}/transcripts/{segmentId}`
```json
{
  "id": "seg_000123",
  "sequenceNumber": 123,
  "speakerName": "Dr. Sarah Chen",
  "text": "Today we are discussing qubits and quantum superposition.",
  "isSystemEvent": false,
  "createdAt": 1710000015000
}
```

### `sessions/{sessionId}/live/current`
```json
{ "partialText": "and the reason this matters is", "updatedAt": 1710000016000 }
```

### `sessions/{sessionId}/glossary/{termId}`
```json
{
  "termId": "term_qubit",
  "term": "Qubit",
  "normalizedKey": "qubit",
  "aliases": ["quantum bit"],
  "category": "TECHNICAL TERM",
  "definition": "The basic unit of quantum information, which can exist in superposition.",
  "source": "transcript",
  "firstSegmentId": "seg_000123",
  "hostEdited": false,
  "hidden": false,
  "timestamp": 1710000020000
}
```

### `sessions/{sessionId}/analyzed_slides/{slideId}`
```json
{
  "id": "s_1",
  "slideNumber": 1,
  "title": "What is a qubit?",
  "summary": "...",
  "explanation": "...",
  "keyTakeaway": "...",
  "analyzedAt": 1710000000000
}
```

### `sessions/{sessionId}/context_notes/{noteId}`
```json
{
  "id": "note_1",
  "category": "BACKGROUND NOTE",
  "title": "Audience level",
  "content": "Undergraduates with no physics background.",
  "timestamp": "8:30 AM",
  "createdAt": 1710000000000
}
```

### `sessions/{sessionId}/explanation_cache/{cacheKey}` (server only)
```json
{
  "segmentIds": ["seg_000123"],
  "language": "taglish",
  "answer": "...",
  "createdAt": 1710000040000
}
```

### `sessions/{sessionId}/participants/{uid}`
```json
{ "joinedAt": 1710000010000, "lastSeenAt": 1710000050000 }
```

### `users/{uid}/saved/{itemId}`
```json
{
  "type": "explanation",
  "sessionId": "session_1710000000000",
  "segmentIds": ["seg_000123"],
  "question": null,
  "answer": "...",
  "createdAt": 1710000040000
}
```
`type` is `explanation` or `term`.

### Access Rules

| Path | Host | Participant | Server |
|---|---|---|---|
| `sessions/{id}` | Read, write own | Read fields needed for the room | Read, write |
| `joinCodes/{code}` | Create | Get a single code only, **no listing** | Read, write |
| `transcripts`, `live` | Write while using the on-device engine | Read | Write |
| `glossary` | Read, edit, hide | Read | Write |
| `analyzed_slides` | Read, write | **No access** | Read |
| `context_notes` | Read, write | **No access** | Read |
| `explanation_cache` | No access | **No access** | Read, write |
| `participants/{uid}` | Read | Write own | Read, write |
| `users/{uid}/**` | Own only | Own only | Read, write |

`attendeesCount` is updated by the server from `participants`, not by each client.

---

## 9. Project Structure

```
app/src/main/java/com/example/konteksto/
├── data/
│   ├── preferences/          # DataStore preferences
│   ├── repository/           # Auth, Session, Transcript, Glossary, Explanation
│   └── speech/               # TranscriptionEngine, Android and Chirp engines
├── di/                       # Hilt modules
├── domain/
│   ├── model/                # HostSession, TranscriptSegment, GlossaryTerm, Explanation
│   └── usecase/              # Only where logic is shared between ViewModels
├── service/
│   ├── LiveBroadcastService.kt
│   ├── LiveBroadcastController.kt
│   └── LiveBroadcastNotification.kt
└── ui/
    ├── components/           # Shared UI
    ├── login/
    ├── home/
    ├── main/                 # Bottom navigation
    ├── navigation/
    ├── host/                 # Session list, creation, QR
    │   └── studio/           # Host Studio, deck, notes, terms
    ├── join/                 # Code entry and QR scanning
    ├── room/                 # Live transcript, Explain mode, room glossary
    ├── sessions/             # Session history and review
    ├── glossary/             # Saved terms across sessions
    ├── profile/
    └── theme/

functions/                    # Cloud Functions: glossary engine, Explain, attendee count
relay/                        # Cloud Run speech-to-text relay
firestore.rules
storage.rules
firebase.json
```

---

## 10. Quality Targets & Acceptance Tests

These are targets to validate on real devices.

| Area | Target |
|---|---|
| Final segment from speech to attendee screen | Under 3 seconds (p95) |
| QR scan to inside the room | Under 5 seconds |
| Explain press to first answer shown | Under 4 seconds (p95) |
| New term appears after being spoken | Under 45 seconds |
| Audience size to load test | 1,000 attendees in one room |

**Background hosting acceptance tests**
- [ ] Start a stream, lock the screen for 60 minutes, and confirm that attendees receive the transcript throughout.
- [ ] Switch to another app for 10 minutes while live, and confirm there is no gap in the transcript.
- [ ] Swipe the app from recents while live, and confirm that streaming continues and the notification remains.
- [ ] Pause from the notification, and confirm that the system microphone indicator turns off.
- [ ] End from the notification, and confirm that the service stops and attendees see `ended`.
- [ ] Make a call during a stream, and confirm that capture recovers afterward.
- [ ] Turn off the network for 2 minutes, and confirm that streaming recovers and the gap is logged.
- [ ] Force-stop the app, and confirm that attendees see `interrupted` within 90 seconds and the host can resume.
- [ ] Deny notification permission, and confirm that streaming still works and the host is warned.
- [ ] Run these on at least a Pixel and a Samsung device, plus a brand with aggressive battery management such as Xiaomi or Oppo.

---

## 11. Current Build Status

Status of the code when this spec was written.

| Area | Status | Remaining work |
|---|---|---|
| Google, email, and anonymous auth | Built | Link anonymous accounts |
| Host session creation | Built | Spoken language field |
| Deck upload and analysis, notes, term editing | Built | Move to Firebase AI Logic (below) |
| Host transcript | Partial | Move out of the composable into `LiveBroadcastService` |
| Join codes and QR | Partial | Unique 6-character codes, on-device QR, App Links |
| Background hosting | Not started | All of Feature D |
| Participant join and live room | Not started | `SessionsScreen` is a placeholder |
| Explain mode and questions | Not started | Client and Cloud Function |
| Live glossary engine | Not started | Only deck-derived terms exist. `GlossaryScreen` is a placeholder |
| Session history | Not started | |
| Explanation language setting | Partial | Card is display-only, so selection must be saved |
| Cloud Functions, Cloud Run, rules files | Not started | None are in the repository |
| App Check | Not started | Dependency and enforcement |
| Hilt | Not started | Manual factories today |

**Fixes to make in the existing code**

| Issue | Location | Fix |
|---|---|---|
| The join code is sent to `api.qrserver.com`, a non-Google service | `PresentQrScreen.kt` | Generate the QR on the device |
| Generated codes have only 900 possible values and no uniqueness check | `SessionRepository.kt` (`createSession`) | Use `joinCodes` reservation |
| The Gemini API key is compiled into the app | `build.gradle.kts`, `GeminiSlideAnalyzer.kt` | Move to Firebase AI Logic with App Check |
| Deprecated SDKs: `generativeai` is archived, and `firebase-vertexai` was removed from the BoM in 35.0.0 | `build.gradle.kts`, `libs.versions.toml` | Replace with `firebase-ai` |
| Model names are hard-coded | `GeminiSlideAnalyzer.kt` | Use Remote Config |
| Transcript save failures are logged but reported as success, so the host can't tell attendees aren't receiving text | `SessionRepository.kt` (`addTranscriptEntry`) | Return the failure and show it |
| Fallback uses a made-up session with sample data | `HostStudioViewModel.kt` (`toggleSessionLiveStatus`) | Remove it and show an error |
| `RecognitionService` `<queries>` entry is missing | `AndroidManifest.xml` | Add it while `SpeechRecognizer` is in use |

---

## 12. Open Decisions

| Decision | Recommendation |
|---|---|
| When to move to Chirp 3 | First test `SpeechRecognizer` inside the foreground service with the screen locked. If it fails or drops too many words, make Chirp 3 required for launch |
| Taglish transcription quality | Test real Taglish speech on Chirp 3 before committing, since `fil-PH` is in Preview and mixed-language accuracy is unproven for this app |
| Billing | Cloud Functions and Cloud Run need the Firebase Blaze plan. Set budget alerts before launch |
| Data retention | Decide how long transcripts are kept and how a host deletes a session, including how speakers and attendees are told |
