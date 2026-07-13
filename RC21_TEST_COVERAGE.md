# RC21 — Test Coverage Audit

## Unit Tests Existing (16 files in app/src/test)

- `DefaultAnalyticsEngineTest.kt` 1.1KB
- `ExerciseCoachContractTest.kt` 4.6KB — tests coaching contract
- `AnatomyRenderingSystemTest.kt` 2.9KB — tests anatomy rendering system
- `ExerciseVisualDomainTest.kt` 8.0KB — tests MovementRegistry contains 33+ families, resolver derives spec correctly, grip/stance variations, unknown equipment fallback, unknown pattern fallback, muscle mapping, validator coverage >85%, automated validation of bundled exercises.json >200 exercises coverage >85%
- `SkeletalAnimationEngineTest.kt` 4.6KB — tests FK solves all joints 19, bone length invariance, joint constraint clamping preventing hyperextension (elbow 250 ->150), pose interpolator rotational slerp, movement families timeline evaluation, equipment anchoring follows wrists
- `TestFixtures.kt` 1.5KB
- `VisualEngineAdapterTest.kt` 4.7KB — tests adapter
- `ActiveWorkoutRecoveryTest.kt` 1.1KB
- `AdaptiveProgramEngineSmokeTest.kt` 2.2KB
- `BackupJsonFullRoundTripTest.kt` 3.9KB
- `BackupJsonTest.kt` 2.4KB
- `PlateCalculatorTest.kt` 1.2KB
- `ProgressionEngineTest.kt` 1.6KB
- `RestoreMergePlannerTest.kt` 1.4KB
- `WorkoutCsvExporterTest.kt` 2.5KB
- `WidgetStateFactoryTest.kt` 1.5KB

**Total unit test files:** 16

## Android Tests (2 files in app/src/androidTest)

- `AppDatabaseMigrationTest.kt` 17KB
- `OnboardingWalkthroughTest.kt` 3.1KB

## Integration Tests

- None dedicated integration tests for visualisation engine, but `ExerciseVisualDomainTest` acts as integration testing resolver + validator + registry + bundled JSON.

## Validation Tests

- `ExerciseVisualDomainTest.testValidatorAndCoverageReporting` — validates VisualCoverageReport total, success, fallback, unknown equipment, etc.
- `ExerciseVisualDomainTest.testAutomatedValidationOfBundledExercisesJson` — loads assets/exercises.json via Gson, maps to Exercise list, validates via ExerciseVisualValidator, asserts total >200 and coverage >85% and not null.
- `SkeletalAnimationEngineTest` — validates FK, bone invariance, constraint clamping, interpolator, timeline evaluation, equipment anchoring.
- `RenderingValidationSuite` and `ExerciseMotionValidator` exist as validation suites but not called in unit tests as automated CI? They are objects with validate() and generateReportText(), but no unit test calls them directly. Could be added.

## Rendering Tests

- `AnatomyRenderingSystemTest.kt` — tests anatomy rendering system (likely checks muscle map, region paths).
- No snapshot tests for Compose Canvas rendering (no Paparazzi or Showkase).

## Animation Tests

- `SkeletalAnimationEngineTest.testMovementFamiliesTimelineEvaluation` — evaluates timeline for 20 families duration positive and pose not null.
- No tests for bar path, COM, stabilisation, IK, hybrid solver, camera system, muscle activation.

## Performance Tests

- No performance tests (no Macrobenchmark, no Jank stats, no allocation tracking).

## Compose UI Tests

- `OnboardingWalkthroughTest` is UI test, `VisualEngineAdapterTest` maybe UI adapter test.
- No Compose UI tests for ExerciseAnimationView, MuscleBodyDiagram.

## Missing Coverage

- **Unit tests missing:**
  - BiomechanicalJointModel clamping realistic limits per joint
  - CentreOfMassCalculator COM calculation and balancing
  - IKSolver FABRIK solveFABRIK, solveTwoBoneArm, solveLegWithFootLock
  - HybridSolver solve with foot locking and hand targets
  - BarPathEngine calculateBarPosition for each BarPathType and validateBarPath
  - StabilisationEngine evaluate core, scapula, etc
  - CommercialMotionLibrary 55 templates existence and realistic angles within limits
  - EquipmentEngine 22 renderers list and resolvePrimary/getEquipmentLayer
  - CameraSystem selectBestView and culling
  - MuscleActivationEngine calculateActivations factor range and phase
  - BodyOrientationEngine orient with various orientations
  - HumanBodyRenderer and VolumetricRenderer (hard to unit test without DrawScope, but could test referenceSize calc)
  - LayeredRenderingPipeline getExpectedLayerOrder
  - ExerciseMotionValidator 9 checks

- **Integration tests missing:**
  - Full pipeline from Exercise -> Spec -> Timeline -> FK -> Hybrid -> Camera -> Body + Equipment rendering integration.

- **Snapshot tests missing:**
  - No screenshot tests for body rendering front/back/side views.

- **Performance tests missing:**
  - No allocation count, GC, frame time, recomposition count tests.

- **UI tests missing:**
  - No tests for ExerciseAnimationView playback controls, Canvas rendering, muscle diagram with activation.

## Coverage Report (Estimated)

- **Visual domain:** Existing tests cover ~60% of old motion system (FK, bone invariance, constraint clamping, timeline evaluation, equipment anchoring, registry, resolver, validator). New biomechanics (IK, COM, bar path, stabilisation, camera, muscle activation) have 0% unit test coverage.
- **Overall app:** 16 unit tests + 2 android tests, likely <30% overall coverage.
- **Visual engine specifically:** After RC20.4, old tests still pass (FK, bone invariance, etc) but new commercial motion library, hybrid solver, COM, bar path, stabilisation, camera, muscle activation not covered.

**Cannot generate exact coverage report because `./gradlew :app:testDebugUnitTest` fails due to offline plugin resolution, cannot run JaCoCo.**

**Must be measured in proper Android environment:**

```
./gradlew :app:testDebugUnitTest --tests "com.replog.domain.visual.*"
./gradlew :app:createDebugUnitTestCoverageReport (if JaCoCo configured)
```

## Recommendations

- Add unit tests for each new biomechanics file:
  - `BiomechanicalJointModelTest` — clamp realistic limits per JointId
  - `CentreOfMassCalculatorTest` — COM over mid-foot balanced
  - `IKSolverTest` — FABRIK reachable/unreachable, two-bone arm, leg foot lock
  - `HybridSolverTest` — foot locking, hand targets
  - `BarPathEngineTest` — vertical xVar<0.06, S-curve 0.02..0.08, etc
  - `StabilisationEngineTest` — neutral spine, hip stable, etc
  - `CommercialMotionLibraryTest` — 55 templates existence, duration positive, joint limits within realistic
  - `EquipmentEngineTest` — 22 renderers list, resolvePrimary
  - `CameraSystemTest` — selectBestView squat->side etc, culling
  - `MuscleActivationEngineTest` — factor range 0.3..1.0, phase
  - `ExerciseMotionValidatorTest` — passRate >85% after fixes

- Add integration test for full pipeline: Exercise -> VisualSpec -> Timeline -> FK -> Hybrid -> Camera -> Body rendering (mock DrawScope).

- Add Compose UI tests with `createComposeRule` for `ExerciseAnimationView`.

- Add performance tests with Macrobenchmark for frame time.

