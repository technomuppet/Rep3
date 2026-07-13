# RC17.4 — Stage 3 Audit: Legacy Animation System (`RC17_STAGE3_AUDIT.md`)

**Date:** July 4, 2026  
**Status:** Phase 1 Complete (`RC17.4`)  
**Scope:** Exhaustive audit of legacy animation components (`ExerciseAnimation.kt` and `ExerciseAnimationView.kt`) prior to implementing the new rotational Forward Kinematics skeletal engine.

---

## 1. Discovered Legacy Components

### 1.1 `app/src/main/java/com/replog/domain/library/ExerciseAnimation.kt`
* **Core Types:** `Pose`, `Point`, `AnimationClip`, `ExerciseAnimation` object.
* **Purpose:** Generates a lightweight stick-figure keyframe clip ($\le 8$ frames, frame duration in milliseconds) for an exercise by evaluating coarse `ExercisePattern` enums.
* **Data Model:**
  ```kotlin
  data class Point(val x: Float, val y: Float)
  data class Pose(
      val head: Point, val shoulder: Point, val elbow: Point, val hand: Point,
      val hip: Point, val knee: Point, val foot: Point, val implement: Point?
  )
  data class AnimationClip(val keyframes: List<Pose>, val cycleMillis: Int, val mirrored: Boolean = false)
  ```
* **Dependencies:** `Exercise`, `ExercisePattern`.

### 1.2 `app/src/main/java/com/replog/ui/exercise/ExerciseAnimationView.kt`
* **Core Functions:** `ExerciseAnimationView(exercise, modifier)`, `interpolate(clip, t)`, `drawFigure(...)`.
* **Purpose:** Jetpack Compose UI component driving an animation loop (`withFrameNanos`), linearly interpolating coordinates between keyframe `Pose` instances, and drawing single-line 2D stick limbs onto a Compose `Canvas`.
* **Dependencies:** Compose UI (`Canvas`, `DrawScope`, `withFrameNanos`), Material 3 controls (`PlayArrow`, `Pause`, `Refresh`).

---

## 2. Technical & Biomechanical Limitations

1. **Linear Cartesian Interpolation (`lerp(Point, Point, t)`):**
   * Legacy `interpolate()` performs independent linear interpolation on `(x, y)` coordinates.
   * When a limb rotates around a joint (e.g. forearm rotating around elbow from $0^\circ$ to $90^\circ$), linear coordinate interpolation traces a straight line across the arc rather than a circle. As a result, the distance between shoulder and hand or hip and foot shrinks mid-frame, causing unnatural bone stretching and rubber-banding.
2. **impoverished 7-Point Single-Limb Structure:**
   * The skeleton consists of only 7 points (`head`, `shoulder`, `elbow`, `hand`, `hip`, `knee`, `foot`).
   * No anatomical separation exists between left and right limbs. Both arms and both legs collapse onto a single 2D plane.
   * Lacks spine segmentation, chest/pelvis articulation, neck joints, and ankle joints.
3. **Hard-Coded Coarse Movements:**
   * Only 10 coarse patterns exist (`squatClip`, `hingeClip`, `pressClip`, `pullClip`, `raiseClip`, `calfClip`, `coreClip`, `carryClip`, `conditioningClip`). Hundreds of diverse exercises collapse into these 9 primitive clips.
4. **Primitive Implement Drawing:**
   * Implements are represented by a single `Point?`. `drawFigure()` renders them as a generic horizontal line segment, failing to distinguish barbells from dumbbells, cables, dip bars, or pull-up stations.

---

## 3. Reusability vs. Deprecation Matrix

| Component | Status | Strategy & Rationale |
| :--- | :--- | :--- |
| `Point(x, y)` | **Deprecated** | Replaced by standard `androidx.compose.ui.geometry.Offset` and rotational angles. |
| `Pose` (Cartesian) | **Deprecated** | Replaced by immutable `SkeletalPose` storing joint rotation angle maps ($\theta_{joint}$). |
| `AnimationClip` | **Deprecated** | Replaced by `SkeletalTimeline` supporting frame-rate independent easing arcs. |
| `ExerciseAnimation.clip(...)` | **Deprecated** | Replaced by parametric families inside `MovementRegistry`. |
| `interpolate(...)` | **Deprecated** | Replaced by angular `PoseInterpolator` evaluating forward kinematics per frame. |
| `drawFigure(...)` | **Deprecated** | Replaced by layered kinematic renderer (`SkeletalRenderer`). |
| Play/Pause/Scrub Loop | **Reused Concept** | The `withFrameNanos` timing loop and play/pause state machine will be adapted for the new UI wrapper in Stage 5. |

---

## 4. Temporary Compatibility Strategy

Per project constraints:
1. **Zero Legacy Modifications:** `ExerciseAnimation.kt` and `ExerciseAnimationView.kt` remain completely untouched in their current package (`com.replog.domain.library` and `com.replog.ui.exercise`).
2. **Isolated Subsystem Development:** The new engine is built inside `com.replog.domain.visual.animation` alongside the domain foundation (`RC17.2`) and anatomical renderer (`RC17.3`).
3. **Compile-Safe Coexistence:** Existing UI screens (`ExerciseLibraryScreen`, `ExerciseDetailDialog`) continue calling `ExerciseAnimationView(ex)` without breaking. Once the engine is verified offline, UI integration will occur seamlessly in the final RC17 phase.
