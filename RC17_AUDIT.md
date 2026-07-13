# RC17 — Exercise Visualisation System Complete Codebase Audit

**Date:** July 4, 2026  
**Status:** Phase 1 Complete Audit (`RC17.0`)  
**Scope:** Exhaustive audit of all codebase components responsible for exercise metadata, exercise rendering, detail dialogs, stick figure rendering, animation, canvas drawing, muscle highlighting, exercise lookup, exercise database, media assets, and animation interpolation.

---

## Executive Summary

Replog’s current exercise visualisation system operates under a strict offline constraint (no APIs, no GIF libraries, no MP4 playback, no Lottie, pure Kotlin/Compose Canvas). While the architecture successfully achieves low asset footprint and offline execution, it currently suffers from severe limitations:

1. **Oversimplified Anatomy & Muscle Highlighting:** The body diagram (`MuscleBodyDiagram`) draws primitive rounded rectangles for a generic front/back silhouette and overlays hard-coded 2D bounding boxes (`REGION_BOXES`) that fail to trace true anatomical contours. Many muscles (e.g., biceps, triceps, forearms) only highlight on one side of the body.
2. **2D Single-Limb Stick Figures:** The animation system (`ExerciseAnimation` & `ExerciseAnimationView`) represents exercises using a 7-point stick figure with only one arm and one leg drawn on top of each other.
3. **Linear Cartesian Interpolation:** Interpolation (`lerp` between normalized `(x, y)` coordinates) causes bone deformation and unnatural rubber-banding during movement cycles because joints do not rotate around fixed bone lengths.
4. **Coarse Movement Families & Missing Equipment:** Only 10 coarse animation clips exist for hundreds of exercises. Implements are drawn as generic horizontal lines without distinguishing barbells, dumbbells, cables, or machines. Support objects like benches, pull-up bars, or dip stations are entirely missing.
5. **Orphaned Media Asset References:** Bundled exercise definitions in `exercises.json` reference non-existent external GIF paths (`exercise_media/*.gif`) in `mediaAsset`, pointing to legacy technical debt from before the offline Canvas transition.

---

## Comprehensive File Audit

### 1. Exercise Database & Media Assets

#### `app/src/main/assets/exercises.json`
* **Category:** Exercise database, Media assets
* **Purpose:** The master JSON catalog containing over 200+ bundled exercises with metadata (`name`, `category`, `equipment`, `type`, `muscles`, `primaryMuscles`, `secondaryMuscles`, `movementPattern`, `difficulty`, `mediaAsset`).
* **Dependencies:** None (raw asset file).
* **Interactions:** Parsed by `DataSeeder.kt` using Gson on application startup to populate Room via `ExerciseDao`.
* **Technical Debt:** Every entry contains a `mediaAsset` string pointing to non-existent GIF files (e.g., `"mediaAsset": "exercise_media/barbell_back_squat.gif"`). Furthermore, `movementPattern` strings are formatted irregularly (e.g., `"Legs • Squat"` vs catalogue strings parsed by `ExercisePattern.of`).
* **Recommendation:** **Refactor.** Keep the file as the seeding source, but clean up orphaned/legacy fields or enrich metadata to include explicit fields for the new visualisation engine (e.g., `movementFamily`, `equipmentKind`, `bodyOrientation`, `supportType`).

#### `app/src/main/res/drawable/exercise_media_placeholder.xml`
* **Category:** Media assets
* **Purpose:** Static vector graphic (drawing a barbell silhouette on a dark background) originally created as a fallback placeholder for missing exercise media assets.
* **Dependencies:** Android Vector Drawable format.
* **Interactions:** None. A repository search reveals this file is completely unreferenced by any code in `app/src/main/java/`.
* **Technical Debt:** Dead code left over from legacy image/GIF loading schemes.
* **Recommendation:** **Replace / Delete.** Should be purged or replaced by the new rendering engine if static preview fallbacks are ever needed.

---

### 2. Exercise Metadata & Classification

#### `app/src/main/java/com/replog/data/model/Exercise.kt`
* **Category:** Exercise metadata
* **Purpose:** Core Room `@Entity` representing an exercise in the local SQLite database.
* **Dependencies:** Room annotations (`@Entity`, `@PrimaryKey`, `@Index`).
* **Interactions:** Returned by `ExerciseDao` and `ExerciseRepository`; consumed across all UI screens (`ExerciseLibraryScreen`, `ActiveWorkoutScreen`), coaching engines (`ExerciseCoach`), and visualisations (`ExerciseAnimationView`, `MuscleBodyDiagram`).
* **Technical Debt:** Stores comma-separated strings (`muscles`, `primaryMuscles`, `secondaryMuscles`) instead of structured types or normalized relation tables. Retains the dead `mediaAsset: String` property. Lacks explicit properties required for scalable visual biomechanics (e.g., grip width, bench angle, support type).
* **Recommendation:** **Refactor.** Keep as the core domain/data model, but extend or complement with rich domain metadata structures for visualisation resolution.

#### `app/src/main/java/com/replog/domain/library/ExercisePattern.kt`
* **Category:** Exercise metadata
* **Purpose:** Categorizes exercises into granular movement patterns (`ExercisePattern` enum with 32 patterns) and equipment buckets (`EquipmentKind` enum with 11 buckets) using heuristic string matching on exercise names, categories, and patterns.
* **Dependencies:** `Exercise` data model.
* **Interactions:** Called by `ExerciseAnimation.clip(ex)` to select which stick-figure clip to generate, and by `ExerciseCoach.coach(ex)` to construct personalized coaching steps and cues.
* **Technical Debt:** Heavy reliance on string matching (`p.contains(...)`, `name.contains(...)`) inside `ExercisePattern.of(ex)`. Coupling granular coaching patterns directly to coarse animation clips creates brittle fallback behaviors (`GENERIC -> pressClip`).
* **Recommendation:** **Refactor / Expand.** The concept of movement patterns and equipment buckets is essential for Phase 5 (`Movement Registry` and `Movement Families`), but must be decoupled from string heuristics and expanded into a formal `MovementFamily` and `EquipmentRenderer` classification system.

#### `app/src/main/java/com/replog/domain/library/ExerciseCoach.kt` & `WhyThisExercise.kt`
* **Category:** Exercise metadata
* **Purpose:** `ExerciseCoach` generates structured coaching advice (`CoachingInfo`: description, setup steps, cues, common mistakes, safety rules). `WhyThisExercise` generates dynamic rationale text based on goal, experience, and recovery state.
* **Dependencies:** `Exercise`, `ExercisePattern`, `EquipmentKind`.
* **Interactions:** Consumed by `ExerciseDetailDialog` inside `ExerciseLibraryScreen.kt` to render instructional text alongside visual diagrams.
* **Technical Debt:** Contains ad-hoc string checks (e.g., hardcoded `"high pull"` check inside `ExerciseCoach.coach`).
* **Recommendation:** **Keep / Refactor.** Keep the offline plain-English generation logic, but harmonize it with the metadata schema of the new visualization engine so setup instructions and biomechanical cues mirror visual animations.

---

### 3. Exercise Lookup & Database Seeding

#### `app/src/main/java/com/replog/data/db/ExerciseDao.kt` & `ExerciseRepository.kt`
* **Category:** Exercise lookup, Exercise database
* **Purpose:** `ExerciseDao` defines Room SQL queries (`getAllExercises`, `searchExercises`, `getExerciseById`, `getExerciseByName`); `ExerciseRepository` provides a clean repository abstraction exposing Kotlin Coroutine Flows.
* **Dependencies:** Room (`SupportSQLiteDatabase`), Kotlin Coroutines / Flow.
* **Interactions:** Queried by ViewModels (`ExerciseViewModel`, `ActiveWorkoutViewModel`) and seeded by `DataSeeder`.
* **Technical Debt:** `searchExercises` uses SQL `LIKE` concatenations across multiple CSV string columns (`muscles`, `primaryMuscles`, `movementPattern`).
* **Recommendation:** **Keep.** Production-quality repository and DAO layer; requires minimal changes unless new schema fields are added during migration.

#### `app/src/main/java/com/replog/util/DataSeeder.kt`
* **Category:** Exercise database
* **Purpose:** Checks local database state on app startup, reads `exercises.json` from assets via Gson, and inserts or upgrades bundled exercises.
* **Dependencies:** Gson, Android `Context`, `ExerciseRepository`.
* **Interactions:** Invoked during application startup (`RepLogApplication` / `MainActivity`).
* **Technical Debt:** Performs string manipulations to convert JSON arrays into comma-separated strings (`joinToString(", ")`).
* **Recommendation:** **Keep / Refactor.** Keep the seeding mechanism; update mapping logic when metadata model is enhanced.

#### `app/src/main/java/com/replog/domain/library/ExerciseFilter.kt`
* **Category:** Exercise lookup
* **Purpose:** Houses `ExerciseFilterState` and defines UI filter dimensions (muscle groups, movement patterns, goals, experience levels, equipment presets).
* **Dependencies:** `Exercise`.
* **Interactions:** Used by `ExerciseLibraryScreen` and `ExerciseViewModel` to filter the library list offline.
* **Technical Debt:** Filter dimensions map to static string lists (`MUSCLE_GROUPS`, `PATTERNS`).
* **Recommendation:** **Keep.** Functions well for UI filtering; should align muscle naming with the new anatomical vector engine.

---

### 4. Stick Figure Rendering, Animation & Canvas Drawing

#### `app/src/main/java/com/replog/domain/library/ExerciseAnimation.kt`
* **Category:** Stick figure rendering, Animation, Animation interpolation
* **Purpose:** Lightweight engine generating `AnimationClip`s (containing up to 8 `Pose` keyframes and cycle duration in milliseconds) for 10 coarse movement families (`squatClip`, `hingeClip`, `pressClip`, `pullClip`, `raiseClip`, `calfClip`, `coreClip`, `carryClip`, `conditioningClip`).
* **Dependencies:** `Exercise`, `ExercisePattern`.
* **Interactions:** Called by `ExerciseAnimationView(ex)` to obtain the animation clip to render.
* **Technical Debt:**
  * **Anatomical Poverty:** Defines `Pose` using only 7 normalized `Point` coordinates (`head`, `shoulder`, `elbow`, `hand`, `hip`, `knee`, `foot`) plus an optional `implement` coordinate. No distinct left/right limbs, no neck, wrist, or torso joints.
  * **Biomechanical Flaws:** Poses are manually hard-coded Cartesian coordinates (`Point(x, y)`). Limbs do not obey joint rotation constraints or maintain fixed bone lengths across keyframes.
  * **Coarse Granularity:** Map hundreds of exercises into only 9 hard-coded pose routines.
* **Recommendation:** **Replace.** Fully replace with the Phase 3/4 Skeletal Animation System (`Skeleton`, `Bone`, `Joint`, `Pose`, `MovementRegistry`).

#### `app/src/main/java/com/replog/ui/exercise/ExerciseAnimationView.kt`
* **Category:** Exercise rendering, Stick figure rendering, Animation, Canvas drawing, Animation interpolation
* **Purpose:** Jetpack Compose UI component that drives a frame loop using `withFrameNanos`, interpolates keyframe poses, renders the 2D stick figure onto a Compose `Canvas`, and provides user play/pause/restart controls.
* **Dependencies:** Jetpack Compose Canvas, `ExerciseAnimation`, `Pose`, `Point`.
* **Interactions:** Embedded inside `ExerciseDetailDialog` in `ExerciseLibraryScreen.kt`.
* **Technical Debt:**
  * **Linear Cartesian Interpolation:** `interpolate()` uses linear interpolation (`lerp(a.x, b.x, f)`) between points. When an elbow or knee flexes between two positions, linear coordinate interpolation causes the bone segment to shrink in length mid-transition.
  * **Flat 2D Single-Limb Rendering:** `drawFigure()` draws a single line for the arm and a single line for the leg.
  * **Primitive Equipment Rendering:** Implements (`pose.implement`) draw as a single unstyled horizontal line (`drawLine(...)`). Bench, cables, and racks cannot be rendered.
* **Recommendation:** **Replace.** Replace with a production-quality `AnimationRenderer` and `PoseInterpolator` supporting angular joint rotation, dual limbs, and modular equipment drawing.

---

### 5. Muscle Highlighting & Anatomical Mapping

#### `app/src/main/java/com/replog/domain/library/MuscleMap.kt`
* **Category:** Muscle highlighting
* **Purpose:** Maps exercise CSV muscle strings into a fixed enum of 19 body regions (`MuscleRegion`) categorized into `Side.FRONT` or `Side.BACK`.
* **Dependencies:** `Exercise`, `ExercisePattern`.
* **Interactions:** Called by `MuscleBodyDiagram(ex)` to determine `primaryRegions` and `secondaryRegions`.
* **Technical Debt:** Resolves regions via linear keyword matching (`keywordToRegion`). Contains hardcoded fallbacks (e.g., forcing `ABS` and `UPPER_BACK` for loaded carries).
* **Recommendation:** **Refactor / Replace.** Replace enum mapping with a robust anatomical taxonomy (`anatomy/MuscleMap.kt` and `MuscleRegion.kt`) linked directly to precise vector paths.

#### `app/src/main/java/com/replog/ui/exercise/MuscleBodyDiagram.kt`
* **Category:** Exercise rendering, Canvas drawing, Muscle highlighting
* **Purpose:** Renders a front and back human silhouette using Jetpack Compose `Canvas` and overlays highlighted primary (filled) and secondary (outlined/patterned) muscle regions.
* **Dependencies:** Jetpack Compose Canvas, `MuscleMap`, `MuscleRegion`.
* **Interactions:** Embedded inside `ExerciseDetailDialog` in `ExerciseLibraryScreen.kt`.
* **Technical Debt:**
  * **Crude Silhouette:** `drawSilhouette` constructs a body out of primitive shapes: a circle for the head and four rounded rectangles (`drawRoundRectN`) for the torso, arms, and legs.
  * **Inaccurate Rectangular Highlights:** Highlights are defined in `REGION_BOXES` as 2D normalized rectangles (`Box(x, y, w, h)`). For example, `BICEPS` is defined as `Box(0.20f, 0.26f, 0.10f, 0.10f)`, which sits strictly over the *left* arm rectangle—leaving the right bicep completely unhighlighted! Similarly, `FRONT_DELTS` is a single block box spanning across the chest.
* **Recommendation:** **Replace.** Replace with a true anatomical vector rendering engine (`MuscleRenderer`, `FrontBody`, `BackBody`) utilizing accurately scaled vector paths (`Path`) for distinct muscle groups on both left and right sides of the body.

---

### 6. Exercise Detail Dialogs & UI Integration

#### `app/src/main/java/com/replog/ui/exercise/ExerciseLibraryScreen.kt`
* **Category:** Exercise detail dialogs, Exercise lookup UI
* **Purpose:** Displays the searchable/filterable exercise catalog and hosts `ExerciseDetailDialog`, an `AlertDialog` presenting exercise summary metadata, coaching cues, `MuscleBodyDiagram(ex)`, and `ExerciseAnimationView(ex)`.
* **Dependencies:** Jetpack Compose Material 3, `ExerciseViewModel`, `ExerciseCoach`, `MuscleBodyDiagram`, `ExerciseAnimationView`.
* **Interactions:** Primary UI entry point for exercise inspection.
* **Technical Debt:** Embeds `ExerciseDetailDialog` directly within the screen file. Currently tightly coupled to `MuscleBodyDiagram` and `ExerciseAnimationView`.
* **Recommendation:** **Keep / Refactor.** During Phase 11 (Migration), update `ExerciseDetailDialog` to host the new scalable `ExerciseVisualisationEngine` components without breaking user interaction or accessibility semantics.

#### `app/src/main/java/com/replog/ui/exercise/ExerciseCoachingSections.kt`
* **Category:** Exercise detail dialogs
* **Purpose:** UI helper components (`CoachingSections`, `ExpandableSection`, `RepLogCard`) used inside `ExerciseDetailDialog`.
* **Dependencies:** Jetpack Compose Material 3.
* **Interactions:** Called inside `ExerciseDetailDialog`.
* **Technical Debt:** Clean, well-structured UI code.
* **Recommendation:** **Keep.**

---

### 7. Incidental Canvas Files (Audit Completeness)

To ensure no Canvas drawing or visualization code was missed across the repository, all other files importing `androidx.compose.foundation.Canvas` or `android.graphics.Canvas` were audited:
* **`app/src/main/java/com/replog/ui/progress/ProgressScreen.kt`**: Uses Compose `Canvas` to render 2D line charts for volume and progression trends. Unrelated to exercise biomechanics.
* **`app/src/main/java/com/replog/util/ShareCardRenderer.kt`**: Uses Android `Canvas` to draw bitmap share cards for social export. Unrelated to exercise biomechanics.

---

## Audit Summary Table

| File Path | Primary Category | Purpose | Technical Debt / Weakness | Decision |
| :--- | :--- | :--- | :--- | :--- |
| `assets/exercises.json` | Database / Media | 200+ exercise catalog definitions | Orphaned `mediaAsset` GIF paths; unstandardized patterns | **Refactor** |
| `res/.../exercise_media_placeholder.xml` | Media assets | Fallback vector placeholder | Unused dead code | **Delete / Replace** |
| `data/model/Exercise.kt` | Metadata | Core entity for exercises | Retains dead fields; lacks visual biomechanics metadata | **Refactor** |
| `domain/library/ExercisePattern.kt` | Metadata | Pattern & equipment classification | String heuristic coupling to 10 coarse animation clips | **Refactor / Expand** |
| `domain/library/ExerciseCoach.kt` | Metadata / Coaching | Offline coaching generation | Minor ad-hoc string checks | **Keep / Refactor** |
| `domain/library/WhyThisExercise.kt` | Metadata / Rationale | Dynamic rationale generation | None | **Keep** |
| `data/db/ExerciseDao.kt` | Lookup | Room SQL queries | String `LIKE` concatenations across CSVs | **Keep** |
| `data/repository/ExerciseRepository.kt` | Lookup | Repository wrapper | None | **Keep** |
| `util/DataSeeder.kt` | Database | JSON asset seeder | String joining for CSV columns | **Keep** |
| `domain/library/ExerciseFilter.kt` | Lookup | Library filter state | Static string dimensions | **Keep** |
| `domain/library/ExerciseAnimation.kt` | Animation / Stick figure | Coarse pose generator | 7-point single limb skeleton; hardcoded coordinates | **Replace** |
| `ui/exercise/ExerciseAnimationView.kt` | Rendering / Canvas | Stick figure loop & canvas draw | Linear coordinate lerp (bone stretching); crude single lines | **Replace** |
| `domain/library/MuscleMap.kt` | Muscle highlighting | CSV to region enum mapping | Linear keyword matching; crude fallback rules | **Replace** |
| `ui/exercise/MuscleBodyDiagram.kt` | Rendering / Canvas | Silhouette & region highlight draw | Rounded-rect body; asymmetric overlapping block boxes | **Replace** |
| `ui/exercise/ExerciseLibraryScreen.kt` | Detail dialogs | Catalog list & detail dialog | Hosts legacy visualisations | **Keep / Refactor** |
| `ui/exercise/ExerciseCoachingSections.kt` | Detail dialogs | Expandable coaching UI cards | None | **Keep** |

---

## Conclusion & Next Steps

With Phase 1 complete and documented, code modification can safely proceed in subsequent phases:
* **Phase 2:** Document exact architectural limitations and biomechanical failures.
* **Phase 3:** Design the modular replacement architecture (`animation/`, `anatomy/`, `equipment/`).
* **Phase 4–10:** Implement the rotational skeletal engine, anatomical vector regions, equipment renderers, and performance optimizations.
* **Phase 11–12:** Incrementally migrate dialog screens and validate all exercises against the new engine.
