# Functionality Verification (Phase 1)

Each feature verified by tracing UI → ViewModel → repository/engine in source.
Status: WORKING (wired end-to-end) unless noted.

| Feature | Verified path | Status |
|---|---|---|
| Workout logging (add set) | `ActiveWorkoutScreen` → `viewModel.addSet` → `workouts.insertSet` + PR check | WORKING |
| Quick Complete | progression/target card → `viewModel.quickCompleteSet` → `addSet(WORKING)` | WORKING |
| Smart Repeat | "Repeat WxR" → `viewModel.repeatLastSet` (no dialog) | WORKING |
| Auto Focus / keyboard | `AddOrEditSetDialog` uses `FocusRequester` + `LocalSoftwareKeyboardController.show()` driven by `state.autoFocusField` ("weight"/"reps") | WORKING |
| Rest Timer | `restTimer.start(...)` on set complete when `restAutoStart`; foreground `RestTimerService` | WORKING |
| Workout recovery (process death) | `ActiveWorkoutViewModel.init` reads `prefs.activeSessionId` → `ActiveWorkoutRecovery.decide` → RESUME/CLEAR_STALE | WORKING |
| Finish workout flow | `viewModel.finishWorkout` writes summary, regenerates DNA, returns Home | WORKING |
| Quick Workouts | `QuickWorkoutsScreen` → start (`WorkoutStarter.startQuickWorkout`) / save (`DataSeeder.duplicateQuickWorkout`) | WORKING |
| Template sharing (export) | per-template Share → `TemplateShare.encode` → FileProvider share sheet (`.replogtemplate`) | WORKING |
| Template import | Settings → file picker → `importTemplateJson` → `TemplateShare.decode` + dedupe | WORKING |
| CSV export | `exportCsv` → `WorkoutCsvExporter.toCsv` → `FileExporter.save` (MediaStore Downloads/RepLog or SAF) | WORKING |
| JSON export | `exportJsonBackup` → `BackupJson.encode` → `FileExporter.save` + private copy | WORKING |
| Export location | SAF `OpenDocumentTree` persisted (`EXPORT_TREE_URI`); default Downloads/RepLog | WORKING |
| Backup / Restore | `restoreJsonText`/`restoreLatestJsonBackup` → `BackupJson.decode` + merge planner | WORKING |
| Recovery Centre | `RecoveryCentreScreen` → `IntelligenceRepository.buildRecoveryCentre` (RecoveryAnalyzer/Dashboard/Calendar) | WORKING |
| Muscle Balance | `MuscleBalanceScreen` → `buildMuscleBalance` (VolumeLandmarks + MuscleGapAnalyzer); Start/Add-to-Template/Dismiss | WORKING |
| DNA Evolution | `DnaEvolutionScreen` → `buildDnaEvolution` (stored snapshots only) | WORKING (needs ≥2 snapshots; otherwise informative empty state) |
| Today's Briefing | `HomeViewModel.briefing` (cached) → `IntelligenceRepository.buildBriefing`; narrative + Why + tap → Recovery Centre | WORKING |
| RepLog Score | `HomeViewModel.repLogScore` → `buildRepLogScore` → `RepLogScoreEngine` with breakdown | WORKING |
| History | `HistoryScreen` → `HistoryViewModel` (`getAllSessions` flow) | WORKING |
| Progress / Statistics | `ProgressScreen` → `ProgressViewModel` | WORKING (see perf note) |
| Settings | units, plates, rest presets, export folder, auto-focus, demo data, delete-all (typed DELETE), template import | WORKING |
| Navigation | all 14 routes reachable; Home always reachable | WORKING (see NAVIGATION_AUDIT.md) |

## Incomplete / partially wired features
None found. Every advertised feature has a complete UI → state → data path.

## Behavioural notes (not defects)
- DNA Evolution and the intelligence briefings require a few logged
  workouts/snapshots before they populate; until then they show empty/loading
  states (verified present on all three intelligence screens).
- "Coach" is not a standalone screen by design — coaching surfaces on the Home
  Coach card + Today's Briefing + Coach History; this is intentional, not a gap.
