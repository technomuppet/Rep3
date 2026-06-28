# Performance Audit (Phases 4 & 6)

Stress target: 10,000 workouts / 250,000 sets / ~20 years history / hundreds of
templates / large export+import.

## Room / query verification
- Foreign keys indexed: `set_logs(sessionExerciseId, timestamp, [sessionExerciseId,setNumber])`,
  `session_exercises(sessionId, exerciseId, supersetGroup)`,
  `workout_prescriptions(sessionId, exerciseId, unique[sessionId,exerciseId])`.
- Aggregate/bounded queries exist and are used by hot paths:
  `getCompletedSessionSummaries()` (SQL `SUM` via JOIN, no set objects),
  `getRecentCompletedSessions(limit)`, `getMaxWeightForReps`,
  `getPreviousWorkoutSetsForExercise` (single exercise).

## Logging hot path — PASS at scale
Set logging (`addSet`/`repeatLastSet`/`quickCompleteSet`) uses single-exercise,
indexed queries (`checkPR`, previous-sets-for-exercise) plus one insert. It does
NOT scale with total history → set logging stays sub-second at 250k sets.

## Home / Coach / ActiveWorkout — PASS
Bounded in prior sprints: Home uses SQL summaries + a 30-session window for the
genome/insight; ActiveWorkout's adaptive plan uses `getRecentCompletedSessions(8)`;
Coach recovery uses `getRecentCompletedSessions(30)`; intelligence briefing/score
use `getRecentCompletedSessions(60)`. None reload the full DB on a set write.

## HIGH — Progress & Training DNA load full history reactively
- `ProgressViewModel.uiState` = `combine(workoutRepository.getAllSessions(), ...)`.
- `TrainingDnaViewModel` = `combine(..., getAllSessions(), ...)`.
`getAllSessions()` is a `@Transaction` that materialises every session + every
session_exercise + every set, inside a `WhileSubscribed` flow that re-emits on any
write. At 250k sets this loads the entire DB into memory each time the screen is
open and on every change → high memory + jank + battery on those two screens.
Severity: HIGH (analytics screens, not the logging path). Recommendation: feed
these from SQL-aggregated projections (extend `SessionSummaryRow`/new per-exercise
aggregates) or a bounded window with "show more", mirroring the Home/Coach fix.

## MEDIUM — History list loads full graph
`HistoryViewModel` uses `getAllSessions()` for the full history list. This is
inherent to a history screen, but at 250k sets it should paginate
(`LazyColumn` + a paged/bounded query) rather than hold all sessions+sets in one
list. Severity: Medium.

## LOW — Export/import hold full dataset in memory
`SettingsViewModel` export/backup/restore call `getAllSessions().first()` and
build a single in-memory CSV/JSON string. For a 20-year history this is a large
allocation. Acceptable for a one-shot user action, but streaming the writer would
be more robust at the extreme. Severity: Low.

## Compose
- ViewModels expose `StateFlow` collected with `collectAsState`; lists use
  `LazyColumn` with stable `key`s. Intelligence screens cache (load-once) in the
  ViewModel. `remember` used for expand/dialog state.
- Opportunity (LOW): a few large composables (ActiveWorkout exercise card, Home)
  could be split for recomposition isolation; not a correctness issue.

## Startup
`MainActivity` is thin; the DB opens lazily; onboarding gate is a single flow.
No heavy work on the main thread at launch observed.

## Verdict
The logging experience and the Home/Coach/intelligence surfaces are bounded and
scale. The actionable performance work is the two analytics screens (Progress,
Training DNA) and history pagination.
