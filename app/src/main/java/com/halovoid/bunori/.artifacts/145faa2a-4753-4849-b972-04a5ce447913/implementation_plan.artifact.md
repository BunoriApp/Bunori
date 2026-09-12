# Centralized Bottom-Modal Control System

This plan details the creation of a reusable bottom-modal infrastructure and the refactoring of existing screens to use it. This will ensure visual and behavioral consistency across the application while allowing each screen to maintain control over its content.

## User Review Required

> [!IMPORTANT]
> The refactoring will consolidate several independent implementations of `ModalBottomSheet`. While the goal is to maintain existing functionality, some minor visual adjustments (like consistent padding and handles) will occur as a byproduct of standardization.

## Proposed Changes

### Core UI Infrastructure

#### [NEW] [AppBottomSheet.kt](file:///home/zenit/Projects/LNCrawler/app/src/main/java/com/halovoid/lncrawler/ui/components/AppBottomSheet.kt)
Created a centralized `AppBottomSheet` component that encapsulates:
- Consistent `containerColor`, `scrimColor`, and `dragHandle`.
- Standardized padding and navigation bar handling.
- Optional title and subtitle with unified typography.
- Helper components like `AppBottomSheetGroup` and `AppBottomSheetDivider` for consistent grouping of actions/options.

### Refactoring Existing Components

#### [MODIFY] [ControlSheet.kt](file:///home/zenit/Projects/LNCrawler/app/src/main/java/com/halovoid/lncrawler/ui/components/ControlSheet.kt)
Refactor `ControlSheet` to use `AppBottomSheet` as its underlying presentation layer. This demonstrates how specialized modals can still use the centralized infrastructure.

#### [MODIFY] [LibraryScreen.kt](file:///home/zenit/Projects/LNCrawler/app/src/main/java/com/halovoid/lncrawler/ui/screens/library/LibraryScreen.kt)
Update `LibraryFilterBottomSheet` to use `AppBottomSheet`.

#### [MODIFY] [RequestScreen.kt](file:///home/zenit/Projects/LNCrawler/app/src/main/java/com/halovoid/lncrawler/ui/screens/request/RequestScreen.kt)
Update `FilterBottomSheet` to use `AppBottomSheet`.

#### [MODIFY] [NovelPreviewScreen.kt](file:///home/zenit/Projects/LNCrawler/app/src/main/java/com/halovoid/lncrawler/ui/screens/request/NovelPreviewScreen.kt)
Update `SimilarityBottomSheet` to use `AppBottomSheet`.

#### [MODIFY] [ChapterFilterSort.kt](file:///home/zenit/Projects/LNCrawler/app/src/main/java/com/halovoid/lncrawler/ui/screens/novel/components/ChapterFilterSort.kt)
Update `ChapterFilterSortSheet` to use `AppBottomSheet`.

#### [MODIFY] [NovelTableOfContents.kt](file:///home/zenit/Projects/LNCrawler/app/src/main/java/com/halovoid/lncrawler/ui/screens/novel/components/NovelTableOfContents.kt)
Update `ChapterActionsBottomSheet` to use `AppBottomSheet`.

#### [MODIFY] [NovelTopBar.kt](file:///home/zenit/Projects/LNCrawler/app/src/main/java/com/halovoid/lncrawler/ui/screens/novel/components/NovelTopBar.kt)
Update `NovelActionsBottomSheet` to use `AppBottomSheet`.

## Verification Plan

### Automated Tests
- Verify that the app builds successfully after refactoring: `gradlew :app:assembleDebug`

### Manual Verification
- Deploy the app and test the following bottom modals to ensure they open, look consistent, and function correctly:
    - Library Filter
    - Novel Actions (Top Bar)
    - Chapter Filter/Sort
    - Chapter Actions (Long press in Table of Contents)
    - Request Filter
    - Similarity results (Novel Preview)
