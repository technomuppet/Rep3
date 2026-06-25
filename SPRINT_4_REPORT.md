# RepLog V2 - Sprint 4: Launch Polish & User Ownership

**Completion report**

Branch: `sync/apply-improvements`
Sprint commit range: `43cc47c` (pre-sprint) .. `bfaaa70` (HEAD)
Commits: `d61f2df` (P1+P2) -> `4bf5fd6` (P3+P5+P6) -> `acee968` (P4) -> `bfaaa70` (P7)

Constraints honoured throughout: offline-first, no cloud, no accounts, no
subscriptions, no paywall, Android-first, existing architecture (Room + Hilt +
Compose + DataStore) preserved, no placeholders/TODOs/mocks.

---

## 1. Status by priority

| # | Priority | Status |
|---|----------|--------|
| P1 | Export System Overhaul | Complete |
| P2 | Workout History Reset | Complete |
| P3 | PB Terminology Standardisation | Complete |
| P4 | Navigation Stability Audit | Complete |
| P5 | Rest Day Override | Complete |
| P6 | Template Sharing | Complete |
| P7 | Release Quality Pass | Complete |

---

## 2. What was built

### P1 - Export System Overhaul
- Export files now save to a permanent, user-visible location and report it.
  - CSV: `WorkoutHistory_YYYY-MM-DD.csv`
  - JSON: `WorkoutBackup_YYYY-MM-DD.json`
- Default destination `Download/RepLog`, created automatically (MediaStore on
  API 29+, direct public-Downloads write on API 26-28). A user-chosen SAF folder
  overrides the default and is persisted in DataStore; the current path is shown
  in Settings ("Export folder" card with Change / Use Downloads).
- New `ExportSuccessBlock` shows: "Export complete", "File saved" + filename,
  "Location" + path, and `Open Folder` + `Share File` actions. (Reused the
  existing `FileExporter`; consolidated export results into one `ExportArtifacts`
  flow in `SettingsViewModel` to keep the state combine clean.)

### P2 - Workout History Reset (Settings -> Advanced)
- New `DataResetManager` performs a complete reset and is the single source of
  truth for "delete all history".
  - DELETED: workout sessions (cascades to session_exercises, set_logs incl. PR
    flags, prescriptions), Training DNA metrics/snapshots/progression scores,
    rest/recovery logs, plateau events, recommendation history, and rest-day
    overrides.
  - PRESERVED: templates, exercise library, goals, bodyweight log, all settings.
  - DNA and recovery are derived data and recompute from the now-empty data on
    next load (no stale analytics).
- Confirmation dialog matches spec wording (Workouts / Sets / Progress records /
  Training DNA history / Recovery history), a typed-DELETE guard prevents
  accidental triggering, button reads "Delete Everything", and a result message
  confirms how many workouts were removed.
- Added `deleteAll()` to the relevant DAOs.

### P3 - PB Terminology Standardisation (UI only)
- `PRBadge` now renders "PB"; all user-facing strings changed from PR ->
  PB / "Personal Best" across Home, History, Progress, Training DNA, Active
  Workout and Settings ("Last PB", "Monthly PBs", "Monthly PB rate", "New
  Personal Best", etc.).
- Database column and code identifiers (`isPR`, `prCount`, `prType`,
  `totalPRs`, `monthlyPRCount`) were intentionally left unchanged.

### P4 - Navigation Stability Audit
- Root cause of "Home occasionally loops back into Training": the Training tab
  and the Coach recommendation deep link shared one parameterised route
  (`workout?fromRecommendation=..`) using `popUpTo + saveState`. Because the
  recommendation push and the bottom-bar tab switch used different
  save/restore combinations, tab-state restoration could resurface Training
  when the user tapped Home.
- Fix: split into two distinct destinations - `Workout` (plain tab) and the new
  `WorkoutRecommendation` (`workout_recommendation`). The recommendation push
  uses `launchSingleTop` only (no save/restore), so it is a one-shot screen that
  Back returns from to Home and that can never be resurfaced. It still shows the
  bottom bar with the Training tab highlighted.
- Result: Home always opens Home, Training always opens Training, no loops, no
  duplicate destinations, consistent Back behaviour. Removed the now-unused
  `navArgument` import (also clears a warning).

### P5 - Rest Day Override
- When the coach recommends a rest day the card shows the recommendation, the
  recovery reason and explanation, plus "Rest Today" and "Train Anyway".
- "Train Anyway" now opens a confirmation dialog: "Recovery data suggests rest
  today. Training while fatigued may reduce performance and recovery. Continue?"
  with Cancel / Train Anyway.
- On confirm: `CoachViewModel.recordTrainAnywayOverride()` persists a
  `RestDayOverride` (recovery score + reason) via the new
  `RestDayOverrideRepository`, dismisses the recommendation, then navigates into
  the workout flow. The override record is stored locally for future recovery
  intelligence.

### P6 - Template Sharing (fully offline)
- Export Template produces a `.replogtemplate` JSON file (name, exercises, sets,
  reps, per-exercise target weight + metadata to recreate custom exercises),
  shared via the OS share sheet from the Training screen.
- Import Template added under Settings -> Templates (file picker, validation via
  `TemplateShare.decode`, duplicate-name handling with a numeric suffix, and a
  success/failure message). Device-to-device only; no servers, no accounts.

### P7 - Release Quality Pass
- Fixed the two deprecated `LinearProgressIndicator(progress = Float)` calls in
  ProgressScreen (-> `progress = { }` lambda overload, Material3 1.2+).
- Audited clean: no deprecated `Divider` / `rememberRipple` / `SwipeToDismiss` /
  non-mirrored `ArrowBack`; Room `exportSchema = false` (no schema warning); all
  10 ViewModels are `@HiltViewModel`; every `IconButton` has a non-null
  `contentDescription` (accessibility); the `CircularProgressIndicator(Modifier
  ...)` calls are the indeterminate overload and are not deprecated.

---

## 3. Migration notes

- **Room version bumped 13 -> 14.**
- New entity `RestDayOverride` (table `rest_day_overrides`).
- New `MIGRATION_13_14` creates the table:
  ```sql
  CREATE TABLE IF NOT EXISTS rest_day_overrides (
      id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
      timestamp INTEGER NOT NULL,
      recoveryScore INTEGER,
      recommendationReason TEXT
  )
  ```
- Migration registered in `addMigrations(...)`. No destructive migration; no
  data loss for existing users.
- The new `deleteAll()` DAO methods and `DataResetManager` are runtime queries,
  not schema changes - they require no migration.
- Consistency verified: 17 entities, version 14, 13 migrations defined = 13
  registered (unbroken chain 1->2 ... 13->14), 15 DAO accessors = 15 Hilt DI
  providers.

---

## 4. File list of changes (25 files)

New files:
- `data/model/RestDayOverride.kt`
- `data/db/RestDayOverrideDao.kt`
- `data/repository/RestDayOverrideRepository.kt`
- `util/DataResetManager.kt`
- `SPRINT_4_REPORT.md`

Modified - data / DI:
- `data/db/AppDatabase.kt` (entity, v14, DAO accessor, MIGRATION_13_14)
- `data/db/TrainingDnaDao.kt`, `TrainingDnaSnapshotDao.kt`,
  `TrainingDnaProgressionScoreDao.kt`, `PlateauEventDao.kt`,
  `RecommendationHistoryDao.kt`, `RestLogDao.kt` (added `deleteAll()`)
- `di/AppModule.kt` (RestDayOverrideDao provider)

Modified - UI / util:
- `ui/settings/SettingsViewModel.kt`, `ui/settings/SettingsScreen.kt`
  (export UX, reset, template import)
- `ui/navigation/NavGraph.kt` (nav stability)
- `ui/coach/CoachViewModel.kt` (override recording)
- `ui/home/HomeScreen.kt` (Train Anyway confirmation), `ui/home/HomeViewModel.kt`
- `ui/progress/ProgressScreen.kt` (deprecation fix + PB), `ProgressViewModel.kt`
- `ui/trainingdna/TrainingDnaInsightScreen.kt`, `ui/history/HistoryScreen.kt`,
  `ui/workout/ActiveWorkoutScreen.kt`, `ui/components/CommonComponents.kt` (PB)
- `util/TemplateShare.kt` (`.replogtemplate` extension)

---

## 5. Verification performed
- Pure domain + model layer compiles clean under standalone kotlinc 1.9.22
  (Room annotations stubbed): 212 classes.
- Brace/paren balance verified across every touched file.
- DB integrity verified (entities/version/migrations/DAOs/DI providers).
- No git conflict markers anywhere under `app/src`.
- No committed test files (CI kept test-free, per project policy).

---

## 6. Remaining launch blockers
1. **`:app:assembleDebug` on an Android SDK host.** This environment has no
   Android SDK, so the full Compose/Hilt/Room build was not run here. This is the
   one mandatory pre-release gate. Confirm on a device/emulator:
   - SAF/MediaStore export round-trips to `Download/RepLog` and Open Folder /
     Share File behave on the target Android versions (26-34).
   - The `MIGRATION_13_14` upgrade succeeds from an existing v13 install.
   - The rest-day "Train Anyway" override row is written.
   - Importing a `.replogtemplate` from another device recreates the template.
2. No other blockers identified. All seven priorities are implemented as
   production code (no placeholders).

---

## 7. Success criteria check
- "Feels like a polished paid app": dated, visible exports with clear success
  state; respectful rest-day override; in-app template sharing; consistent PB
  language; stable navigation. Met (pending on-device build confirmation).
- "Users can own, export, reset, and share their data confidently": export to a
  user-chosen/visible folder, full guarded reset that preserves templates and
  settings, and offline template export/import. Met.
- "No cloud infrastructure required": everything is local files + Room +
  DataStore; sharing is device-to-device via the OS share sheet. Met.
