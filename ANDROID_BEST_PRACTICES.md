# Konteksto Android Best Practices

These are the engineering rules for this codebase. `APP_SPECIFICATION.md` describes **what** we build, and this guide describes **how** we build it.

Each rule is one of two kinds:
- **Must:** Follow it in all new and changed code. Breaking it needs a stated reason in the pull request.
- **Should:** The default choice. Deviate when you have a good reason.

Section 13 lists where today's code doesn't follow these rules yet. When you touch that code, leave it closer to the rule.

---

## 1. Project Baseline

| Item | Value |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3, single activity |
| Min / target SDK | 33 / 36 |
| Backend | Firebase and Google Cloud only (see the spec's platform rule) |
| Dependency versions | `gradle/libs.versions.toml` is the source of truth |

**Must**
- Add every dependency through the version catalog.
- Declare libraries the code imports directly, instead of relying on another library to bring them in.

---

## 2. Architecture

### Layers
```
┌─────────────────────────────────────────┐
│ UI: Routes, Screens, ViewModels         │
└───────────────┬─────────────────────────┘
                │ depends on
┌───────────────▼─────────────────────────┐
│ Domain (optional): models, use cases    │
└───────────────▲─────────────────────────┘
                │ depends on
┌───────────────┴─────────────────────────┐
│ Data: repositories, Firebase, Gemini    │
└─────────────────────────────────────────┘

Services (for example LiveBroadcastService) use the data layer and
expose state to ViewModels. They never depend on UI classes.
```

**Must**
- **Dependencies point the right way.** `data` and `domain` never import from `ui`. A model shared between layers belongs in `domain/model`.
- **Composables and ViewModels never touch a data source directly.** Firebase, Gemini, DataStore, and speech APIs sit behind a repository or service.
- **Create a repository even when it has one data source.** Define an interface and name the implementation for what makes it different, as with `SessionRepository` and `FirebaseSessionRepository`.
- **Keep one owner for each piece of state.** Only that owner changes it, and everyone else observes it.
- **State flows down and events flow up.** ViewModels expose state, and the UI calls ViewModel functions.

**Should**
- Add a use case only when logic is shared by several ViewModels or a ViewModel has become too complex. A use case that only forwards a call to a repository adds nothing.

### Project Structure
Section 9 of the spec is the single source for the package layout. Add new features in their own `ui/<feature>` package.

### Naming
| Item | Convention | Example |
|---|---|---|
| Route and screen | `<Name>Route`, `<Name>Screen` | `HostStudioRoute`, `HostStudioScreen` |
| UI state | `<Screen>UiState` | `HostStudioUiState` |
| Streams | `get<Model>Stream` | `getTranscriptsStream()` |
| Functions | Verb phrase | `updateContextNote()` |
| Test doubles | `Fake<Name>` | `FakeSessionRepository` |

---

## 3. UI Layer

### Route and Screen
**Must** split screens that have a ViewModel into two composables:
- The **Route** collects state from the ViewModel, which `KontekstoNavHost` creates and scopes to the destination, and passes down values and callbacks.
- The **Screen** receives only plain values and callbacks, so it can be previewed and tested without a ViewModel.

```kotlin
@Composable
fun HostStudioRoute(
    viewModel: HostStudioViewModel,
    onBackClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HostStudioScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onTabSelected = viewModel::setSelectedTab,
    )
}
```

A Screen must not keep a local copy of state that the ViewModel owns. Two copies drift apart, and the UI can then show something that isn't true. For previews, pass a sample `UiState` instead of adding a local fallback.

### ViewModels
**Must**
- Hold no `Activity`, `Context`, `Resources`, or lifecycle types, and don't use `AndroidViewModel`. If a ViewModel seems to need a `Context`, that work belongs in the data layer or a service.
- Use ViewModels only at screen or navigation-destination level. Reusable components use plain state holder classes or hoisted state.
- Expose state as a read-only `StateFlow`, and keep the `MutableStateFlow` private.
- Change `MutableStateFlow` with `update`, which is atomic. `_uiState.value = _uiState.value.copy(...)` can lose a change when two writers overlap.
- Not expose `suspend` functions for business logic. The ViewModel launches the coroutine and reports the result through state.

```kotlin
_uiState.update { it.copy(isAnalyzingPdf = true) }
```

**Should**
- Build state from streams with `stateIn` and `WhileSubscribed(5_000)`, so collection stops when nothing on screen is observing it. Collectors launched in `init` keep Firestore listeners open while the app is in the background.

```kotlin
val hostSessions: StateFlow<List<HostSession>> = authRepository.getAuthStateStream()
    .flatMapLatest { user -> sessionRepository.getHostSessionsStream(user?.uid.orEmpty()) }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )
```

### Events from the ViewModel
**Must** handle results by updating state. Don't send one-time events such as `Channel<ShowToast>` to the UI, because an event can be lost if nothing is collecting at that moment.

```kotlin
// The UI shows the message, then reports that it was dismissed.
uiState.errorMessage?.let { message ->
    SlideDeckErrorDialog(message = message, onDismiss = onErrorDismissed)
}
```

### Collecting State and Lifecycle
**Must**
- Collect with `collectAsStateWithLifecycle()`, never plain `collectAsState()`.
- Use `LifecycleStartEffect`, `LifecycleResumeEffect`, or `repeatOnLifecycle` for work tied to the lifecycle, rather than overriding `Activity` callbacks.

### Saving State
| Kind of state | Where it lives | Survives rotation | Survives process death |
|---|---|---|---|
| Animation, scroll helpers | `remember` | No | No |
| Form input, selections, dialog visibility | `rememberSaveable` | Yes | Yes |
| Screen data and business state | ViewModel | Yes | No |
| Screen IDs and input the ViewModel must restore | `SavedStateHandle` | Yes | Yes |
| Sessions, transcripts, preferences | Firestore, DataStore | Yes | Yes |

**Must** use `rememberSaveable` for form text and for dialog visibility flags. This matters more now that rotation can't be blocked on large screens (Section 5).

```kotlin
// Bad: the dialog disappears when the screen rotates
var showEndConfirmDialog by remember { mutableStateOf(false) }

// Good
var showEndConfirmDialog by rememberSaveable { mutableStateOf(false) }
```

For objects that can't be saved directly, save an ID and look the object up, or write a custom `Saver`.

```kotlin
var selectedThemeId by rememberSaveable {
    mutableStateOf(ConferenceThemeBanner.DefaultCategories.first().id)
}
val selectedTheme = remember(selectedThemeId) {
    ConferenceThemeBanner.DefaultCategories.find { it.id == selectedThemeId }
        ?: ConferenceThemeBanner.DefaultCategories.first()
}
```

### File Organization
**Must**
- Put each public composable in its own file, as in `LoginRoute.kt`, `LoginScreen.kt`, and `GoogleSignInButton.kt`.
- Keep a `private` composable in the same file only if that file alone uses it.
- Keep `@Preview` functions at the bottom of the file containing the composable they render.

**Should**
- Split a file once it passes about 400 lines. That's a prompt to look for a boundary, not a hard limit.

### Navigation
**Must**
- Keep one activity.
- Pass IDs between destinations, never whole objects. The destination loads what it needs.
- Build routes in one place (`NavRoutes.kt`), and never assemble route strings at the call site.

**Should**
- Use type-safe routes in new navigation work. Android now recommends Navigation 3, and its stable release replaces string routes completely. The move is planned work (Section 13), so don't mix the two approaches within a feature.

---

## 4. Compose Performance

Strong skipping is on by default in our Kotlin version. Composables with unstable parameters can still skip recomposition, and lambdas are remembered automatically.

**Don't add `@Stable`, `@Immutable`, `ImmutableList`, or a manual `remember` around lambdas by reflex.** Annotating a model that holds a large list makes Compose compare every element on each pass, which can be slower. Add them only when profiling shows a problem.

### Long Lists
**Must** render lists that can grow with `LazyColumn` or `LazyRow`, using stable keys. A `Column` with `forEach` composes every item, so a one-hour transcript gets slower as the talk goes on.

```kotlin
LazyColumn(state = listState) {
    items(items = transcripts, key = { it.id }) { entry ->
        TranscriptRow(entry)
    }
}
```

### Expensive Work
Do sorting and filtering in the ViewModel, or at least cache the result.

```kotlin
// Bad: sorts on every recomposition
items(terms.sortedBy { it.term }) { ... }

// Good
val sortedTerms = remember(terms) { terms.sortedBy { it.term } }
```

### Fast-Changing State
Use `derivedStateOf` when input changes often but the UI cares about only some of the changes.

```kotlin
val showJumpToLive by remember {
    derivedStateOf { listState.firstVisibleItemIndex > 0 }
}
```

### Defer State Reads
For animation and scroll values, use lambda-based modifiers so Compose can skip composition.

```kotlin
// Bad: recomposes on every scroll change
Modifier.offset(y = scrollState.value.dp)

// Good: reads during layout
Modifier.offset { IntOffset(x = 0, y = scrollState.value) }

// Good: reads during drawing
Modifier.drawBehind { drawRect(animatedColor) }
```

### Backwards Writes
**Must not** write to state in a composable's body after reading it. Update state in event handlers or effects.

### Measuring
Check performance in a **release build**. Debug builds of Compose are much slower and give misleading results.

---

## 5. Android Platform Requirements

Targeting API 36 turns these on for us.

| Requirement | What it means for us |
|---|---|
| **Edge-to-edge can't be disabled** | Every screen handles insets, using `safeDrawingPadding()` or Scaffold padding. Don't use `windowOptOutEdgeToEdgeEnforcement` |
| **Predictive back is on by default** | `onBackPressed` isn't called and `KEYCODE_BACK` isn't dispatched. Use `BackHandler` |
| **Large screens ignore orientation and resize limits** | On displays 600dp wide or more, `screenOrientation` and `resizeableActivity` have no effect. Layouts must work at any width |
| **Rotation recreates the activity more often** | Follow the state table in Section 3. The temporary opt-out ends at API 37, so don't rely on it |

### Permissions
**Must**
- Ask at the moment a feature needs the permission, and explain why.
- Check the current state again on resume, because the user can revoke a permission in Settings. A value remembered once goes stale.
- Keep the app usable after a denial, with a clear explanation of what is unavailable.

### Accessibility
A live transcript is especially important to Deaf and hard-of-hearing attendees, so accessibility is a core requirement.

**Must**
- Make touch targets at least 48dp.
- Give meaningful icons a `contentDescription`, and use `null` only for decorative ones.
- Use `sp` for text, and check screens at the largest system font size.
- Never use color as the only signal. A live indicator also needs text.

### Strings and Theme
**Must**
- Put user-visible text in `strings.xml`. This is what makes translation possible.
- Take colors from `MaterialTheme.colorScheme` and define new ones in `ui/theme`, so light and dark modes both work.

### Images
Use vector drawables for icons and WebP for bitmaps.

---

## 6. Coroutines & Flow

**Must**
- **Keep suspend functions main-safe.** The class doing blocking work moves it off the main thread, and callers don't need to think about it.
- **Inject dispatchers** as constructor parameters, so tests can replace them.
- **Expose `suspend` for one-shot calls and `Flow` for changes over time** from the data layer.
- **Never use `GlobalScope`.** Inject a `CoroutineScope` for work that must outlive a screen.
- **Never swallow `CancellationException`** (Section 7).
- **Catch errors from flows.** An uncaught exception in `viewModelScope` crashes the app, so use `.catch` for any stream that can close with an error.

```kotlin
class GeminiSlideAnalyzer(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun analyzeSlideDeck(...) = withContext(ioDispatcher) { ... }
}
```

**Scope choice**

| Work | Scope |
|---|---|
| UI logic such as an animation | Composition (`rememberCoroutineScope`, `LaunchedEffect`) |
| Business logic for a screen | `viewModelScope` |
| Must finish even if the user leaves the screen | Injected application scope |
| Live session work | The service's own scope (Section 8) |

---

## 7. Error Handling

**Must**
- **Never report a failure as success.** If a repository function returns `Result`, every failure path returns `Result.failure`. Otherwise the UI tells the user something worked when it didn't.
- **Rethrow cancellation.** `catch (e: Exception)` and `runCatching` also catch `CancellationException`, which stops a cancelled coroutine from cancelling properly.
- **Prefer specific exceptions** such as `FirebaseException` and `IOException`.
- **Show users what happened and what to do,** as in: "Couldn't save your changes to 'Qubit'. Check your connection and try again."
- **Don't fall back to made-up data.** Sample data belongs in previews and tests only.

```kotlin
// Bad: also catches cancellation
suspend fun deleteContextNote(id: String): Result<Unit> = runCatching { ... }

// Good
suspend fun deleteContextNote(id: String): Result<Unit> = try {
    noteDocument(id).delete().await()
    Result.success(Unit)
} catch (e: FirebaseException) {
    Result.failure(e)
}

// Good: when you need a catch-all at a boundary
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(e)
}
```

### Logging and Crash Reports
**Must**
- Record unexpected failures with `Firebase.crashlytics.recordException(e)`. `Log` output never reaches us from users' devices.
- Never log or send to Crashlytics any transcript text, questions, context notes, email addresses, or tokens.

---

## 8. Background & Long-Running Work

**Must:** work that has to outlive a screen never runs inside a composable. Composition can end at any moment, and anything started there stops with it.

| Need | Use |
|---|---|
| Only matters while the screen is visible | `viewModelScope` |
| Must finish after leaving the screen, while the app is alive | Injected application scope |
| Can be deferred but must eventually run | WorkManager |
| The user is actively aware of it and it must keep running, like live transcription | Foreground service |

### Foreground Service Rules
**Must**
- **Declare the type.** Add `android:foregroundServiceType` and its matching permission, such as `FOREGROUND_SERVICE_MICROPHONE`.
- **Start it while the app is visible.** A `microphone` service can't be started from the background or from `BOOT_COMPLETED`.
- **Check `RECORD_AUDIO` first,** because it is a prerequisite for starting the service.
- **Show an honest ongoing notification** with controls to pause and stop.
- **Release the resource when paused.** A paused microphone must actually be closed, and the UI must never say the mic is off while it is listening.
- **Return `START_NOT_STICKY` from a microphone service.** The system can't restore mic access after restarting it in the background, so a restart would appear live while hearing nothing. Report the interruption instead.
- **Use the service's own `CoroutineScope`,** cancelled in `onDestroy`.
- **Keep the service as the owner of its state.** ViewModels observe it through a controller and send it commands.
- **Declare the type in Play Console** under **Policy > App content**.

**Should**
- Hold a wake lock only while needed and with a timeout.
- Test on devices from several manufacturers, because battery management differs between brands.

Live session behavior is specified in Feature D of `APP_SPECIFICATION.md`.

---

## 9. Firebase & Gemini

### Keep Firebase Inside the Data Layer
**Must not** expose `DocumentSnapshot`, `FirebaseUser`, or other SDK types outside `data`. Map them to domain models, as with `FirebaseUser.toDomainUser()`.

**Should** map documents by hand rather than with `toObject()`. Reflection-based mapping breaks under R8 unless model classes are kept, and explicit defaults make missing fields safer.

### One-Shot Reads
Use `.await()` from `kotlinx-coroutines-play-services` instead of callbacks.

```kotlin
suspend fun getSession(id: String): HostSession? =
    sessions.document(id).get().await().toHostSession()
```

### Writes and Offline Behavior
Firestore's offline cache is **on by default** on Android, so don't add code to enable it.

**Must not** make the UI wait on `.await()` for a write when the app should work offline. The task only completes once the **server** acknowledges the write, so it never completes while offline and never throws. A spinner waiting on it spins forever.

```kotlin
// Live data: apply locally, let Firestore sync it, and report later rejection
segmentDocument.set(data)
    .addOnFailureListener { e -> transcriptWriteErrors.tryEmit(e) }
```

- Use `.await()` for a write only when the user must know the server accepted it, and pair it with a timeout and a clear message.
- Generate document IDs on the client, so a retry overwrites the same document instead of creating a duplicate.
- Remember that security rules can reject a queued write later, so always handle the failure.

### Real-Time Listeners
Wrap listeners in `callbackFlow` and remove them in `awaitClose`.

```kotlin
fun getTranscriptsStream(sessionId: String): Flow<List<TranscriptEntry>> = callbackFlow {
    val registration = transcripts(sessionId)
        .orderBy("sequenceNumber")
        .limitToLast(PAGE_SIZE)
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            snapshot?.let { trySend(it.documents.mapNotNull { doc -> doc.toTranscriptEntry() }) }
        }
    awaitClose { registration.remove() }
}
```

**Must**
- Handle the error that `close(error)` raises, using `.catch` in the collector.
- **Listen to the smallest query that does the job.** Each document delivered is a billed read for every listener, so one new segment in a room of 1,000 attendees means 1,000 reads. Use `limitToLast`, and listen to one document rather than a whole collection when one is all you need.
- Limit how often one document is updated. Keep it to about once per second, as with `live/current`.
- Use a distributed approach for counters that many clients would change. Attendee counts are updated by the server.

### Auth State
Expose auth as a `Flow`, and keep the listener inside `callbackFlow`, as `AuthRepository.getAuthStateStream()` does.

### Project Configuration in the Repository
**Must** keep `firestore.rules`, `storage.rules`, and `firestore.indexes.json` in version control, and deploy them from there rather than editing them in the console. Rules are security code and need review and tests.

### Secrets and Abuse Protection
**Must**
- **Ship no API keys or service credentials in the app.** `BuildConfig` values can be extracted from an APK. Use Firebase AI Logic on the device, or call a backend.
- **Enforce App Check with Play Integrity** on every Firebase and backend service.
- **Enforce access in security rules.** Hiding something in the UI doesn't protect it.
- **Keep private data off attendee devices.** If a client shouldn't read host notes, build the prompt on the server.

### Gemini
**Must**
- Use the Firebase AI Logic SDK (`firebase-ai`). `firebase-vertexai` and the Google AI client SDK (`generativeai`) are deprecated.
- Read model names from **Remote Config**, and use explicit stable names rather than `-latest`. Models retire on a schedule.
- **Treat all model input as untrusted.** Fence transcripts, notes, and questions as reference data that can't change instructions, as `buildContextNotesPrompt` does.
- Request structured JSON where the output is parsed, and handle invalid output without crashing.
- Have the model leave out what it isn't sure of instead of guessing.
- Show a clear failure state for every AI call, and never display an error as if it were an answer.

**Should**
- Cache shared results, and rate-limit requests per user.

---

## 10. Dependency Injection

**Must**
- Use **constructor injection**, with dependencies typed as interfaces.
- Scope a shared instance only if it holds shared mutable data or is expensive to create.

**Target: Hilt.** Android recommends it for apps with multiple ViewModel screens, and Konteksto now has several. It also makes injecting into `LiveBroadcastService` straightforward.

```kotlin
@HiltViewModel
class HostStudioViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionRepository: SessionRepository,
) : ViewModel()
```

**Until Hilt is adopted,** follow the existing pattern: constructor parameters plus a `provideFactory` companion function. When Hilt arrives, migrate everything together rather than leaving two systems in place.

---

## 11. Release Performance & Size

**Must, before the first production release**
- Enable R8 with `isMinifyEnabled = true` and `isShrinkResources = true`, and test the release build, because R8 problems only appear there.
- Add Baseline Profiles and Startup Profiles for app launch and the live room.
- Measure startup and transcript scrolling with Macrobenchmark.

**Should**
- Use the App Startup library only if you add several components that need initialization at launch. Firebase already initializes itself.

---

## 12. Testing

**Minimum coverage**

| Test | Tools | Priority targets |
|---|---|---|
| ViewModels and state logic | JUnit, `runTest`, fakes | Mic state, session state, error paths |
| Repositories and mapping | JUnit, fakes | Document mapping, failure results |
| Security rules | Firebase Emulator Suite | Attendees can't read notes or write transcripts |
| Cloud Functions | Firebase Emulator Suite | Glossary dedupe, Explain cache, rate limits |
| UI and navigation | `createComposeRule()` | Join flow, Explain mode, start and end session |
| Foreground service | Real devices | Checklist in Section 10 of the spec |
| Performance | Macrobenchmark | Startup, long transcript scrolling |

**Must**
- **Use fakes rather than mocks.** A fake's `Flow` must react to changes, so back it with `MutableStateFlow`. `flowOf(list)` emits once and hides update bugs.
- **Replace `Dispatchers.Main`** with a test dispatcher, using `Dispatchers.setMain`, and share one scheduler across test dispatchers.
- **Use `runTest`** rather than `runBlocking`, so delays are skipped and timing is predictable.
- **Assert on `StateFlow.value`** where possible.
- **Add a regression test for each bug fix,** ideally by moving the logic into a function that can be tested on its own, as with `resolveAudioBroadcasting`.

```kotlin
class FakeSessionRepository : SessionRepository {
    private val sessions = MutableStateFlow<List<HostSession>>(emptyList())

    override fun getHostSessionsStream(hostUid: String): Flow<List<HostSession>> =
        sessions.map { list -> list.filter { it.hostUid == hostUid } }

    fun emit(vararg updated: HostSession) {
        sessions.value = updated.toList()
    }
}
```

---

## 13. Known Gaps in the Current Code

Measured when this guide was written. Product and feature fixes are tracked in Section 11 of the spec.

| Rule | Current state | Where |
|---|---|---|
| Data layer doesn't import from UI | 31 references to `ui.host.studio` types | `data/repository/` |
| Long-running work stays out of composables | Speech recognition runs in a composable and its class lives in `ui` | `HostStudioBroadcastView.kt`, `SpeechToTextManager.kt` |
| Never swallow cancellation | 34 generic `catch` or `runCatching` sites, with none rethrowing `CancellationException` | `data/repository/` |
| Never report failure as success | Transcript save returns success after a failure | `addTranscriptEntry` |
| Don't wait on writes offline | 14 awaited writes | `SessionRepository.kt` |
| Report unexpected failures | Crashlytics is installed, with no `recordException` calls | Whole app |
| Atomic state updates | 63 `_uiState.value =` assignments and no `update` calls | ViewModels |
| Collect streams with `stateIn` | 5 collectors launched in `init` | `HostStudioViewModel.kt` |
| Listen to the smallest query | Studio subscribes to all of the host's sessions to find one | `observeSession` |
| One owner for state | 5 `local*` copies of ViewModel state | `HostStudioScreen.kt` |
| Check permissions on resume | Mic permission is read once with `remember` | `HostStudioHeaderCard.kt` |
| No fake data outside tests | Hard-coded sample session in live code | `toggleSessionLiveStatus` |
| Long lists are lazy | Transcript uses `Column` with `forEach` | `HostStudioBroadcastView.kt` |
| Dialog flags survive rotation | 5 flags use plain `remember`, and 8 already use `rememberSaveable` | `ui/` |
| One public composable per file | 9 in one file | `HostStudioDialogs.kt` |
| Split large files | 1,248 and 1,071 lines | `HostStudioSlidesView.kt`, `SessionRepository.kt` |
| Strings in resources | 74 hard-coded `text = "..."` literals, including some in previews | Mostly the studio and profile screens |
| Colors from the theme | Hard-coded `Color(0x...)` in 6 files | `ui/` |
| Inject dispatchers | Hard-coded `Dispatchers.IO` | `GeminiSlideAnalyzer.kt` |
| Declare direct dependencies | `lifecycle-runtime-compose` is used but not declared | `libs.versions.toml` |
| Test tools | No `kotlinx-coroutines-test`, no emulator tests | `build.gradle.kts` |
| Type-safe navigation | Navigation Compose 2.8.8 with string routes | `NavRoutes.kt` |
| Hilt | Manual factories | ViewModels |
| R8 and profiles | `isMinifyEnabled = false`, no Baseline Profile | `build.gradle.kts` |

---

## 14. References

- [Architecture recommendations](https://developer.android.com/topic/architecture/recommendations)
- [Coroutines best practices](https://developer.android.com/kotlin/coroutines/coroutines-best-practices)
- [Strong skipping mode](https://developer.android.com/develop/ui/compose/performance/stability/strongskipping)
- [Android 16 behavior changes](https://developer.android.com/about/versions/16/behavior-changes-16)
- [Foreground service types](https://developer.android.com/develop/background-work/services/fgs/service-types)
- [Navigation 3](https://developer.android.com/guide/navigation/navigation-3)
- [Firestore offline persistence](https://firebase.google.com/docs/firestore/manage-data/enable-offline)
- [Firebase AI Logic models](https://firebase.google.com/docs/ai-logic/models)
- [Firebase AI Logic security checklist](https://firebase.google.com/docs/ai-logic/security-checklist)
