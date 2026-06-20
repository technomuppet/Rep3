# RepLog Phase 0 Comprehensive Audit

Audit date: 2026-06-19

Scope: architecture, dependencies, database, UI, features, release readiness, risks, and recommended implementation plan for safely evolving RepLog into a local-first Strength Operating System.

Important: This document is an audit and planning artifact only. No feature implementation is approved by this audit. All proposed changes should be performed incrementally after approval and validated before moving to the next phase.

---

## 1. Executive Summary

RepLog has evolved into a sophisticated local-first Android strength training app. It already contains many capabilities expected from a serious training platform:

- workout logging
- active workout recovery
- templates
- programmed template targets
- persisted workout prescriptions
- adaptive vNext workout generation
- target hit/miss evaluation
- advanced set types
- RPE and tempo logging
- supersets
- progress analytics
- Training DNA™ early model
- strength forecasting
- recovery signal
- plateau detection
- movement and muscle balance analysis
- bodyweight tracking
- CSV export
- JSON backup/import/restore
- no paywall
- no cloud dependency

The project direction is strategically strong, but the codebase is now in a high-complexity prototype state. Before adding new systems, RepLog needs release hardening, compile validation, migration validation, and modularisation of large ViewModels/screens.

### Current status

```text
Feature-rich V3/V4 prototype
Offline-first architecture intact
No paid APIs
No cloud dependency
Not yet release-ready until local Android Studio build/QA passes
```

### Highest-priority risks

1. Full Android compile is not yet verified in a proper local environment.
2. Several files are very large and should be modularised.
3. Room migration chain is long and must be validated from older schemas.
4. Analytics and adaptive logic are concentrated in ViewModels.
5. Restore/import flow merges data and uses only simple duplicate detection.
6. Navigation has six bottom tabs, likely crowded on small screens.
7. Exercise media architecture exists, but real media content is not bundled.

---

## 2. Architecture Audit

## 2.1 Package Map

```text
com.replog
├── MainActivity.kt
├── RepLogApplication.kt
├── data
│   ├── db
│   │   ├── AppDatabase.kt
│   │   ├── BodyweightDao.kt
│   │   ├── ExerciseDao.kt
│   │   ├── PrescriptionDao.kt
│   │   ├── SessionDao.kt
│   │   ├── SetLogDao.kt
│   │   └── TemplateDao.kt
│   ├── model
│   │   ├── BodyweightLog.kt
│   │   ├── Exercise.kt
│   │   ├── ExerciseAnalytics.kt
│   │   ├── Relations.kt
│   │   ├── SessionExercise.kt
│   │   ├── SetLog.kt
│   │   ├── WorkoutPrescription.kt
│   │   ├── WorkoutSession.kt
│   │   └── WorkoutTemplate.kt
│   └── repository
│       ├── BodyweightRepository.kt
│       ├── ExerciseRepository.kt
│       └── WorkoutRepository.kt
├── di
│   └── AppModule.kt
├── ui
│   ├── components
│   ├── exercise
│   ├── history
│   ├── home
│   ├── navigation
│   ├── onboarding
│   ├── progress
│   ├── settings
│   ├── theme
│   └── workout
└── util
    ├── AdaptiveProgramEngine.kt
    ├── BackupJson.kt
    ├── DataSeeder.kt
    ├── DemoDataGenerator.kt
    ├── PlateCalculator.kt
    ├── PreferencesManager.kt
    ├── ProgressionEngine.kt
    └── WorkoutCsvExporter.kt
```

## 2.2 Module Map

Current project is a single Android app module:

```text
RepLog
└── app
```

This is acceptable for the current stage. If the project continues to grow, consider future modules:

```text
:app
:core:data
:core:domain
:feature:workout
:feature:progress
:feature:library
```

Do not modularise until compile stability is achieved.

## 2.3 Dependency Map

Current app-level dependencies:

- AndroidX Core
- Lifecycle runtime/ViewModel Compose
- Activity Compose
- Compose BOM
- Material 3
- Material icons extended
- Navigation Compose
- Room runtime/ktx/compiler via KSP
- Hilt Android/compiler
- Hilt Navigation Compose
- DataStore Preferences
- Gson
- JUnit / AndroidX test dependencies

### Positive findings

- No cloud SDKs.
- No paid APIs.
- Google Play Billing removed.
- No networking dependencies.
- Local-first stack is intact.

### Risks

- KSP/Hilt/AGP/Kotlin versions need local verification.
- Compose compiler `1.5.9` with Kotlin `1.9.22` is plausible but should be validated.
- Sandbox build cannot resolve all Maven dependencies due TLS issues, so local build is required.

## 2.4 Data Flow Map

### Workout logging flow

```text
Compose Screen
→ ActiveWorkoutViewModel
→ WorkoutRepository
→ SessionDao / SetLogDao / PrescriptionDao
→ Room database
→ Flow/query refresh
→ Compose state update
```

### Exercise library flow

```text
ExerciseLibraryScreen
→ ExerciseViewModel
→ ExerciseRepository / WorkoutRepository
→ ExerciseDao / SetLogDao
→ Exercise list + exercise history analytics
```

### Progress analytics flow

```text
ProgressScreen
→ ProgressViewModel
→ WorkoutRepository + BodyweightRepository + PreferencesManager
→ Room + DataStore
→ in-ViewModel analytics calculations
→ ProgressUiState
```

### Backup/export flow

```text
SettingsScreen
→ SettingsViewModel
→ WorkoutRepository / BodyweightRepository / ExerciseRepository
→ BackupJson / WorkoutCsvExporter
→ local file in context.filesDir
→ FileProvider share sheet
```

### Seeding flow

```text
RepLogApplication.onCreate
→ DataSeeder.seedDataIfFirstLaunch
→ exercises.json asset
→ ExerciseRepository / WorkoutRepository
→ Room
```

## 2.5 Architecture Strengths

- Clear MVVM-ish screen structure.
- Hilt injection is present.
- Room is central source of truth.
- DataStore handles settings and active workout ID.
- Repositories abstract DAOs.
- Local-first and offline-first architecture preserved.
- Adaptive/programming logic is currently deterministic and on-device.

## 2.6 Architecture Weaknesses

- `ActiveWorkoutScreen.kt` is ~996 lines.
- `ProgressScreen.kt` is ~543 lines.
- `ProgressViewModel.kt` is ~490 lines.
- `ActiveWorkoutViewModel.kt` is ~449 lines.
- Analytics engines are mixed into ViewModels.
- Workout/template/prescription concerns are mixed in one ViewModel.
- Backup/restore logic is in SettingsViewModel rather than a domain service.

## 2.7 Architecture Recommendation

Before new features, extract domain engines without changing behavior:

```text
util/analytics/RecoveryAnalyzer.kt
util/analytics/PlateauDetector.kt
util/analytics/ForecastEngine.kt
util/analytics/TrainingDnaEngine.kt
util/analytics/BalanceAnalyzer.kt
util/backup/BackupRestoreManager.kt
util/workout/PrescriptionEvaluator.kt
```

This should happen only after a successful local compile baseline.

---

## 3. Database Audit

## 3.1 Current Entities

| Entity | Purpose | Status |
|---|---|---|
| Exercise | Exercise library metadata | Strong foundation |
| WorkoutSession | Session container | Good |
| SessionExercise | Exercise instance in workout | Good; supports supersets |
| SetLog | Set data | Advanced; supports set type/RPE/tempo |
| WorkoutTemplate | Template container | Good |
| TemplateExercise | Programmed template item | Good; supports target reps/weight |
| BodyweightLog | Bodyweight tracking | Good |
| WorkoutPrescription | Persisted programmed/adaptive target | Strong strategic addition |

## 3.2 Relations

- WorkoutSession → SessionExercise → SetLog
- SessionExercise → Exercise
- WorkoutTemplate → TemplateExercise → Exercise
- WorkoutSession + Exercise → WorkoutPrescription

## 3.3 Migration Chain

Current Room version: `6`

```text
1 → 2 bodyweight_logs
2 → 3 exercise metadata, set metadata, supersets
3 → 4 exercise mediaAsset
4 → 5 template targetReps/targetWeight
5 → 6 workout_prescriptions
```

## 3.4 Index Audit

Current indexes:

- session_exercises.sessionId
- session_exercises.exerciseId
- session_exercises.supersetGroup
- set_logs.sessionExerciseId
- template_exercises.templateId
- template_exercises.exerciseId
- workout_prescriptions.sessionId
- workout_prescriptions.exerciseId
- unique workout_prescriptions(sessionId, exerciseId)

## 3.5 Missing / Recommended Indexes

Recommended additions, after baseline validation:

1. `workout_sessions.startTime`
   - Used heavily in ordering and time-window analytics.
2. `workout_sessions.endTime`
   - Used to filter completed sessions.
3. `set_logs.timestamp`
   - Used for PR and recent set ordering.
4. Composite index on `set_logs(sessionExerciseId, setNumber)`
   - Used to order sets within an exercise.
5. Exercise name unique-ish index is not safe because custom exercises may duplicate names, but consider non-unique `name` index.

## 3.6 Performance Bottlenecks

### Current bottleneck candidates

- `getAllSessions()` loads full relation graph for all history.
- Progress analytics calculate in memory over all sessions.
- Backup/export load all sessions into memory.
- No paging for long-term histories.

### Near-term acceptable?

Yes for early release/local datasets, but heavy users with years of data may see slow Progress screen loads.

## 3.7 Database Recommendation Before Implementation

Do not add more schema until migration tests pass.

Recommended after validation:

- add time indexes
- add summary cache tables only if profiling shows slow analytics
- consider `WorkoutSessionSummary` read model later

---

## 4. UI Audit

## 4.1 Navigation

Current bottom nav:

- Home
- Workout
- Progress
- History
- Exercises
- Settings

### Findings

- Functional but crowded.
- Six tabs may be cramped on smaller devices.
- Settings should probably move out of primary nav before release.

### Recommendation

Release nav candidate:

```text
Home | Workout | Progress | History | Library
Settings via top-right gear or Home card
```

## 4.2 Home Screen

### Status

Partially complete / useful.

### Strengths

- clear start workout CTA
- recent stats
- recent workouts
- recent PRs
- intelligence card

### Risks

- Home intelligence duplicates logic separate from Progress engines.
- Could show adaptive plan CTA directly.

## 4.3 Workout Screen

### Status

Feature-rich but high technical debt.

### Strengths

- active workout recovery
- start empty
- start templates
- adaptive vNext
- programmed targets
- persisted prescriptions
- advanced set dialog
- superset grouping
- target hit/miss summary

### Risks

- File is too large.
- Dialogs are complex and may struggle on small screens.
- Needs more accessibility pass.
- Shared superset rest behavior not implemented.

## 4.4 Progress Screen

### Status

Strategic differentiator; technically dense.

### Strengths

- Training DNA™
- recovery signal
- forecasts
- plateau watch
- movement/muscle balance
- milestones
- bodyweight tracking

### Risks

- Analytics should be extracted.
- Charts are custom Canvas; text summaries mostly exist but need full accessibility QA.
- No time-window filters.

## 4.5 History Screen

### Status

Good but needs polish.

### Strengths

- calendar-style overview
- grouped workouts
- delete confirmation

### Risks

- Calendar not weekday-aligned.
- Full session detail inline can become long.
- No filter/search.

## 4.6 Exercise Library

### Status

Good foundation.

### Strengths

- smart metadata
- exercise analytics
- media placeholder
- search by movement/muscle

### Risks

- custom exercise creation lacks explicit fields for movement pattern/difficulty/media.
- actual media files are not bundled.
- detail dialog should become a screen later.

## 4.7 Settings

### Status

Functional and robust.

### Strengths

- no paywall messaging
- exports/backups
- restore confirmations
- demo data generation
- custom plate settings

### Risks

- screen is long
- demo data should be hidden or clearly marked before production if not intended for users

## 4.8 UI Improvement Report

High priority:

1. Move Settings out of bottom nav.
2. Split large dialogs into screens on smaller devices.
3. Add confirmation dialogs consistently everywhere — mostly done.
4. Add full TalkBack descriptions to charts and icon-only actions.
5. Improve empty states for insufficient analytics data.

## 4.9 UX Improvement Report

High priority:

1. Make adaptive vNext more prominent from Home.
2. Add clear explanation of Training DNA™ confidence/data requirements.
3. Add restore mode labels: Merge / Replace / Cancel. Currently merge only.
4. Add onboarding screen explaining local-first/no account/no cloud.
5. Improve custom exercise creation with metadata fields.

---

## 5. Feature Audit

## 5.1 Complete

| Feature | Evidence |
|---|---|
| Local workout logging | WorkoutSession, SessionExercise, SetLog, ActiveWorkoutScreen |
| Exercise library | ExerciseDao, ExerciseViewModel, ExerciseLibraryScreen |
| Custom exercises | ExerciseViewModel.addCustomExercise |
| Templates | WorkoutTemplate, TemplateDao, template editor |
| Template targets | TemplateExercise targetReps/targetWeight |
| Advanced set types | SetType + AddOrEditSetDialog |
| RPE/tempo | SetLog fields and UI |
| Bodyweight tracking | BodyweightLog, Progress UI |
| CSV export | WorkoutCsvExporter |
| JSON backup/import/restore | BackupJson + Settings flows |
| No paywall | Billing removed, no billing dependency |
| File sharing | FileProvider and share intents |
| Demo data | DemoDataGenerator |

## 5.2 Partially Complete

| Feature | Gap |
|---|---|
| Active workout recovery | DataStore active ID exists; needs process death/reboot QA and tests |
| Adaptive programming | Engine exists; progression persistence and next target update not fully automatic |
| Prescriptions | Persisted and evaluated; not yet used to mutate future template/adaptive targets |
| Supersets | Grouped UI exists; shared rest timer behavior missing |
| Training DNA™ | First draft; needs response modeling over time |
| Recovery score | Rule-based first pass; needs validation and more nuance |
| Plateau detection | First-pass watchlist; needs better trend model |
| Exercise media | Placeholder architecture only; actual assets missing |
| Restore safety | Confirm + simple duplicate skip; no full replace/merge UX |

## 5.3 Placeholder

| Feature | Evidence |
|---|---|
| Exercise GIF/images | mediaAsset paths and placeholder vector only |
| Store screenshots | HTML concept file, not real Play screenshots |
| Local AI trainer | Roadmap only |
| Knowledge graph | Roadmap only |
| Voice logging | Roadmap only |
| Camera rep counting | Roadmap only |

## 5.4 Broken / Unverified

| Area | Status |
|---|---|
| Full Android build | Unverified due sandbox Maven TLS issue |
| Room schema validation | Not run successfully yet |
| Foreground rest timer on Android 14/15 | Needs device QA |
| FileProvider share flow | Needs device QA |
| Notification permission UX | Needs device QA |

## 5.5 Technical Debt

- Large UI files.
- Analytics in ViewModel.
- Backup/restore in SettingsViewModel.
- String-based muscle fields.
- String-based set types.
- Six bottom nav items.
- No automated tests yet.

---

## 6. Release Readiness Audit

## 6.1 TODO/FIXME Scan

No TODO/FIXME/NotImplemented markers found in app code.

The only `lateinit` found is Hilt-injected `dataSeeder` in `RepLogApplication`, which is normal.

## 6.2 Crash / Null Risk Areas

1. Restore/import may fail on malformed JSON; caught and status shown.
2. FileProvider share silently returns if file missing.
3. Activity casting is no longer used for billing; billing removed.
4. Some long dialogs may overflow on small screens if font scale is high.
5. Template target dialog and exercise detail need large-font testing.

## 6.3 Memory / Performance Concerns

1. Progress loads and processes all sessions.
2. Export loads all sessions.
3. Backup loads all sessions.
4. Exercise analytics can load full exercise history.

Acceptable for early release, but should be profiled with demo data and a synthetic large dataset.

## 6.4 Build Risk

Current sandbox build fails due Maven TLS issues, not source compile. Local Android Studio build is mandatory.

Known build-sensitive areas:

- high-arity Flow combine overloads
- Hilt constructor update for WorkoutRepository
- Room schema migration validation
- Compose Material API overloads
- icon availability in material-icons-extended

---

## 7. Dependency Map

```text
UI Screens
 ├─ hiltViewModel()
 ├─ ViewModels
 │   ├─ Repositories
 │   │   ├─ DAOs
 │   │   └─ Room database
 │   ├─ PreferencesManager → DataStore
 │   └─ Util engines
 │       ├─ AdaptiveProgramEngine
 │       ├─ ProgressionEngine
 │       ├─ PlateCalculator
 │       ├─ BackupJson
 │       └─ WorkoutCsvExporter
 └─ Compose Material UI
```

No cloud or paid API dependencies exist.

---

## 8. Recommended Changes Before Implementation

## 8.1 Must Do Before New Features

1. Local Android Studio build with JDK 17+.
2. Fix compile errors.
3. Run Room migration validation.
4. Run QA test plan.
5. Add minimal automated tests for export/restore engines.

## 8.2 Database Recommendations

Do after build baseline:

1. Add indexes on workout_sessions startTime/endTime.
2. Add set_logs timestamp and `(sessionExerciseId, setNumber)` indexes.
3. Consider summary tables only after profiling.

## 8.3 UI Recommendations

1. Move Settings out of bottom nav.
2. Split large screens into components.
3. Convert complex dialogs to dedicated screens.
4. Add accessibility descriptions to all chart cards and icon buttons.

## 8.4 Domain Recommendations

Extract analytics engines:

```text
RecoveryAnalyzer
PlateauDetector
ForecastEngine
TrainingDnaEngine
BalanceAnalyzer
```

Extract backup manager:

```text
BackupRestoreManager
```

---

## 9. Proposed Implementation Plan

## Phase 1 — Release Hardening

Goal: production stability.

Tasks:

1. Compile locally and fix all build errors.
2. Add/verify active workout recovery tests.
3. Add export/restore validation tests.
4. Validate migrations v1 → v6.
5. Add missing indexes after migration plan.
6. Create widget architecture only; no widget UI yet.

Validation:

- `./gradlew clean assembleDebug`
- migration tests pass
- JSON restore roundtrip test passes
- manual QA smoke pass

## Phase 2 — Insights Platform

Goal: domain architecture for analytics.

Tasks:

1. Create Insights domain package.
2. Move volume/frequency/progression/recovery logic into engines.
3. Keep UI unchanged initially.
4. Add unit tests for engines.

Validation:

- existing Progress UI outputs unchanged
- analytics tests pass

## Phase 3 — Training DNA Engine

Goal: learn user-specific responses deterministically.

Tasks:

1. Create TrainingDnaEngine.
2. Track rep preference, volume tolerance, frequency tolerance, recovery speed.
3. Create stable DTO outputs.
4. Add tests with synthetic histories.

Validation:

- deterministic outputs
- no network
- no paid APIs

## Phase 4 — RepLog Coach

Goal: rule-based coach engine.

Tasks:

1. Create CoachEngine.
2. Inputs: history, recovery, volume, DNA.
3. Outputs: recommendations, warnings, progression suggestions.
4. UI later.

Validation:

- rule tests
- no LLM/API dependency

## Phase 5 — Program Builder

Goal: local generated programs.

Tasks:

1. ProgramBuilder architecture.
2. Strength/hypertrophy/powerlifting/fat-loss modes.
3. DNA integration points.
4. Program persistence design.

Validation:

- deterministic program generation tests

## Phase 6 — Knowledge Graph

Goal: local relationship discovery.

Tasks:

1. Design graph storage.
2. Store relationships between bodyweight, frequency, volume, exercise performance and PRs.
3. Build query layer.

Validation:

- relationship generation tests

## Phase 7 — Exercise Media

Goal: local media system.

Tasks:

1. Add real local media assets.
2. Optimize sizes.
3. Add instructions field/model.
4. Add media display with fallback.

Validation:

- APK size check
- offline display check

---

## 10. Release Risk Report

| Risk | Severity | Mitigation |
|---|---:|---|
| Project not compile-verified | Critical | Local Android Studio build immediately |
| Room migration failures | Critical | Migration test suite |
| Large screens/ViewModels | High | Refactor after baseline |
| Restore duplicates | Medium | Simple skip implemented; add merge/replace later |
| Analytics performance | Medium | Profile with large dataset |
| Foreground service restrictions | Medium | Device test Android 14/15 |
| Crowded nav | Medium | Move Settings out of bottom nav |
| Accessibility gaps | Medium | TalkBack/font-scale QA |
| Placeholder media | Low | Add real media in Phase 7 |

---

## 11. Approval Gate

Phase 0 audit is complete.

Recommended next action is **Phase 1 Release Hardening**, but implementation should not begin until this audit is approved.

Phase 1 first task should be:

```text
Establish local compile baseline in Android Studio/JDK 17+
```

Only after the build passes should we implement further architecture or feature changes.
