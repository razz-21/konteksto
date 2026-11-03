# Android Development Best Practices, Architecture & Optimization Guide

This document outlines standard guidelines and best practices for building robust, scalable, performant, and maintainable Android applications using Kotlin and Jetpack Compose.

---

## 1. Core Architectural Principles

- **Separation of Concerns:** Divide your app into distinct layers and modules with clearly defined responsibilities. Avoid placing business or data logic inside `Activity` or `Fragment` classes.
- **Single Source of Truth (SSOT):** Every data flow in your app should have a unique owner responsible for modifying and exposing that data immutably.
- **Unidirectional Data Flow (UDF):** State flows down from data/state holders to UI components; events flow up from UI interactions to state holders.
- **Drive UI from Persistent Data Models:** Ensure application state survives configuration changes (rotation, window resizing) and app process death by persisting critical state in repositories or local storage (e.g., Room / DataStore).

---

## 2. Layered Architecture Overview

Recommended architecture consists of three layers:

```
┌──────────────────────────────────────────────────┐
│                   UI Layer                       │
│  (Jetpack Compose UI Screens & AAC ViewModels)   │
└────────────────────────┬─────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────┐
│             Domain Layer (Optional)              │
│       (Use Cases / Complex Business Logic)       │
└────────────────────────┬─────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────┐
│                    Data Layer                    │
│   (Repositories & Data Sources: Room, Network)   │
└──────────────────────────────────────────────────┘
```

### A. UI Layer
- **Jetpack Compose:** Use Jetpack Compose for UI declaration and single-activity navigation.
- **ViewModels:** Hold screen-level UI state using `StateFlow`.
- **State Collection:** Always collect `StateFlow` in Compose using `collectAsStateWithLifecycle()` to prevent background resource leaks when the UI is hidden or paused.

```kotlin
@Composable
fun MyScreen(
    viewModel: MyViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Render UI according to uiState
}
```

- **Expose UI State via `stateIn`:** Use `SharingStarted.WhileSubscribed(5000)` so background collection pauses 5 seconds after screen destruction/stop.

```kotlin
class MyViewModel(
    repository: MyRepository
) : ViewModel() {

    val uiState: StateFlow<MyUiState> = repository.getDataStream()
        .map { MyUiState.Success(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MyUiState.Loading
        )
}
```

### B. Domain Layer (Optional)
- Use domain Use Cases (Interactors) when business logic needs to be shared across multiple ViewModels or complex logic needs encapsulation.
- Keep Use Cases focused on a single responsibility (e.g., `GetFormattedUserFeedUseCase`).

### C. Data Layer
- **Repositories:** Expose data to the rest of the application. The UI or ViewModel should never access data sources (Room DB, Retrofit API) directly.
- **Data Sources:** Abstract external data sources (network requests, local SQLite databases, DataStore preferences).
- **Concurrency:** Ensure data layer operations are `main-safe` using appropriate Coroutine Dispatchers (`Dispatchers.IO` for network/disk operations).

---

## 3. Jetpack Compose Best Practices & Optimization

### A. Minimize Recompositions with `remember`
Store expensive operations (such as sorting or filtering lists) in `remember` or perform them inside the `ViewModel`.

```kotlin
// BAD: Sorts list on every recomposition
LazyColumn {
    items(contacts.sortedBy { it.name }) { contact ->
        ContactRow(contact)
    }
}

// GOOD: Caches sorted list until contacts change
val sortedContacts = remember(contacts) { contacts.sortedBy { it.name } }
LazyColumn {
    items(sortedContacts) { contact ->
        ContactRow(contact)
    }
}
```

### B. Stable Item Keys in Lazy Layouts
Always provide stable `key` functions for `LazyColumn` or `LazyRow` items to allow Compose to reorder instead of fully recomposing items.

```kotlin
LazyColumn {
    items(
        items = itemsList,
        key = { item -> item.id }
    ) { item ->
        ItemRow(item)
    }
}
```

### C. Use `derivedStateOf` for Rapidly Changing States
Use `derivedStateOf` when state changes frequently (e.g., scroll position) but UI only updates at specific thresholds.

```kotlin
val listState = rememberLazyListState()
val showScrollToTopButton by remember {
    derivedStateOf { listState.firstVisibleItemIndex > 0 }
}
```

### D. Defer State Reads to Layout or Draw Phases
When state changes rapidly (animations, scrolling offset), use lambda-based modifiers so Compose skips composition and goes straight to layout/draw phases.

```kotlin
// BAD: Recomposes composables on every scroll change
Modifier.offset(y = scrollState.value.dp)

// GOOD: Reads state during layout phase, skipping recomposition
Modifier.offset { IntOffset(x = 0, y = scrollState.value) }

// GOOD: Reads color state during draw phase
Modifier.drawBehind { drawRect(animatedColor) }
```

### E. Avoid Backwards Writes
Never update state inside the body of a Composable function after reading it. Update state only in response to events or within lambda event handlers (e.g., `onClick`).

### F. One Composable Per File (Composable Separation)
To maintain code quality, readability, modularity, and isolated preview rendering, each Composable function should reside in its own separate file:
- **Dedicated File per Composable:** Do not stack multiple top-level UI or component composables in a single file. For instance, route wrappers (`LoginRoute.kt`), screen content (`LoginScreen.kt`), feature rows (`FeatureItem.kt`), and custom buttons (`GoogleSignInButton.kt`) should each have their own file.
- **Preview Placement:** Keep `@Preview` composables at the bottom of the file containing the primary Composable they render.
- **Benefits:** Prevents massive bloated screen files, simplifies unit testing and preview generation, improves compile times, and makes component reusability straightforward.

### G. Maintain UI State Across Configuration Changes (`rememberSaveable`)
- **Preserve User Progress & Input:** Standard `remember` only retains state during recompositions. On configuration changes (such as device rotation, screen resizing, or dark/light mode toggles), the Activity is recreated and standard `remember` state is lost.
- **Use `rememberSaveable` for Local Form & Component State:** Use `rememberSaveable` for form text inputs, selections, active dialog/picker flags, and validation error messages so user data persists seamlessly across screen rotations.

```kotlin
// BAD: Form input resets to empty string when screen is rotated
var sessionTitle by remember { mutableStateOf("") }

// GOOD: Form input survives screen rotation and Activity recreation
var sessionTitle by rememberSaveable { mutableStateOf("") }
```

- **Identifier-Based Saving for Non-Parcelable Objects:** For custom domain objects or complex selection types, save a primitive identifier in `rememberSaveable` and resolve the object dynamically, or provide a custom `Saver`.

```kotlin
// GOOD: Store primitive identifier in rememberSaveable and resolve object
var selectedThemeId by rememberSaveable {
    mutableStateOf(ConferenceThemeBanner.DefaultCategories.first().id)
}
val selectedTheme = remember(selectedThemeId) {
    ConferenceThemeBanner.DefaultCategories.find { it.id == selectedThemeId }
        ?: ConferenceThemeBanner.DefaultCategories.first()
}
```

- **ViewModel-Backed State Hoisting:** Screen-level data streams and business logic states should be hoisted into ViewModels, which automatically survive configuration changes via `ViewModelProvider`.

---

## 4. Coroutines & Kotlin Flow Best Practices

- **Never block the Main Thread:** Mark long-running operations as `suspend` and switch dispatchers explicitly inside data sources.
- **Inject Dispatchers:** Inject `CoroutineDispatcher` (e.g. `Dispatchers.IO`) to facilitate unit testing.
- **ViewModel Scope:** Launch UI-related asynchronous work inside `viewModelScope` so it cancels automatically when the ViewModel is cleared.
- **Avoid GlobalScope:** Avoid using `GlobalScope` as it breaches lifetime boundaries and causes memory leaks.

---

## 5. Dependency Injection (Hilt)

- Use **Hilt** for managing dependencies across your app lifecycle.
- Favor **Constructor Injection** over field injection wherever possible.
- Avoid passing `Context` or lifecycle references into ViewModels.

```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {
    // ViewModel implementation
}
```

---

## 6. App Performance & Startup Optimization

- **Baseline Profiles:** Include Baseline Profiles to reduce app cold-start times and JIT compilation overhead.
- **R8 / Proguard:** Keep code shrinking and obfuscation enabled for release builds (`isMinifyEnabled = true`, `isShrinkResources = true`).
- **App Startup Library:** Use Jetpack `App Startup` for initializing components lazily during app startup.
- **Images & Drawables:** Use WebP image formats and Vector drawables (`VectorAsset`) to minimize app bundle size and RAM consumption.

---

## 7. Testing Strategy

- **Test Pyramid:**
  - **Unit Tests (High ratio):** Test ViewModels, UseCases, and Repositories using Kotlin Coroutine test utilities (`runTest`, `StandardTestDispatcher`).
  - **Integration Tests:** Test Room DAO operations and data sources.
  - **UI Tests:** Use Compose Testing framework (`createComposeRule()`) for critical user flows.
- **Fakes over Mocks:** Prefer writing in-memory fake repositories over mocking framework interfaces for predictable and maintainable tests.

```kotlin
class FakeUserRepository : UserRepository {
    private val users = mutableListOf<User>()
    override fun getUsers(): Flow<List<User>> = flowOf(users)
}
```

---

## 8. Recommended Package Structure (Feature-First)

Organize code by features to maintain modularization readiness and clean separation:

```
com.example.konteksto/
├── data/
│   ├── local/            # Room DB, DataStore
│   ├── network/          # Retrofit / Ktor interfaces
│   ├── firebase/         # Firebase data sources & mappers
│   └── repository/       # Repository implementations
├── di/                   # Hilt DI Modules
├── ui/
│   ├── components/       # Shared design system components
│   ├── login/            # LoginRoute.kt, LoginScreen.kt, GoogleSignInButton.kt, FeatureItem.kt, LoginViewModel.kt
│   ├── profile/          # ProfileScreen.kt, ProfileScreenContent.kt, SignedInProfileCard.kt, SignedOutProfileCard.kt, ProfileViewModel.kt
│   ├── home/             # HomeScreen.kt, HomeViewModel.kt
│   └── theme/            # Color, Type, Theme
└── util/                 # Utility functions & extensions
```

---

## 9. Firebase Architecture & Optimization Best Practices

### A. Convert Firebase Tasks to Suspend Functions
Use `kotlinx-coroutines-play-services` `.await()` instead of callbacks for single-shot Firebase operations.

```kotlin
// GOOD: Idiomatic suspend function with coroutines
suspend fun fetchDocument(docId: String): MyData? {
    return firestore.collection("my_collection")
        .document(docId)
        .get()
        .await()
        .toObject(MyData::class.java)
}
```

### B. Convert Real-Time Firestore Listeners to Kotlin `Flow`
Wrap real-time `addSnapshotListener` calls in `callbackFlow` and clean up listeners in `awaitClose`.

```kotlin
fun getFeedStream(): Flow<List<FeedItem>> = callbackFlow {
    val listenerRegistration = firestore.collection("feed")
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val items = snapshot?.toObjects(FeedItem::class.java) ?: emptyList()
            trySend(items)
        }
    awaitClose { listenerRegistration.remove() }
}
```

### C. Abstract Firebase Behind Repositories
Never expose Firebase objects (`DocumentSnapshot`, `FirebaseUser`) directly to ViewModels or Composable screens. Map them to domain data classes inside your data repository.

```kotlin
// BAD: ViewModel directly coupled to Firebase SDK
class FeedViewModel : ViewModel() {
    val firestore = FirebaseFirestore.getInstance()
}

// GOOD: ViewModel receives clean domain data from Repository interface
@HiltViewModel
class FeedViewModel @Inject constructor(
    feedRepository: FeedRepository
) : ViewModel() {
    val feedState: StateFlow<FeedUiState> = feedRepository.getFeedStream()
        .map { FeedUiState.Success(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FeedUiState.Loading)
}
```

### D. Offline Persistence & Index Optimization
- Enable persistent disk cache for Firestore to support seamless offline usage.
- Create composite indexes in Firebase Console for complex queries involving multiple `.where()` or `.orderBy()` fields.

### E. Firebase Auth State Flow
Expose authentication state immutably as a Kotlin `Flow`:

```kotlin
fun getAuthStateStream(): Flow<UserAuthStatus> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        val user = auth.currentUser
        if (user != null) {
            trySend(UserAuthStatus.Authenticated(user.uid))
        } else {
            trySend(UserAuthStatus.Unauthenticated)
        }
    }
    firebaseAuth.addAuthStateListener(listener)
    awaitClose { firebaseAuth.removeAuthStateListener(listener) }
}
```

### F. Firebase Crashlytics & App Check
- Log non-fatal errors explicitly: `Firebase.crashlytics.recordException(e)`.
- Enforce **Firebase App Check** (with Play Integrity on Android) to safeguard your Firebase Firestore and Auth backend against abuse.

