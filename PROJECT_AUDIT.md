# RepLog Project Audit

Audit date: 2026-06-19

## Executive Summary

RepLog is now a substantial local-first Android strength training app with workout logging, templates, history, analytics, adaptive programming prototypes, prescriptions, backups, and no paywall. The strategic direction is strong and differentiated. The codebase is feature-rich but needs a dedicated compile/fix pass in Android Studio with JDK 17+ before it should be considered release-ready.

Current status: **feature-complete V3 prototype, not yet compile-verified in a full Android environment**.

## Audit Scope

Audited:

- Gradle project structure
- Android manifest and resources
- Kotlin source files
- Room database entities, DAOs and migrations
- Hilt dependency wiring
- Repositories
- UI screens and navigation
- Data export/backup utilities
- Strategic/docs files
- Static grep checks for TODO, billing remnants and obvious stale references
- Attempted Gradle debug build in sandbox

## File Inventory

Total tracked project files excluding Gradle build caches: **74** at first scan, **73** after latest generated list depending on transient file-list state. Current `FILE_LIST.txt` is updated.

Major file groups:

- Root docs/config: README, release checklist, roadmap docs, UI mockups
- Gradle: root/app build files, wrapper
- Android resources: manifest, strings, themes, file provider paths, backup rules
- Data layer: Room entities, DAOs, database, repositories
- UI layer: Home, Workout, Progress, History, Exercise Library, Settings, Onboarding
- Utilities: adaptive engine, backup JSON, data seeding, plate calculator, preferences, CSV export

## Build System Audit

### Files

- `settings.gradle.kts`
- `build.gradle.kts`
- `app/build.gradle.kts`
- `gradlew`, `gradlew.bat`
- `gradle/wrapper/gradle-wrapper.properties`

### Current configuration

- Android Gradle Plugin: `8.3.2`
- Kotlin: `1.9.22`
- KSP: `1.9.22-1.0.17`
- Hilt: `2.52`
- compileSdk/targetSdk: `35`
- minSdk: `26`
- Compose compiler extension: `1.5.9`
- Compose BOM: `2024.09.02`

### Build attempt result

Command attempted:

```bash
./gradlew :app:assembleDebug --no-daemon --stacktrace
```

Result: **failed before compilation** due to KSP plugin resolution in the sandbox:

```text
Plugin [id: 'com.google.devtools.ksp', version: '1.9.22-1.0.17'] was not found
```

This appears to be an environment/dependency-resolution issue in the sandbox rather than a Kotlin compile result. Earlier sandbox limitations also included JDK/TLS/Maven issues. A proper local Android Studio/JDK 17+ build is required.

### Build risks

High priority:

1. Verify KSP plugin resolution locally.
2. Consider removing custom KSP/Hilt `resolutionStrategy` in `settings.gradle.kts` if Android Studio resolves marker plugins normally.
3. Consider version alignment update:
   - AGP 8.6.x or current stable
   - Kotlin 1.9.24/1.9.25 if Compose compiler aligned
   - matching KSP version
4. Verify Java 17 is used, not Java 11.

## Android Manifest Audit

### File

- `app/src/main/AndroidManifest.xml`

### Good

- Billing permission removed.
- FileProvider added for CSV/JSON sharing.
- Backup/data extraction rules referenced.
- MainActivity exported correctly.
- RestTimerService declared.

### Issues/Risks

1. Launcher icon still uses a placeholder foreground vector as full icon.
2. `POST_NOTIFICATIONS` is requested; runtime permission is handled in `MainActivity`, but UX rationale is minimal.
3. `RestTimerService` uses foreground short service. Needs real-device testing for Android 14/15 foreground-service restrictions.

## Resources Audit

### Files

- `strings.xml`
- `themes.xml`
- `ic_launcher_foreground.xml`
- `backup_rules.xml`
- `data_extraction_rules.xml`
- `file_paths.xml`

### Good

- Billing product string removed.
- FileProvider paths added.
- Backup rules exist.

### Needs work

- Replace placeholder launcher artwork.
- Add adaptive icon resources (`mipmap-anydpi-v26/ic_launcher.xml`, background/foreground, round icon).
- Add proper app theme parent if desired; current Compose theme handles most UI.

## Data Model Audit

### Entities

- `Exercise`
- `WorkoutSession`
- `SessionExercise`
- `SetLog`
- `WorkoutTemplate`
- `TemplateExercise`
- `BodyweightLog`
- `WorkoutPrescription`

### Strengths

The schema now supports serious future analytics:

- smart exercise metadata
- movement pattern
- primary/secondary muscles
- advanced set types
- RPE
- tempo
- supersets
- bodyweight tracking
- template targets
- persisted prescriptions

### Risks

1. `muscles`, `primaryMuscles`, `secondaryMuscles` are comma-separated strings. This is pragmatic but limits advanced querying.
2. `setType` is a raw string. Consider enum-like constants are present, but DB cannot enforce valid values.
3. `WorkoutPrescription` has unique `(sessionId, exerciseId)`, which prevents prescribing the same exercise twice in a workout. This is currently consistent with UI duplicate prevention but may limit edge cases.
4. Template targets are in schema, but not yet shown inside workouts except as prescriptions when starting from template.

## Room Database + Migration Audit

### File

- `AppDatabase.kt`

### Current version

```kotlin
version = 6
```

### Migration chain

- `1 -> 2`: bodyweight logs
- `2 -> 3`: exercise metadata, set metadata, supersets
- `3 -> 4`: exercise media asset
- `4 -> 5`: template targets
- `5 -> 6`: workout prescriptions

### Good

- No destructive migration in current version.
- Migration chain is explicit.
- New prescriptions table has indexes and foreign keys.

### Must test

- Fresh install schema generation.
- Upgrade from each old schema version.
- Room schema validation on build.
- JSON restore into v6 database.

## DAO Audit

### Files

- `ExerciseDao.kt`
- `SessionDao.kt`
- `SetLogDao.kt`
- `TemplateDao.kt`
- `BodyweightDao.kt`
- `PrescriptionDao.kt`

### Good

- DAOs are compact and focused.
- Analytics queries exist for exercise history.
- Prescriptions are inserted with `REPLACE` semantics.

### Risks

1. Some analytics still happen in ViewModels rather than SQL. Fine for small local datasets, but may need optimisation later.
2. `SetLogDao.getRecentSetsForExercise` returns recent sets, but if a user logs many sets, advanced analytics may benefit from paging or summary tables.

## Repository Audit

### Files

- `ExerciseRepository.kt`
- `WorkoutRepository.kt`
- `BodyweightRepository.kt`

### Good

- Repositories wrap DAOs cleanly.
- WorkoutRepository exposes prescriptions and template target operations.

### Risks

- `WorkoutRepository` is becoming large and may eventually need splitting into `SessionRepository`, `TemplateRepository`, `AnalyticsRepository`, `PrescriptionRepository`.

## Dependency Injection Audit

### File

- `AppModule.kt`

### Good

All current DAOs are provided:

- ExerciseDao
- SessionDao
- SetLogDao
- TemplateDao
- BodyweightDao
- PrescriptionDao

### Risk

Must compile-verify Hilt after repository constructor changes.

## App Startup / Seeding Audit

### Files

- `RepLogApplication.kt`
- `DataSeeder.kt`
- `exercises.json`

### Good

- Hilt application class exists.
- Seeding runs on IO scope.
- Existing built-in exercises get metadata upgrade if missing.
- Exercise JSON now has structured metadata and media paths.

### Risks

1. `seedTemplates()` may insert duplicate built-in templates if first-launch flag/data gets into an inconsistent state.
2. Metadata upgrade only updates built-in exercise names that match JSON names.
3. Media asset paths are stored, but actual files are not bundled yet.

## UI Navigation Audit

### File

- `NavGraph.kt`

### Current nav

- Home
- Workout
- Progress
- History
- Exercises
- Settings

### Issue

Six bottom navigation items is crowded. For production, consider:

- bottom nav: Home, Workout, Progress, History, Library
- Settings via Home top-right icon

## Home UI Audit

### Files

- `HomeScreen.kt`
- `HomeViewModel.kt`

### Good

- Shows start workout CTA.
- Shows recent stats.
- Shows intelligence card.
- Shows recent workouts/PRs.

### Needs work

- Home insight is separate logic from Progress recommendations. Consider centralising intelligence logic later.
- Could add direct link to adaptive workout.

## Workout UI Audit

### Files

- `ActiveWorkoutScreen.kt`
- `ActiveWorkoutViewModel.kt`
- `AdaptiveProgramEngine.kt`
- `RestTimerService.kt`

### Strengths

- Empty workout start.
- Template start.
- Adaptive vNext plan.
- Persistent active session recovery.
- Advanced set types.
- RPE/tempo.
- Superset labels.
- Prescriptions shown in cards.
- Hit/miss summary.

### Risks / Technical debt

1. `ActiveWorkoutScreen.kt` is very large at 850+ lines. Should be split into components:
   - WorkoutHeader
   - ActiveExerciseCard
   - TemplateEditorDialog
   - SetDialog
   - AdaptivePlanCard
   - SummaryDialog
2. `ActiveWorkoutViewModel.kt` is large and mixes template editing, active workout, prescriptions and adaptive logic.
3. Delete/discard actions need confirmation dialogs.
4. Adaptive prescriptions are persisted, but exact prescription backup/export does not yet include prescriptions.
5. Superset UI is assignment/label only; grouped layout and shared rest behaviour are still pending.

## Progress UI + Analytics Audit

### Files

- `ProgressScreen.kt`
- `ProgressViewModel.kt`
- `V3_ANALYTICS_NOTES.md`

### Strengths

Progress tab is now the strategic heart of the app:

- lifetime volume
- total reps
- total sets
- PR count
- streaks
- milestones
- bodyweight chart
- recovery signal
- plateau watch
- movement balance
- muscle distribution
- strength forecasts
- Training DNA™
- next-best-action recommendations

### Risks

1. `ProgressViewModel.kt` is ~490 lines and contains many analytics engines. Should be extracted:
   - RecoveryAnalyzer
   - PlateauDetector
   - ForecastEngine
   - TrainingDnaEngine
   - BalanceAnalyzer
2. Current analytics are deterministic approximations. This is fine, but UI copy should remain careful.
3. Some calculations are volume-based and do not distinguish bodyweight exercises well.
4. No time-window selector yet.

## Exercise Library Audit

### Files

- `ExerciseLibraryScreen.kt`
- `ExerciseViewModel.kt`

### Strengths

- Smart exercise metadata.
- Search by movement/muscle/equipment.
- Exercise detail analytics.
- Media slot architecture.

### Needs work

- Actual exercise media files are not bundled.
- Custom exercise creation should allow movement pattern, primary/secondary muscles and difficulty explicitly.
- Detail dialog is large; may become its own screen.

## History Audit

### Files

- `HistoryScreen.kt`
- `HistoryViewModel.kt`

### Strengths

- Calendar-like monthly view.
- Grouped session history.
- Advanced set metadata display.

### Needs work

- Confirmation before delete session.
- Detail screen would be better than expanding everything inline.
- Calendar is simplified; not aligned to weekday columns yet.

## Settings + Export/Backup Audit

### Files

- `SettingsScreen.kt`
- `SettingsViewModel.kt`
- `BackupJson.kt`
- `WorkoutCsvExporter.kt`
- `PlateCalculator.kt`
- `PreferencesManager.kt`

### Strengths

- No paywall.
- CSV export/share.
- JSON backup/share/import/restore.
- Custom kg/lb plates.
- Plate calculator.
- Data ownership messaging.

### Risks

1. JSON backup does **not yet include workout prescriptions**.
2. JSON restore can duplicate workouts/bodyweights.
3. Restore should require confirmation.
4. Restore should support merge/replace modes.
5. Settings screen is long and should eventually be sectioned.

## Backup/Export Audit

### CSV includes

- session data
- exercise metadata
- movement pattern
- muscles
- superset group
- set type
- RPE
- tempo
- volume

### JSON includes

- sessions
- exercises
- set metadata
- bodyweights
- media asset

### Missing

- workout prescriptions
- templates themselves as independent templates
- preferences/settings
- custom plate lists
- onboarding/unit preference

## Billing/Paywall Audit

Billing has been removed from app code and manifest.

Static scan found no app references to:

- BillingManager
- billingclient
- billing permission
- billing product ID
- isPro

Remaining mentions only exist in documentation as historical roadmap notes. App implementation is open-access.

## Security/Privacy Audit

### Good

- No account system.
- No server.
- No billing.
- Local-first.
- FileProvider used for sharing files.

### Needs work

- Privacy policy must be finalised with support email.
- Exported JSON/CSV may contain personal fitness data; UI should remind users before sharing.
- Restore/import should validate schema before inserting.

## Documentation Audit

### Present

- README
- Release checklist
- Store listing draft
- Privacy policy draft
- Strategic roadmap
- V2 product map
- V3 analytics notes
- UI mockups

### Needs work

- Add `ARCHITECTURE.md`.
- Add `MIGRATIONS.md`.
- Add `QA_TEST_PLAN.md`.
- Add screenshots once UI stabilises.

## Most Important Issues to Fix Next

### Critical

1. Compile locally with Android Studio/JDK 17+.
2. Fix KSP/Gradle plugin resolution.
3. Run Room schema validation.
4. Test database migrations v1→v6.
5. Add delete/discard/restore confirmations.

### High

1. Add prescriptions to JSON backup/export.
2. Prevent duplicate restore or add merge/replace modes.
3. Split large UI files and analytics ViewModel.
4. Add actual exercise media assets or hide media card until content exists.
5. Replace placeholder app icon.

### Medium

1. Move Settings out of crowded bottom nav.
2. Add template target display inside active workouts from template prescriptions — mostly done via prescriptions, needs QA.
3. Add superset grouped layout.
4. Add accessibility labels and TalkBack pass.

## Release Readiness Rating

| Area | Rating |
|---|---:|
| Feature depth | 9/10 |
| Product strategy | 9.5/10 |
| Local-first privacy | 9.5/10 |
| Data ownership | 8/10 |
| Analytics moat | 8/10 |
| Adaptive programming foundation | 8/10 |
| UI polish | 6.5/10 |
| Code maintainability | 6/10 |
| Compile confidence | 5/10 until local build passes |
| Store readiness | 6/10 |

## Recommended Next Sprint

V3.7 should be a stabilisation sprint:

1. Fix Gradle/KSP resolution locally.
2. Compile and address all Kotlin/Room/Hilt errors.
3. Add confirmations for destructive actions.
4. Add restore safety warnings and duplicate handling.
5. Add prescriptions to JSON backup and CSV export.
6. Split large UI files.
7. Add accessibility labels.
8. Replace icon and prepare screenshots.

## Bottom Line

RepLog is strategically strong and feature-rich. It now has the foundations for the “Obsidian of strength training” vision: local-first ownership, structured training data, analytics, prescriptions, and adaptive programming. The next phase should prioritise stability, migration validation, maintainability, and release polish over adding more features.
