# RepLog — Changes since the last APK build

**Base (your building APK):** `79debc5` — *Remove duplicate ExperimentalLayoutApi import*
**Branch:** `sync/apply-improvements`
**Scope:** full product-quality + differentiation push — the 14-item roadmap **plus** the strategy-review moat features (Goal Engine, Coach Dashboard, Training Genome, shareable cards).

All changes are **additive**, layered on top of your APK-building `main`. None of your build-stabilising edits were reverted (CI workflow, `gradlew_fixed`, `CoachContracts`, `SettingsViewModel`/`RestTimerService` fixes, the `TrainingDnaRepository.kt` filename, removed unit tests).

> **Schema:** the Room DB moved **v11 -> v13** vs your APK. Two migrations run on update: `MIGRATION_11_12` (adds `WorkoutSession.sessionRating`) and `MIGRATION_12_13` (adds the `goals` table). Worth an on-device upgrade check.
>
> **Not built here:** there is no Android SDK in the dev environment, so all verification was pure-Kotlin compile (kotlinc 1.9.22) + logic tests + static wiring checks. **Run `:app:assembleDebug` before release.** Give the share-card image renderer a quick on-device smoke test (it was verified only against Android stubs).

---

## Commits (oldest -> newest)

| Commit | Summary |
|--------|---------|
| `c7bc1e5` | Sync: 516-exercise library + 27 templates + personalized onboarding |
| `0157fce` | Priority 2 UX: reps quick-add, RPE/Tempo dropdowns, Home dashboard |
| `2454cf4` | Priority 3: progression forecasts, recovery dashboard, adaptive templates |
| `6e68bc9` | Muscle Gap Analysis with one-tap Add to template (#8) |
| `961e9b5` | Session Rating (#10): post-workout "How was this workout?" |
| `eb48fee` | Volume Landmarks (#12): weekly sets per muscle vs optimal range |
| `45fb581` | Goal Engine: goal-centric forecasting |
| `95569d2` | Coach Dashboard: unified "Good morning" advisor on Home |
| `b95074c` | Recovery Calendar, Volume Heatmap, Smart Exercise Swap |
| `ab167b7` | Training Genome: learn how THIS user grows best (the moat) |
| `45f8a7a` | Phase D: shareable progress cards (image export, offline) |

_(Two earlier commits, `7a7f8d1` and `c8fc0af`, added an interim changelog/patch set that this revision supersedes.)_

---

## Feature coverage

### Roadmap (all 14 items)
| # | Feature | Where |
|---|---------|-------|
| 1 | 516 tagged exercises | `assets/exercises.json`, `tools/generate_exercises.py` |
| 2 | 27 built-in templates | `domain/templates/BuiltInTemplates.kt`, `util/DataSeeder.kt` |
| 3 | Home dashboard (vs Workout) | `ui/home/*`, `domain/home/HomeDashboardStats.kt` |
| 4 | Onboarding personalization | `ui/onboarding/*`, `domain/templates/ProgramGenerator.kt` |
| 5 | Reps +1/+2/+5 | `ui/workout/ActiveWorkoutScreen.kt` |
| 6 | Tempo dropdown | `domain/logging/SetInputPresets.kt` |
| 7 | RPE dropdown | `domain/logging/SetInputPresets.kt` |
| 8 | Muscle gap + add to template | `domain/musclegap/MuscleGapAnalyzer.kt`, `ui/trainingdna/*` |
| 9 | Offline workout builder | `domain/templates/ProgramGenerator.kt` |
| 10 | Session rating | `data/model/WorkoutSession.kt`, `AppDatabase.kt` (v12), `ui/workout/*` |
| 11 | Progression forecast | `domain/forecast/ProgressionForecaster.kt` |
| 12 | Volume landmarks | `domain/volume/VolumeLandmarks.kt` |
| 13 | Recovery dashboard | `domain/recovery/RecoveryDashboard.kt` |
| 14 | Adaptive templates | `domain/adaptive/AdaptiveTemplateAdvisor.kt` |

### Strategy-review moat / growth features
| Feature | Where |
|---------|-------|
| Goal Engine (goal-centric forecasting: 1RM/reps/bodyweight, ETA, milestones) | `domain/goals/GoalEngine.kt`, `data/model/Goal.kt`, `data/db/GoalDao.kt` (v13), `data/repository/GoalRepository.kt`, `ui/goals/*` |
| Coach Dashboard ("Good morning" unified advisor on Home) | `domain/coachdash/CoachBriefing.kt`, `ui/coach/CoachDashboardCard.kt`, `ui/coach/CoachViewModel.kt` |
| Recovery Calendar (green/amber/red day strip) | `domain/recovery/RecoveryCalendar.kt`, `ui/trainingdna/*` |
| Volume Heatmap (muscle-group grid) | `ui/trainingdna/TrainingDnaInsightScreen.kt` (reuses VolumeLandmarks) |
| Smart Exercise Swap (offline alternatives) | `domain/swap/ExerciseSwapEngine.kt`, `ui/exercise/*` |
| Training Genome (per-user growth drivers over months) | `domain/genome/TrainingGenome.kt`, `ui/trainingdna/*` |
| Shareable progress cards (PNG image export) | `util/ShareCardRenderer.kt`, `ui/workout/ActiveWorkoutScreen.kt`, `ui/home/HomeScreen.kt` |

---

## Files created (24)

**Domain (pure Kotlin):**
- `domain/templates/BuiltInTemplates.kt`, `domain/templates/ProgramGenerator.kt`
- `domain/logging/SetInputPresets.kt`
- `domain/home/HomeDashboardStats.kt`
- `domain/musclegap/MuscleGapAnalyzer.kt`
- `domain/forecast/ProgressionForecaster.kt`
- `domain/recovery/RecoveryDashboard.kt`, `domain/recovery/RecoveryCalendar.kt`
- `domain/adaptive/AdaptiveTemplateAdvisor.kt`
- `domain/volume/VolumeLandmarks.kt`
- `domain/goals/GoalEngine.kt`
- `domain/coachdash/CoachBriefing.kt`
- `domain/genome/TrainingGenome.kt`
- `domain/swap/ExerciseSwapEngine.kt`

**Data:**
- `data/model/Goal.kt`, `data/db/GoalDao.kt`, `data/repository/GoalRepository.kt`

**UI:**
- `ui/goals/GoalsScreen.kt`, `ui/goals/GoalsViewModel.kt`
- `ui/coach/CoachDashboardCard.kt`

**Util / tooling:**
- `util/ShareCardRenderer.kt`
- `tools/generate_exercises.py`

**Docs:**
- `CHANGELOG_SINCE_APK.md` (this file)

## Files edited (17)

- `app/src/main/assets/exercises.json` (32 -> 516 exercises)
- `data/db/AppDatabase.kt` (entities, DAOs, `MIGRATION_11_12`, `MIGRATION_12_13`, v13)
- `data/model/WorkoutSession.kt` (`sessionRating`)
- `data/repository/WorkoutRepository.kt` (`setSessionRating`)
- `di/AppModule.kt` (`provideGoalDao`)
- `util/DataSeeder.kt` (template seeding + program install + missing-exercise upgrade)
- `util/PreferencesManager.kt` (training-profile + coach cache keys)
- `androidTest/.../AppDatabaseMigrationTest.kt` (migrate11To12, migrate12To13)
- `ui/home/HomeScreen.kt`, `ui/home/HomeViewModel.kt`
- `ui/onboarding/OnboardingScreen.kt`, `ui/onboarding/OnboardingViewModel.kt`
- `ui/navigation/NavGraph.kt` (Goals route + Home wiring)
- `ui/workout/ActiveWorkoutScreen.kt`, `ui/workout/ActiveWorkoutViewModel.kt`
- `ui/trainingdna/TrainingDnaInsightScreen.kt`, `ui/trainingdna/TrainingDnaViewModel.kt`
- `ui/exercise/ExerciseLibraryScreen.kt`, `ui/exercise/ExerciseViewModel.kt`
- `ui/coach/CoachViewModel.kt`

---

## Verification performed

- **Full pure-Kotlin domain compiles clean** on kotlinc 1.9.22 (latest run: 208 classes).
- **Logic tests pass in-sandbox** (not committed, to keep your build/CI test-free): ProgramGenerator (7), SetInputPresets (3), HomeDashboardStats (5), Progression/Recovery/Adaptive (6), MuscleGap (4), GoalEngine (7), CoachBriefing (4), Volume/Swap/Calendar (6), Training Genome (3).
- `ShareCardRenderer` type-checked against Android stubs (caught + fixed a Float/Int issue).
- DB schema chain validated: 13 migrations declared = 13 registered; `sessionRating`/`goals` consistent between entities and migrations.
- No merge-conflict markers anywhere.

---

## How to bring this into your repo

A git **bundle** is provided at `patches/replog-improvements.bundle` (covers `79debc5..HEAD`). From a clone of your repo:

```bash
# 1) fetch the commits from the bundle
git fetch /path/to/replog-improvements.bundle HEAD:bundle/improvements

# 2) merge onto main (fast-forward, since they sit on top of 79debc5)
git checkout main
git merge --ff-only bundle/improvements

# 3) build + test, then push
./gradlew :app:assembleDebug
git push origin main
```

Alternatives in `patches/`: a flat `replog-improvements.patch` (`git apply`) and per-commit patches in `patches/commits/` (`git am`).
