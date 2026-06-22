# RepLog2 — Audit, Merge Reconciliation & Release-Readiness Assessment

_Date: 2026-06-22 · Integration branch: `integration/sprint4-sprint5` · Merge commit: `f94b805` (+ build fix `9b60d0c`)_

---

## 1. Executive summary

Sprint 4 (`d09b4e6`, branch `main`) and Sprint 5 (`1ee4d74`, branch `sprint5-dna`) are **divergent siblings** off the initial commit `5561ef3` — not a linear history. They were reconciled with a genuine three-way Git merge.

- **3 textual conflicts** (`AppDatabase.kt`, `AppModule.kt`, `ActiveWorkoutViewModel.kt`) — all resolved.
- **3 latent defects that already existed in Sprint 5** were found and fixed (they would have broken compile/runtime regardless of the merge).
- The pure-Kotlin domain layer (Training DNA + recommendation engines + data models) **type-checks clean** with `kotlinc 1.9.22` (the project's exact Kotlin version).
- A full Android build (`:app:assembleRelease`) **cannot be executed in this environment** (no Android SDK). Final compile + release sign-off must run on an Android-SDK host/CI — see §5.

**All ten requested features are preserved and wired** (§3). No new features were added; Sprint 6 was not started.

---

## 2. Branch reconciliation

```
*   f94b805  Merge sprint5-dna into Sprint 4   <-- integration HEAD (then 9b60d0c build fix)
|\
| * 1ee4d74  Sprint 5 training DNA and recommendation engine   (branch sprint5-dna)
* | d09b4e6  Sprint 4 rest logging and progression systems      (branch main)
|/
* 5561ef3  Initial RepLog commit   <-- merge base
```

Obtaining Sprint 5: the commit was absent from the initial clone and a bare-SHA fetch failed. It was retrieved by fetching the **named branch** `sprint5-dna` from the public remote.

### 2.1 Conflict resolutions

**`AppDatabase.kt` (schema — the critical one).** Both sprints branched from the v8 base and *both* defined a `MIGRATION_8_9`:
- Sprint 4: `8→9` creates `rest_logs`; `9→10` adds workout-scoring columns (`qualityScore`, `totalVolume/Sets/Reps`, `prCount`), `session_exercises.notes`, `set_logs.prType/completed`. DB version **10**.
- Sprint 5: `8→9` instead created `training_dna_snapshots`, `training_dna_progression_scores`, `plateau_events`. DB version **10**.

Two different definitions of the same migration number cannot coexist. **Resolution:** keep Sprint 4's full chain `1→10` intact, then relocate Sprint 5's tables into a **new `MIGRATION_10_11`** and bump the database to **version 11**. Both upgrade paths and a fresh install now produce identical schema. `MIGRATION_10_11` is registered in `addMigrations(...)`.

**`AppModule.kt` (DI).** Union of both DAO-provider sets (rest-log + the four Sprint 5 DAOs).

**`ActiveWorkoutViewModel.finishWorkout()`.** Sprint 4 computes the workout score/summary and cancels the rest timer; Sprint 5 triggers `trainingDnaRepository.generateDNA()`. **Resolution:** keep Sprint 4's scoring/summary/rest-timer flow and insert Sprint 5's DNA generation as a best-effort `try/catch` after the session is persisted (DNA generation must never block or fail the summary). Sprint 5's conflicting branch referenced symbols (`entity`, `restEndMillis`, `NO_REST_TIMER`) from its own rest-timer design that do not exist in the Sprint-4-based merged file, so those lines were correctly dropped.

### 2.2 Defects fixed during reconciliation (pre-existing in Sprint 5)

1. **`ProgressionScorer` would not compile.** Line 15 filtered `perSessionBestEstimatedOneRm(...)` (a `List<Pair<Long, Double>>`) with `it.value` — `Pair` has no `.value`. Corrected to `it.second`. Confirmed via standalone `kotlinc`.
2. **Duplicate `TrainingDNARepository` class.** Sprint 5 added `TrainingDNARepository.kt` **and** overwrote the old `TrainingDnaRepository.kt` with a byte-identical copy of the same class — a redeclaration (and a same-name file clash on case-insensitive filesystems). Removed the duplicate. The original `TrainingDnaStore` implementation is preserved intact as `TrainingDnaSignalRepository.kt`.
3. **`recommendation_history` table was never created.** Sprint 5 declared the `RecommendationHistory` entity and its DAO but added no migration to create the table — Room would fail schema validation at startup. `MIGRATION_10_11` now creates `recommendation_history` with its `timestamp` and `recommendationType` indices. A `recommendationHistoryDao` provider was also added to Hilt.
4. **Migration test corrected to the reconciled schema.** `AppDatabaseMigrationTest` previously asserted the DNA tables appeared after `8→9`. Updated: `8→9` now asserts `rest_logs`; a new `migrate10To11_addsTrainingDnaAndRecommendationTables` test (with a v10 baseline) asserts all four Sprint 5 tables, including `recommendation_history`.

### 2.3 Build-portability fixes (`9b60d0c`)
- Removed `android.aapt2FromMavenOverride=/data/data/com.termux/...` from `gradle.properties` (hard-coded Termux path; breaks clean builds — AGP resolves `aapt2` from the SDK automatically).
- Removed `gradlew_fixed` (contained a shell here-doc, not a runnable script).

---

## 3. Preserved-feature verification

| Feature | Status | Key location(s) |
|---|---|---|
| Rest logging | ✅ preserved | `RestLog`, `RestLogDao`, `MIGRATION_8_9`, `util/timer/*`, `RestTimerService` |
| PR detection | ✅ preserved | `domain/pr/PRDetector.kt`, `util/PRCalculator.kt` |
| Progression suggestions | ✅ preserved | `domain/progression/ProgressionSuggester.kt` |
| Workout scoring | ✅ preserved | `domain/scoring/WorkoutScorer.kt`, scoring columns (`9→10`) |
| Training DNA | ✅ merged in | `domain/trainingdna/TrainingDnaEngine.kt`, `TrainingDnaModels.kt`, `TrainingDNARepository` |
| Plateau detection | ✅ merged in | `domain/trainingdna/PlateauDetector.kt`, `PlateauEvent`(+DAO) |
| Progression scoring | ✅ merged in (bug fixed) | `domain/trainingdna/ProgressionScorer.kt`, `TrainingDnaProgressionScore`(+DAO) |
| Recommendation engine | ✅ merged in | `domain/recommendation/{RecommendationEngine,ProgressionDecider,RecoveryAnalyzer,WorkoutPlanGenerator,RecommendationModels}.kt` |
| Recommendation history | ✅ merged in (table fixed) | `RecommendationHistory`(+DAO), `RecommendationRepository`, `recommendation_history` table |
| Training DNA UI | ✅ merged in | `ui/trainingdna/TrainingDnaInsightScreen.kt` + `TrainingDnaViewModel.kt`, routed via `NavGraph` & `ProgressScreen` |

Cross-file consistency confirmed: every repository→DAO call resolves; ViewModel→repository calls (`getLatestDNA`, `getPlateauEvents`, `generateDNA`) resolve; merged unit tests reference real APIs. Sprint 4's `domain/coach` & `domain/recovery` contract types share simple names (`Recommendation`, `RecoveryAnalyzer`) with Sprint 5's `domain/recommendation` types but live in **different packages** and are never imported together — no ambiguity (left as-is to avoid scope creep).

---

## 4. Compilation verification performed here

- **Tool:** standalone `kotlinc 1.9.22` (matches `build.gradle.kts`), JRE 11.
- **Scope:** `data/model/*` + `domain/trainingdna/*` + `domain/recommendation/*` + `util/PRCalculator.kt`, with `androidx.room` annotations stubbed (compile-time only).
- **Result:** `exit 0`, 79 classes — **clean**. This is the highest-risk merged logic and it type-checks.
- **Not verifiable here:** Room codegen (KSP), Hilt graph, Jetpack Compose UI, instrumentation tests — all require the Android SDK/build host.

---

## 5. Release-readiness assessment

Status: **NOT yet release-ready.** Reconciliation is done and the codebase is internally consistent, but the following must be cleared on a proper Android build host.

### 5.1 Blockers (must fix before store submission)
1. **Full build + tests not yet run.** Execute on an Android-SDK host (see §6). Treat any KSP/Hilt/Compose error as a blocker.
2. **`targetSdk = 34`.** As of 2026, Google Play requires **new** apps and updates to target **API 35 (Android 15)**. Bump `compileSdk`/`targetSdk` to 35 and re-test (note the existing `android.suppressUnsupportedCompileSdk=35` flag hints this was already anticipated).
3. **No release signing config.** `app/build.gradle.kts` defines no `signingConfigs`; a release AAB cannot be uploaded unsigned. Add a keystore-based release signing config (keys via env/`local.properties`, never committed).
4. **Room schema export is off.** `exportSchema = false` and no `room.schemaLocation` ⇒ migrations can't be validated against exported schemas and schema drift is invisible at build time. Strongly recommended before shipping migration v11: enable schema export and run `runMigrationsAndValidate`.

### 5.2 High priority
5. **Run the (now-corrected) migration instrumentation test on-device/emulator**, especially the new `10→11` path, to confirm real upgrades from a v10 install succeed.
6. **`versionCode`/`versionName` still `1`/`1.0.0`** despite five sprints — bump appropriately for the release.
7. **`RestTimerService` is a foreground service** — verify `FOREGROUND_SERVICE` (and on API 34+, the typed `FOREGROUND_SERVICE_*`) permissions + notification channel are declared/handled for the target SDK.

### 5.3 Medium / cleanup
8. The minimal `gradlew` stub works but is not the canonical Gradle wrapper; regenerate via `gradle wrapper --gradle-version 8.7` for robustness on all hosts.
9. Sprint 4's now-superseded `domain/coach` & `domain/recovery` contracts are dead code — consider removing in a later cleanup (out of scope here).
10. Release build enables `minify`+`shrinkResources`; verify `proguard-rules.pro` keeps Room/Hilt/Gson model & Compose entry points (run a `release` build, not just `debug`).

---

## 6. How to finish verification on an Android-SDK host

```bash
# Prereqs: JDK 17, Android SDK with platform 34 (and 35 after the targetSdk bump).
git checkout integration/sprint4-sprint5

# (optional) regenerate the canonical wrapper
gradle wrapper --gradle-version 8.7

# Compile + JVM unit tests (Training DNA, plateau, progression, etc.)
./gradlew :app:compileDebugKotlin testDebugUnitTest

# Room/Hilt codegen sanity (KSP) + lint
./gradlew :app:kspDebugKotlin :app:lintDebug

# Migration instrumentation test (needs device/emulator) — verifies v10 -> v11
./gradlew :app:connectedDebugAndroidTest

# Release artifact (after adding signing config + targetSdk 35)
./gradlew :app:assembleRelease   # or :app:bundleRelease for an AAB
```

A green run of the first command confirms the merge compiles end-to-end; the `connectedDebugAndroidTest` run confirms the reconciled migration chain upgrades real databases.
