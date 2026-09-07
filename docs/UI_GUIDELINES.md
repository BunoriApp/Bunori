# LNCrawler UI and Code Architecture Rules

This document defines the architectural and UI convention for LNCrawler

These rules are not suggestions

Any new feature, screen, component, ViewModel, repository integration, dialog, bottom sheet,
or UI related functionality must follow these rules

The purpose of these rules is to keep LNCrawler understandable and maintainable as the project
grows, including for contributors who have never worked on the project before.

The goal is not to create unnecessarily completed architechtures

The goal is:

- predictable code location
- clear ownership
- reusable infra
- minimal duplication
- separation of UI and business logic
- consistent UI
- easy onboarding for contributors
- safe modifications by coding agents

## 1. Core Architectural Principle

Organize code by FEATURE, not by technical type wherever possible

A contributor should be able to answer:

"Where is UI for novels?"

by looking at:

ui/feature/novel/

rather than searching through:

ui/component/
ui/screens/
ui/dialogs/
ui/utils/
etc...

The preffered high level structure is:

ui/
    core/
        components/
        theme/

    feature/
        crawler/
        downloads/
        library/
        novel/
        request/
        search/
        reader/
        settings/
        onboarding/

    nvaigation/

Non-UI infrastrucure must not be placed inside ui/ merely because the code is being used by the screen

## 2. Feature Ownership

Every screen or UI component must have a clear owning feature

Examples - 

NovelDetailScreen
    -> ui/feature/novel/

DownloadScreen
    -> ui/feature/downloads/

LibraryScreen
    -> ui/feature/library/

BackupSettingsScreen
    -> ui/feature/settings/

Novel-specific components belong to the novel feature:
ui/feature/novel/components/

Settings-specific components belong to:
ui/feature/settings/components/

Do not put feature-specific components into global components/.

## 3. Shared component rule

A component belongs in:

ui/core/components/

ONLY when it is genuinely feature-independent and is used or clearly
intended to be used by multiple features.

Examples:

- generic EmptyState
- generic LoadingIndicator
- generic ScreenHeader
- generic AppBottomSheet
- generic confirmation dialog
- generic reusable settings row

A component must NOT be moved to core merely because it could
technically be reused.

Prefer feature ownership.

Bad:

ui/core/components/NovelCard.kt

if NovelCard only exists for the novel/library experience.

Good:

ui/feature/library/components/NovelCard.kt

If another feature later needs the same functionality, determine whether
the component should actually become shared at that point.

Do not create a global "components" dumping ground.

## 4. Screen rule

A Screen is responsible for:

- composing UI
- displaying state
- collecting UI state
- sending user actions/events
- navigation triggers
- launching UI-only platform APIs through a defined UI abstraction

A Screen must NOT be responsible for:

- database operations
- network operations
- creating repositories
- constructing API clients
- performing business logic
- manipulating files directly
- performing long-running work
- deciding how data is persisted

Keep Screens relatively declarative.

Preferred flow:

User interaction
    ↓
Screen
    ↓
ViewModel
    ↓
Use Case / Repository
    ↓
Data/API/Database

## 5. ViewModel Rule

ViewModels own screen state and screen-level business orchestration.

ViewModels should:

- expose UI state
- receive user actions
- invoke domain/data operations
- expose loading/error/success states
- coordinate operations required by the screen

ViewModels should NOT:

- directly manipulate Compose UI
- contain Composable functions
- launch Activity/Fragment UI APIs
- directly invoke Android UI elements
- create repositories manually
- create API clients manually
- directly launch file pickers
- directly show dialogs
- directly manipulate navigation UI

A ViewModel should be usable without knowing what the screen looks like.

## 6. Repository Rule

ViewModels must NOT manually construct repositories.

Bad:

class SomeViewModel : ViewModel() {
    private val repository = NovelRepository(...)
}

or:

val repository = RepositoryFactory.create(...)

inside every ViewModel.

Repositories should be provided through dependency injection or a
centralized dependency provider.

A repository should have one clear source of construction.

Preferred:

ViewModel
    ↓
Injected Repository

or:

ViewModel
    ↓
Injected UseCase
    ↓
Repository

The same repository must not be reconstructed independently in every
ViewModel.

If a dependency is needed by multiple ViewModels, it should be possible
to provide that dependency centrally.

## 7. NO REPEATED INFRASTRUCTURE

If the same infrastructure operation appears in more than one ViewModel,
STOP and determine whether it belongs in a shared abstraction.

Examples include:

- obtaining repositories
- file selection
- folder selection
- exporting files
- importing files
- backup creation
- backup restoration
- permission handling
- URI handling
- source synchronization
- preferences access
- database access

Do not copy the same implementation into every ViewModel.

Create an appropriate abstraction instead.

## 8. FILE PICKER / FILE SELECTOR RULE

ViewModels must NOT directly open Android file selectors.

A ViewModel should express an intent such as:

SelectBackupFile

or:

RequestFileSelection

The UI/platform layer handles the actual Android Activity Result /
Storage Access Framework interaction.

Preferred conceptual flow:

User taps "Restore Backup"
        ↓
Screen sends RestoreBackupClicked
        ↓
ViewModel emits RequestBackupFile
        ↓
Screen/UI file-picker abstraction launches selector
        ↓
Selected URI is returned to ViewModel
        ↓
ViewModel starts restore operation

This keeps Android UI APIs out of ViewModels.

There must be ONE reusable mechanism for common file/folder selection
operations rather than every screen implementing its own launcher.

If multiple types of file selection are required, create a reusable
abstraction that supports the required contracts.

Do not duplicate:

rememberLauncherForActivityResult(...)
Intent(...)
OpenDocument(...)
OpenDocumentTree(...)
GetContent(...)
etc.

throughout individual screens.

## 9. PLATFORM API RULE

Platform APIs should have a clear ownership boundary.

Examples:

File picker
    -> UI/platform abstraction

Permissions
    -> UI/platform abstraction

Clipboard
    -> UI/platform abstraction

Notifications
    -> appropriate platform/service layer

Network
    -> data layer

Database
    -> data layer

File persistence
    -> data/service layer

Business decisions
    -> domain/application layer

Do not allow Android framework APIs to spread randomly through
ViewModels and repositories.

## 10. DEPENDENCY DIRECTION

Dependencies should flow inward/downward.

UI
 ↓
Application/Domain
 ↓
Data
 ↓
External systems

UI should not know implementation details of the API.

For example:

Bad:

Screen
 → Retrofit API
 → JSON parsing
 → database

Good:

Screen
 → ViewModel
 → Repository
 → API / Database

The UI should care about what operation is being performed, not how it
is implemented.

## 11. VIEWMODEL STATE

Every non-trivial screen should have a clear UI state.

Prefer a single state model where appropriate:

data class ScreenUiState(
    val isLoading: Boolean = false,
    val data: ...,
    val error: ...
)

The screen should render state rather than independently maintaining
pieces of application state that belong to the ViewModel.

Transient one-time events such as:

- navigation
- opening a file
- showing a snackbar
- launching a system action

should be represented separately from persistent screen state.

Do not use arbitrary mutable globals to communicate between screens.

## 12. UI COMPONENT RULE

Components should have one clear responsibility.

Bad:

NovelScreenEverything.kt

containing:

- hero
- metadata
- synopsis
- chapters
- downloads
- artifacts
- dialogs
- navigation
- networking

Good:

NovelDetailScreen
NovelHeroSection
NovelMetadataSection
NovelSynopsisSection
NovelTableOfContents
NovelArtifactsSection

However, do not split every 10 lines into a component.

Create a component when:

- it has a meaningful responsibility
- it is reused
- it makes the parent screen significantly easier to understand
- it represents a meaningful UI section

## 13. DO NOT CREATE GOD COMPONENTS

Avoid components such as:

CommonUtils
UIUtils
Helper
AppManager
GlobalManager
CommonViewModel
EverythingRepository

Do not solve architectural uncertainty by creating a giant utility class.

If functionality belongs to a feature, keep it with that feature.

If functionality is genuinely shared, define a focused abstraction.

## 14. DIALOG RULE

Use dialogs for:

- confirmation
- destructive actions
- short focused decisions
- small forms

Do not create a new dialog implementation if an existing reusable
dialog pattern already exists.

If the application already has:

ConfirmCancelDialog
ConfirmDeleteDialog
etc.

reuse the established pattern.

If multiple dialogs are solving the same problem with slightly
different implementations, consider consolidating them.

## 15. BOTTOM SHEET RULE

Bottom sheets should be the preferred interaction for:

- selectable options
- action lists
- filter/sort controls
- configuration choices
- contextual actions

Reuse the existing BottomSheet implementation.

Do not introduce another independent bottom-sheet implementation unless
there is a concrete requirement the existing one cannot satisfy.

All new bottom sheets must follow the existing:

- typography
- spacing
- corner radius
- drag handle
- button placement
- colors
- animation
- dismissal behavior

## 16. SETTINGS UI RULE

Settings screens should follow one consistent pattern.

Preferred structure:

SECTION HEADER

Setting
Supporting description                         Current value >

Setting
Supporting description                         Current value >

SECTION HEADER

Setting
Supporting description                         >

Avoid creating custom cards for every setting.

Use cards only when the content genuinely requires a visually distinct
container.

Settings should feel like one coherent system across:

- Download Preferences
- Backup & Restore
- Advanced Settings
- Support Settings
- other settings screens

## 17. UI DESIGN SYSTEM RULE

Never introduce arbitrary:

- colors
- typography
- spacing
- corner radii
- shadows
- icon sizes

if an existing theme/design token already exists.

Use the application's theme.

If a value is repeatedly needed and does not have an existing token,
consider adding a design token rather than repeatedly hardcoding it.

The UI should look like one application, not a collection of individually
designed screens.

## 18. ICON RULE

Icons should communicate function.

Avoid:

- oversized decorative icons
- inconsistent icon sizes
- mixing unrelated icon styles
- using icons merely to fill empty space

Use the existing icon sizing and visual treatment.

## 19. EMPTY STATE RULE

Empty states should explain:

1. What is empty.
2. Why it may be empty.
3. What the user can do next, when applicable.

Do not leave large blank areas without context.

Do not fabricate data merely to make a screen look populated.

## 20. ERROR / LOADING RULE

Every operation that can:

- fail
- take significant time
- require network access
- access files
- access the database

should have an explicit loading/error/success state where appropriate.

Do not silently swallow exceptions.

Do not expose raw implementation exceptions directly to users unless
appropriate.

## 21. NAVIGATION RULE

Navigation definitions belong in the navigation layer.

Screens should not contain arbitrary navigation graph construction.

A feature should expose navigation destinations in a predictable way.

When adding a screen:

1. Add the screen to its feature package.
2. Add its route/destination to navigation.
3. Keep navigation wiring centralized.
4. Do not create feature-specific navigation systems without a strong
   reason.

## 22. NAMING RULE

Names must describe responsibility.

Prefer:

BackupSettingsScreen
BackupViewModel
BackupRepository
BackupManager

over:

BackupHelper
BackupUtils
BackupStuff

For components:

NovelHeroSection
NovelMetadataSection
BackupOptionSheet

rather than:

BackupThing
BackupView
BackupComponent2

Use consistent terminology throughout the codebase.

## 23. FILE ORGANIZATION RULE

A feature should ideally look like:

feature/
    feature/
        FeatureScreen.kt
        FeatureViewModel.kt
        components/
            FeatureHeader.kt
            FeatureRow.kt

Additional files should only be added when their responsibility
justifies them.

Do not create:

utils/
helpers/
misc/
common/
stuff/

to avoid deciding where a file belongs.

If it is difficult to determine where something belongs, that is a sign
that its responsibility is unclear and should be resolved before adding
it.

## 24. REFACTORING RULE

When moving files:

- update package declarations
- update imports
- update references
- update navigation
- update tests
- verify resource references
- verify DI/dependency wiring
- build the project

Do not combine a structural refactor with a large behavioral rewrite
unless necessary.

Prefer small, independently verifiable migrations.

## 25. AGENT RULE

Before modifying an unfamiliar area of the project, inspect:

1. The owning feature.
2. Existing components in that feature.
3. Existing shared components.
4. Related ViewModels.
5. Related repositories/use cases.
6. Navigation.
7. Existing patterns for the same UI interaction.

Do not create a new implementation simply because the existing
implementation was not immediately found.

Search first.

Reuse second.

Create new infrastructure third.

## 26. DUPLICATION RULE

Before adding code, search for existing implementations.

If something similar already exists:

- reuse it if appropriate
- generalize it if genuinely shared
- leave it feature-local if it is feature-specific

Do not create:

FileSelectorA
FileSelectorB
FileSelectorC

when one reusable abstraction can handle the required behavior.

Likewise do not create multiple repository factories or multiple
implementations of the same settings-row behavior.

## 27. BUSINESS LOGIC RULE

UI code should describe WHAT the user wants.

It should not describe HOW the application accomplishes it.

For example:

Good:

viewModel.restoreBackup(uri)

Bad:

screen:
    unzip file
    parse manifest
    open database
    copy files
    update preferences

The screen should never become the place where business logic accumulates.

## 28. DATA ACCESS RULE

Never access the database directly from:

- Composables
- Screens
- UI components

Never access network APIs directly from:

- Composables
- Screens
- UI components
- ViewModels when a repository abstraction already exists

All data access must have an identifiable owner.

## 29. FEATURE COMMUNICATION RULE

Features should not reach into each other's internal implementation.

Bad:

NovelScreen directly accessing DownloadViewModel internals.

Good:

Novel feature requests a download operation through an appropriate
application/domain abstraction.

Features may share well-defined application services/repositories.

Do not create hidden coupling between screens.

## 30. PREFER SIMPLE ARCHITECTURE

Do not introduce layers simply to satisfy architectural terminology.

For a small operation:

UI
 ↓
ViewModel
 ↓
Repository

may be enough.

For complicated business logic:

UI
 ↓
ViewModel
 ↓
UseCase
 ↓
Repository

may be appropriate.

The architecture should reflect actual complexity.

The goal is clarity, not maximum number of classes.

## 31. WHEN ADDING A NEW FEATURE

Before implementation:

1. Identify the feature owner.
2. Identify required screens.
3. Identify reusable components.
4. Identify existing infrastructure that can be reused.
5. Identify whether a new repository/use case is actually required.
6. Identify whether the feature needs platform APIs.
7. Decide where those platform APIs belong.

Then implement.

Do not start by creating arbitrary files under ui/components/.

## 32. WHEN ADDING A NEW UI PATTERN

If a new screen requires something that does not currently exist:

First determine:

"Is this actually a new UI pattern?"

If yes, implement it consistently and consider whether it should become
a reusable component.

If it is only a variation of an existing pattern, extend/reuse the
existing pattern instead.

Do not create a second version of an existing component because the
existing component requires minor modification.

## 33. MAINTAINABILITY TEST

Every change should pass this test:

A developer who has never worked on LNCrawler should be able to find
the relevant code by knowing the feature name.

They should not need to:

- search the entire repository
- inspect unrelated ViewModels
- understand historical implementation decisions
- guess which global component is responsible
- duplicate an existing implementation

If a change makes this harder, reconsider the design.

## 34. DEPENDENCY CONSTRUCTION RULE

A class must not construct its own external dependencies when those
dependencies are application-level services.

Examples:

- repositories
- API clients
- database instances
- preference stores
- file managers
- backup managers
- source managers
- download managers

Bad:

class DownloadViewModel : ViewModel() {
    private val repository = DownloadRepository(
        ApiClient(...),
        Database(...)
    )
}

Bad:

class LibraryViewModel : ViewModel() {
    private val repository = DownloadRepository(...)
}

Good:

class DownloadViewModel(
    private val repository: DownloadRepository
) : ViewModel()

class LibraryViewModel(
    private val repository: DownloadRepository
) : ViewModel()

The construction of DownloadRepository is centralized.

If the project does not yet use a dependency injection framework,
introduce a lightweight application-level dependency provider/factory
before adding more manual dependency construction.

Do not introduce a full DI framework solely to solve one small problem,
but do not continue duplicating dependency construction across
ViewModels either.

## 35. FINAL PRINCIPLE

The codebase should make the correct thing easy to do.

Adding a new screen should be predictable.

Adding a file picker should be predictable.

Adding a repository should be predictable.

Adding a bottom sheet should be predictable.

Adding a setting should be predictable.

Adding a new feature should be predictable.

If a contributor repeatedly has to ask:

"Where does this go?"

the architecture needs improvement rather than expecting every
contributor to learn undocumented historical conventions.
