# RC20.4 — Developer Guide — How to Extend Commercial Engine

## Overview
Preserve offline-first, Compose-only, no external libs, existing public APIs unless fully justified.

## How to Add New Exercise with Accurate Biomechanics Reusing Primitives

### Step 1: Identify Movement Pattern
- Push, pull, legs, hinge, shoulders, arms, core, Olympic?
- Primary joint action? e.g., Incline Dumbbell Fly = shoulder horizontal abduction + elbow slight flex + scap retraction.

### Step 2: Define Realistic Joint Angles Using BiomechanicalJointModel

Use `BiomechanicalJointModel` limits to stay within realistic:
- Pelvis tilt -20..20, shoulder flex -60..180, abduct -10..150, elbow -5..145, hip -30..120, knee 0..140, ankle -50..20

Example Incline Dumbbell Fly:
```kotlin
fun inclineDumbbellFlyTimeline(): SkeletalTimeline {
    val bottom = pose(PoseMarker.BOTTOM, LEFT_SHOULDER to -70f, LEFT_ELBOW to 15f, RIGHT_SHOULDER to -70f, RIGHT_ELBOW to 15f, CHEST to -8f, rootY=0.55f)
    val top = pose(PoseMarker.TOP, LEFT_SHOULDER to -20f, LEFT_ELBOW to 15f, RIGHT_SHOULDER to -20f, RIGHT_ELBOW to 15f, rootY=0.52f)
    return SkeletalTimeline(2.8f, listOf(Keyframe(0f, top), Keyframe(1.4f, bottom), Keyframe(2.8f, top)))
}
```
Reuse `pose()` helper, `Keyframe`, `SkeletalPose`.

### Step 3: Choose Bar Path Type

- Bench, overhead press → VERTICAL
- Squat, front squat → S_CURVE
- Deadlift → CLOSE_VERTICAL
- Curl, triceps → ARC_ELBOW
- Lateral raise, rear delt → ARC_SHOULDER
- Cable → CABLE_CONSTRAINED
- Leg press → HORIZONTAL
- Plank → FIXED

Bar path automatically handled in `ExerciseAnimationView` via `BarPathEngine.calculateBarPosition`.

### Step 4: Add Stabilisation Cues

Ensure neutral spine, scap retracted, hip stable, foot pressure:
- For bench: scap retracted -5 chest, elbows 45-60 deg, 5-point contact
- For squat: hips back -100, knees forward 125, ankle 18 dorsiflexion, chest 12 incline, COM over mid-foot
- Evaluated automatically in `StabilisationEngine.evaluate()`.

### Step 5: Register in CommercialMotionLibrary

Add function then mapping in `getTimelineForExerciseName`:

```kotlin
lower.contains("incline") && lower.contains("fly") -> inclineDumbbellFlyTimeline()
```

And in `getTimelineForFamily` if new family.

The `applyCommercialPolish` will automatically add pause at lockout/stretch 0.15s, variable tempo eccentric 1.15x slower concentric 0.85x faster, smooth EASE_IN_OUT_CUBIC.

### Step 6: Add Equipment Handling if New

If new equipment, create object in `EquipmentRenderers.kt` implementing `EquipmentRenderer`:

```kotlin
object MyNewMachineRenderer : EquipmentRenderer {
    private val pathPool = Path()
    override fun draw(...) = with(drawScope) {
        pathPool.reset()
        pathPool.moveTo(...)
        drawPath(pathPool, ...)
    }
    override fun validateAttachment(...) = ValidationResult(...)
}
```

Register in `EquipmentEngine.resolvePrimary` via EquipmentType or name contains, and in `getAllRenderers()` list, and define layer BEHIND/SUPPORT/FRONT.

### Step 7: Add Camera Best-View

In `CameraSystem.selectBestView`, add case:

```kotlin
family == "CABLE_FLY" -> CameraView.FRONT
```

For side view to prevent overlap, culling automatically handled in `HumanBodyRenderer`.

### Step 8: Add Muscle Activation

In `MuscleActivationEngine.calculatePrimaryFactor`, add case for new family:

```kotlin
"CABLE_FLY" -> (0.35f + 0.65f * t) // chest high at contracted t=1
```

Secondary factor = primary *0.65.

### Step 9: Validate

```kotlin
val specs = listOf(ExerciseVisualSpec(...))
val skeletons = specs.map { ForwardKinematicsSolver.solve(...) }
val report = ExerciseMotionValidator.validate(specs, skeletons, toScreen)
assert(report.passRate > 85f)
assert(report.jointLimitFails.isEmpty())
```

### Step 10: Performance

- Cache topBar/bottomBar via remember(timeline) already done.
- Pre-bake timeline via bakeTimeline at 60fps remember(timeline) already done.
- Path pooling via Path.reset() reuse already done.
- Isolate Canvas via separate composable already done.

No extra work needed for performance if you reuse existing patterns.

## How to Add New Body Type

- Add constants in `Anthropometry.kt`: e.g., `FEMALE_SHOULDER_BREADTH = 0.23f`, `FEMALE_PELVIS_BREADTH = 0.21f`
- Create enum `BodyType { DEFAULT, ATHLETIC, FEMALE }`
- In `HumanBodyRenderer.BodyPalette.fromMaterial`, accept BodyType and adjust skin, thickness factors:
```kotlin
val thighStart = Anthropometry.thighThicknessStart(ref) * when(bodyType) { ATHLETIC -> 1.2f else -> 1f }
```
- In `drawHumanBody`, compute chestBottomWidth factor based on body type.
- Add validation in `ExerciseMotionValidator` for new type proportions within ANSUR.

## How to Add New Equipment

Steps in previous section.

## Package Structure

- `biomechanics`: joint model, COM, IK, hybrid, bar path, stabilisation, motion library, validator
- `body`: anthropometry, volumetric renderer, human renderer
- `equipment`: renderer interface + 22 renderers + engine
- `camera`: camera system
- `layered`: pipeline
- `orientation`: body orientation
- `anatomy`: muscle map, renderer, activation engine
- `animation`: FK, pose, interpolator, timeline, bone, joint, skeletal renderer commercial
- `resolver`, `registry`, `spec`, `validation`

## API Cleanliness

- Public APIs: `CommercialMotionLibrary.getTimelineForFamily`, `getTimelineForExerciseName`, `benchPressTimeline` etc.
- `EquipmentEngine.resolvePrimary`, `resolveSupport`, `getAllRenderers`
- `HumanBodyRenderer.drawHumanBody` with cameraView default FRONT for backward compat
- `SkeletalRenderer.drawCommercial` with progress and cameraView defaults.

Preserve backwards compatibility where required: defaults for progress and cameraView.

## Offline-First

- No APIs, no AI services, no motion capture libraries, no video, no GIF, no MP4, no Unity, no OpenGL.
- Only Kotlin, Compose, Canvas, local assets.
- All calculations offline.

