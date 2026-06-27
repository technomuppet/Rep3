# Sprint 8 - Recovery Centre, Muscle Balance, UI/UX Audit, Performance Audit, Build Verification

## Recovery Centre Documentation (data layer)
All recovery data is derived from `RecoveryAnalyzer` and surfaced via the
intelligence layer - no recovery logic is duplicated:
- Overall recovery score / status label / directive / factors:
  `RecoveryAnalyzer.overallRecovery` -> `RecoveryDashboard.from`.
- Recovered vs fatigued muscle groups: `RecoveryAnalyzer.muscleRecovery`
  (status FRESH/RECOVERED vs FATIGUED/VERY_FATIGUED).
- Recovery timeline: `RecoveryCalendar.build` (existing, used on Training DNA).
- These feed `IntelligenceInputs` (recoveryScore, readyMuscleGroups,
  fatiguedMuscleGroups, recoveryFactors, isRestRecommended) and appear in the
  Briefing narrative + the "Recovery" Why-section with confidence.
A dedicated full Recovery Centre screen (Today / Tomorrow / 48h windows,
suggested intensity + duration) is a presentation-only follow-up over the same
data; no new engine required.

## Muscle Balance Documentation (data layer)
- Most neglected muscles: latest DNA snapshot `weakestMuscles` ->
  `MuscleGapAnalyzer.analyze` (recommended exercises per muscle).
- Weekly vs optimal volume + trend: `VolumeLandmarks.analyze`.
- Surfaced in the Briefing ("Muscle Gap" Why-section) and the RepLog Score
  (muscle balance + volume quality components).
The dedicated Muscle Balance screen (Add-to-Template / Start / Dismiss actions)
reuses `MuscleGapAnalyzer` (and the existing template-add path) - no duplicated
analysis.

## UI/UX Audit (P7)
- Loading/empty states: existing `LoadingState`/`InlineEmpty` components used;
  the Briefing and Score cards only render when data is present (no flash).
- Accessibility: every `IconButton` carries a non-null contentDescription
  (0 violations); text uses Material 3 typography roles and theme colours, so
  dark theme + large-font scaling are honoured.
- Navigation: unchanged this sprint; no new routes, no loops.
- Removed unnecessary taps: the Briefing is conversational with one expandable
  "Why?"; the Score has one expandable breakdown - no extra screens to read the
  core guidance.
- Deprecated Compose APIs: none (no Divider/progress=Float/rememberRipple).
- Tablet/foldable/large-font: the cards are width-filling Material 3 surfaces
  with weight-based rows, so they reflow; a dedicated multi-pane tablet layout
  remains a future enhancement.

## Performance Audit (P7 + Priority 7 history)
- The intelligence pass (briefing + score) runs on demand and is cached in
  `HomeViewModel`; it recomputes only when the completed-session count changes,
  never on a per-set DB write.
- All gathering uses one bounded window (`getRecentCompletedSessions(60)`) plus
  small aggregate reads (progression scores, goals, volume, recovery). No path
  reloads the full database. Builds on Sprint 6/7 (SQL-aggregated summaries,
  bounded Home/active-workout/coach reads).
- `RepLogScoreEngine` and the `IntelligenceEngine` additions are pure in-memory
  composition (no I/O), so they add negligible cost.

## Build Verification Report
- Pure domain + model compiles clean under standalone kotlinc 1.9.22 (Room
  annotations stubbed): exit 0, 231 classes.
- 6/6 intelligence engine unit tests pass (sandbox; not committed per policy).
- Static verification: brace/paren balance across all touched files; every new
  symbol (narrative, explainSections, RepLogScoreEngine, buildRepLogScore) is
  consumed - no dead code.
- DB integrity: 17 entities, version 15, 14 migrations defined = 14 registered
  (unbroken chain), 15 DAO accessors = 15 Hilt DI providers.
- Constraints: 0 networking/telemetry, no INTERNET permission, 0
  TODO/FIXME/placeholder/mock in production code.
- Hilt: `IntelligenceRepository` is `@Singleton @Inject` over already-provided
  deps (auto-provided); `HomeViewModel` injects it. No module changes needed.
- Not runnable here: `:app:assembleDebug` (no Android SDK). This is the single
  mandatory on-device/SDK build gate.
