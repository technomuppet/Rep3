# Reliability Report (Sprint 10, Priority 7)

Verified from source. No reliability regression introduced this sprint.

## Coroutines & flows
- All DB access is `suspend`/`Flow` via repositories, off the main thread; no
  main-thread DB calls found. No `GlobalScope`.
- ViewModel flows are scoped to `viewModelScope` with `SharingStarted.WhileSubscribed(5000)`
  so collectors stop shortly after the screen leaves — no leaked collectors.
- New `HistoryViewModel` uses `flatMapLatest` on a window-limit flow: growing the
  window cancels the previous query cleanly (no overlap/leak).

## State & configuration changes
- All screen state is in `@HiltViewModel` ViewModels exposed as `StateFlow`,
  collected with `collectAsState` — survives rotation/config changes.
- `remember` used for transient UI (expand/dialog/window); not for data that must
  survive config changes.

## Process death / low memory
- Active workout is recovered after process death: `ActiveWorkoutViewModel.init`
  reads `prefs.activeSessionId` and uses `ActiveWorkoutRecovery.decide` to
  RESUME / CLEAR_STALE / NONE. Verified present and unchanged.
- The active session id is persisted in DataStore at start and cleared on
  finish/discard, so a killed process resumes the in-progress workout.

## Exception handling / graceful degradation
- Intelligence/recovery/forecast engine calls are wrapped in `runCatching{}` and
  degrade to empty/loading states rather than crashing — verified across the
  intelligence repository and the three centre ViewModels.
- Export/import wrap failures and surface a status message; no unhandled throw on
  the UI thread.
- Streaming export uses `.use{}` on the OutputStream/Writer, so file handles are
  always closed even on failure.

## Nullability
- Safe calls + defaults throughout the new code; no force-unwrap (`!!`) hotspots
  introduced. Aggregate query returns are non-null (`COALESCE`/`COUNT`).

## Residual notes (Low, unchanged from audit)
- Broad `runCatching{}.getOrNull()` can mask real errors in debug — consider
  debug logging.
- Positional `combine(Array<Any?>)` index casts remain brittle (now also in
  ProgressViewModel) — co-located and index-verified, but a typed wrapper would
  be safer long-term.

## Verdict
Every failure path degrades gracefully. No memory/coroutine leak, race, or
crash path identified by source inspection.
