# RC21 — Runtime Validation — Does Commercial Renderer Actually Run?

## Can Application Be Launched?

- **Attempt:** No Android emulator or device available in sandbox, no `adb`, no Android Studio emulator. Cannot launch APK.
- **Alternative:** Verify via source code call graph and Compose preview (if available) that commercial renderer is actually used and legacy paths are not active.

## Verification Chain — Commercial Renderer Actually Being Used

### Call Graph Evidence

**ExerciseLibraryScreen.kt line 299:** `ExerciseAnimationView(ex)` — entry point for exercise detail.

**ExerciseAnimationView.kt:**
- `when (mode = remember(... VisualEngineAdapter.resolveAnimation))` — resolves via adapter.
- `is SkeletalEngine -> CommercialAnimationCanvas`
- `CommercialAnimationCanvas`:
  - `remember(timeline) { bakeTimeline(timeline, 60) }` pre-bake 60fps lookup
  - `remember(timeline) { topBar/bottomBar via 2 FK solves once }` cached
  - `AnimationCanvasContent` separate composable with own `elapsedSeconds` State via `remember(resetKey)` + `LaunchedEffect withFrameNanos` delta clamped 0..0.05
  - Inside Canvas DrawScope:
    - `bakedTimeline[frameIndex].pose` O(1) lookup no alloc
    - `ForwardKinematicsSolver.solve` 1 FK per frame (was 3, now 1 after cache)
    - `BarPathEngine.calculateBarPosition` per family VERTICAL/S_CURVE/CLOSE_VERTICAL/ARC_ELBOW/ARC_SHOULDER/CABLE_CONSTRAINED/HORIZONTAL/FIXED
    - `HybridSolver.Targets` left/right hand from desiredBar ± half gripWidth, left/right foot from current foot world if footLock true (support != HANGING)
    - `HybridSolver.solve` FABRIK 3 joints 10 iter
    - `CentreOfMassCalculator.calculate` Dempster masses, `StabilisationEngine.evaluate`
    - `CameraSystem.selectBestView` auto best-view
    - `SkeletalRenderer.drawCommercial` with progress t and cameraView
- `SkeletalRenderer.drawCommercial`:
  - `BodyOrientationEngine.orient` upper/lower decoupled around pelvis
  - `LayeredRenderingPipeline.drawPipeline` with RenderContext progress + cameraView
- `LayeredRenderingPipeline.drawPipeline`:
  - `FloorRenderer` always + support + benchFromAngle
  - PrimaryLayer BEHIND vs FRONT: `EquipmentEngine.resolvePrimary` + `resolveSupport` + `resolveBenchFromAngle`
  - `HumanBodyRenderer.drawHumanBody` volumetric with cameraView culling left/right to prevent overlap, path pooling chestPath abdomenPath pelvisPath reset() reuse
  - `EquipmentEngine` 22 renderers each path pooling ezPathPool etc, OlympicBarbell bar at wrist midpoint plates aligned, PullUpBar fixed overhead 8% height vertical lines to hands
  - Hands highlight, muscle overlay via `MuscleActivationEngine.calculateActivations`, coaching overlay COM green dot mid-foot yellow red line if unbalanced bar path blue dots

**Conclusion:** Commercial pipeline is genuinely executed via call graph, not just documented.

### Legacy Rendering Paths Still Active?

- `LegacyExerciseAnimationView` inside `ExerciseAnimationView.kt` — **REMOVED in RC20.4** (file now only commercial, fallback to commercial generic bench press). Grep shows no `LegacyExerciseAnimationView` in file after removal.
- `LegacyMuscleBodyDiagram` inside `MuscleBodyDiagram.kt` — **REMOVED in RC20.4**, now only vector engine, fallback to vector even if legacy boxes.
- `EquipmentAnchoring.kt` old avg wrists single line — **REMOVED** `rm .../EquipmentAnchoring.kt` in RC20.4.
- `ExerciseAnimation.kt` legacy Cartesian Points rubber-banding — **REMOVED** in RC20.4.
- `domain/library/MuscleMap.kt` legacy 19 regions — **REMOVED**, only new anatomy MuscleMap 27 regions remains.
- `VisualEngineAdapter` previously returned `LegacyStickFigure` on exception, now returns `SkeletalEngine` with commercial generic bench press even on exception — no longer returns legacy clip as primary. Sealed types `LegacyStickFigure` and `LegacyBoxes` kept for binary compat but never returned as primary in normal case (resolveAnatomy fallback to vector even if legacy, resolveAnimation fallback to commercial generic).
- **Result:** No legacy rendering paths still active in main UI. Only commercial path drives UI. Legacy fallbacks now point to commercial generic, not old stick figure.

### Every Commercial System Genuinely Executed

- **CommercialMotionLibrary:** Yes, via `KinematicMovementFamilies` facade which delegates to CommercialMotionLibrary, and via `VisualEngineAdapter` exercise-specific `getTimelineForExerciseName` 55 templates. Verified via grep `CommercialMotionLibrary` in `KinematicMovementFamilies`, `VisualEngineAdapter`, `ExerciseAnimationView`.
- **Timeline:** Yes, `SkeletalTimeline.evaluate` called per frame via baked lookup.
- **Hybrid Solver:** Yes, `HybridSolver.solve` called per frame in `AnimationCanvasContent` after bar path calculation.
- **Forward Kinematics:** Yes, `ForwardKinematicsSolver.solve` base + top/bottom cached.
- **Camera:** Yes, `CameraSystem.selectBestView` called per frame, passed to `SkeletalRenderer.drawCommercial` and `HumanBodyRenderer.drawHumanBody` with culling.
- **Equipment:** Yes, `EquipmentEngine.resolvePrimary` and `resolveSupport` called in `LayeredRenderingPipeline.drawPipeline`, 22 renderers drawn.
- **Body Renderer:** Yes, `HumanBodyRenderer.drawHumanBody` called in pipeline.
- **Muscle Overlay:** Yes, `MuscleActivationEngine.calculateActivations` called in `LayeredRenderingPipeline.drawMuscleOverlay` and `AnatomicalMuscleDiagram` with progress param.
- **Canvas:** Yes, `Canvas` in `AnimationCanvasContent` draws via `SkeletalRenderer.drawCommercial`.

### Screens Using New Engine

- **Exercise Library Detail Screen** (`ExerciseLibraryScreen.kt` line 299): `ExerciseAnimationView(ex)` commercial + `MuscleBodyDiagram` vector.
- **Exercise ViewModel** resolves via adapter, so any screen showing exercise detail uses new engine.
- **Coach screens** (`ui/coach/*`) do not render body.

### Screens Still Using Legacy Code

- **None for primary path.** Fallbacks now point to commercial generic, not legacy.

## UI Validation (Code Analysis, No Runtime)

- **Spacing, Alignment, Material 3 Compliance:** Uses `MaterialTheme.colorScheme.primary`, `tertiary`, `onSurface`, `onSurfaceVariant`, `Arrangement.spacedBy(4.dp)`, `Alignment.CenterVertically`, `padding(horizontal = 4.dp)`, Material icons Pause/PlayArrow/Refresh. Compliant.
- **Dark Mode / Light Mode:** `BodyPalette.fromMaterial(primary, onSurface)` skin neutral warm #D8BFA0, shirt primary, shorts #2E2E3A, outline onSurface alpha 0.35, adapts via primary/onSurface from MaterialTheme. Muscle palette dark/light bodyFill slate 800/200 primaryFill red 500 etc. Code suggests handles.
- **Tablet/Phone Scaling:** referenceSize = min(width,height)*0.32 ensures scaling, toScreen width*x height*y responsive, Canvas fillMaxWidth height 240dp.
- **Rotation:** BodyOrientationEngine handles 8 orientations via rotateAroundPivot cos/sin.
- **Landscape/Portrait:** Same scaling, Canvas fillMaxWidth height fixed 240dp may be small on tablet landscape but okay.
- **Accessibility:** contentDescription "Animated demonstration of ${exercise.name} with commercial biomechanics" in Canvas semantics, muscle diagram contentDescription primary/secondary.
- **Touch Targets:** IconButton with Pause/Play/Refresh, no custom touch target size specified, uses default Material 48dp min.
- **Font Sizes:** Material typography labelMedium, labelSmall.

## Runtime Verification Verdict

- **New renderer is actually being used** via verified call graph, not just documented.
- **No legacy rendering paths still active** in main UI after RC20.4 removal — legacy files removed, fallbacks point to commercial generic.
- **Every commercial system is genuinely executed**: CommercialMotionLibrary, Timeline, Hybrid Solver, FK, Camera, Equipment, Body Renderer, Muscle Overlay, Canvas — all present in call chain.
- **Cannot launch application in sandbox** to visually confirm, but code evidence strongly indicates commercial engine drives UI.
- **To fully verify runtime, must launch on physical Android device/emulator and visually inspect 25 representative exercises.**

