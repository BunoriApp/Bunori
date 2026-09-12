# Filter Novel Detail History to Ongoing Activities

The goal is to modify the `NovelDetailScreen` so that the history section (Active crawls) only displays requests that are currently in progress. If a request is completed or cancelled, it will no longer appear in this section, allowing the user to focus on active tasks like range downloads.

## User Review Required

> [!NOTE]
> This change will hide completed and cancelled requests from the `NovelDetailScreen`. These requests can still be viewed in the global "Requests" screen.

## Proposed Changes

### UI Layer

#### [MODIFY] [NovelDetailScreen.kt](file:///home/zenit/Projects/LNCrawler/app/src/main/java/com/halovoid/lncrawler/ui/screens/novel/NovelDetailScreen.kt)

- Define a set of "ongoing" statuses: `RUNNING`, `PENDING`, `PAUSED`, `BLOCKED`, and `CANCELLING`.
- Filter `requestHistory` to only include ongoing requests before passing it to `requestHistorySection`.
- Update `downloadingChapters` logic to also consider these ongoing statuses for consistent UI feedback in the Table of Contents.

## Verification Plan

### Manual Verification
1. Open a Novel Detail screen.
2. Start a "Range Download".
3. Verify that only the active Range Download appears in the history section.
4. Wait for it to complete.
5. Verify that it disappears from the history section once it's `SUCCESS` or `CANCELLED`.
6. Verify that it still appears in the global "Requests" screen.
