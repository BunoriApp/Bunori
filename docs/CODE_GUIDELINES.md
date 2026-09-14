# LNCrawler Code Guidelines

`ARCHITECTURE.md` says *where* code goes. This file says *how* code should be
written once it's there — down to the level of "should this be a class,"
"should this be a boolean," and "should this be typed here every time" — and,
starting at Section 11, *which layer is allowed to depend on which*, since
in-file discipline alone doesn't produce a layered codebase on its own.

These rules exist because of a specific, recurring problem in the current
codebase: the same small decision (how to build a batch, how to track which
dialog is open, how to color a top bar, how to launch a file picker) gets
solved slightly differently every time it comes up, instead of being solved
once. The result is a codebase where no two files quite agree with each
other. Every rule below is aimed at stopping that.

---

## 1. The Core Principle

> **If a piece of logic or a piece of state has a name, it deserves its own
> small class — even if that class is 10 lines long.**

A small, well-named class is not overhead. It is the thing that makes a
concept *findable*, *testable*, and *reusable* on its own, independent of
whatever screen or ViewModel first needed it.

The instinct to avoid this ("it's only used once, I'll just inline it") is
exactly how the same logic ends up rewritten five times in five different
files, each slightly different, each equally hard to find.

**Ask this before writing inline logic:**

- Does this represent a *concept* (a batch payload, a dialog, a filter, a
  sort order, a file-export operation)? → It's a class.
- Is this just local, throwaway plumbing for one function (a temp index, a
  loop accumulator)? → It's fine inline.

If you're not sure which one it is, it's almost always the first one.

---

## 2. One Class Per Concept, No Matter How Small

### 2.1 Model your states, don't flag them

**Don't** represent "which one thing is currently happening" with a pile of
independent booleans:

```kotlin
// Bad — 4 variables to track ONE concept: "is a dialog open, and which one"
var showDeleteConfirmation by remember { mutableStateOf(false) }
var showDownloadDialog by remember { mutableStateOf(false) }
var showExportDialog by remember { mutableStateOf(false) }
var showExportWarning by remember { mutableStateOf(false) }
var pendingExportFormat by remember { mutableStateOf<ExportFormat?>(null) }
var selectedArtifact by remember { mutableStateOf<Artifact?>(null) }
```

This shape is easy to write and easy to get wrong: nothing stops two of these
being `true` at once, and `pendingExportFormat`/`selectedArtifact` only exist
to carry data between two flags that are conceptually one thing.

**Do** model it as one small sealed type, with each variant carrying exactly
the data it needs:

```kotlin
sealed interface NovelDetailDialog {
    data class ConfirmDelete(val novel: Novel) : NovelDetailDialog
    data class DownloadRange(val totalChapters: Int) : NovelDetailDialog
    data class ExportWarning(
        val format: ExportFormat,
        val downloaded: Int,
        val total: Int,
    ) : NovelDetailDialog
}
```

```kotlin
// ViewModel
private val _dialog = MutableStateFlow<NovelDetailDialog?>(null)
val dialog: StateFlow<NovelDetailDialog?> = _dialog.asStateFlow()

fun openDeleteConfirmation(novel: Novel) { _dialog.value = NovelDetailDialog.ConfirmDelete(novel) }
fun dismissDialog() { _dialog.value = null }
```

```kotlin
// Screen
when (val dialog = dialogState) {
    is NovelDetailDialog.ConfirmDelete -> ConfirmDeleteDialog(...)
    is NovelDetailDialog.DownloadRange -> DownloadRangeDialog(...)
    is NovelDetailDialog.ExportWarning -> ExportWarningDialog(...)
    null -> Unit
}
```

One field. No impossible states. No leftover data hanging around after a
dialog closes. This is the standard pattern — use it for **every** screen
with more than one dialog/sheet, not just the ones that are currently
painful.

### 2.2 Extract repeated construction into a named builder

If you catch yourself building the same kind of object (a batch, a
metadata blob, a file name, an export payload) more than once with slightly
different fields each time, that construction logic is a concept and
deserves its own class.

**Don't** rebuild the same shape by hand at every call site:

```kotlin
// Repeated 6 times across NovelDetailViewModel, each slightly different
val metadata = JSONObject().apply {
    put("crawlerName", novel.crawlerName)
    put("startIndex", start)
    put("endIndex", end)
}.toString()

val batch = RequestEntity(
    id = requestId,
    type = RequestType.RANGE_DOWNLOAD,
    novelUrl = novel.url,
    name = "Download: ${novel.title} ($start-$end)",
    metadata = metadata,
    parentNovel = novel.url,
    url = novel.url,
    status = RequestStatus.PENDING,
    rstatus = RequestStatus.PENDING,
    completedAt = null,
    progressTotal = rangeChapters.size,
)
```

**Do** give the construction logic one home:

```kotlin
class RequestFactory {
    fun rangeDownload(novel: Novel, range: IntRange, chapterCount: Int): RequestEntity = ...
    fun downloadAll(novel: Novel, chapterCount: Int): RequestEntity = ...
    fun downloadVolume(novel: Novel, volumeIndex: Int, chapters: List<Chapter>): RequestEntity = ...
    fun chapter(novel: Novel, chapter: Chapter): RequestEntity = ...
    fun metadata(novel: Novel): RequestEntity = ...
    fun export(novel: Novel, format: ExportFormat, range: IntRange): RequestEntity = ...
}
```

```kotlin
// ViewModel becomes intent, not construction
fun fetchRange(novel: Novel) {
    viewModelScope.launch {
        batchRepository.insertRequests(listOf(requestFactory.rangeDownload(novel, range, count)))
        batchRepository.startScheduler()
    }
}
```

Now there's exactly one place that knows what fields a `RequestEntity`
needs, and every batch looks the same shape by construction — not by
everyone remembering to copy the previous function correctly.

### 2.3 Small ≠ inline

A 3-line `data class` in its own file is not clutter. It's a name a future
contributor can search for. Compare:

```kotlin
// Buried as a local var inside a 40-line composable, invisible to search
var isCompactMode by remember { mutableStateOf(false) }
```

vs. surfacing it as a real, named concept when it's actually a mode the
feature supports, not incidental UI state:

```kotlin
enum class SearchResultLayout { COMFORTABLE, COMPACT }
```

Rule of thumb: if you'd ever want to grep for the concept by name from
outside the file it's declared in, it needs to be a top-level declaration —
even a one-line `enum class` — not a local `var`.

---

## 3. Pick One Way to Get a Dependency, and Use It Everywhere

Do not mix construction styles within the same class:

```kotlin
// Bad — one dependency is injected, five are fetched from a static singleton,
// in the same constructor block. A reader can't tell which pattern is "the real one."
class NovelDetailViewModel(
    application: Application,
    private val batchRepository: BatchRepository,       // injected
) : AndroidViewModel(application) {
    private val novelRepository = NovelRepository.getInstance(application)      // not injected
    private val volumeRepository = VolumeRepository.getInstance(application)    // not injected
    private val artifactRepository = ArtifactRepository.getInstance(application)
    private val chapterRepository = ChapterRepository.getInstance(application)
}
```

**Do** inject everything the class needs, and let the provider/factory own
the `getInstance()` calls in exactly one place:

```kotlin
class NovelDetailViewModel(
    private val novelRepository: NovelRepository,
    private val volumeRepository: VolumeRepository,
    private val chapterRepository: ChapterRepository,
    private val artifactRepository: ArtifactRepository,
    private val batchRepository: BatchRepository,
    private val requestFactory: RequestFactory,
) : ViewModel()
```

If a `ViewModelFactory` currently constructs this by calling `getInstance()`
five times, that's fine — but it should be the **only** place that does, and
every ViewModel should look the same shape as a result. Consistency here is
what lets a new contributor read one ViewModel and understand the
construction pattern for all of them.

---

## 4. Platform Actions Get One Shared Implementation, Not One Per Screen

If two screens both need "let the user pick a file/folder," write it once.

**Don't** let each screen invent its own launcher:

```kotlin
// NovelArtifactsScreen.kt
val exportLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/epub+zip")
) { uri -> ... }

// DownloadPreferencesScreen.kt — a different, independently-written flow
// for the same underlying capability (pick a filesystem location)
val launcher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocumentTree()
) { uri -> ... }
```

**Do** wrap the capability once, and give screens a small, declarative API:

```kotlin
// ui/core/platform/FilePicker.kt
@Composable
fun rememberFileExportLauncher(onResult: (Uri?) -> Unit): FileExportLauncher { ... }

@Composable
fun rememberFolderPickerLauncher(onResult: (Uri?) -> Unit): FolderPickerLauncher { ... }
```

Every future screen that needs to export or pick a folder calls the shared
function. If the permission-taking logic (`takePersistableUriPermission`)
needs to change, it changes in one place instead of two (soon to be three,
four...).

---

## 5. Reach for a Type Before You Reach for a String

If two pieces of code need to agree on a fixed set of possibilities, that's
an `enum class` or `sealed interface` — not a string that both sides have to
spell identically.

**Don't:**

```kotlin
private val _context = MutableStateFlow<Pair<String, String>?>(null)
// ...
when (type) {
    "ALL" -> batchRepository.getRootRequests()
    "NOVEL" -> batchRepository.getRootRequestByNovelFlow(value)
    "DEPENDENCY" -> batchRepository.getRequestsByDependenceFlow(value)
    else -> flowOf(emptyList())
}
```

Nothing catches a typo here at compile time, and nothing tells a reader what
the valid values are without reading every call site.

**Do:**

```kotlin
sealed interface RequestScope {
    data object All : RequestScope
    data class ByNovel(val novelUrl: String) : RequestScope
    data class ByDependency(val requestId: String) : RequestScope
}
```

```kotlin
when (scope) {
    is RequestScope.All -> batchRepository.getRootRequests()
    is RequestScope.ByNovel -> batchRepository.getRootRequestByNovelFlow(scope.novelUrl)
    is RequestScope.ByDependency -> batchRepository.getRequestsByDependenceFlow(scope.requestId)
}
```

The compiler now enforces exhaustiveness (`when` won't compile if a case is
missing), and there's no `else -> flowOf(emptyList())` silently swallowing a
typo'd string.

This applies everywhere a raw `String`, `Int`, or `Pair<String, String>` is
being used to represent "one of a few known kinds of thing."

---

## 6. Repeated Visual Values Are Tokens, Not Retyped Literals

If you're typing the same `TopAppBarDefaults.topAppBarColors(...)` block, or
the same `Spacer(modifier = Modifier.height(16.dp))` rhythm, in more than two
screens, it's not a coincidence — it's a missing shared component or token.

**Don't** retype the same theming block per screen:

```kotlin
// Repeated near-verbatim in six different screens
TopAppBar(
    title = { Text(title, fontWeight = FontWeight.Bold) },
    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, ...) } },
    colors = TopAppBarDefaults.topAppBarColors(
        containerColor = DarkBackground,
        titleContentColor = PrimaryText,
        navigationIconContentColor = PrimaryText,
    )
)
```

**Do** define it once as a shared composable:

```kotlin
// ui/core/components/AppTopBar.kt
@Composable
fun AppTopBar(title: String, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
    TopAppBar(
        title = { Text(title, fontWeight = FontWeight.Bold) },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = DarkBackground,
            titleContentColor = PrimaryText,
            navigationIconContentColor = PrimaryText,
        )
    )
}
```

Same logic for spacing: if `8.dp` / `16.dp` / `24.dp` keep showing up as the
gaps between sections, promote them to a token object instead of retyping
the number:

```kotlin
// ui/core/theme/Spacing.kt
object Spacing {
    val extraSmall = 4.dp
    val small = 8.dp
    val medium = 16.dp
    val large = 24.dp
    val extraLarge = 32.dp
}
```

Changing the app's rhythm later becomes a one-line change instead of a
find-and-replace across twelve files.

---

## 7. Generic Utilities Don't Live Inside the Screen That First Needed Them

If you write something inside a screen file that has nothing to do with that
screen's specific job (a markdown parser, a custom animated shape, a date
formatter), it doesn't belong there — even if it's currently only used once.

```kotlin
// Bad — a markdown renderer and a custom Canvas animation, both living
// inside UpdateDetailScreen.kt, a screen whose actual job is "show update info"
@Composable
fun MarkdownContent(markdown: String) { ... }
fun parseBasicMarkdown(text: String): AnnotatedString { ... }
@Composable
fun FlowingSineWave(...) { ... }
```

**Do** give each one its own file, named for what it does, in the layer it
belongs to:

```
ui/core/components/MarkdownText.kt      // generic — could render any markdown string
ui/core/components/FlowingSineWave.kt   // generic — a decorative progress visual
```

The test: could this function's doc comment describe it without mentioning
the screen it happens to be used from right now? If yes, it's not
screen-local — move it.

---

## 8. Never Swallow an Exception Silently

```kotlin
// Bad — if this delete fails, nobody will ever know
try {
    storageRepository.delete(Uri.parse(location))
} catch (e: Exception) {}
```

At minimum, log it. If the failure matters to the user (a delete that
silently didn't happen), surface it through the same
loading/error/success state the rest of the screen already uses — don't
introduce a second, invisible failure mode.

```kotlin
try {
    storageRepository.delete(Uri.parse(location))
} catch (e: Exception) {
    Log.w(TAG, "Failed to delete chapter file: $location", e)
    // and/or propagate a result the ViewModel can turn into an error state
}
```

---

## 9. Naming Reflects the One Job a Class Does

A class name should make its single responsibility obvious without opening
the file. If you can't summarize what a class does in under ten words, it's
probably doing more than one job and should be split.

| If the file contains... | Name it... |
|---|---|
| Logic to build one kind of domain object | `<Thing>Factory` / `<Thing>Builder` |
| A fixed set of mutually exclusive states | `sealed interface <Thing>` |
| One reusable Composable section | `<Feature><Purpose>Section` |
| A wrapper around one platform capability | `<Capability>Launcher` / `<Capability>Handler` |

Avoid names that describe *where* something is used instead of *what* it
does (`NovelScreenHelper2`, `Utils`, `Stuff`). If the "what it does" name
feels awkward to write, that's a sign the responsibility itself isn't clear
yet — fix that before naming it.

---

## 11. Module Boundaries Are Rules, Not Suggestions

Everything above this line is about how to write code *inside* a class or
screen. That's not enough on its own — a codebase where every file is clean
in isolation can still have zero real separation between layers, because
nothing stops a screen from reaching directly into a database, or a domain
class from importing `android.content.Context`.

Three layers, one direction of dependency:

```
presentation  →  domain  →  data
```

- `domain` knows nothing about `data` or `presentation`. It has no Android
  imports (no `Context`, no `Uri`, no `Room` annotations). It's plain Kotlin.
- `data` implements the interfaces `domain` defines. It's allowed to know
  about `domain`, never the other way around.
- `presentation` (screens, ViewModels) only ever imports from `domain`. It
  never imports a `data`-layer class directly, even "just this once."

**Don't** let a ViewModel reach past domain into a concrete data class:

```kotlin
// Bad — ViewModel imports RequestDao directly, skipping domain entirely
class NovelDetailViewModel(
    private val requestDao: RequestDao,   // data-layer class, in a presentation-layer file
) : ViewModel()
```

**Do** put a `domain` interface between them:

```kotlin
// domain/repository/BatchRepository.kt — pure Kotlin, no Android imports
interface BatchRepository {
    suspend fun insertRequests(batches: List<RequestEntity>)
    fun getRootRequests(): Flow<List<RequestEntity>>
}

// data/repository/BatchRepositoryImpl.kt — the only file that knows about Room
class BatchRepositoryImpl(private val dao: RequestDao) : BatchRepository {
    override suspend fun insertRequests(batches: List<RequestEntity>) = dao.insertAll(batches)
    override fun getRootRequests(): Flow<List<RequestEntity>> = dao.getRootRequests()
}

// presentation/NovelDetailViewModel.kt — only ever sees the interface
class NovelDetailViewModel(
    private val batchRepository: BatchRepository,
) : ViewModel()
```

If your project is a single Gradle module today, you can't get compiler
enforcement of this yet — but you can still enforce it by convention
(package structure + code review) until the module split happens. The rule
matters more than the enforcement mechanism; add the mechanism as soon as
you can.

This is the rule that turns "many small well-named classes" (Section 1)
into an actual layered architecture instead of a pile of tidy classes that
still all know about each other.

---

## 12. Cross a Layer Boundary? Define the Interface First

Section 3 says "pick one way to get a dependency." This section says which
of those dependencies should be interfaces: **any class instantiated in one
layer and consumed in another is an interface, defined in the layer being
depended on.**

**Don't** let a ViewModel depend on a concrete repository class:

```kotlin
// Bad — concrete class crossing a layer boundary. Untestable without a real DB,
// and nothing stops NovelDetailViewModel from calling Room-specific methods
// that shouldn't be visible outside the data layer.
class NovelDetailViewModel(
    private val novelRepository: NovelRepositoryImpl,
)
```

**Do** depend on the interface, and let a DI container/factory hand you the
implementation:

```kotlin
class NovelDetailViewModel(
    private val novelRepository: NovelRepository,  // interface
)
```

This isn't just style — it's what makes a class fake-able in a test without
spinning up a real database, and it's what stops "just add one more Room
query" from leaking into a screen file six months from now.

**Rule of thumb:** if you can't write a fake implementation of a class in
under 15 lines for a test, it's a sign the class should have been an
interface with a real implementation and a fake implementation, not a
concrete class passed around directly.

---

## 13. One Class Per Business Action, Not Just One Class Per Data Shape

Section 2 gives every *piece of state* and every *repeated construction* its
own class. Business *actions* need the same treatment — a "use case" (also
called an interactor): one class, one verb, one job, living in `domain`.

**Don't** let ViewModels call repositories directly for anything beyond a
trivial pass-through:

```kotlin
// Bad — the ViewModel itself contains the business logic for "what does it
// mean to delete a chapter," including the multi-step cleanup order. This
// logic is invisible to anyone not reading this specific ViewModel, and it
// will get rewritten slightly differently the next time someone needs it.
fun deleteChapter(chapter: Chapter) {
    viewModelScope.launch {
        chapterRepository.markUnread(chapter)
        storageRepository.deleteFile(chapter.fileLocation)
        chapterRepository.delete(chapter)
        downloadRepository.removeFromQueue(chapter.id)
    }
}
```

**Do** name the action and give it its own class:

```kotlin
// domain/usecase/DeleteChapterDownload.kt
class DeleteChapterDownload(
    private val chapterRepository: ChapterRepository,
    private val storageRepository: StorageRepository,
    private val downloadRepository: DownloadRepository,
) {
    suspend operator fun invoke(chapter: Chapter) {
        chapterRepository.markUnread(chapter)
        storageRepository.deleteFile(chapter.fileLocation)
        chapterRepository.delete(chapter)
        downloadRepository.removeFromQueue(chapter.id)
    }
}
```

```kotlin
// ViewModel becomes a one-liner — it expresses intent, not process
class ChapterListViewModel(
    private val deleteChapterDownload: DeleteChapterDownload,
) : ViewModel() {
    fun deleteChapter(chapter: Chapter) {
        viewModelScope.launch { deleteChapterDownload(chapter) }
    }
}
```

Now "what does deleting a chapter actually involve" has exactly one
authoritative answer, it's independently testable without a ViewModel or
Android framework in the loop, and any screen that needs the same action —
a bulk delete screen, a settings "clear all downloads" button — calls the
same class instead of reimplementing the steps.

**Ask this before writing business logic inside a ViewModel function:**

- Does this represent something a user or the system *does* (delete,
  download, export, sync, migrate)? → It's a use case.
- Is it a single, direct pass-through to one repository method with no
  extra steps? → Calling the repository directly is fine.

---

## 14. Platform Facades Cover the Whole Capability, Not Just the Entry Point

Section 4 wraps the launcher for a platform capability (a file picker) in
one shared function. That's necessary but not sufficient — the *entry
point* isn't the only part that gets duplicated. Everything you do
afterward with what the picker gives you back needs the same treatment,
and so do the other two capabilities that get reinvented most often:
**logging** and **storage I/O**.

**Don't** let every file that needs to log or touch storage roll its own
call:

```kotlin
// Bad — three different ways to log the same kind of event, scattered
// across three files, with three different tags and no shared filtering
Log.d("NovelDetailVM", "Download started for ${novel.title}")
Log.d(this::class.java.simpleName, "download started: $novel")
println("Starting download: $novel")

// Bad — raw Uri/File operations copy-pasted per screen, each handling
// (or not handling) SAF permission edge cases slightly differently
val stream = context.contentResolver.openOutputStream(uri)
```

**Do** put one facade in front of each capability:

```kotlin
// core/logging/AppLog.kt — the only place that knows how logs are tagged and filtered
object AppLog {
    fun d(tag: String, message: String) { ... }
    fun w(tag: String, message: String, throwable: Throwable? = null) { ... }
    fun e(tag: String, message: String, throwable: Throwable? = null) { ... }
}

// core/storage/StorageHandler.kt — the only place that touches Uri/File directly
interface StorageHandler {
    suspend fun write(uri: Uri, bytes: ByteArray): Result<Unit>
    suspend fun read(uri: Uri): Result<ByteArray>
    suspend fun delete(uri: Uri): Result<Unit>
}
```

Every screen, ViewModel, and use case calls `AppLog` and `StorageHandler` —
never `Log` or `contentResolver` directly. If you need to add crash
reporting to every warning-level log, or add a retry to every storage
write, that's a one-file change instead of a grep-and-pray across the
codebase.

**Rule of thumb:** if a capability touches the Android framework directly
(logging, file I/O, clipboard, sharing, notifications, permissions), it
gets exactly one facade in `core`, and nothing outside that facade imports
the underlying platform API.

---

## 15. Screens Use the Same State + Event Shape, Not Just Dialogs

Section 2.1 fixes flag-soup for dialogs specifically. The same disease
shows up everywhere a screen tracks "what's currently going on" — loading,
error, and one-off things like snackbars or navigation triggers — and it
deserves the same fix, applied consistently to *every* screen, not just the
ones with multiple dialogs.

**Don't** track screen status and one-shot messages as separate flags that
can drift out of sync:

```kotlin
// Bad — isLoading, error, and snackbarMessage can all be set independently,
// so the screen can end up "loading" and "showing an error" at the same time
var isLoading by remember { mutableStateOf(false) }
var errorMessage by remember { mutableStateOf<String?>(null) }
var snackbarMessage by remember { mutableStateOf<String?>(null) }
var novels by remember { mutableStateOf<List<Novel>>(emptyList()) }
```

**Do** give every screen one immutable `State` and a separate one-shot
`Event` channel for things that should fire once (snackbars, navigation)
rather than persist in state:

```kotlin
data class LibraryState(
    val novels: List<Novel> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

sealed interface LibraryEvent {
    data class ShowSnackbar(val message: String) : LibraryEvent
    data class NavigateToDetail(val novelUrl: String) : LibraryEvent
}
```

```kotlin
class LibraryViewModel : ViewModel() {
    private val _state = MutableStateFlow(LibraryState())
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    private val _events = Channel<LibraryEvent>()
    val events = _events.receiveAsFlow()   // collected once, never replayed on rotation
}
```

Use this `State` + `Event` shape on every screen from the start, not only
once a screen's flags become unmanageable. Consistency is what lets a new
contributor open any screen file and already know where to look for "what's
on screen right now" versus "what just happened once."

---

## 16. Data Crossing a Layer Boundary Gets a Mapper

Section 1's test ("does this have a name?") applies to *transformations*,
not just state and actions. A `RequestEntity` (data layer) turning into a
`Request` (domain model) turning into a `RequestUiModel` (presentation) is a
concept every time it happens — so it gets a named class, not an inline
`.let { }` or `.map { }` repeated at every call site.

**Don't** transform between layers by hand, differently, in each file that
needs it:

```kotlin
// Repeated with slightly different field mappings in three different ViewModels
val uiModel = RequestUiModel(
    id = entity.id,
    title = entity.name.uppercase(),
    statusLabel = if (entity.status == RequestStatus.PENDING) "Waiting" else "Done",
)
```

**Do** give the transformation one home:

```kotlin
// data/mapper/RequestMapper.kt
fun RequestEntity.toDomain(): Request = Request(
    id = id,
    name = name,
    status = status,
)

// presentation/mapper/RequestUiMapper.kt
fun Request.toUiModel(): RequestUiModel = RequestUiModel(
    id = id,
    title = name.uppercase(),
    statusLabel = if (status == RequestStatus.PENDING) "Waiting" else "Done",
)
```

Now there's exactly one place that knows how a `RequestEntity` becomes a
`Request`, and one place that knows how a `Request` becomes what the screen
displays — instead of that knowledge being reconstructed from memory every
time a new screen needs the same data.

---

## 17. Domain Code Has No Android Imports — This Is What Makes "Testable" True

Section 1 claims small classes are testable. That claim is only true if
`domain` classes (use cases, repository interfaces, domain models) can be
constructed and run in a plain JVM unit test, with no `Context`, no
`Application`, no Android framework class anywhere in the import list.

**Don't** let a use case quietly depend on Android to do its job:

```kotlin
// Bad — this "use case" can't be unit tested without Robolectric or an
// instrumented test, because it holds an Application reference
class ExportNovel(private val application: Application) {
    suspend operator fun invoke(novel: Novel, format: ExportFormat) {
        val dir = application.getExternalFilesDir(null)
        // ...
    }
}
```

**Do** push the Android-specific part behind an interface defined in
`domain` and implemented in `data`/`app`, so the use case itself stays pure:

```kotlin
// domain — no Android imports anywhere in this file
class ExportNovel(
    private val storageHandler: StorageHandler,   // interface, see Section 14
    private val novelRepository: NovelRepository,
) {
    suspend operator fun invoke(novel: Novel, format: ExportFormat) {
        val bytes = buildExport(novel, format)
        storageHandler.write(novel.exportLocation, bytes)
    }
}
```

**Rule of thumb:** if you can't write `ExportNovelTest` in a plain
`src/test` JVM module — no emulator, no `@RunWith(AndroidJUnit4::class)` —
something in that class's dependency list is in the wrong layer.

---

## 18. Enforce It: Lint Rules, Not Just a Checklist

Every rule above this line can and will decay under deadline pressure if
the only thing enforcing it is a human remembering to re-read this
document. Written guidelines are necessary but not sufficient — the rules
that actually survive contact with a real codebase are the ones a build
fails on.

At minimum, wire up:

- **ktlint** for formatting and import-order consistency, so code review
  never has to argue about style.
- **detekt**, with custom rules added over time for the project-specific
  patterns in this document — for example, a rule that fails the build on
  a raw `android.util.Log` call outside `core/logging`, or on a `data`-layer
  import inside a `presentation` package.
- **Module-level dependency checks** (Gradle module boundaries, or a
  detekt/lint rule if still single-module) that fail the build if
  `domain` ever imports `android.*` or a `data`-layer class.

Run these in CI, not just as a local pre-commit hook — a rule that only
runs on a contributor's machine is a rule that gets skipped the day
someone's in a hurry (including an AI assistant working across many files
in one session, which won't remember a rule that isn't enforced somewhere
it can see the failure).

**Rule of thumb:** for every "Don't" example in this document, ask whether
a lint rule could catch it automatically. If yes, that's a ticket to write
the rule — not a permanent reliance on everyone remembering to check by
hand.

---

## 19. Before You Commit — Quick Self-Check

Run through this on any non-trivial change:

- [ ] Did I build the same kind of object (batch, dialog, filter, filename...)
  more than once by hand? → extract a factory/builder.
- [ ] Do I have more than one boolean tracking overlapping UI state? → collapse
  into one sealed state.
- [ ] Did I fetch a dependency a different way than the rest of this class
  does? → make it consistent.
- [ ] Did I write a `rememberLauncherForActivityResult`, `Intent`, or similar
  platform call that already exists somewhere else in the app? → reuse it.
- [ ] Did I use a raw `String`/`Int`/`Pair` to represent "one of a few known
  cases"? → make it a type.
- [ ] Did I retype a color/spacing/shape value that's already a token
  elsewhere? → use the token.
- [ ] Did I write a generic helper inside a feature-specific file? → move it
  to `core` (or the right feature) with its own name.
- [ ] Did I catch an exception and do nothing with it? → log it or surface it.
- [ ] Did a `presentation`-layer file import a `data`-layer class directly,
  skipping `domain`? → put a `domain` interface between them.
- [ ] Did I pass a concrete repository/class across a layer boundary instead
  of an interface? → define the interface where it's depended on.
- [ ] Did I write more than a one-line pass-through business action inside a
  ViewModel? → extract a use case in `domain`.
- [ ] Did I call `Log`, `contentResolver`, or another platform API directly
  instead of going through the shared facade? → use `AppLog` /
  `StorageHandler` (or add the facade if it doesn't exist yet).
- [ ] Does this screen track loading/error/one-shot messages as separate
  flags instead of one `State` + an `Event` channel? → collapse them.
- [ ] Did I transform a model between layers inline instead of through a
  named mapper? → extract a mapper function.
- [ ] Does this `domain` class import anything from `android.*`? → push that
  dependency behind an interface instead.
- [ ] Is this rule something a lint rule could catch automatically, and
  nobody's written that rule yet? → file it instead of relying on memory.

If any answer is "yes, and I didn't fix it," that's the thing to fix before
this is done — not a follow-up ticket.