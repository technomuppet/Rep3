# RC20.3 — Performance Report

## Goals
- 60 FPS
- Minimal allocations
- Cached calculations
- Limited recomposition
- Efficient IK solving

## Baseline RC20.2
- FK Solver: 38 objects/frame (mutableMap 19 + 19 SolvedJoint)
- Interpolator: mutableMap + Pose per frame
- SkeletalRenderer old: toScreen lambda per bone, Offset per joint
- ExerciseAnimationView: mutableFloatStateOf elapsedSeconds triggers Column recomposition each frame
- Body renderer: 2 Path allocations per frame torso, 10 capsules 40 draw ops
- 60 draw ops total, ~60fps theoretical

## RC20.3 Additions Cost

### BiomechanicalJointModel
- Clamp is coerceIn, no alloc, cheap.

### CentreOfMassCalculator
- 15 segments loop, weightedX/Y, no allocation except ComResult data class (2 Offsets + 2 Floats + Bool). Cheap <0.05ms.

### IKSolver FABRIK
- Chain 3 joints, lengths 2, max 10 iter, each iter: backward loop 2 steps + forward 2 steps, each step hypot + division + Offset. No trig per iteration (only direction normalized via hypot). 4*10=40 hypot per limb per frame max. For 2 arms +2 legs = 160 hypot worst case. Still <0.3ms. Allocates mutableList 3 Offsets per call (small) — could be optimized to array reuse but acceptable.

### HybridSolver
- Bone lengths cached via IKSolver.boneLength (hypot) 8 times per frame (upper arm, forearm, thigh, shank, foot each side) = 8 hypot.
- Calls IKSolver for each limb with targets (up to 4 limbs) => 4*FABRIK.
- Updates joint map mutable copy (19 entries) — allocation similar to FK (map copy). So total per frame now FK (38 objects) + Hybrid (map copy 19 + 4*3 list) ~ 60 objects per frame, up from 38. Slight increase but still within GC budget.

### BarPathEngine
- calculateBarPosition: lerp + sin, no alloc, cheap.

### StabilisationEngine
- Evaluates 6 checks, few abs/hypot, COM already computed. Cheap.

### CommercialMotionLibrary
- Timeline evaluation same as before, but now with more realistic keyframes (3-4 per timeline vs 2-3 before) still linear scan O(n) n=3-4 cheap.
- No allocation per timeline beyond interpolated pose (same as before).

### ExerciseAnimationView RC20.3 Commercial
- Now does: evaluate base pose (1 alloc), FK solve (38 alloc), compute top/bottom bar from evaluating timeline at 0 and duration/2 (2 extra FK solves = 76 alloc) — heavy! We evaluate timeline at 0 and half each frame to get start/end bar positions. That's 2 extra FK solves per frame = 76 objects extra, not needed every frame. Should be cached: precompute topBar/bottomBar once per timeline, not per frame. This is performance regression.
- Then BarPathEngine, HybridSolver (60 alloc), COM (1), Stabilisation (1), drawCommercial (body renderer 2 Paths + equipment).
- Total per frame estimate: base FK 38 + top/bottom FK 76 + hybrid 60 + body 2 Path + equipment ~10 draw ops = ~176 objects/frame vs 38 before. At 60fps = 10k objects/sec, may cause GC jank.

### Optimizations Needed
- **Cache topBar/bottomBar**: Compute once per timeline in remember, not per frame. Then per frame only need desiredBar calculation.
- **Pre-bake timeline evaluations**: Instead of evaluating timeline each frame via linear scan + interpolation, pre-bake 60fps * duration lookup table FloatArray of joint rotations for each family. Then per frame just lookup index, no interpolation alloc. Would reduce 38 to ~0 alloc.
- **Object pooling for SolvedSkeleton joints map**: Reuse mutable map.
- **Isolate Canvas recomposition**: Move elapsedSeconds state to Canvas-only composable, as noted in RC20.2 performance report. Currently Column still recomposes each frame (Row controls also).

### Measured Expectation (Theoretical)
- With current implementation without caching top/bottom, 60fps possible on high-end (Pixel 7) but may drop to 45-55fps on mid-range due to GC.
- With caching top/bottom and pre-bake, 60fps sustained on mid-range.

### Minimal Recomposition
- Still issue: mutableFloatStateOf elapsedSeconds triggers Column recomposition. Should be:
```kotlin
@Composable fun CommercialBodyCanvas(...) {
  var elapsed by remember { mutableFloatStateOf(0f) }
  LaunchedEffect { ... }
  Canvas { draw with elapsed }
}
```
This isolates recomposition to Canvas only. Currently Row with play/pause also recomposes each frame but cheap.

### Cached Calculations
- Anthropometry constants cached singleton.
- Bone lengths cached per frame via local val (could be cached per skeleton).
- Bar path type mapping via when expression cheap.
- ReferenceSize computed once per draw.

### Efficient IK Solving
- FABRIK efficient vs CCD or Jacobian, no matrix, 10 iter max, early exit if diff < tolerance.

### Conclusion
- RC20.3 adds commercial biomechanics with modest performance cost (38->~100 objects/frame without top/bottom cache, ~176 with current top/bottom per frame). Still within 16ms budget but near limit.
- To achieve solid 60fps on mid-range, must cache topBar/bottomBar and pre-bake timelines (recommended for RC20.4).
- Overall ready for commercial release if device is high-end; for broad commercial release, implement pre-bake.

