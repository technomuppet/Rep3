# Sprint 7 - Architecture, Performance, File Inventory & Build Verification

## Architecture Report
- Layering unchanged and reinforced: `domain` (pure Kotlin engines) <- `data`
  (Room + repositories) <- `ui` (Compose + Hilt ViewModels).
- The new `IntelligenceEngine` lives in `domain` and is pure (no Android import),
  so it is unit-testable and KMP-ready. The new `IntelligenceRepository` is the
  single data-gathering point; it composes existing repositories and engines and
  is the only place that knows how to assemble a briefing.
- DI: `IntelligenceRepository` is `@Singleton @Inject constructor` over
  already-provided repositories/PreferencesManager, so Hilt auto-provides it -
  no new module needed. `HomeViewModel` (`@HiltViewModel`) injects it.
- Navigation unchanged (no new routes this sprint; the briefing and continue
  card live on the existing Home destination).

## Performance Report
- Intelligence is computed on demand and cached in `HomeViewModel`; it is
  recomputed only when the number of completed sessions changes, so it never
  runs on a per-set DB change.
- All intelligence data-gathering uses bounded/aggregate reads:
  - `getRecentCompletedSessions(60)` (recovery, genome, volume, recommendation).
  - DNA snapshot / progression scores / goals / exercise library are small reads.
- `RecommendationRepository.buildContext` was changed from `getAllSessions()` to
  the bounded recent-completed query (Priority 7), so generating a recommendation
  no longer loads the entire database.
- Combined with Sprint 6 (SQL-aggregated `SessionSummaryRow`, bounded Home /
  active-workout / coach reads), no hot path reloads the full history. This keeps
  behaviour stable at 1,000 workouts / 50,000 sets / 200 templates.

## Updated File Inventory (Sprint 7)
New:
- `app/src/main/java/com/replog/domain/intelligence/IntelligenceEngine.kt`
- `app/src/main/java/com/replog/data/repository/IntelligenceRepository.kt`
- `SPRINT_7_REPORT.md`, `INTELLIGENCE_ENGINE.md`, this file.

Modified:
- `app/src/main/java/com/replog/ui/home/HomeViewModel.kt`
  (inject IntelligenceRepository; `briefing` + `continueWorkout` flows;
  `loadIntelligence()`; `ContinueWorkout` model)
- `app/src/main/java/com/replog/ui/home/HomeScreen.kt`
  (ContinueWorkoutCard + TodaysBriefingCard, collected + rendered)
- `app/src/main/java/com/replog/data/repository/RecommendationRepository.kt`
  (bounded history)

## Build Verification Report
- Pure domain + model layer compiles clean under standalone kotlinc 1.9.22
  (Room annotations stubbed): exit 0, 225 classes.
- IntelligenceEngine unit tests: 4/4 pass (explainable briefing, rest case,
  insufficient-data case, no-invented-insights guarantee). Tests were run in a
  throwaway sandbox and are NOT committed (CI is test-free by project policy).
- Static verification: brace/paren balance across all touched files; all Sprint 7
  symbols are consumed (no dead wiring); imports resolve.
- DB integrity: 17 entities, version 15, 14 migrations defined = 14 registered
  (unbroken chain), 15 DAO accessors = 15 Hilt DI providers.
- Constraints: 0 networking/telemetry references, no INTERNET permission, no
  deprecated Compose APIs, 0 TODO/placeholder/mock in production code.
- Not runnable here: `:app:assembleDebug` (no Android SDK in this environment).
  This remains the single mandatory on-device/SDK build gate, where the Compose/
  Hilt/Room/KSP build and the new Home cards are confirmed end to end.
