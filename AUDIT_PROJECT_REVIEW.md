# RepLog - Whole-Project Audit

Branch: `sync/apply-improvements`  HEAD at audit: `6b80f0b`
Scope: codebase, architecture, database, dependency injection, UI/feature wiring,
release quality. Verified without an Android SDK (no `assembleDebug` possible
here) via standalone kotlinc compile of the pure layers plus static analysis.

## Overall verdict: HEALTHY, release-candidate quality
No compile-breaking errors. Architecture is consistent and offline-first. Two
in-flight / dead-wiring issues from Sprint 5 were found and fixed during this
audit (see "Issues found & fixed"). Remaining items are non-blocking polish and
one launch gate (`assembleDebug`).

---

## 1. Codebase metrics
- Kotlin source files (main): 137
- Lines of code (main): ~15,660
- Composable screens: 11
- ViewModels: 11 (all `@HiltViewModel`)
- Bundled exercise library: 516 exercises
- Quick Workout catalogue: 17 curated sessions, 7 named programmes

## 2. Architecture
- Clean layering: `data` (Room entities/DAOs/repositories) -> `domain` (pure
  engines: recommendation, recovery, training DNA, genome, goals, library,
  templates) -> `ui` (Compose screens + Hilt ViewModels).
- The pure `domain` + `model` layer compiles standalone (kotlinc 1.9.22): exit 0,
  **219 classes**. This confirms the business logic has no hidden Android coupling.
- Sprint 5 introduced `util/WorkoutStarter` as a single source of truth for
  starting a session (from a Quick Workout / template / repeated session). Home,
  Quick Workouts and the active-workout engine all delegate to it - no duplicated
  session-creation logic.

## 3. Database integrity (Room) - PASS
- 17 entities; `version = 15`.
- 14 migrations defined = 14 registered, unbroken chain `MIGRATION_1_2` ...
  `MIGRATION_14_15`.
- 15 DAO accessors = 15 Hilt DI providers.
- Latest two migrations:
  - `MIGRATION_13_14` - `rest_day_overrides` table (Sprint 4 P5).
  - `MIGRATION_14_15` - `workout_templates.isFavorite` column (Sprint 5 P5).
- Foreign-key cascade chain (sessions -> exercises -> sets / prescriptions)
  remains intact; bulk delete relies on it correctly.

## 4. Dependency injection (Hilt) - PASS
- All ViewModels annotated `@HiltViewModel`; all DAO providers present in
  `AppModule`. New singletons (`WorkoutStarter`, `DataResetManager`,
  `RestDayOverrideRepository`) are constructor-injected with `@Singleton`/`@Inject`.
- No field injection, no missing provider detected by static review.

## 5. Offline-first constraint - PASS
- Zero network libraries (Retrofit/OkHttp/Firebase/ktor/HTTP/AWS/googleapis).
- No `INTERNET` permission in the manifest.
- Export/sharing is local files + Storage Access Framework + the OS share sheet.

## 6. Release quality - PASS
- No deprecated Compose APIs: `Divider`, `progress = Float` ProgressIndicator,
  `rememberRipple`, non-mirrored `ArrowBack` all absent.
- Accessibility: every `IconButton` has a non-null `contentDescription`
  (0 violations found).
- Navigation: the "Home loops to Training" defect was fixed in Sprint 4 (distinct
  `workout` vs `workout_recommendation` routes; consistent tab-switch semantics).

## 7. Issues found & fixed during this audit
1. **In-flight incomplete wire (Sprint 5 P1).** `prefs.autoFocusField` had been
   added as a 12th `combine` flow in `ActiveWorkoutViewModel` but was never
   extracted or exposed - dead collection. FIXED: extracted as `args[11]` and
   surfaced on `ActiveWorkoutUiState.autoFocusField` (commit `94ecb8c`).
2. **P5 favourites unreachable.** `toggleFavorite` existed but no screen called
   it, so the Home "Quick Start" row could never be populated - the feature was
   non-functional end to end. FIXED: added a star/unstar button to the Training
   screen's `TemplateCard` (commit `6b80f0b`).
3. **Dead code.** Three `ActiveWorkoutViewModel` methods (`startQuickWorkout`,
   `repeatWorkout`, `duplicateQuickWorkout`) were superseded by the
   `WorkoutStarter`-backed Home/QuickWorkouts paths and had no callers. REMOVED
   (commit `6b80f0b`) - functionality unchanged.

## 8. Remaining issues (non-blocking)
- **P1 auto-focus behaviour not implemented in UI.** The `autoFocusField`
  preference is now plumbed into state, but the set-entry dialog does not yet use
  a `FocusRequester` to focus weight/reps, and there is no Settings toggle to
  change it. The preference defaults to "weight" and is inert until wired. (Low
  risk; see optimisation report for the plan.)
- **Migration test lag.** `AppDatabaseMigrationTest` covers through v13 only; it
  does not exercise `MIGRATION_13_14` or `MIGRATION_14_15`. The test does not
  assert a fixed final version, so it does not fail - but the two newest
  migrations are untested. (Not added here per the project's no-committed-tests
  policy; recommended for the maintainer on an SDK host.)
- **Performance: full-session fan-out.** `HomeViewModel` and
  `ActiveWorkoutViewModel` each subscribe to `getAllSessions()` (a `@Transaction`
  load of every session + all child exercises + all sets) inside a `combine`,
  recomputing on every DB change. See the optimisation report.

## 9. Only outstanding launch gate
- `:app:assembleDebug` on a host with the Android SDK. This environment has no
  SDK, so the full Compose/Hilt/Room build (and KSP annotation processing) cannot
  be run here. It remains the single mandatory pre-release verification, and the
  place to confirm: the v14->v15 migration on an existing install, the favourites
  round-trip (star in Training -> Home Quick Start), and Quick Workout start/save.
