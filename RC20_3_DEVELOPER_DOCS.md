# RC20.3 — Developer Documentation — Commercial Motion Library & Biomechanics

## Overview
Replaces arbitrary angles with commercial biomechanics: realistic joint limits, hybrid FK/IK, COM balancing, bar paths, stabilisation, 55+ exercise templates. Offline, Kotlin + Canvas only.

## Packages
- `domain/visual/biomechanics` — BiomechanicalJointModel, CentreOfMassCalculator, IKSolver, HybridSolver, BarPathEngine, StabilisationEngine, CommercialMotionLibrary, ExerciseMotionValidator
- Existing `domain/visual/animation` — ForwardKinematics now uses biomechanical limits, PoseInterpolator shortest-angle, Bone lengths anthropometric
- `domain/visual/body` — volumetric renderer retained
- `domain/visual/equipment` — 22 renderers
- `ui/exercise/ExerciseAnimationView` — commercial canvas with hybrid solve

## How New Exercises Can Be Added With Accurate Biomechanics Reusing Primitives

### 1. Identify Movement Pattern
- Is it push, pull, legs, hinge, shoulders, arms, core, Olympic?
- What is primary joint action? e.g., Bench Press = shoulder horizontal adduction + elbow extension + scapular protraction.

### 2. Define Realistic Joint Angles Using Biomechanical Primitives
Use `BiomechanicalJointModel` limits to stay within realistic:

```kotlin
fun myNewExerciseTimeline(): SkeletalTimeline {
    val top = pose(PoseMarker.TOP,
        JointId.LEFT_SHOULDER to -15f, // within -60..180
        JointId.LEFT_ELBOW to 10f, // -5..145
        JointId.CHEST to -5f, // scap retraction
        rootY = 0.52f
    )
    val bottom = pose(PoseMarker.BOTTOM,
        JointId.LEFT_SHOULDER to -45f,
        JointId.LEFT_ELBOW to 105f,
        JointId.CHEST to -5f,
        rootY = 0.55f
    )
    return SkeletalTimeline(2.6f, listOf(Keyframe(0f, top), Keyframe(1.3f, bottom), Keyframe(2.6f, top)))
}
```

Reuse primitives: `pose()` helper, `Keyframe`, `SkeletalPose`.

### 3. Add Bar Path
Choose BarPathType:
- Bench, overhead press → VERTICAL
- Squat → S_CURVE (hips back slight horizontal)
- Deadlift → CLOSE_VERTICAL (bar close shins)
- Curl, triceps → ARC_ELBOW
- Lateral raise, rear delt → ARC_SHOULDER
- Cable → CABLE_CONSTRAINED
- Leg press → HORIZONTAL
- Plank → FIXED

Bar path automatically handled in `ExerciseAnimationView` via `BarPathEngine.calculateBarPosition`.

### 4. Add Stabilisation Cues
- Core bracing: ensure pelvis and chest alignment neutral spine (chest.x - pelvis.x <0.15)
- Scapular retraction: for push, shoulderWidth <0.22
- Shoulder depression: for pull/deadlift, shoulder.y > upperChest.y -0.05
- Hip stable: knee.x - ankle.x <0.08
- Foot pressure: COM over mid-foot via CentreOfMassCalculator.

These are auto evaluated in `StabilisationEngine.evaluate()`.

### 5. Register in CommercialMotionLibrary
Add function then mapping:

```kotlin
fun getTimelineForExerciseName(name: String): SkeletalTimeline {
    val lower = name.lowercase()
    return when {
        lower.contains("my new exercise") -> myNewExerciseTimeline()
        ...
    }
}
```

Also add to `getTimelineForFamily` if new family.

### 6. Add Equipment Handling
If new equipment, create renderer in `EquipmentRenderers.kt` implementing `EquipmentRenderer`, register in `EquipmentEngine.resolvePrimary` and `getAllRenderers()`.

### 7. Validate
Run validation:

```kotlin
val specs = listOf(ExerciseVisualSpec(...))
val skeletons = specs.map { ForwardKinematicsSolver.solve(...) }
val report = ExerciseMotionValidator.validate(specs, skeletons, toScreen)
assert(report.passRate > 85f)
assert(report.jointLimitFails.isEmpty())
```

### 8. Performance
- Cache topBar/bottomBar via remember (currently per frame, should cache).
- Use shortest-angle interpolation already in PoseInterpolator.
- Avoid allocations inside draw loops: reuse Offsets, use primitive Floats.

## Example: Adding "Incline Dumbbell Fly"

- Movement: chest fly, shoulder horizontal abduction -60 bottom -20 top, elbow slight bend 15, scap retracted.

```kotlin
fun inclineDumbbellFlyTimeline(): SkeletalTimeline {
    val bottom = pose(PoseMarker.BOTTOM, LEFT_SHOULDER to -70f, LEFT_ELBOW to 15f, RIGHT_SHOULDER to -70f, RIGHT_ELBOW to 15f, CHEST to -8f, rootY=0.55f)
    val top = pose(PoseMarker.TOP, LEFT_SHOULDER to -20f, LEFT_ELBOW to 15f, RIGHT_SHOULDER to -20f, RIGHT_ELBOW to 15f, rootY=0.52f)
    return SkeletalTimeline(2.8f, listOf(Keyframe(0f, top), Keyframe(1.4f, bottom), Keyframe(2.8f, top)))
}
```

- Bar path: ARC_SHOULDER (hands arc around shoulder).
- Equipment: DumbbellsRenderer (one per hand).
- Add to getTimelineForExerciseName: `lower.contains("incline") && lower.contains("fly") -> inclineDumbbellFlyTimeline()`.

## Architecture for Future Body Types

- Already have `Anthropometry.kt` with male 50th percentile. Add female proportions: shoulder 0.23, pelvis 0.21, etc. Create BodyType enum, factor in HumanBodyRenderer.

## Checklist

- [ ] Angles within BiomechanicalJointModel limits
- [ ] COM balanced (hips back knees forward torso incline for squat etc)
- [ ] Bar path correct type
- [ ] Stabilisation cues: core braced, scap retracted, neutral spine, hip stable, foot pressure
- [ ] Equipment attachment validated (hands attached, feet planted, no float)
- [ ] Validation suite passRate >85%
- [ ] Performance: no per-frame top/bottom FK (cache), no map allocation (pre-bake)

## Remaining Tech Debt

- TopBar/bottomBar computed per frame → cache via remember
- Pre-bake lookup tables not yet implemented → for 60fps solid
- Canvas recomposition per frame → isolate Canvas composable
- Foot direction after supine rotation → adjust
- Old EquipmentAnchoring.kt still present → remove after validation

## Commercial Release Readiness

See final summary.

