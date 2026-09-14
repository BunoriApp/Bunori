# LNCrawler Architecture & Contribution Guidelines

> These rules exist to keep LNCrawler predictable as it grows. They apply to every new
> screen, ViewModel, repository, dialog, bottom sheet, or feature — whether written by a
> human contributor or a coding agent.
>
> **Test for every change:** *A developer who has never touched LNCrawler should be able
> to find the relevant code just by knowing the feature name — without reading history,
> guessing at global helpers, or duplicating something that already exists.*
> If a change makes that harder, reconsider it before merging.

---

## 1. The Five-Minute Version

If you read nothing else, read this:

1. **Organize by feature, not by type.** `ui/feature/novel/`, not `ui/screens/`, `ui/dialogs/`.
2. **Screens render state and send events. Nothing else.** No DB, no network, no file I/O.
3. **ViewModels orchestrate. They don't touch Android UI or construct their own dependencies.**
4. **Repositories/UseCases own data and business logic.** Injected, never `new`'d inside a ViewModel.
5. **Search before you build.** If something similar exists, reuse or generalize it — don't fork it.
6. **No `Utils`, `Helper`, `Manager`, or `Common*` grab-bags.** Name things by what they own.

Everything below is the detail behind these six points.

---

## 2. Project Structure

```
ui/
  core/
    components/        # genuinely feature-independent, reused across features
    theme/              # colors, typography, spacing, shape tokens

  feature/
    crawler/
    downloads/
    library/
    novel/
    batch/
    search/
    reader/
    settings/
    onboarding/
      <FeatureName>Screen.kt
      <FeatureName>ViewModel.kt
      components/
        <FeatureSpecific>Section.kt

  navigation/
```

**Rule of thumb:** if you're asking "where should this file go?", the answer is almost
always "inside the feature it belongs to." Non-UI infrastructure (repositories, managers,
DI providers) does **not** live under `ui/` just because a screen happens to use it.

---

## 3. Layers & Responsibilities

| Layer | Owns | Must never do |
|---|---|---|
| **Screen** (Composable) | Rendering state, collecting user actions, navigation triggers | DB/network calls, business logic, constructing repositories, launching file pickers directly |
| **ViewModel** | UI state, orchestrating use cases/repositories, exposing loading/error/success | Composable functions, Android UI APIs (file pickers, dialogs, Activity results), constructing its own repositories/API clients |
| **UseCase** *(optional)* | A single, meaningful unit of business logic reused across ViewModels | Owning UI state, knowing about Compose |
| **Repository** | Single source of truth for a data domain; talks to API/DB | UI concerns, navigation |
| **Data/API/DB** | Persistence and network | Business decisions |

**Standard flow:**

```
User interaction → Screen → ViewModel → UseCase/Repository → API/Database
```

Small operations can skip the UseCase layer (`Screen → ViewModel → Repository` is fine).
Add a UseCase only when logic is genuinely complex or shared — not for architectural
completeness.

### Dependency direction
Dependencies flow inward: `UI → Application/Domain → Data → External systems`.
A Screen should know *what* operation happens, never *how*.

```
Bad:  Screen → Retrofit API → JSON parsing → database
Good: Screen → ViewModel → Repository → API / Database
```

---

## 4. Dependency Construction

ViewModels and Screens must never manually construct repositories, API clients, or
managers.

```kotlin
// Bad — every ViewModel builds its own copy
class LibraryViewModel : ViewModel() {
    private val repository = NovelRepository(ApiClient(...), Database(...))
}

// Good — constructed once, injected everywhere
class LibraryViewModel(
    private val repository: NovelRepository
) : ViewModel()
```

Repositories, managers, and API clients should have **one clear place of construction**
(a DI framework, or — if the project isn't there yet — a lightweight, centralized
provider). Don't introduce a full DI framework just to fix one ViewModel, but don't keep
copy-pasting construction logic either. The second duplicate is your signal to
centralize it.

---

## 5. Platform APIs (Files, Permissions, Clipboard...)

Platform/Android APIs are UI-layer concerns and must stay out of ViewModels.

| Concern | Owner |
|---|---|
| File/folder picker | UI/platform abstraction |
| Permissions | UI/platform abstraction |
| Clipboard | UI/platform abstraction |
| Notifications | Service/platform layer |
| Network | Data layer |
| Database | Data layer |
| File persistence | Data/service layer |

**Pattern for anything that needs a platform action (e.g. file picking):** the
ViewModel expresses *intent*, the UI layer performs the *action*, the result flows back.

```
Screen sends RestoreBackupClicked
        ↓
ViewModel emits RequestBackupFile (one-time event)
        ↓
Screen's file-picker abstraction launches the system picker
        ↓
Selected URI returned to ViewModel
        ↓
ViewModel starts the restore operation
```

There should be **one reusable file-picker abstraction** for the whole app — not a new
`rememberLauncherForActivityResult` in every screen that needs one.

---

## 6. UI State

Prefer one state model per non-trivial screen:

```kotlin
data class LibraryUiState(
    val isLoading: Boolean = false,
    val novels: List<Novel> = emptyList(),
    val error: String? = null,
)
```

- **Persistent state** (what's on screen) lives in the state model above.
- **One-time events** (navigate, show a snackbar, open a file, launch a picker) are
  modeled separately — e.g. a `SharedFlow` of events — never as fields the screen has
  to remember to "consume."
- No global mutable state to pass data between unrelated screens.

---

## 7. Components, Dialogs, Bottom Sheets

**Where a component lives:**

| Component is... | Goes in |
|---|---|
| Used by exactly one feature | `ui/feature/<name>/components/` |
| Genuinely feature-independent AND used/intended for 2+ features | `ui/core/components/` |

Don't move something to `core` "because it could theoretically be reused" — wait until a
second feature actually needs it, then decide if it should graduate.

**Dialogs** — for confirmations, destructive actions, small forms. Reuse
`ConfirmCancelDialog` / `ConfirmDeleteDialog` etc. before writing a new one.

**Bottom sheets** — preferred for option selection, filters/sort, contextual actions.
Reuse the existing bottom sheet implementation (typography, spacing, corner radius, drag
handle, animations, dismissal behavior) rather than building a parallel one.

**Splitting a screen into components** — do it when a piece:
- has a distinct responsibility (e.g. `NovelHeroSection`, `NovelSynopsisSection`)
- is reused, or
- meaningfully simplifies the parent screen

Don't split every 10 lines into its own file, and don't let one file (`NovelScreenEverything.kt`)
grow to own hero + metadata + chapters + downloads + dialogs + networking at once.

---

## 8. Settings, Empty States, Loading/Error

**Settings** follow one layout pattern throughout the app:

```
SECTION HEADER
  Setting
  Supporting description                    Current value >

  Setting
  Supporting description                    >
```

Use a custom card only when content genuinely needs a visually distinct container —
not by default.

**Empty states** must answer: *what's empty → why it might be → what to do next.* Never
leave a blank area with no context, and never fabricate data just to look populated.

**Loading/Error**: any operation that can fail, take time, or touch network/DB/files
needs an explicit loading/error/success path. Don't swallow exceptions silently; don't
expose raw stack traces to users.

---

## 9. Design System

Never hardcode colors, type styles, spacing, radii, shadows, or icon sizes if a theme
token already exists. If a value is needed repeatedly and no token exists, add one —
don't repeat the magic number. The app should read as one product, not a set of
independently-styled screens.

---

## 10. Naming

Name things after what they're responsible for.

| Prefer | Avoid |
|---|---|
| `BackupSettingsScreen`, `BackupViewModel`, `BackupRepository` | `BackupHelper`, `BackupUtils`, `BackupStuff` |
| `NovelHeroSection`, `BackupOptionSheet` | `BackupThing`, `BackupComponent2` |

**Banned class/file names** (these are architecture smells, not naming style):
`CommonUtils`, `UIUtils`, `Helper`, `AppManager`, `GlobalManager`, `CommonViewModel`,
`EverythingRepository`, and any `utils/`, `helpers/`, `misc/`, `common/`, `stuff/`
package created to dodge deciding where something belongs. If you can't name it clearly,
its responsibility isn't clear yet — fix that first.

---

## 11. Navigation & Cross-Feature Communication

- Route/destination definitions live in `ui/navigation/`, not scattered per screen.
- Features don't reach into each other's internals (e.g. `NovelScreen` should never
  poke at `DownloadViewModel`'s private state). Cross-feature needs go through a shared
  application service/repository.

---

## 12. Cookbook — "How Do I Add...?"

Use these as literal checklists.

### Add a new screen
1. Confirm the owning feature (create `ui/feature/<name>/` if it's genuinely new).
2. Create `<Name>Screen.kt` + `<Name>ViewModel.kt` inside it.
3. Define its `UiState` and one-time events.
4. Register the route in `ui/navigation/`.
5. Wire the ViewModel's dependencies through the existing DI/provider — don't construct them inline.

### Add a new feature
1. Identify the feature name and create its package.
2. List the screens it needs.
3. Check `ui/core/components/` for anything reusable before building new components.
4. Check existing repositories/use cases before deciding a new one is needed.
5. Identify any platform APIs required and route them through the existing abstractions (§5).
6. Only then start writing screens.

### Add a repository
1. Search for an existing repository that already owns this data domain.
2. If none exists, create it in the data layer (not under `ui/`).
3. Register its construction in the central DI/provider — don't let ViewModels build it.

### Add a dialog / bottom sheet
1. Check whether an existing dialog/sheet already solves this shape of problem.
2. If yes, reuse it with different content/params.
3. If no, build one following the existing visual and interaction conventions (§7), and consider whether it should live in `core` or the feature.

### Add a setting
1. Confirm which settings screen it belongs to.
2. Follow the standard row layout (§8) — don't create a bespoke card.
3. Route its persistence through the existing preferences abstraction, not a new one.

### Add anything that touches files/permissions/clipboard
1. Check `ui/core/` for the existing platform abstraction.
2. Extend it if it doesn't yet support your use case.
3. Never call Android APIs (`OpenDocument`, `rememberLauncherForActivityResult`, etc.) directly inside a Screen or ViewModel body as a one-off.

---

## 13. Writing New Code — Practical Rules

These apply regardless of which layer you're touching.

1. **Search before you write.** Grep the feature folder, then `core`, before adding
   anything. If something 80% does what you need, extend it or generalize it —
   don't fork a near-duplicate (`FileSelectorA`, `FileSelectorB`, ...).
2. **One file, one responsibility.** If a file is doing UI + business logic + platform
   calls, split it along those lines before adding more to it.
3. **New code goes where the cookbook (§12) says it goes** — not wherever is fastest to
   type at the moment. If you're unsure, that uncertainty is a signal to ask/check,
   not to default to a `utils` package.
4. **Keep functions small and named for intent.** `restoreBackup(uri)` at the call site,
   not the unzip/parse/copy steps inline in a Screen or ViewModel.
5. **State changes go through the ViewModel.** Composables read state and emit events;
   they don't mutate application state directly.
6. **Prefer composition over inheritance** for UI components — build small sections and
   assemble them in the Screen, rather than deep Composable hierarchies with shared base
   classes.
7. **Match existing patterns exactly** for anything with an established convention
   (dialogs, bottom sheets, settings rows, error states) — visual and structural
   consistency beats a "slightly better" one-off.
8. **Don't mix a structural refactor with a behavioral change** in the same commit/PR.
   Move code first (update packages, imports, navigation, DI wiring, tests, build),
   verify it still works, then change behavior separately.
9. **Every operation that can fail gets a visible error path** — no bare `catch {}` that
   discards the exception.
10. **New public APIs (ViewModel methods, repository functions) should read like a
    sentence of intent** — `loadLibrary()`, `deleteNovel(id)` — not `doStuff()` or
    `handleClick2()`.
11. **When done, re-run the Maintainability Test** in §1: could a new contributor find
    this by feature name alone? If not, move it before merging.

---

## 14. Agent-Specific Instructions

Before modifying an unfamiliar area:

1. Open the owning feature folder and read what's already there.
2. Check `ui/core/components/` for reusable pieces before creating new ones.
3. Check for an existing ViewModel/repository/use case that already covers this need.
4. Check `ui/navigation/` for how similar screens are wired in.
5. Look for an existing pattern for the same *kind* of interaction (a similar dialog,
   a similar settings row, a similar file-picker flow) and follow it.

**Order of operations: search → reuse → generalize → create new.**
Do not create a new implementation just because the existing one wasn't immediately
obvious — that's almost always a sign to search harder, not to build a parallel version.

---

## 15. Anti-Patterns — Quick Reference

| Don't | Do instead |
|---|---|
| `ui/components/NovelCard.kt` | `ui/feature/library/components/NovelCard.kt` |
| ViewModel constructs its own repository | Repository injected via central DI/provider |
| ViewModel calls `rememberLauncherForActivityResult` | ViewModel emits an intent event; UI layer launches the picker |
| `CommonUtils`, `Helper`, `Manager` grab-bag classes | Named, feature- or domain-owned classes |
| New dialog/sheet built from scratch each time | Reuse/extend the existing dialog and bottom-sheet implementations |
| Hardcoded colors/spacing | Theme tokens |
| Screen contains unzip/parse/DB logic | `viewModel.restoreBackup(uri)` — logic lives in the data/domain layer |
| Combining a file-move refactor with new features in one PR | Move first, verify, then change behavior |

---

## 16. Final Principle

The architecture should make the *correct* thing the *easy* thing: adding a screen,
a file picker, a repository, a bottom sheet, or a setting should all be predictable and
look the same way every time. If a contributor keeps having to ask "where does this go?",
that's a gap in the architecture to fix — not something to route around with an
undocumented convention.