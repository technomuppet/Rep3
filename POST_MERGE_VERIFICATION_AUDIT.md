# RepLog V2 — Post-Merge Verification & Release-Readiness Audit

_Date: 2026-06-22 · Branch: `integration/sprint4-sprint5` · Merge: `f94b805` (HEAD `38fab52`)_
_Scope: verification only — no new features, no Sprint 6, no architecture redesign. Only build-breaking defects fixed (none found beyond those already fixed in the merge)._

---

## Verdict at a glance

| Question | Answer |
|---|---|
| **1. Is the merge safe?** | **Yes.** All 11 systems present and coherently wired; no conflict residue; pure-Kotlin domain + models compile clean. |
| **2. Is the app buildable?** | **Very likely yes**, but **unproven here** — no Android SDK in this environment. Domain/model/util slice compiles with `kotlinc 1.9.22`; the Room/Hilt/Compose codegen build must be confirmed on an SDK host. |
| **3. Is the app releasable?** | **No — not yet.** Blocked by targetSdk 34 (Play requires 35), a foreground-service type/crash risk on Android 14+, and missing release signing. |
| **Top 10 blockers** | See §6. |

**Scores (out of 100):** Stability **78** · Architecture quality **72** · Feature completeness **88** · Release readiness **55**.

---

## Phase 1 — Merge Integrity Audit

Tracing each system through compile / DI / repository / navigation / database / migration:

| System | Compile | DI | Repository | Navigation | DB | Migration | Status |
|---|---|---|---|---|---|---|---|
| Rest logging | ✅ | ✅ `provideRestLogDao` | ✅ via `WorkoutRepository`/`RestTimerManager` | n/a (in-workout) | ✅ `rest_logs` | ✅ `8→9` | **Wired** |
| Rest timer enhancements | ✅ | ✅ `RestTimerManager @Inject` | ✅ `RestLogDao` | n/a | ✅ | ✅ | **Wired** (⚠ FGS runtime risk — §5) |
| PR detection | ✅ | n/a (`object`) | ✅ used by `ActiveWorkoutViewModel` | n/a | ✅ `set_logs.prType` (`9→10`) | ✅ | **Wired** |
| Workout scoring | ✅ | n/a (`object WorkoutScorer`) | ✅ `WorkoutRepository.updateSession` | n/a | ✅ scoring cols (`9→10`) | ✅ | **Wired** |
| Progression suggestions | ✅ | n/a | ✅ `ActiveWorkoutViewModel` | n/a | n/a | n/a | **Wired** |
| Training DNA | ✅ | ✅ 4 DAOs provided | ✅ `TrainingDNARepository` | ✅ `training_dna` route | ✅ `training_dna_snapshots` | ✅ `10→11` | **Wired** |
| Plateau detection | ✅ | ✅ `providePlateauEventDao` | ✅ `TrainingDNARepository` | ✅ (surfaced in DNA UI) | ✅ `plateau_events` | ✅ `10→11` | **Wired** |
| Progression scoring | ✅ (bug fixed in merge) | ✅ `provideTrainingDnaProgressionScoreDao` | ✅ `TrainingDNARepository` | ⚠ persisted, not shown | ✅ `training_dna_progression_scores` | ✅ `10→11` | **Wired (data only)** |
| Recommendation engine | ✅ | ✅ DAO provided | ✅ `RecommendationRepository` (`engine = RecommendationEngine()`) | ❌ **no UI consumer** | ✅ | ✅ `10→11` | **PARTIALLY CONNECTED** |
| Recommendation history | ✅ | ✅ `provideRecommendationHistoryDao` | ✅ `RecommendationRepository` | ❌ **no UI consumer** | ✅ `recommendation_history` | ✅ `10→11` | **PARTIALLY CONNECTED** |
| Training DNA UI | ✅ | ✅ `TrainingDnaViewModel @HiltViewModel` | ✅ `TrainingDNARepository` | ✅ Progress → "View Training DNA" → `TrainingDnaInsightScreen` | ✅ | ✅ | **Wired** |

### Partially-connected findings
- **Recommendation engine + history are fully implemented and injectable but have NO UI surface.** `RecommendationRepository` is referenced only by itself; `RecommendationEngine` only by `RecommendationRepository`; no ViewModel injects either. `recommendation_history` is therefore never written or read at runtime. This is *backend-complete, frontend-absent*. It does not break the build and is not harmful, but it is dead at the app level. **Decision needed:** ship it dormant (fine — the table exists and migrates correctly) or wire a minimal UI later (out of scope now).
- **Progression scores** are computed and persisted by `TrainingDNARepository.generateDNA()` but the Training DNA screen displays the snapshot + plateaus, not the per-exercise progression-score rows. Data is captured; presentation is partial. Non-blocking.

Everything else is end-to-end wired and reachable.

---

## Phase 2 — Database Audit & Migration Risk Report

**Schema:** version **11**, 15 entities, 13 DAOs, migrations `1→11` all declared and registered in order.

### Validated
- **Version chain:** continuous `1→2→…→11`, no gaps. `addMigrations(...)` lists all ten migration objects in order. ✅
- **Migration ordering:** Sprint 4 owns `8→9` (`rest_logs`) and `9→10` (scoring/notes/prType/completed). Sprint 5's Training-DNA + recommendation tables were correctly relocated to a new `10→11`. ✅
- **`MIGRATION_10_11` ↔ entities:** column names, types, nullability, and **every declared index** match the four entities exactly (`training_dna_snapshots`→`generatedAt`; `training_dna_progression_scores`→`exerciseId`,`calculatedAt`; `plateau_events`→`exerciseId`,`detectedAt`; `recommendation_history`→`timestamp`,`recommendationType`). ✅
- **Foreign keys:** all FK child columns have backing indices (Room requirement) — `session_exercises`, `set_logs`, `rest_logs`, `workout_prescriptions`, `template_exercises` all index their FK columns. ✅
- **`recommendation_history` table** — previously declared as an entity by Sprint 5 with **no creating migration** (would have crashed Room at startup). Fixed in the merge; now created in `10→11`. ✅
- **No duplicate entities, no orphan tables** in the schema. (`training_dna_metrics` + `knowledge_graph_relations` exist and migrate but are written only by the unused `TrainingDnaSignalRepository`/`KnowledgeGraphRepository` — orphan *data layer*, not orphan tables; see Phase 4.)

### Risks
| # | Risk | Severity | Detail |
|---|---|---|---|
| D1 | **Full fresh-install vs migrated schema not validated** | **High** | `exportSchema = false`, no `room.schemaLocation`. Room validates the entity-generated schema against the actual DB at open; a single column/index/default mismatch anywhere in the 15 tables throws `IllegalStateException` at first launch / first upgrade. Cannot be machine-verified without the Android build. **Must** run a real fresh-install + `1→11` upgrade on device/emulator. |
| D2 | **Migration test covers only 3 hops** | Medium | Instrumentation test validates `6→7`, `8→9`, `10→11`. Hops `1→6`, `7→8`, `9→10` are untested. Recommend a full `1→11` walk via `MigrationTestHelper`. |
| D3 | **`set_logs` boolean columns** | Low | `isBodyweight`/`isPR` (added in v1 base) stored as INTEGER; defaults must match entity (no `@ColumnInfo(defaultValue)` — Room compares declared schema). Verify on the D1 device run. |
| D4 | **Restored sessions lose scoring fields** | Low | `restoreJson` rebuilds `WorkoutSession` without `qualityScore/totalVolume/...` though the backup carries `qualityScore`. Data-quality nit, not a schema risk. |

**No missing migrations, no duplicate entities, no orphan tables.** The single material risk is the unverifiable D1 (must be cleared on an SDK host).

---

## Phase 3 — Build Audit

**Environment limitation:** no Android SDK here, so `assembleDebug`, `test`, and `lint` **could not be executed**. Performed the strongest available substitutes.

- **Pure-Kotlin compile (kotlinc 1.9.22, the project's exact version):** `data/model/*` + all pure `domain/*` (trainingdna, recommendation, scoring, progression, pr) + pure `util` → **compiles clean, exit 0, 88 classes**, with `androidx.room` annotations stubbed. This is the highest-risk merged logic.
- **Conflict residue:** none (`grep` for markers across repo = clean).
- **Duplicate top-level declarations:** `Recommendation`, `RecommendationType`, `RecoveryAnalyzer` appear twice — but in **different packages** (`domain.coach`/`domain.recovery` vs `domain.recommendation`) and **no file imports both**, so **no compile clash**. (Dead duplicates — see Phase 4.)
- **Stubs / TODO / NotImplemented:** none in `app/src/main`.

### Findings
| Type | Item | Severity | Action taken |
|---|---|---|---|
| Deprecated API | `LinearProgressIndicator(progress = Float)` at `ProgressScreen.kt:249,280` — non-lambda overload deprecated in Material3 BOM 2024.09.x (the lambda form is used correctly elsewhere, e.g. `CircularProgressIndicator(progress = { … })`). | Warning (non-breaking) | **Not changed** — not build-breaking; per scope. Flagged for cleanup. |
| Hilt | Graph looks complete: every `@Inject`/`@HiltViewModel` dependency has a `@Provides` or constructor binding. `@HiltAndroidApp`, `@AndroidEntryPoint` present. | — | Verify with `kspDebugKotlin` on host. |
| Room | KSP codegen + schema validation unverifiable here (see D1). | — | Verify on host. |
| Compose | Compiler ext `1.5.9` matches Kotlin `1.9.22`. UI not compiled here. | — | Verify on host. |

**No build-breaking defects identified in the verifiable surface; no code changes required in this phase.** The merge's earlier fixes (ProgressionScorer `.second`, duplicate-repo removal, `recommendation_history` migration) already cleared the known breakers.

---

## Phase 4 — Architecture Audit (cleanup list, ranked by risk — NOT removed)

| Rank | Item | Type | Risk to remove | Notes |
|---|---|---|---|---|
| 1 | `domain/coach/CoachContracts.kt` (`Recommendation`, `RecommendationType`, `CoachEngine`, `RecommendationGenerator`) | Legacy contract superseded by `domain/recommendation` | **Low** | No consumers in `main`. Safe to delete in a dedicated cleanup commit. |
| 2 | `domain/recovery/RecoveryContracts.kt` (`RecoveryAnalyzer` interface, etc.) | Legacy contract superseded by `domain/recommendation/RecoveryAnalyzer` | **Low** | No consumers. |
| 3 | `domain/forecasting`, `domain/programs`, `domain/discoveries`, `domain/insights` contracts | Unused interfaces (roadmap placeholders) | **Low–Med** | Confirm zero consumers before deleting; some may be referenced by `ProgressViewModel`/analytics — verify per file. |
| 4 | `RecommendationRepository` + `domain/recommendation/*` | Implemented but **unconsumed** by UI | **Medium** | Do **not** delete — it's a requested preserved feature; it's dormant pending UI. Leave as-is. |
| 5 | `TrainingDnaSignalRepository` + `TrainingDnaMetric` + `TrainingDnaDao` + `training_dna_metrics` table | Orphaned data layer (no consumer) | **Medium** | Superseded by the snapshot-based `TrainingDNARepository`. Removing touches DB schema (a new migration) — defer; keep for now. |
| 6 | `KnowledgeGraphRepository` + `KnowledgeGraphRelation` + `knowledge_graph_relations` table | Orphaned (no consumer) | **Medium** | Same as #5 — schema-touching; defer. |
| 7 | `PrescriptionDao`/`WorkoutPrescription` heavy paths | Lightly used | **High** | Used by adaptive workout + restore — keep. |

**Duplicate systems:** the `coach`/`recovery` contracts (Sprint 4) vs `recommendation` package (Sprint 5) are the only true duplication, and it's harmless dead code. **Recommendation: one low-risk cleanup commit removing ranks 1–3 only, after the on-host build is green.** Ranks 5–6 require schema migrations and should wait.

---

## Phase 5 — Release Readiness

**Play Store readiness**
- ❌ **`targetSdk = 34`.** As of Aug 2024, Google Play requires new apps and updates to **target API 35 (Android 15)**; a 34-targeted first submission will be rejected. Bump `compileSdk`/`targetSdk` to 35 and retest. (`gradle.properties` already carries `android.suppressUnsupportedCompileSdk=35`, suggesting this was anticipated.)
- ❌ **No release signing config.** `buildTypes.release` has no `signingConfig`; a release AAB cannot be produced/uploaded. Add keystore signing (secrets via `local.properties`/CI, never committed).
- ⚠ **`versionCode = 1`, `versionName = "1.0.0"`** after five sprints — set deliberately for the public release.
- ✅ `allowBackup`, data-extraction/backup rules, FileProvider, launcher/round icons present.

**targetSdk compliance / runtime**
- ❌/⚠ **Foreground-service crash & policy risk (Android 14+).** `RestTimerService.startForeground(id, notification)` is called **without a type argument**; the manifest declares `foregroundServiceType="mediaPlayback"` and the app requests `FOREGROUND_SERVICE_MEDIA_PLAYBACK`. A rest timer is **not** media playback — this is a **Play policy violation** and, on API 34+, a runtime risk (`startForeground` must satisfy the declared type, and `mediaPlayback` requires an actual ongoing media session). The app also declares `FOREGROUND_SERVICE_SHORT_SERVICE` but never uses the `shortService` type. **Recommended fix (separate, scoped task):** switch the rest timer to `shortService` (or drop the FGS entirely in favor of an exact alarm + notification) and pass the matching type to `ServiceCompat.startForeground(...)`. **Not patched here** (behavioral/architectural).

**Backup/restore reliability** — ✅ Solid. JSON export/import with fingerprint-based duplicate detection (`RestoreMergePlanner`); exercises matched/recreated **by name** so custom exercises survive restore on a fresh install. Derived data (DNA, plateaus, recommendations, rest logs) intentionally not backed up — acceptable (regenerable). Minor: restored sessions don't carry forward `qualityScore` (D4).

**Crash-risk areas** — **Low.** Only 2 `!!` uses, both locally guarded. All `.average()` call sites are empty/NaN-guarded (`if (isEmpty) return`, `size < 2`, `takeIf{isNotEmpty}?.average()`, or `WorkoutScorer`'s `takeIf{!isNaN}`). `TrainingDnaEngine` degrades to `buildEmptySnapshot` on low data. Main residual crash vector is the FGS issue above and the unverified Room schema (D1).

**Onboarding flow** — ✅ Clean. `NavGraph` gates on `preferencesManager.onboardingComplete` (DataStore); `OnboardingScreen.finish(useKg)` sets unit preference + completion. First-launch seeds the bundled exercise library + built-in templates from `assets/exercises.json`.

**Offline behaviour** — ✅ **Fully offline / local-first.** No `INTERNET` permission, no Retrofit/OkHttp/HTTP usage anywhere. Room + DataStore + bundled JSON only. Matches the product vision.

### Scores (out of 100) — with rationale
| Dimension | Score | Rationale |
|---|---|---|
| **Stability** | **78** | Clean domain compile, low crash surface, graceful empty-data handling. Capped by the unverified Room fresh-install/upgrade (D1) and the FGS runtime risk. |
| **Architecture quality** | **72** | Clear layered MVVM + Hilt + repositories. Docked for duplicate legacy contracts, orphaned DNA-metric/knowledge-graph data layer, and an implemented-but-unconsumed recommendation subsystem. |
| **Feature completeness** | **88** | All 11 requested systems present; Training DNA UI shipped. Docked because recommendation engine/history has no UI and progression scores aren't surfaced. |
| **Release readiness** | **55** | Hard blockers: targetSdk 34, no signing config, FGS type/crash risk, unverified full build. Strong fundamentals (offline, backup, onboarding) keep it above midpoint. |

---

## Phase 6 — Final Recommendation (strict)

1. **Is the merge safe?** **Yes.** Coherent, no conflict residue, schema reconciled correctly, highest-risk logic compiles.
2. **Is the app buildable?** **Unproven but probable.** The pure-Kotlin core compiles; the Android (Room/KSP/Hilt/Compose) build must be confirmed on an SDK host. Treat as *not done* until `:app:assembleDebug` is green.
3. **Is the app releasable?** **No.** Not until the critical blockers below are cleared.

### Top 10 remaining blockers (ranked)
1. **Full Android build + unit/instrumentation tests not yet run** (no SDK here). Run `:app:assembleDebug testDebugUnitTest connectedDebugAndroidTest`.
2. **Room fresh-install + `1→11` upgrade unvalidated** (D1) — must pass on device/emulator before release.
3. **`targetSdk = 34`** — bump to 35 for Play; retest behavior changes.
4. **No release signing config** — cannot ship an AAB.
5. **Foreground-service type mismatch / `startForeground` without type** (rest timer declared `mediaPlayback`) — Play rejection + Android 14+ runtime risk.
6. **`exportSchema = false` / no schema export** — enable to make migration validation testable and prevent silent drift.
7. **Migration test covers only 3 of 10 hops** — add a full `1→11` validation.
8. **Recommendation engine/history has no UI** — decide ship-dormant vs. minimal surface (must not block, but confirm the intent).
9. **`versionCode`/`versionName` still 1 / 1.0.0** — set release values.
10. **`release` build uses minify + shrinkResources** — verify `proguard-rules.pro` keeps Room/Hilt/Gson models & Compose; run a real `assembleRelease`, not just debug.

### Issue tiers
- **Critical (block release):** #1 build, #2 Room validation, #3 targetSdk 35, #4 signing, #5 foreground service.
- **High:** #6 schema export, #7 migration coverage, R8/ProGuard keep verification (#10).
- **Medium:** #8 recommendation UI decision, restored-session scoring (D4), orphaned DNA-metric/knowledge-graph layers (Phase 4 #5–6).
- **Nice-to-have:** remove duplicate legacy contracts (Phase 4 #1–3), migrate off deprecated `LinearProgressIndicator(progress=Float)`, surface progression scores in the DNA UI, regenerate the canonical Gradle wrapper, bump unused-roadmap-package cleanup.

**Bottom line:** the merge is production-*coherent* but the app is **not production-*ready***. Clear the five critical blockers — chiefly a green on-device build with Room validation, targetSdk 35, signing, and the foreground-service correction — and RepLog V2 is a credible stable first public release. No feature work or Sprint 6 is required to get there.
