# RC17.5 — Stage 5 UI Audit: Exercise Visualisation Integration (`RC17_STAGE5_UI_AUDIT.md`)

**Date:** July 4, 2026  
**Status:** Phase 1 Complete (`RC17.5`)  
**Scope:** Exhaustive audit of all presentation layer components displaying exercise visuals prior to integrating the new rotational Skeletal Animation Engine and Anatomical Muscle Renderer.

---

## 1. Discovered UI Components & Call Sites

### 1.1 `app/src/main/java/com/replog/ui/exercise/ExerciseAnimationView.kt`
* **Purpose:** Composable widget (`ExerciseAnimationView(exercise, modifier)`) displaying an animated demonstration of an exercise on a Compose Canvas.
* **Current Implementation:** Runs a manual `withFrameNanos` timing loop, calls `ExerciseAnimation.clip(exercise)` to get a legacy 7-point stick-figure keyframe clip, interpolates linear Cartesian coordinates (`lerp`), and draws single-line limbs and implements (`drawFigure`). Includes Play/Pause/Restart icon buttons.
* **Dependencies:** `ExerciseAnimation` (legacy generator), `AnimationClip`, `Pose`, Jetpack Compose Material 3 UI.
* **Replacement Strategy:** Replace internal rendering with `SkeletalAnimationAdapter`. The adapter will invoke `ExerciseVisualResolver.resolve(exercise)`, retrieve the appropriate `SkeletalTimeline` from `KinematicMovementFamilies`, interpolate rotational angles via `PoseInterpolator`, solve Forward Kinematics (`ForwardKinematicsSolver`), and render via `SkeletalRenderer`. If resolution falls back or fails, it cleanly delegates to the legacy renderer.
* **Migration Risk:** **Low.** The public function signature `ExerciseAnimationView(exercise: Exercise, modifier: Modifier)` remains unchanged, ensuring zero API regression.

---

### 1.2 `app/src/main/java/com/replog/ui/exercise/MuscleBodyDiagram.kt`
* **Purpose:** Composable widget (`MuscleBodyDiagram(exercise, modifier)`) displaying anterior (Front) and posterior (Back) body diagrams with highlighted primary and secondary muscle groups.
* **Current Implementation:** Calls `MuscleMap.primaryRegions(exercise)` and `secondaryRegions(exercise)` from the legacy enum, draws four crude rounded rectangles (`drawRoundRectN`) representing silhouette torso/limbs, and overlays hard-coded rectangular bounding boxes (`REGION_BOXES`).
* **Dependencies:** Legacy `MuscleMap`, legacy `MuscleRegion`, Jetpack Compose Canvas.
* **Replacement Strategy:** Replace internal rendering with `AnatomicalMuscleAdapter`. The adapter will resolve `AnatomySpec` from `ExerciseVisualResolver.resolve(exercise)` and delegate directly to `AnatomicalMuscleDiagram(anatomySpec)` (`com.replog.domain.visual.anatomy`), which draws precise vector anatomical silhouettes and muscle paths (`FrontBody`, `BackBody`) with dashed secondary strokes and solid primary fills.
* **Migration Risk:** **Low.** Public signature `MuscleBodyDiagram(exercise: Exercise, modifier: Modifier)` remains compatible.

---

### 1.3 `app/src/main/java/com/replog/ui/exercise/ExerciseLibraryScreen.kt` (`ExerciseDetailDialog`)
* **Purpose:** Primary screen where users browse, search, filter, and inspect exercises. Clicking an exercise opens `ExerciseDetailDialog` (`lines 234–340`).
* **Current Implementation:** Inside `ExerciseDetailDialog`, two `RepLogCard` sections display visual feedback:
  1. `"Muscles worked"` card containing `MuscleBodyDiagram(ex)` (`line 288`).
  2. `"Movement"` card containing coaching description and `ExerciseAnimationView(ex)` (`line 299`).
* **Dependencies:** `Exercise`, `ExerciseInsight`, `ExerciseCoaching`, `MuscleBodyDiagram`, `ExerciseAnimationView`.
* **Replacement Strategy:** Since `MuscleBodyDiagram(ex)` and `ExerciseAnimationView(ex)` will internally adapt to the new visual engine via our UI adapters while preserving their public API, `ExerciseDetailDialog` automatically gains production-quality skeletal animation and vector anatomical muscle highlighting without altering layout, navigation, or scrolling behavior.
* **Migration Risk:** **Very Low.** Layout constraints and UI semantics are preserved.

---

### 1.4 Exercise Cards, Previews & Thumbnails Across UI
* **Audit Findings:**
  * `ExerciseItem` in `ExerciseLibraryScreen.kt` (`line 178`) and `WorkoutExerciseCard` in `ActiveWorkoutScreen.kt` (`line 571`) display textual exercise metadata alongside a static vector icon (`ExerciseIcon()` from `CommonComponents.kt`).
  * Standalone preview composables for the new anatomical renderer exist in `app/src/main/java/com/replog/domain/visual/anatomy/AnatomicalPreviews.kt`.
* **Replacement Strategy:** Keep static list icons (`ExerciseIcon`) untouched to maintain fast list scrolling. No thumbnail replacements are required.

---

## 2. Summary Migration Plan

1. **Phase 2 (UI Adapter):** Create `com.replog.ui.exercise.adapter.VisualEngineAdapter.kt` acting as a unified facade bridging `Exercise` entities to the new domain resolvers and renderers.
2. **Phase 3 & 4 (Internal Renderer Replacement):** Update `MuscleBodyDiagram.kt` and `ExerciseAnimationView.kt` to call the UI adapter, keeping legacy code available strictly as a fallback.
3. **Phase 5 (Dialog Verification):** Verify `ExerciseDetailDialog` layout, controls, and scrolling.
4. **Phase 6–10 (Validation, Performance & Clean Up):** Validate across catalog, benchmark 60 FPS performance, classify legacy code, and finalize documentation.
