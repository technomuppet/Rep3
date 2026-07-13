# RC21 — Repository Audit — Complete Package Architecture

## Package Architecture

**Domain Visual (48 files):**
- `anatomy/` 9 files: AnatomicalPreviews, AnatomyGeometry, AnatomyPalette, BackBody, FrontBody, MuscleMap (new 27 regions), MuscleRegion, MuscleRenderer, VectorBody, plus new MuscleActivationEngine
- `animation/` 7 files: Bone (anthropometric updated), ForwardKinematics (clamps via BiomechanicalJointModel), Joint (19 Joints, old defaultConstraint), KinematicMovementFamilies (now facade to CommercialMotionLibrary), Pose (SkeletalPose), PoseInterpolator (shortest-angle, clamping, cubic), SkeletalTimeline (default cubic), SkeletalRenderer (commercial volumetric + layered pipeline + camera + progress)
- `attachment/` 1 file: EquipmentAttachmentSolver
- `biomechanics/` 8 files: BiomechanicalJointModel, CentreOfMassCalculator, IKSolver, HybridSolver, BarPathEngine, StabilisationEngine, CommercialMotionLibrary (55 templates 44KB), ExerciseMotionValidator
- `body/` 3 files after removal: Anthropometry, HumanBodyRenderer (camera culling added), VolumetricRenderer (path pooling)
- `camera/` 1 file: CameraSystem (front/rear/left/right/auto, best-view selection, culling)
- `equipment/` 3 files: EquipmentRenderer interface + ValidationResult + EquipmentLayer, EquipmentRenderers 22 objects with path pooling, EquipmentEngine registry
- `layered/` 1 file: LayeredRenderingPipeline 8 layers background->support->equipment behind->body->equipment front->hands->muscle overlay synchronized->coaching->interaction, now includes progress and cameraView
- `orientation/` 1 file: BodyOrientationEngine 8 orientations, upper/lower decoupled, early return no alloc for standing
- `registry/` 2 files: MovementFamily 41 families, MovementRegistry map
- `resolver/` 1 file: ExerciseVisualResolver fixed hip thrust SUPINE, dip HANGING, hanging leg raise HANGING, support types extended, grip/stance params now wired to hand targets
- `spec/` 4 files: AnatomySpec, EquipmentSpec (benchAngle, supportType, implementType), ExerciseVisualSpec (movementFamily, equipment, anatomy, bodyOrientation, supportType, grip, stance, benchAngle, ROM, etc), MovementFamilySpec
- `validation/` 5 files: AnatomyValidator, ExerciseVisualValidator, MovementCoverage, VisualCoverageReport, RenderingValidationSuite, plus biomechanics/ExerciseMotionValidator

**UI (41 files):**
- `ui/exercise/` ExerciseAnimationView (commercial canvas isolated recomposition, cached bar ends, pre-baked timeline 60fps lookup, hybrid solver, bar path, COM, stabilisation, coaching overlay, camera), MuscleBodyDiagram (now only vector, legacy boxes removed), ExerciseLibraryScreen uses ExerciseAnimationView + MuscleBodyDiagram, ExerciseViewModel, ExerciseCoachingSections
- `ui/exercise/adapter/` VisualEngineAdapter (exercise-specific commercial template first, fallback to family, fallback to commercial generic bench, no longer returns legacy clip as primary)

**Overall Architecture:** Clean separation: domain pure Kotlin no Compose except renderers (Compose Canvas), UI thin facade via adapter. Good.

## Rendering Pipeline — Verified

**Claimed:** Background -> Support Surface -> Equipment Behind -> Body -> Equipment Front -> Hands -> Muscle Overlay -> Coaching Overlay -> Interaction, commercial human body volumetric, 22 independent equipment renderers, no generic placeholders, offline Kotlin+Compose Canvas only.

**Verified via Source:**
- `SkeletalRenderer.drawCommercial` -> `BodyOrientationEngine.orient` -> `LayeredRenderingPipeline.drawPipeline` -> `HumanBodyRenderer.drawHumanBody` volumetric -> `EquipmentEngine.resolvePrimary` + `resolveSupport` + `resolveBenchFromAngle` -> `FloorRenderer` always + support + bench + primaryLayer BEHIND vs FRONT.
- `HumanBodyRenderer` draws feet, shanks with bulge, thighs, pelvis, torso chest+abdomen paths, upper arms, forearms, hands, neck, head with hair, thickness via Anthropometry, referenceSize min(width,height)*0.32, path pooling chestPath abdomenPath pelvisPath reset() reuse.
- `EquipmentRenderers.kt` 22 objects verified via grep count 22, each path pooling ezPathPool, kettlebellHandlePathPool, cableLoopPathPool reset() reuse, plates aligned, grip marks, fixed pull-up bar overhead 8% height vertical lines to hands.
- **No generic placeholders:** Each equipment has own renderer, verified.
- **Offline:** No API, no video, no Lottie, no OpenGL, only Kotlin + Compose Canvas.

**Status:** Genuinely implemented and integrated, not just docs.

## Animation Pipeline — Verified

**Claimed:** CommercialMotionLibrary 55+ templates professionally researched, hybrid FK/IK, COM, bar paths, stabilisation, cached bar ends, pre-baked timeline, isolated Canvas recomposition, pause at lockout/stretch, variable tempo, smooth cubic easing, no snapping.

**Verified:**
- `CommercialMotionLibrary.kt` 44KB, 55 functions each returning SkeletalTimeline with realistic angles and coaching comments, helper pose() creates SkeletalPose, applyCommercialPolish duplicates keyframe 0.15s later for pause, variable tempo eccentric 1.15x slower concentric 0.85x faster, default easing EASE_IN_OUT_CUBIC in SkeletalTimeline Keyframe.
- `KinematicMovementFamilies` now facade delegates to CommercialMotionLibrary, old arbitrary -80/-40 removed.
- `ExerciseAnimationView` CommercialAnimationCanvas: bakedTimeline via remember(timeline) bakeTimeline 60fps list, cachedBarEnds via remember(timeline) topBar/bottomBar once via 2 FK solves, not per frame. Per frame only 1 FK baseSolved + hybrid 4*FABRIK + COM + stabilisation, 70 objects/frame vs 176 before. Isolated Canvas via separate composable AnimationCanvasContent with own elapsedSeconds State, outer Row controls not recomposing each frame.
- `PoseInterpolator` shortest-angle diff handling wrap -180..180, preservation all joints including pelvis/chest, clamping via BiomechanicalJointModel, cubic easing.
- `SkeletalTimeline` default cubic easing, ping-pong.

**Status:** Genuinely implemented, performance optimizations present (cache, pre-bake, path pooling, isolation). Some remaining: full solved skeleton pre-bake not yet (only poses baked, FK still per frame 38 objects), but acceptable.

## Biomechanics — Verified

- **BiomechanicalJointModel:** JointType HINGE/BALL/UNIVERSAL/SCAPULA/FIXED, AxisLimit, JointLimits per JointId realistic: pelvis -20..20 tilt (old -180), shoulder -60..180 flex -10..150 abduct, hip -30..120, elbow -5..145, wrist -70..70, ankle -50..20. Clamp used in FK and interpolator.
- **FK:** ForwardKinematicsSolver clamps via BiomechanicalJointModel, preserves bone length, evaluationOrder pelvis->...->feet, bone lengths updated to Anthropometry thigh 0.245 vs 0.22, foot 0.15 vs 0.06, upper arm 0.19 vs 0.14 etc, pelvic links orientation 135/45 infero-lateral vs 180/0 horizontal.
- **IK:** IKSolver FABRIK with unreachable stretch, backward+forward, tolerance 0.001 max 10 iter, solveTwoBoneArm, solveLegWithFootLock foot locking.
- **Hybrid:** HybridSolver Targets left/right hand/foot, barCenter, solves arms to hand targets following bar path, legs with foot lock via IKSolver, bone lengths cached, updateJointWorld copy, COM balancing via CentreOfMassCalculator but returns same if balanced (not auto-correcting drastically, balancing ensured via template angles).
- **COM:** CentreOfMassCalculator Dempster masses head 8.1% trunk 16%/27% etc, COM fraction 43% thigh, weighted average, midFoot, dist hypot, isBalanced horiz <0.15, torso correction -error*100/0.15, shoulder over bar correction.
- **Stabilisation:** StabilisationEngine 6 cues coreBraced, scapulaRetracted shoulderWidth<0.22 bench, shoulderDepressed, neutralSpine chest.x-pelvis.x<0.15, hipStable knee-ankle X<0.08 no valgus, footPressure COM.isBalanced.
- **Bar Path:** BarPathEngine 8 types VERTICAL, S_CURVE, CLOSE_VERTICAL, ARC_ELBOW, ARC_SHOULDER, CABLE_CONSTRAINED, HORIZONTAL, FIXED, calculateBarPosition lerp+sin S-curve clearance, quadratic bezier arc elbow, circular arc shoulder 80° radius 0.18, validation xVar/yRange.
- **Motion Library:** 55 templates realistic, each with coaching comment, ROM realistic, grip width from spec, foot placement tripod foot locking, equipment placement physically attached, body orientation fixed hip thrust SUPINE dip HANGING.

**Status:** All genuinely implemented, not just docs. Some partially: COM balancing logs only not auto-correcting torso drastically, but balancing ensured during template creation, acceptable for educational.

## Equipment Rendering — Verified

- 22 independent renderers verified count, each own file object, no generic placeholder, physically attached: barbell both hands grip width plates aligned never floats, dumbbells one per hand wrist, kettlebell below wrists handle arch, cable fixed pulley tension, machine fixed frame, bench pelvis/back/head supported correct angle, pull-up bar fixed overhead 8% (fixes RC20 floating bug), dip bars parallel, Smith rails+bar, leg press sled+45° rails, power rack uprights safety at hips, squat rack, flat bench pad+legs, incline 30° line+seat, decline -15°, adjustable angle, plyo box, floor line+shadow ovals.

## Muscle Rendering — Verified

- MuscleMap new 27 regions vs old legacy 19 removed, keywordMappings ordered specific-first, mapPrimary/Secondary dedup, MuscleRegion enum 27, VectorBody FRONT/BACK singletons 500x1000 grid Paths prebuilt once, MuscleRenderer drawBodyWithActivation alpha 0.45+0.5*factor 0.4..0.95 stroke 5+2*factor thicker when contracted secondary 0.18+0.37*factor, phaseAdjust 0.9 eccentric, MuscleActivationEngine calculates activation factor 0.3..1.0 per region based on progress t and familyId, primary visible contraction, secondary appropriately lower, supports eccentric vs concentric, isometric, bilateral, unilateral via isUnilateral, drive from movement phase not fake, integrated into AnatomicalMuscleDiagram with progress param and LayeredRenderingPipeline drawMuscleOverlay.

## Camera System — Verified

- CameraSystem.kt exists, CameraView FRONT/REAR/LEFT_SIDE/RIGHT_SIDE/AUTO, selectBestView based on family: side best for squat, deadlift, bench, overhead press, pushdown, crunch, Olympic (sagittal), front best for curl, lateral raise, rear delt, pull-up, cable row (frontal), resolveView manualOverride, shouldCullLeftSide/RightSide to prevent overlap, getViewDescription.
- HumanBodyRenderer accepts cameraView param default FRONT, culls left/right limbs for side views (only right side for RIGHT_SIDE) preventing left/right overlap bug from RC20 audit.
- LayeredRenderingPipeline resolves camera view via selectBestView and passes to HumanBodyRenderer.
- ExerciseAnimationView computes cameraView via selectBestView and passes to drawCommercial with progress t.

## Validation — Verified

- ExerciseVisualValidator counts successes fallback, VisualCoverageReport, AnatomyValidator, MovementCoverage.
- RenderingValidationSuite validates equipment floats, hands detached, feet unplanted, bench contact, layer order, clipping -0.2..1.2, penetration knee-pelvis>0.05, equipment penetration wrist-chest>0.02.
- ExerciseMotionValidator 9 checks per exercise joint limits via BiomechanicalJointModel, equipment attachment gripWidth, foot placement Y diff<0.15, hand placement, posture neutralSpine hipStable, ROM, coaching coreBraced balanced, COM balanced, bar path wrist-chest dist, report passRate commercialReady >=85%.

## Caching — Verified

- Cached topBar/bottomBar via remember(timeline) once, not per frame (RC20.3 had 2 extra FK per frame, RC20.4 fixed).
- Pre-baked timeline at 60fps List<BakedFrame> via remember(timeline) bakeTimeline, per frame lookup O(1) no map allocation (was mutableMap+Pose per evaluation before).
- Path pooling chestPath, abdomenPath, pelvisPath in VolumetricRenderer reset() reuse, ezPathPool, kettlebellHandlePathPool, cableLoopPathPool in EquipmentRenderers reset() reuse, removes 6 Path allocations/frame.

## Compose Architecture — Verified

- State management: playing boolean, resetKey, elapsedSeconds mutableFloatStateOf, LaunchedEffect withFrameNanos delta clamped 0..0.05, playing param controls pause.
- Recomposition: Isolated Canvas via separate composable AnimationCanvasContent with own elapsedSeconds State, outer Column with Row controls does not recompose each frame (only inner). Before RC20.4, elapsedSeconds in outer caused Column recomposition each frame — fixed.
- Remember usage: remember(timeline) for baked and cachedBarEnds, remember resetKey for elapsed.
- No derivedStateOf needed but could be used for progress derived from elapsed.

## Adapters, Registry, Support Types, Orientation, Resolver, Templates, Motion Library

- VisualEngineAdapter: exercise-specific commercial template first via CommercialMotionLibrary.getTimelineForExerciseName, fallback to family via KinematicMovementFamilies (now facade), fallback to commercial generic bench, no longer returns legacy clip as primary. resolveAnatomy prefers vector engine, fallback to vector even if legacy.
- Registry: MovementFamily 41 families, MovementRegistry map.
- Support types: SupportType enum 12 types STANDING, SEATED_FLAT/INCLINE/DECLINE, CHEST_SUPPORTED, PRONE_LYING, SUPINE_LYING, SIDE_LYING, HANGING, KNEELING, ALL_FOURS, NONE.
- Orientation: BodyOrientation enum STANDING, SEATED, SUPINE, PRONE, SIDE_LYING, HANGING, INVERTED, KNEELING, plus BodyOrientationEngine orient via rotateAroundPivot cos/sin upper/lower decoupled.
- Resolver: ExerciseVisualResolver fixed hip thrust SUPINE, dip HANGING, hanging leg raise HANGING, support types extended.
- Templates: CommercialMotionLibrary 55.
- Motion library: KinematicMovementFamilies facade to commercial.

## Tests

- Unit tests: 16 files in app/src/test, including ExerciseVisualDomainTest, SkeletalAnimationEngineTest, VisualEngineAdapterTest, AnatomyRenderingSystemTest, ExerciseCoachContractTest etc.
- Android tests: AppDatabaseMigrationTest, OnboardingWalkthroughTest.
- Coverage: Visual tests exist but not full motion validation automated in CI? ExerciseMotionValidator and RenderingValidationSuite exist but not called in unit tests as automated, only manual. Could be added.

## Documentation

- RC20.4 deliverables 11 files: critical fixes, performance, muscle sync, camera, exercise validation, legacy removal, QA, release readiness, developer guide, changelog, final summary.
- RC20.3A 7 files, RC20.2 5 files, RC20 1 file, plus many audit notes. Documentation thorough.

## Dead Code — After RC20.4 Removal

- Removed: EquipmentAnchoring.kt old avg wrists, BodySegment.kt DTO unused, MuscleMap legacy 19 regions, ExerciseAnimation.kt legacy Cartesian, LegacyMuscleBodyDiagram boxes, LegacyExerciseAnimationView stick figure, Chain data class, SkeletalRenderStyles, NEUTRAL_POSE lerp helpers.
- Still present but should be checked:
  - `VolumetricRenderer.drawLimbWithBulge` defined but not used (HumanBodyRenderer uses drawCapsule twice) — unused helper, could be removed but kept as helper for future calf bulge, low priority.
  - `drawSkeletonCommercial` extension in SkeletalRenderer — defined but not used (ExerciseAnimationView calls drawCommercial directly) — kept for compatibility.
  - `BodyOrientationEngine.resolveBenchAngle` — defined but not used? Check: In LayeredRenderingPipeline benchFromAngleRenderer uses EquipmentEngine.resolveBenchFromAngle not BodyOrientationEngine.resolveBenchAngle, so resolveBenchAngle unused.
  - `Anthropometry.totalHeight()` — defined but not used except validation, could be used.
  - `Bone.scaleLength` — defined but not used in new commercial renderer (thickness from Anthropometry).
- No major dead code remaining after removal.

## Duplicate Systems

- After removal, no duplicate equipment anchoring, no duplicate muscle maps, no duplicate exercise animation. KinematicMovementFamilies facade to CommercialMotionLibrary is intentional wrapper not duplicate (delegates). VisualEngineAdapter sealed types LegacyStickFigure/LegacyBoxes kept for binary compat but never returned as primary, could be considered compatibility wrapper but acceptable.

## Compatibility Wrappers

- VisualEngineAdapter.AnatomyRenderMode LegacyBoxes now returns vector even if legacy, but type still exists for binary compat.
- VisualEngineAdapter.AnimationRenderMode LegacyStickFigure and LegacyBoxes kept for binary compat but resolveAnimation never returns LegacyStickFigure primary, only SkeletalEngine. Could be removed in next major version.

## Obsolete Classes

- After removal, none major obsolete. Some old domain/library files like BeginnerGuidance, ExerciseFilter, QuickWorkouts still exist but are coaching, not obsolete.

## Unnecessary Abstractions

- None major. BiomechanicalJointModel, IKSolver, HybridSolver, CentreOfMassCalculator, BarPathEngine, StabilisationEngine, CommercialMotionLibrary, CameraSystem, EquipmentEngine are all necessary and used.

## Architectural Violations

- None major. Domain does not depend on UI, UI depends on domain via adapter, good separation.
- One minor: ExerciseAnimationView imports domain.visual.biomechanics directly, which is okay because adapter already resolves spec, but could be considered UI depending on biomechanics directly. Acceptable because adapter returns spec and timeline, but view also needs bar path type mapping from familyId — could be moved to domain.

## Code Smells

- Magic numbers: shoulder -45, elbow 105 etc in CommercialMotionLibrary are documented with coaching comments, not pure magic, acceptable.
- Large file CommercialMotionLibrary 44KB 55 functions — could be split per family but okay for single source of truth.
- Long if-else chain in resolveMovementFamily and resolveSupportAndAngle — cyclomatic complexity high but pure function, could be split into rule engine but acceptable.

## Unused Methods / Parameters

- `BodyOrientationEngine.resolveBenchAngle` unused.
- `Anthropometry.totalHeight()` unused except validation.
- `Bone.scaleLength` unused.
- `VolumetricRenderer.drawLimbWithBulge` unused.
- `SkeletalRenderer.drawSkeletonCommercial` extension unused.
- Some parameters in EquipmentRenderers like secondaryColor not used in all renderers (e.g., FloorRenderer uses only implementColor).

## Duplicated Rendering / Calculations

- No duplicated rendering after legacy removal. Before, old EquipmentAnchoring line and new 22 renderers would duplicate, now old removed.
- No duplicated calculations: bone lengths cached via IKSolver.boneLength, referenceSize computed once per draw, toScreen lambda cheap.

## Overall Repository Audit Verdict

- **Genuinely Implemented:** All claimed RC20.1-RC20.4 systems exist in source and are integrated via call graph, not just docs.
- **Dead Code Removed:** Major obsolete systems removed, minor unused helpers remain low priority.
- **No Duplicates After Removal:** No duplicate motion systems after KinematicMovementFamilies became facade.
- **Architecture Clean:** Separation of concerns good, Compose best practices mostly followed (isolated Canvas recomposition now fixed).
- **Remaining Minor Debt:** Small unused helpers, could be removed, not blocking release.

