# Sprint 9 - Recovery Centre / Muscle Balance / DNA Evolution Docs + Reviews

## Recovery Centre Documentation
Entry point: the Home Recovery/Briefing card (tap) and the Home intelligence hub
("Recovery"). Data source: `IntelligenceRepository.buildRecoveryCentre()` which
reuses, with no duplicated logic:
- `RecoveryAnalyzer.overallRecovery` + `RecoveryDashboard.from` -> score, status
  label, directive, directive detail, factors.
- `RecoveryAnalyzer.muscleRecovery` -> recovered vs fatigued groups (status
  FRESH/RECOVERED vs FATIGUED/VERY_FATIGUED), per-muscle score and hours-to-ready.
- `RecoveryCalendar.build` -> 14-day recovery timeline.
Displayed: overall score (colour-coded) + status, today/tomorrow/48h projection,
suggested intensity/duration/type, estimated full recovery, recovered/fatigued
muscle groups with progress bars, recovery timeline, factors ("Why?"),
improvements and warnings. Tomorrow/48h are projections of the analyzer score;
they are presentation, not a second recovery model.

## Muscle Balance Documentation
Entry point: Home intelligence hub ("Muscle Balance"). Data source:
`IntelligenceRepository.buildMuscleBalance()`:
- `VolumeLandmarks.analyze(weeks=1)` -> per-group weekly sets, optimal range,
  status (UNDER/IN_RANGE/ABOVE/NONE), and a derived gap severity.
- `MuscleGapAnalyzer.suggestionsFor` -> recommended exercises for under-volume
  groups.
- Latest `TrainingDnaSnapshot` -> weakest/strongest groups.
Displayed: overall balance score, weakest/strongest, per-group volume vs optimal
with gap severity and recommended exercises, estimated weeks to rebalance. One-tap
actions reuse existing paths: Start (create + activate a focus session),
Add to Template (create a custom template), Dismiss (hide locally).

## DNA Evolution Documentation
Entry point: Home intelligence hub ("DNA"). Data source:
`IntelligenceRepository.buildDnaEvolution()` which reads ONLY stored
`TrainingDnaRepository.getHistoricalDNA()` snapshots - no new values computed.
Displayed: genome maturity (from snapshot count), consistency trend, and
first->latest deltas for preferred rep range, workout duration, recovery ability,
training frequency, volume tolerance and monthly PRs, plus a volume-tolerance
timeline across recent snapshots. Requires >=2 snapshots; otherwise an
informative empty state.

## UI/UX Review
- Material 3 throughout; colour-coded recovery (green/primary/tertiary/error),
  progress bars for scores, confidence labels carried from the engine.
- Loading + empty states use the shared `LoadingState`/`EmptyState`.
- Cards are width-filling with weight-based rows, so they reflow on tablets /
  large fonts / dark mode (theme colours + typography roles, no hard-coded sizes
  beyond small dots/bars). Interactive cards expand explanations (Briefing "Why?",
  Score breakdown).
- Navigation: 3 secondary screens with a back-arrow TopAppBar; no dead screens,
  no duplicated navigation, Recovery card repointed to the Recovery Centre.

## Performance Review
- Each screen uses a small ViewModel that loads its data ONCE and caches it in a
  StateFlow (load-once guard), so reopening or recomposing does not re-query.
- All gathering is bounded: `getRecentCompletedSessions(60)` or stored snapshots.
  There are zero `getAllSessions()` calls in the new code, so behaviour is stable
  at 10,000 workouts / 250,000 sets (the hot reads never scale with total history).
- Lazy layouts (`LazyColumn`) for all lists; `remember` for expand state.

## Build Verification Report
- Pure domain + model compiles clean under standalone kotlinc 1.9.22 (Room
  stubbed): exit 0, 231 classes.
- Static verification: brace/paren balance across all 7 files; 3 routes each
  declared + composable + navigated-to; all new repository APIs match existing
  model/repository signatures.
- DB integrity: 17 entities, v15, 14 migrations defined = 14 registered, 15 DAO
  accessors = 15 Hilt DI providers (unchanged this sprint - no schema change).
- Hilt: 3 new `@HiltViewModel` ViewModels inject the existing
  `@Singleton IntelligenceRepository`; no module changes needed.
- Constraints: 0 networking/telemetry, no INTERNET permission, 0
  TODO/placeholder/mock, no deprecated Compose APIs.
- Not runnable here: `:app:assembleDebug` (no Android SDK). This is the single
  mandatory on-device/SDK build gate.
