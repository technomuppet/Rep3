# RC17.1 — Exercise Visualisation Architecture Specification

**Date:** July 4, 2026  
**Status:** Phase 3 Architecture Design & Specification (`RC17.1`)  
**Scope:** Complete architectural blueprint for the production-quality, offline Exercise Visualisation Engine replacing legacy stick figures and primitive rectangular muscle diagrams in Replog.

---

## 1. Executive Summary & Architectural Goals

The legacy exercise visualisation system audited in `RC17.0` relies on ad-hoc 2D single-limb Cartesian coordinate interpolation (`ExerciseAnimationView`) and crude rectangular bounding boxes (`MuscleBodyDiagram`). To support hundreds or thousands of exercises offline with zero media dependencies (no APIs, GIF downloads, video files, or Lottie assets), Replog requires a data-driven, parametric visual engine built entirely on pure Kotlin and Jetpack Compose Canvas.

### Core Architectural Pillars
1. **Biomechanical Forward Kinematics (FK):** Replace linear `(x, y)` coordinate interpolation with hierarchical skeletal animation. Joints rotate around fixed bone segments, ensuring invariant limb lengths and eliminating "rubber-banding" bone deformation.
2. **Strict Separation of Concerns:** Decouple the system into three independent, cohesive subsystems:
   * `animation/`: Skeletal kinematics, movement families, interpolation, and rendering.
   * `anatomy/`: Precise anatomical vector paths (`FrontBody`, `BackBody`) and muscle mapping.
   * `equipment/`: Modular parametric equipment renderers synchronized via spatial anchor points.
3. **Parametric Movement Families:** Instead of authoring unique animations per exercise, exercises reference highly reusable `MovementFamily` templates parameterized by metadata (stance width, grip spacing, bench angle, body orientation, arm elevation).
4. **Data-Driven Scalability:** Future exercises require zero code changes. Adding a new exercise entry with valid metadata automatically resolves into a complete, biomechanically accurate demonstration and anatomical diagram.
5. **Zero Downtime Migration:** The system is designed for multi-stage, compile-safe incremental integration, maintaining 100% UI stability across existing screens.

---

## 2. Module Dependency Diagram

The architecture establishes strict unidirectional data flow. Subsystems do not depend on external UI components or each other in circular loops. The central orchestrator (`ExerciseVisualResolver`) compiles domain metadata into an immutable visual specification consumed by independent renderers.

```text
       +-------------------------------------------------------------+
       |                  Domain Layer / Database                    |
       |                 (Exercise Data Model / Room)                |
       +------------------------------+------------------------------+
                                      |
                                      v
       +-------------------------------------------------------------+
       |                   ExerciseVisualResolver                    |
       |     Compiles Exercise Metadata into ExerciseVisualSpec       |
       +-------+----------------------+----------------------+-------+
               |                      |                      |
               | (Anatomy Spec)       | (Kinematic Spec)     | (Equipment Spec)
               v                      v                      v
       +---------------+      +---------------+      +---------------+
       |   anatomy/    |      |  animation/   |      |  equipment/   |
       |  MuscleMap    |      |MovementRegistry|     | BenchRenderer |
       | Front/BackBody|      | PoseInterpolator|    |BarbellRenderer|
       |MuscleRenderer |      | Skeleton/Bone |      | CableRenderer |
       +---------------+      +-------+-------+      +-------+-------+
               |                      |                      |
               +----------------------+----------------------+
                                      |
                                      v
       +-------------------------------------------------------------+
       |             Presentation Layer / Compose Canvas             |
       |       (AnimationRenderer / ExerciseAnimationView 2.0)       |
       +-------------------------------------------------------------+
```

---

## 3. Package & Module Structure

```text
com.replog.domain.visual/
├── spec/
│   ├── ExerciseVisualSpec.kt       # Immutable compiled visual specification
│   ├── MovementFamilySpec.kt       # Parameterized family reference
│   ├── EquipmentSpec.kt            # Equipment type, dimensions, angle, attachments
│   └── AnatomySpec.kt              # Primary/secondary activated muscle sets
├── resolver/
│   └── ExerciseVisualResolver.kt   # Pure translator: Exercise -> ExerciseVisualSpec
├── animation/
│   ├── Joint.kt                    # Anatomical joint definitions & identifiers
│   ├── Bone.kt                     # Rigid segment definitions (parent -> child, length)
│   ├── Skeleton.kt                 # Hierarchical kinematic tree & FK solver
│   ├── Pose.kt                     # Snapshot of joint angles, root position, attachments
│   ├── PoseInterpolator.kt         # Angular spherical/linear easing interpolator
│   ├── MovementFamily.kt           # Base interface & keyframe arc generator
│   ├── MovementRegistry.kt         # Catalog of standardized movement patterns
│   └── AnimationRenderer.kt        # Layered Canvas draw orchestrator (Skeleton + Implements)
├── anatomy/
│   ├── MuscleRegion.kt             # Bilateral anatomical taxonomy enum
│   ├── MuscleMap.kt                # Resolver mapping muscle strings to specific regions
│   ├── FrontBody.kt                # Anterior vector path geometry (Path definitions)
│   ├── BackBody.kt                 # Posterior vector path geometry (Path definitions)
│   └── MuscleRenderer.kt           # Canvas renderer for layered vector muscle diagrams
└── equipment/
    ├── EquipmentRenderer.kt        # Common interface & spatial transform utilities
    ├── BenchRenderer.kt            # Parametric renderer (Flat, Incline, Decline benches)
    ├── BarbellRenderer.kt          # Olympic barbell, EZ bar, plates, collars
    ├── DumbbellRenderer.kt         # Single & dual dumbbell renderer
    ├── CableRenderer.kt            # Pulley towers, cables, handles, attachments
    └── MachineRenderer.kt          # Chest press, leg press, lat pulldown frame renderers
```

---

## 4. Detailed Component Responsibilities

### 4.1 Subsystem: `animation/` (Skeletal Kinematics & Movement Engine)

#### `Joint.kt`
Defines the 15 anatomical pivot joints necessary for realistic dual-limb biomechanics:
* `ROOT_PELVIS` (Kinematic origin point)
* `SPINE_TORSO`, `NECK`, `HEAD`
* `L_SHOULDER`, `R_SHOULDER`, `L_ELBOW`, `R_ELBOW`, `L_WRIST`, `R_WRIST`
* `L_HIP`, `R_HIP`, `L_KNEE`, `R_KNEE`, `L_ANKLE`, `R_ANKLE`

#### `Bone.kt`
Defines rigid anatomical links connecting a parent joint to a child joint. Stores normalized anatomical resting lengths ($L_n \in [0, 1]$ relative to total height). By strictly enforcing rigid bone lengths during rendering, impossible limb geometry and stretching are mathematically eliminated.

#### `Skeleton.kt`
Implements the Forward Kinematics (FK) solver. Given root coordinates $(X_{root}, Y_{root})$ and relative joint rotation angles ($\theta_{joint}$) for all segments, `Skeleton.solve()` recursively computes world-space Cartesian coordinates for every joint and evaluates spatial attachment anchors (e.g., left/right hand grip points, shoulder contact pads).

#### `Pose.kt`
An immutable state snapshot representing a single frame of motion:
```kotlin
data class Pose(
    val rootPosition: Offset,
    val jointAngles: Map<Joint, Float>,
    val implementAttachment: ImplementAttachment?
)
```

#### `PoseInterpolator.kt`
Replaces linear coordinate interpolation (`lerp(x, y)`). Interpolates between keyframe `Pose` objects by applying rotational angular interpolation (angle lerp / shortest-path slerp) on joint rotation maps combined with non-linear easing functions (`FastOutSlowInEasing`, cubic ease-in/out). This produces organic, non-robotic human motion where limbs arc naturally through space.

#### `MovementFamily.kt` & `MovementRegistry.kt`
`MovementFamily` defines parameterized base animations. For example, `HorizontalPushFamily` defines the base arm press arc (shoulder flexion/extension, elbow flexion/extension). It accepts modifiers:
* `gripWidth`: Modifies shoulder abduction angle and elbow flare.
* `benchAngle`: Modifies torso angle relative to horizontal ($0^\circ$ for flat bench, $30^\circ$-$45^\circ$ for incline bench, $-15^\circ$ for decline bench).
* `bodyOrientation`: Determines whether the skeleton is prone, supine, standing, or seated.

`MovementRegistry` acts as an offline catalog storing parameterized instances for all primary movement archetypes (Horizontal Push, Vertical Push, Horizontal Pull, Vertical Pull, Squat, Hinge, Lunge, Isolation Curls/Extensions, Core Anti-Extension, Core Rotation, Loaded Carries).

#### `AnimationRenderer.kt`
A pure Kotlin canvas drawing orchestrator. It consumes the evaluated world-space kinematic pose and executes Z-ordered draw layers:
1. Support Equipment Background (Bench frame, Machine seat, Rack posts).
2. Posterior Body Limbs (Back leg, Back arm).
3. Torso & Head.
4. Anterior Body Limbs (Front leg, Front arm).
5. Dynamic Implements & Attachments (Barbell shaft/plates, Dumbbells, Cables drawn from pulley anchor to wrist attachment).

---

### 4.2 Subsystem: `anatomy/` (Vector Muscle Mapping)

#### `MuscleRegion.kt`
Replaces the legacy 19-region enum with a comprehensive, bilaterally distinct anatomical taxonomy:
* **Anterior (Front):** `UPPER_CHEST`, `MID_LOWER_CHEST`, `FRONT_DELTS`, `SIDE_DELTS`, `BICEPS_L`, `BICEPS_R`, `FOREARMS_FL`, `FOREARMS_FR`, `UPPER_ABS`, `LOWER_ABS`, `OBLIQUES`, `QUADS_L`, `QUADS_R`, `ADDUCTORS`, `ABDUCTORS`.
* **Posterior (Back):** `UPPER_TRAPS`, `MID_TRAPS`, `LATS_L`, `LATS_R`, `REAR_DELTS`, `TRICEPS_L`, `TRICEPS_R`, `ERECTORS_LOWER_BACK`, `GLUTES_L`, `GLUTES_R`, `HAMSTRINGS_L`, `HAMSTRINGS_R`, `CALVES_L`, `CALVES_R`.

#### `MuscleMap.kt`
An offline rule-based parser that maps standardized exercise muscle metadata (`primaryMuscles`, `secondaryMuscles`, `muscles`) into exact sets of `MuscleRegion`. Unlike legacy code, bilateral muscles highlight symmetrically on both left and right vector paths.

#### `FrontBody.kt` & `BackBody.kt`
Pure Kotlin vector geometry definitions exporting immutable `androidx.compose.ui.graphics.Path` objects. Each body side consists of:
* `silhouettePath`: The clean outer anatomical perimeter.
* `regionPaths`: A map of `MuscleRegion -> Path` defining precisely contoured muscle shapes that fit seamlessly inside the silhouette without overlapping or boxy distortion.

#### `MuscleRenderer.kt`
Renders the body diagram onto a Compose Canvas:
1. Draws base silhouette filled with background surface color and outlined.
2. Iterates over activated `primary` regions, drawing vector paths with solid primary accent fills.
3. Iterates over `secondary` regions, drawing vector paths with distinct hatched or outlined alpha fills to ensure accessibility compliance (distinguishable without relying solely on color hue).

---

### 4.3 Subsystem: `equipment/` (Modular Equipment Layer)

#### `EquipmentRenderer.kt`
Defines the base interface:
```kotlin
interface EquipmentRenderer {
    fun draw(drawScope: DrawScope, spec: EquipmentSpec, attachments: Map<String, Offset>)
}
```

#### Specialized Renderers
* **`BenchRenderer`**: Draws bench base, adjustable seat pad, and backrest tilted exactly to `spec.inclineAngle`. Synchronizes seat cushion surface with `Skeleton` root pelvis position.
* **`BarbellRenderer`**: Draws knurled shaft, collars, and weight plates centered along the axis formed by left and right wrist attachment coordinates.
* **`CableRenderer`**: Draws vertical pulley tower, adjustable height pin, dynamic cable path (`Path` connecting pulley origin pin to active handle attachment point), and handle hardware.
* **`MachineRenderer`**: Draws fixed structural frames, guide rods, and weight stacks synchronized to support seat anchors.

---

## 5. Data Flow Specification

The flow below illustrates the deterministic lifecycle from a database `Exercise` model to pixels on screen:

```text
+-----------------------------------------------------------------------------+
| Exercise Entity (ID: 101, Name: "Incline Barbell Bench Press")             |
| Category: "Chest", Equipment: "Barbell", Pattern: "Push - Incline Press"    |
+-----------------------------------------------------------------------------+
                                       |
                                       v
+-----------------------------------------------------------------------------+
| ExerciseVisualResolver.resolve(exercise)                                    |
|  -> Identifies Family: HORIZONTAL_PUSH                                      |
|  -> Derives Parameters: benchAngle = 30.0f, grip = WIDE, implement = BARBELL |
|  -> Compiles AnatomySpec: Primary = [UPPER_CHEST, FRONT_DELTS, TRICEPS_*]   |
+-----------------------------------------------------------------------------+
                                       |
                                       v
+-----------------------------------------------------------------------------+
| Immutable ExerciseVisualSpec                                                |
|  ├── MovementFamilySpec(id = "INCLINE_PRESS", params = {angle: 30, grip: 0.7})|
|  ├── EquipmentSpec(type = BARBELL_BENCH, inclineAngle = 30.0f)              |
|  └── AnatomySpec(primary = {...}, secondary = {...})                        |
+-----------------------------------------------------------------------------+
                  |                                           |
                  v                                           v
+-----------------------------------+   +-------------------------------------+
| Animation Loop (withFrameNanos)   |   | MuscleRenderer.draw(AnatomySpec)    |
|  -> Evaluates Phase t in [0..1]   |   |  -> Draws vector Front/Back bodies  |
|  -> PoseInterpolator.interpolate  |   |  -> Applies primary accent path fill|
|  -> Skeleton.solve(Pose)          |   |  -> Applies secondary outline fill  |
|  -> AnimationRenderer.draw()      |   +-------------------------------------+
+-----------------------------------+
```

---

## 6. Mapping Exercises to Movement Families

To support hundreds or thousands of exercises without authoring unique animations, exercises map to parameterized families via `MovementRegistry`. 

### Specialization Parameter Matrix
When resolving an exercise, `ExerciseVisualResolver` extracts behavioral modifiers:

| Movement Family | Core Archetype | Key Parameter Modifiers | Example Mapped Exercises |
| :--- | :--- | :--- | :--- |
| `HORIZONTAL_PUSH` | Bench Press / Push-up | `benchAngle` ($0^\circ, 30^\circ, -15^\circ$), `implement` (Bar/Dumbbell/Machine), `gripWidth` | Flat Bench Press, Incline Dumbbell Press, Decline Machine Press, Push-ups |
| `VERTICAL_PUSH` | Overhead Press | `seated` (true/false), `implement` (Bar/Dumbbell/Kettlebell), `armPath` | Barbell Overhead Press, Seated Dumbbell Shoulder Press, Arnold Press |
| `HORIZONTAL_PULL` | Row | `torsoAngle` ($45^\circ, 90^\circ, 0^\circ$), `support` (Chest-supported/Free/Cable) | Bent Over Barbell Row, Seated Cable Row, Chest-Supported Dumbbell Row |
| `VERTICAL_PULL` | Pulldown / Pull-up | `gripWidth` (Wide/Close), `gripOrientation` (Pronated/Supinated/Neutral) | Wide-Grip Lat Pulldown, Chin-ups, Neutral-Grip Pull-ups |
| `SQUAT` | Knee Dominant Bend | `implementAnchor` (Back/Front/Goblet), `stanceWidth`, `depth` | Barbell Back Squat, Front Squat, Goblet Squat, Hack Squat Machine |
| `HINGE` | Hip Dominant Bend | `kneeFlexion` (Minimal/Moderate), `implementAnchor` | Deadlift, Romanian Deadlift (RDL), Kettlebell Swing, Good Morning |
| `ISOLATION_ELBOW` | Curl / Extension | `jointTarget` (Elbow Flexion vs Extension), `shoulderElevation` | Barbell Bicep Curl, Preacher Curl, Overhead Triceps Extension, Pushdowns |

---

## 7. Independent Equipment Rendering Architecture

Equipment rendering is strictly separated from skeletal animation to allow modular combinations (e.g., combining a `Barbell` with an `Incline Bench`, or combining a `Cable` with a `Preacher Bench`).

### Spatial Anchor Synchronization
Equipment renderers define static or dynamic attachment anchors:
1. **Support Anchors (Static relative to Canvas origin):** The bench pad surface or machine seat anchor provides the exact `(x, y)` coordinate where `Skeleton.ROOT_PELVIS` and `SPINE_TORSO` rest.
2. **Implement Anchors (Dynamic relative to Skeleton Kinematics):** The left and right wrist joint positions (`L_WRIST`, `R_WRIST`) computed by the forward kinematics solver act as the exact grip attachment points. The implement renderer (`BarbellRenderer`, `DumbbellRenderer`) queries these world coordinates per frame and draws the shaft connecting them, ensuring zero slippage or drift between hands and equipment.

### Canvas Z-Ordering Layer Table
```text
Layer 0 (Background):  Machine frames, Cable towers, Bench support structures.
Layer 1 (Far Limbs):   Left leg, Left arm (when viewed from right 3/4 perspective).
Layer 2 (Core Body):   Pelvis, Spine, Torso, Neck, Head.
Layer 3 (Near Bench):  Bench seat pad front edge / safety rails.
Layer 4 (Near Limbs):  Right leg, Right arm.
Layer 5 (Foreground):  Barbell shaft, Dumbbells, Cable line and handles attached to wrists.
```

---

## 8. Anatomical Muscle Highlighting Generation

The `MuscleRenderer` replaces rectangular bounding boxes with scalable vector geometry (`androidx.compose.ui.graphics.Path`).

```text
+-----------------------------------------------------------------+
|                       MuscleRenderer                            |
|                                                                 |
|  1. Draw Silhouette Base Layer:                                 |
|     drawPath(path = FrontBody.SILHOUETTE, color = SurfaceVar)   |
|                                                                 |
|  2. Draw Primary Activated Muscle Paths:                        |
|     for region in AnatomySpec.primary:                          |
|         drawPath(path = FrontBody.PATHS[region], color = Acc)   |
|                                                                 |
|  3. Draw Secondary Activated Muscle Paths (Accessibility):      |
|     for region in AnatomySpec.secondary:                        |
|         drawPath(path = FrontBody.PATHS[region], style = Stroke)|
|         drawPath(path = FrontBody.PATHS[region], alpha = 0.2f)  |
+-----------------------------------------------------------------+
```

This guarantees 100% anatomical accuracy: biceps highlight symmetrically on both arms, deltoids trace exact shoulder caps, and abdominal segments render distinct anatomical tiers.

---

## 9. Zero-Code Scalability Strategy for Future Exercises

To add a new exercise in future updates (e.g., "Seated Cable Neutral-Grip Row"), developers or database seeders simply add an entry to `exercises.json` or SQLite:

```json
{
  "name": "Seated Cable Neutral-Grip Row",
  "category": "Back",
  "equipment": "Cable",
  "primaryMuscles": ["Lats", "Upper Back"],
  "secondaryMuscles": ["Biceps", "Forearms"],
  "movementPattern": "Pull • Horizontal Row"
}
```

### Automatic Resolution Pipeline
1. At runtime, `ExerciseVisualResolver` parses the exercise entity.
2. It detects `equipment = "Cable"` and pattern `"Horizontal Row"`.
3. It maps the exercise to `MovementFamily.HORIZONTAL_PULL` with parameters `torsoAngle = 90.0f`, `support = SEATED_BENCH`, and `implement = CABLE_HANDLE`.
4. `MuscleMap` maps `"Lats"` to `[LATS_L, LATS_R]` and `"Upper Back"` to `[MID_TRAPS, REAR_DELTS]`.
5. **Result:** A fully animated, anatomically accurate demonstration renders immediately with zero lines of new code or media assets required.

---

## 10. Incremental Migration Strategy

To comply with engineering constraints, the rewrite proceeds in compile-safe, non-breaking stages. The existing UI screens continue functioning at the end of every stage.

### Stage 1: Domain Specification & Resolver Foundation
* Create `com.replog.domain.visual.spec` classes (`ExerciseVisualSpec`, `AnatomySpec`, etc.).
* Implement `ExerciseVisualResolver` and write unit tests verifying correct metadata translation for existing catalog exercises.
* **Compilation & Application State:** Fully compiles; legacy UI remains untouched.

### Stage 2: Anatomical Muscle Rendering Subsystem (`anatomy/`)
* Implement vector definitions (`FrontBody`, `BackBody`, `MuscleRegion`, `MuscleMap`).
* Implement `MuscleRenderer` and a new composable `VectorMuscleDiagram(ex)`.
* In `ExerciseLibraryScreen.kt`, swap out the legacy `MuscleBodyDiagram(ex)` for `VectorMuscleDiagram(ex)`.
* **Compilation & Application State:** Fully compiles; UI immediately gains production-quality anatomical muscle highlighting while retaining legacy stick figure animations.

### Stage 3: Equipment Rendering Subsystem (`equipment/`)
* Implement `EquipmentRenderer` interface and parametric equipment renderers (`BenchRenderer`, `BarbellRenderer`, `CableRenderer`, `MachineRenderer`).
* Write standalone preview tests verifying correct anchor transforms.
* **Compilation & Application State:** Fully compiles; ready for kinematic engine coupling.

### Stage 4: Skeletal Kinematic Animation Engine (`animation/`)
* Implement `Joint`, `Bone`, `Skeleton` (FK solver), `Pose`, `PoseInterpolator`.
* Build `MovementRegistry` populated with base parametric families.
* Implement `AnimationRenderer` combining skeletal drawing with Stage 3 equipment rendering.
* **Compilation & Application State:** Fully compiles; standalone engine tested offline.

### Stage 5: UI Integration & Screen Migration
* Create `SkeletalAnimationView(ex)` wrapper composable consuming `ExerciseVisualResolver`.
* In `ExerciseLibraryScreen.kt` and `ExerciseDetailDialog`, replace `ExerciseAnimationView(ex)` with `SkeletalAnimationView(ex)`.
* **Compilation & Application State:** Fully compiles; application fully transitioned to the new engine.

### Stage 6: Codebase Cleanup & Technical Debt Purge
* Remove deprecated files: `ExerciseAnimation.kt`, old `ExerciseAnimationView.kt`, legacy `MuscleBodyDiagram.kt`, and unused placeholder drawables (`exercise_media_placeholder.xml`).
* Run full verification audit across all exercise categories.
* **Compilation & Application State:** Clean, production-ready codebase with zero legacy technical debt.

---

## 11. Comprehensive Risk Analysis & Mitigation

| Risk / Failure Mode | Root Cause | Impact | Mitigation Strategy |
| :--- | :--- | :--- | :--- |
| **Canvas Frame Rate Drop (< 60 FPS)** | Excessive object allocation (creating `Path`, `Offset`, or `Pose` instances inside `onDraw` frame loop). | UI stutter / jank during animation playback on lower-end Android devices. | Cache immutable vector paths and skeletal segment buffers in `remember` blocks outside the frame loop. Re-use allocated float arrays during FK solving. |
| **Infinite Recomposition Loop** | Improper state reading of animation phase within Compose recomposition scopes. | Excessive CPU usage, battery drain, and thermal throttling. | Isolate phase advancement inside `LaunchedEffect` using `withFrameNanos`. Restrict mutable state reading strictly to `Canvas` draw scopes (`drawBehind` / `DrawScope`). |
| **Unmapped Exercise Fallback Failure** | Custom user exercise or poorly formatted catalogue string fails family matching. | Application crash or blank/empty visualization canvas. | Implement rigorous safe defaults in `ExerciseVisualResolver`. If pattern matching fails, fall back to a generic posture matching the exercise category while logging a non-fatal warning. |
| **Memory / Bundle Footprint Bloat** | Over-complicating vector geometry with thousands of bezier nodes. | Increased APK size and slow vector parsing startup times. | Optimize vector paths to use clean, minimal cubic bezier anchor points. Keep all path definitions compiled directly as efficient Kotlin bytecodes rather than XML inflation. |

---

## 12. Verification Checklist Before Code Integration

Before proceeding to implementation in Phase 4 (`RC17.2`), ensure:
- [x] Architecture fully reviewed against all 11 audit categories.
- [x] Unidirectional data flow verified from `Exercise` to Canvas renderers.
- [x] Mathematical preservation of bone lengths confirmed via FK solver design.
- [x] 6-stage migration plan guarantees compile-safe increments.
