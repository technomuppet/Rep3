# RepLog — Error & Unimplemented-Code Audit

_Scope: branch `sync/apply-improvements` @ `e8ea98f` (= the state currently in the workspace; equal to remote `main` minus your one `size`-import build fix). Verified by pure-Kotlin compile of the domain+model layer (kotlinc 1.9.22) plus exhaustive static wiring checks. No Android SDK here, so the Compose/Hilt build itself was not executed — run `:app:assembleDebug` to confirm._

## TL;DR
- **No compile-breaking errors found** in the verifiable layer: domain + model compile clean (208 classes). DB schema, DI graph, navigation routes, ViewModels and domain engines are all internally consistent and wired.
- **No abandoned/orphaned engines:** every domain engine, DAO, repository and ViewModel has at least one real consumer.
- The genuine issues are **(a) one navigation UX defect**, **(b) a few small dead/loose ends**, and **(c) several VNext-spec features that are written-but-not-yet-built** (scope, not bugs).

---

## A. Errors / defects

| # | Severity | Issue | Location | Notes |
|---|----------|-------|----------|-------|
| A1 | **Medium (UX)** | Secondary screens have **no back affordance**. Goals, Training DNA, Coach History show the bottom bar but no TopAppBar/back arrow; the system back button pops the whole stack. Violates "every secondary screen must support back navigation." | `ui/navigation/NavGraph.kt` (bottom bar shown unconditionally; no top bar) | Not a hard dead-end (a tab tap escapes), but not production-grade. Fix: per-route TopAppBar with back arrow / hide bottom bar on secondary routes. |
| A2 | Low | **Dead file**: `RestTimerPrefs.kt` is a comment-only placeholder, referenced nowhere. | `util/timer/RestTimerPrefs.kt` | Safe to delete; harmless. |
| A3 | Low (verify on device) | Build not executed here (no SDK). Compose UI, Hilt codegen, KSP/Room codegen unverified. | — | Domain/model compile clean; risk is low but real for the UI layer. |

**Checked and found OK (not errors):**
- `confirmButton = {}` in the superset & exercise-picker dialogs — intentional (action on item tap).
- `BackupJson` doesn't store `sessionRating` — acceptable (nullable/derived; restore reconstructs fine).
- FileProvider `share/` subdir is covered by the existing `cache-path path="."`.
- `material-icons-extended` is a dependency, so all `Icons.Default.*` resolve.
- `TestFixtures`/migration tests reference only existing symbols; migration test covers v10→13.

---

## B. Schema / data integrity — PASS
- 16 entities, 14 DAO accessors, 14 DI providers — all consistent.
- **12 migrations declared = 12 registered**; DB `version = 13`; chain `1→13` continuous.
- `WorkoutSession.sessionRating` (entity) ↔ `MIGRATION_11_12` (`ADD COLUMN sessionRating INTEGER`) ↔ test — all aligned.
- `goals` table: entity `Goal` ↔ `GoalDao` ↔ `provideGoalDao` ↔ `MIGRATION_12_13` ↔ test — all aligned.

---

## C. Feature wiring — every engine is consumed
Engine → consumer(s) confirmed for: TrainingGenome, GoalEngine, CoachBriefing, ExerciseSwapEngine, RecoveryCalendar, RecoveryDashboard, VolumeLandmarks, MuscleGapAnalyzer, AdaptiveTemplateAdvisor, ProgressionForecaster, ProgramGenerator, BuiltInTemplates, SetInputPresets (RPE/Tempo), HomeDashboardStats, ShareCardRenderer. All 10 `@HiltViewModel`s are used; all 9 nav routes are registered AND reachable.

---

## D. Written-but-NOT-implemented (vs the VNext brief — scope gaps, not bugs)

| Brief item | Status | Gap |
|------------|--------|-----|
| P4 Multi-select exercise filtering | **Not implemented** | `ExerciseViewModel.selectedCategory: String` is **single-select** (`cat == "All" || it.category == cat`). No multi muscle chips, no equipment/difficulty/movement-pattern filters. Cards don't show primary+secondary muscles. |
| P6 Goal cards on Home | **Not implemented** | Home has a *Goals button* but no goal **progress cards** on the dashboard. |
| P2 Home as full dashboard | **Partial** | Home shows the Coach Dashboard card + week stats + last PR, but does NOT surface Genome summary, Goal progress, Muscle Gap, or Forecasts as requested. |
| P7 Onboarding "preferred workout style" | **Partial** | Onboarding collects goal/level/equipment/days; "preferred workout style" not collected. |
| A1 Back navigation on secondary screens | **Not implemented** | See A1. |
| P5 RPE/Tempo dropdowns | **Implemented** ✅ | Preset chips wired in the set dialog. |
| P3 Feature reachability | **OK** ✅ | All systems reachable (some only via deep buttons — discoverability is a P2/P3 polish item, not a wiring bug). |

---

## Recommended fix order (low risk → high impact)
1. **A1** — add back arrows / hide bottom bar on Goals, Training DNA, Coach History (small, removes the only real nav defect).
2. **P6/P2** — add Goal-progress + Genome + Muscle-Gap cards to Home (pure wiring of existing engines).
3. **P4** — multi-select Exercise Library filtering (the largest net-new UI work).
4. **A2** — delete `RestTimerPrefs.kt`.
5. **P7** — add "preferred workout style" to onboarding.
6. **A3** — run `:app:assembleDebug` + `connectedDebugAndroidTest` on an SDK host before release.
