# RC20.2 — Commercial Body Rendering & Equipment Engine — Rendering Audit

**Fresh Clone:** `Frogman1978/rep` @ `rc19-product-completion` 400c78f
**Date:** 2026-07-13
**Scope:** All rendering components, no prior RC knowledge

## Component Inventory (Every Rendering File)

### 1. Exercise Rendering Facade
**File:** `ui/exercise/ExerciseAnimationView.kt`
- Purpose: Top-level composable choosing between SkeletalEngine and LegacyStickFigure, animation playback controls, Canvas 180dp
- Dependencies: `VisualEngineAdapter`, `ForwardKinematicsSolver`, `SkeletalRenderer`, `ExerciseAnimation` legacy, Compose material icons, withFrameNanos loop
- Weaknesses: Recomposition per frame via `mutableFloatStateOf` elapsedSeconds triggering Column recompose; Canvas lambda captures State causing extra work; hard-coded 180dp; no lifecycle pause/resume; legacy interpolation still present but fallback rarely used
- Performance: withFrameNanos loop with deltaSeconds no clamp, unbounded Float growth, per-frame allocations in FK and interpolator
- Tech debt: LegacyExerciseAnimationView inside same file duplicating logic; lerp functions duplicate across files
- Retain? Yes facade pattern good, but rewrite internals to use commercial pipeline and isolate Canvas recomposition
- Verdict: **Rewrite internals, retain facade**

### 2. Stick Figure Rendering (Legacy)
**File:** `domain/library/ExerciseAnimation.kt` + legacy part of `ExerciseAnimationView.kt` drawFigure()
- Purpose: Generate AnimationClip <=8 keyframes normalized Points from ExercisePattern family, interpolate linearly
- Dependencies: ExercisePattern, Point, Pose
- Weaknesses: Bone length varies (rubber banding) mathematically proven, only 7 joints single arm (no L/R), implement as single Point?, all families share same axis, no volume
- Perf: Tiny allocation <5KB per clip but per-frame Point lerp creates objects
- Debt: Should have been removed after FK engine introduced
- Verdict: **Remove after RC20.2 verification** — keep only until commercial renderer stable

### 3. Skeleton Rendering (Old)
**File:** `domain/visual/animation/SkeletalRenderer.kt` (pre-RC20.2)
- Purpose: Draw bones as thick lines, joints as circles, equipment as horizontal line at wrist average
- Dependencies: BoneCatalog, EquipmentAnchoring, EquipmentType, DrawScope
- Weaknesses: Pipe-like, no volume, head dot 0.045*width too small, left/right overlap same plane, equipment floating, no bench, no orientation, bar length fixed 0.18*width regardless of grip
- Perf: Claims zero allocation but for loops allocate Offset per iteration via toScreen lambda, still per-frame but cheap
- Debt: renderThickness field in Bone mixes rendering with domain
- Verdict: **Rewrite completely** — replaced in RC20.2 with HumanBodyRenderer + EquipmentEngine + LayeredPipeline

### 4. Body Rendering / Muscle Rendering
**Files:** `domain/visual/anatomy/*` — FrontBody.kt, BackBody.kt, VectorBody.kt, MuscleRenderer.kt, MuscleMap.kt, MuscleRegion.kt, AnatomyPalette.kt, AnatomicalPreviews.kt, MuscleBodyDiagram.kt (ui)
- Purpose: Vector body silhouette 500x1000 grid, 27 muscle regions, primary solid red, secondary dashed orange, palette dark/light
- Dependencies: AnatomyGeometry buildPath, Compose Canvas, AnatomySpec
- Weaknesses: Blocky rectangular muscles not anatomical fan shapes, no fiber direction, no side view, no clipping to silhouette, shoulder maps to anterior+lateral causing over-highlight, stabiliser colors unused (blue), secondary alpha 0.28 hard to see, aspect ratio stretch scaleX != scaleY if canvas not 0.5 aspect
- Perf: Good — paths prebuilt once, scale transform withTransform, cached stroke styles zero allocation
- Debt: Hand-coded Paths hard to maintain, no SVG import pipeline, no animation of muscle contraction
- Retain: MuscleRenderer architecture, palette, BodySide enum, VectorBody singleton pattern
- Rewrite: FrontBody/BackBody geometry to professional illustration
- Verdict: **Retain renderer, rewrite geometry content** — separate concern

### 5. Equipment Rendering (Old)
**Files:** `domain/visual/animation/EquipmentAnchoring.kt`, part of `SkeletalRenderer.kt`, `domain/visual/spec/EquipmentSpec.kt`
- Purpose: Solve anchor as average wrists, draw horizontal line bar
- Dependencies: SolvedSkeleton, JointId, EquipmentType
- Weaknesses: Single generic placeholder for all equipment types, no differentiation barbell/dumbbell/cable/machine, floating, no plates, no pulley, no bench, pull-up bar moves with wrists inverted world
- Perf: Trivial, but computes hypot each frame
- Debt: EquipmentSpec has benchAngle, attachments map but unused
- Verdict: **Remove generic, replace with 22 independent renderers** — RC20.2 done

### 6. Exercise Detail Dialogs
**Files:** `ui/exercise/ExerciseLibraryScreen.kt`, `ExerciseCoachingSections.kt`, `ExerciseViewModel.kt`
- Purpose: List exercises, detail screen shows animation view + muscle diagram + coaching text
- Dependencies: Navigation, ViewModel, Exercise model
- Weaknesses: Detail screen always shows same 180dp animation regardless of orientation, no layer toggle, no equipment explanation
- Perf: OK
- Debt: None major
- Verdict: **Retain**, update detail to use new layered pipeline height 220dp

### 7. Canvas Drawing Utilities
**Files:** `domain/visual/anatomy/AnatomyGeometry.kt`, `domain/library/ExerciseAnimation.kt` Point, `ui/components/CommonComponents.kt`
- Purpose: Helpers for Path building, common UI
- Dependencies: Path
- Weaknesses: AnatomyGeometry.buildPath just wraps Path().apply { block(); close() } trivial
- Perf: No issue
- Verdict: **Retain**

### 8. Animation Playback
**Files:** `domain/visual/animation/PoseInterpolator.kt`, `SkeletalTimeline.kt`, `Pose.kt`, `KinematicMovementFamilies.kt`
- Purpose: Interpolates joint rotations using easing, timeline evaluation with LOOP/PING_PONG/ONCE, repository of hard-coded timelines per family
- Dependencies: JointId, Offset
- Weaknesses: PoseInterpolator omits joints where both angles 0 causing snap to 0, interpolation only linear angle lerp not shortest path, timelines arbitrary magic numbers (-80,-40 etc) same axis for all families, rootPositionOffset hack breaks foot planting, ping-pong reversal velocity discontinuity
- Perf: MutableMap allocation per interpolation, per frame
- Debt: KinematicMovementFamilies 260 lines hard-coded angles not data-driven, gripWidthFactor param unused
- Retain: Timeline interface, PlaybackMode, EasingCurve
- Rewrite: Interpolator to preserve all joints, timelines data-driven from kinesiology research (RC20.3)
- Verdict: **Rewrite interpolator & timelines, retain interface**

### 9. Visual Adapters
**File:** `ui/exercise/adapter/VisualEngineAdapter.kt`
- Purpose: Facade bridging UI to new engines, safe fallback, logs, AnatomyValidationLogger
- Dependencies: ExerciseVisualResolver, KinematicMovementFamilies, ExerciseAnimation, Log
- Weaknesses: Always returns SkeletalEngine even if GENERIC_UNMAPPED, fallback to legacy only on exception not on poor mapping, swallows exceptions logging generic message hiding root cause
- Perf: OK, remember caches in UI
- Debt: Should have metrics for fallback rate
- Verdict: **Retain & improve** — adapter pattern good for offline safety

### 10. Rendering Utilities & Layer Ordering
**Files:** `domain/visual/spec/*`, `domain/visual/registry/*`, `domain/visual/resolver/*`
- Purpose: Resolve exercise to visual spec, metadata registry
- Dependencies: Exercise model, spec enums
- Weaknesses: No layer ordering concept previously, equipment and support mixed, color management via MaterialTheme primary/tertiary not semantic body palette, no layered pipeline
- Perf: parseCsv per resolve allocates sets
- Debt: Large when chains fragile
- Verdict: **Retain and extend** — added layered pipeline enumeration in RC20.2

### 11. Color Management
**Files:** `ui/theme/Color.kt`, `Theme.kt`, `domain/visual/anatomy/AnatomyPalette.kt`
- Purpose: Material theme, anatomy palette dark/light with bodyFill, primaryFill red 500, secondary orange, stabiliser blue, outline
- Dependencies: Color
- Weaknesses: BoneColor = primary (purple) not skin, jointColor = onSurface not outline semantic, muscle palette red/orange similar hue problematic for colorblind
- Perf: No issue
- Verdict: **Retain theme, extend with BodyPalette** — introduced HumanBodyRenderer.BodyPalette.fromMaterial

## Summary Table

| Component | Purpose | Weakness | Debt | Action |
|-----------|---------|----------|------|--------|
| ExerciseAnimationView | Facade playback | Recompose per frame | Legacy fallback | Rewrite internals |
| Legacy Animation | Cartesian Points | Rubber banding | Duplicate | Remove |
| SkeletalRenderer old | Line bones | Pipe-like | Thickness in Bone | Rewrite commercial |
| Muscle anatomy | Vector paths | Blocky | Hand-coded | Retain renderer, rewrite geometry |
| Equipment old | Avg wrists line | Floating generic | Unused params | Replace with 22 renderers |
| Detail dialogs | List/detail | Fixed height | None | Retain |
| Canvas utils | Path helper | Trivial | None | Retain |
| Playback | Interpolator + Timeline | Magic angles, snap | Hard-coded families | Rewrite timelines later, fix interpolator |
| Adapters | Safe fallback | Swallows errors | No metrics | Retain improved |
| Layer ordering | None previously | No pipeline | None | New layered pipeline |
| Color management | Theme | Not semantic body | Colorblind | Extend BodyPalette |

## Critical Gaps for Commercial Quality

- No volumetric body, pipe lines
- No independent equipment renderers
- No attachment validation
- No orientation (supine/prone etc)
- No layered pipeline
- Head too small 4.5% vs 13%
- Arm span < height
- Foot tiny
- Equipment floats

All gaps addressed by RC20.2 implementation below.

---

# RC20.2 Implementation Summary

Implemented:

- **Commercial Body Renderer** `HumanBodyRenderer.kt` + `VolumetricRenderer.kt` + `Anthropometry.kt`
- **Equipment Engine** 22 renderers in `EquipmentRenderers.kt` + `EquipmentEngine.kt` registry
- **Attachment System** `EquipmentAttachmentSolver.kt`
- **Orientation System** `BodyOrientationEngine.kt`
- **Layered Pipeline** `LayeredRenderingPipeline.kt`
- **Validation Suite** `RenderingValidationSuite.kt`

Offline, Kotlin + Compose Canvas only, no external libs.
