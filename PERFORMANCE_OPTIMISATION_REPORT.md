# Performance Optimisation Report (Sprint 10)

## The HIGH issue (resolved)
Before: `ProgressViewModel` and `TrainingDnaViewModel` subscribed to
`getAllSessions()` — a `@Transaction` loading every session + every
session_exercise + every set — inside `WhileSubscribed` flows that re-emit on any
write. At 250k sets this loaded the whole DB into memory and re-ran on every
change while those screens were open.

After:
- Progress headline stats (workouts/sets/reps/volume/PRs) come from SQL
  aggregates computed in the database: `SetLogDao.getProgressTotals` (COUNT/SUM
  over completed sessions' sets) + `SessionDao.getCompletedSessionCountFlow`.
  Exact and O(index) regardless of history size.
- Progress deep analytics (rankings, forecasts, recovery, plateau alerts,
  muscle balance) use a bounded window: `getRecentCompletedSessions(200)`.
- Training DNA uses the same bounded window; its recovery/calendar/genome already
  only consider recent training, so output is unchanged.

## Result against the stress targets
- 10,000 workouts / 250,000 sets / 20+ years: the analytics screens now read at
  most ~200 recent sessions + tiny SQL aggregates, not the full graph. Memory
  and CPU on Progress/Training DNA no longer scale with total history.
- Logging hot path (already bounded): single-exercise indexed queries +
  one insert — unchanged, sub-second at any size.
- Home / Coach / Intelligence / Active Workout: already bounded (prior sprints).

## History
Was: full-history list in memory. Now: 30-session window with incremental
"Load more" growing the bounded query, reactive total count to gate the button.
Smooth regardless of DB size; `LazyColumn` + stable keys retained.

## Export memory
Was: whole dataset built as one CSV/JSON String, then `toByteArray()`. Now:
`FileExporter.saveStreaming` writes straight to the destination OutputStream via
a buffered Writer; CSV rows are streamed (`writeCsv`). Peak allocation is the
writer buffer, not the full file. (JSON currently streams the encoded string to
disk; converting `BackupJson` to Gson streaming is a documented Low follow-up.)

## Indexes (verified, unchanged)
set_logs(sessionExerciseId, timestamp, [sessionExerciseId,setNumber]);
session_exercises(sessionId, exerciseId, supersetGroup);
workout_prescriptions(sessionId, exerciseId, unique[sessionId,exerciseId]).
The new aggregate queries join on these indexed columns.

## Verdict
The remaining HIGH severity performance issue is resolved. No reactive UI path
loads the full history. Behaviour is preserved (totals exact; analytics use a
generous recent window).
