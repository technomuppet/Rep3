# Release Blockers & Prioritised Remediation Plan

Severity per the brief: Critical / High / Medium / Low. No automatic fixes were
applied — this is the plan to action after the audit.

## CRITICAL
- None found by source inspection. (No crash path, data-loss path, navigation
  trap, inaccessible Home, corrupt-DB path, or export-failure path identified.)

## Must-do before store release (process, not code)
1. **Run `:app:assembleDebug` (and a release build) on an Android SDK host.**
   This environment has no SDK, so the full Compose/Hilt/Room/KSP build was not
   executed. Treat as a release gate. Confirm on-device: the three intelligence
   screens render, the v14→v15 migration upgrades an existing install, export to
   Downloads/RepLog round-trips, and the rest-timer foreground service starts.

## HIGH
2. **Progress & Training DNA load full session history reactively**
   (`getAllSessions()` in their `combine`). At 10k workouts/250k sets this is a
   memory/jank/battery risk on those screens. Remediation: drive them from
   SQL-aggregated projections or a bounded window (mirror the Home/Coach fix).

## MEDIUM
3. **`FOREGROUND_SERVICE_MEDIA_PLAYBACK` for the rest timer.** A countdown timer
   is not media playback; Google Play scrutinises foreground-service types.
   Remediation: switch `RestTimerService` to an appropriate type (e.g. a regular
   timer notification / `specialUse` with declared justification, or drop the FGS
   in favour of an exact alarm + notification) and update the manifest permission.
4. **History list holds all sessions+sets in memory.** Paginate with a paged
   query + `LazyColumn` for very large histories.
5. **Bottom bar hidden on the 7 secondary screens.** Home is still one action
   away (back arrow), but consider showing the bottom bar on the intelligence
   hubs for smoother tab-hopping. (UX, not a trap.)
6. **Positional `combine` casts** are brittle (see CODE_QUALITY_AUDIT). Harden
   with a typed wrapper when next editing those ViewModels.

## LOW
7. Export/import build the whole dataset as one in-memory string — stream writers
   for extreme histories.
8. Split the largest composables (`ActiveWorkoutScreen`, `HomeScreen`,
   `SettingsScreen`) for recomposition isolation/testability.
9. Add debug logging around the broad `runCatching{}.getOrNull()` engine calls.
10. Housekeeping: consolidate the many per-sprint `.md` reports.

## Suggested order of work
1) SDK build verification (gate). 2) HIGH #2 (analytics-screen perf). 3) MEDIUM
#3 (foreground-service type — Play policy). 4) MEDIUM #4/#5 (history pagination,
bottom-bar UX). 5) Remaining Medium/Low cleanups.
