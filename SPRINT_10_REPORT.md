# RepLog V2.5 — Sprint 10 Completion Report (Release Hardening)

Branch: `sync/apply-improvements`. This sprint actioned the verified remediation
plan from the production audit. No new features; only performance, scalability,
compliance, and reliability hardening.

## Phase 0 — re-verified each issue before fixing
- P1: `getAllSessions()` confirmed in `ProgressViewModel` (line 128) and
  `TrainingDnaViewModel` (line 78) reactive flows. FIXED.
- P2: `HistoryViewModel` confirmed loading full history. FIXED.
- P3: export confirmed building full in-memory strings. FIXED (CSV streamed;
  all exports now stream to disk).
- P5: rest-timer FGS confirmed `mediaPlayback`. FIXED → `shortService`.

## Status by priority
| # | Priority | Status |
|---|----------|--------|
| 1 | Analytics performance (HIGH) | DONE |
| 2 | History scalability | DONE |
| 3 | Export optimisation | DONE (CSV fully streamed; JSON streams the string to disk, Gson-streaming deferred Low) |
| 4 | Navigation consistency | REVIEWED — keep current architecture (documented) |
| 5 | Google Play FGS compliance | DONE |
| 6 | Compose hardening | REVIEWED — no unnecessary refactor (documented) |
| 7 | Reliability | VERIFIED |
| 8 | Final verification | DONE |

## What changed
### P1 — Analytics performance (the audit's only HIGH)
- `ProgressViewModel`: dropped reactive `getAllSessions()`. All-time headline
  totals (workouts/sets/reps/volume/PRs) now come from SQL aggregates
  (`SetLogDao.getProgressTotals` + `SessionDao.getCompletedSessionCountFlow`),
  exact at any size. Deep analytics use a bounded `getRecentCompletedSessions(200)`.
- `TrainingDnaViewModel`: switched to `getRecentCompletedSessions(200)`
  (recovery/calendar/genome only consume recent data — behaviour preserved).
- New `ProgressTotalsRow` projection (query result; not an entity → no migration).

### P2 — History scalability
- `HistoryViewModel` renders a 30-session window with incremental "Load more"
  (`flatMapLatest` on a growing limit) + a reactive total count to know when more
  remain. `LazyColumn` + stable `key`s retained. No full-history load.

### P3 — Export optimisation
- `FileExporter` is streaming-first: `saveStreaming { writer -> }` pipes content
  straight to the destination `OutputStream` via a buffered `Writer`. `save(String)`
  delegates to it (removes the `toByteArray()` full-buffer for every export).
- `WorkoutCsvExporter.writeCsv { out -> }` streams rows; `toCsv()` delegates.
  Output is byte-identical (verified by a regression test). CSV export streams.

### P4 — Navigation consistency (kept; documented in NAVIGATION_VERIFICATION_REPORT.md)
### P5 — Google Play compliance (see GOOGLE_PLAY_COMPLIANCE_REPORT.md)
- Rest timer is a brief, user-initiated countdown → `foregroundServiceType`
  changed `mediaPlayback` → `shortService`; removed the inappropriate
  `FOREGROUND_SERVICE_MEDIA_PLAYBACK` permission.

## Verification
- Pure domain+model compiles clean (kotlinc 1.9.22): 232 classes.
- CSV streaming == in-memory output (sandbox regression test passed).
- Zero `getAllSessions()` in any reactive UI ViewModel (only 3 one-shot Settings
  export/restore calls remain, which now stream to disk).
- DB unchanged: 17 entities, v15, 14 migrations defined = registered, 15 DAOs =
  15 providers. Navigation unchanged: 14 routes = 14 composables, Home reachable.
- Offline intact (no INTERNET/network/telemetry); 0 conflict markers; 0 TODOs.
- Gate: `:app:assembleDebug` on an SDK host (none here).
