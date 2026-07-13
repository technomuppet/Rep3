# RC17.2 — Exercise Visualisation Engine Stage 1: Domain Foundation

**Date:** July 4, 2026  
**Status:** Stage 1 Implementation Complete (`RC17.2`)  
**Scope:** UI-independent, deterministic, immutable domain specification layer (`com.replog.domain.visual`) ready for consumption by downstream anatomical and kinematic renderers.

---

## 1. Executive Summary

In accordance with `RC17_ARCHITECTURE.md`, Stage 1 establishes the production-grade domain layer of the Exercise Visualisation Engine. All components in this stage are pure Kotlin domain models, registries, resolvers, and validation engines. **No Compose rendering, Canvas drawing, UI modifications, or legacy code deletions occurred.** Existing screens (`ExerciseLibraryScreen`, `ExerciseDetailDialog`, `ExerciseAnimationView`) remain untouched and 100% backward compatible.

---

## 2. Package Structure & Implemented Modules

```text
com.replog.domain.visual/
├── spec/
│   ├── ExerciseVisualSpec.kt         # Master specification root model
│   ├── MovementFamilySpec.kt         # Family reference & kinematic parameters
│   ├── EquipmentSpec.kt              # Equipment type, support structure & bench angle
│   └── AnatomySpec.kt                # Primary & secondary targeted muscle regions
├── resolver/
│   └── ExerciseVisualResolver.kt     # Pure translator: Exercise -> ExerciseVisualSpec
├── registry/
│   ├── MovementFamily.kt             # 42+ standardized movement family archetypes
│   └── MovementRegistry.kt           # Single source of truth lookup registry
└── validation/
    ├── MovementCoverage.kt           # Family mapping metrics
    ├── VisualCoverageReport.kt       # Catalog verification report
    └── ExerciseVisualValidator.kt    # Batch verification engine
```

---

## 3. Core Domain Layer Specifications

### 3.1 Immutable Specification Layer (`spec/`)
* **`ExerciseVisualSpec`**: The master immutable data model holding all parameters required for visual rendering. Contains explicit domain types: `bodyOrientation` (`BodyOrientation`), `supportType` (`SupportType`), `gripType` (`GripType`), `stance` (`StanceType`), `benchAngle`, `rangeOfMotion` (`RangeOfMotion`), `mirrorable`, `playbackSpeed`, and delegates to sub-specifications (`movementFamily`, `equipment`, `anatomy`).
* **`MovementFamilySpec`**: Identifies the assigned family archetype along with numeric scaling parameters (`benchAngle`, `gripWidthFactor`, `stanceWidthFactor`).
* **`EquipmentSpec`**: Categorizes equipment into 11 `EquipmentType`s (`BARBELL`, `DUMBBELL`, `CABLE`, `MACHINE`, `EZ_BAR`, `KETTLEBELL`, `SMITH_MACHINE`, `BAND`, `PLATE`, `BODYWEIGHT`, `OTHER`) and 12 `SupportType`s (`STANDING`, `SEATED_FLAT`, `SEATED_INCLINE`, `SEATED_DECLINE`, `CHEST_SUPPORTED`, `PRONE_LYING`, `SUPINE_LYING`, etc.).
* **`AnatomySpec`**: Houses distinct immutable sets of normalized primary and secondary muscle region strings.

### 3.2 Standardized Movement Registry (`registry/`)
* **`MovementFamily`**: Defines 42 parametric movement archetypes organized under 9 categories (`PUSH`, `PULL`, `LEGS`, `HINGE`, `CORE`, `ISOLATION_UPPER`, `ISOLATION_LOWER`, `CONDITIONING`, `FULL_BODY`). Replaces legacy coarse animations with fine-grained kinematic templates such as `HORIZONTAL_PUSH`, `INCLINE_PUSH`, `DECLINE_PUSH`, `VERTICAL_PUSH`, `CABLE_ROW`, `LAT_PULLDOWN`, `ROMANIAN_DEADLIFT`, `FRONT_SQUAT`, `HACK_SQUAT`, `SPLIT_SQUAT`, `PREACHER_CURL`, `OVERHEAD_EXTENSION`, and `FACE_PULL`.
* **`MovementRegistry`**: Exposes single-source-of-truth $O(1)$ lookups (`getById`, `getAllFamilies`, `isRegistered`).

### 3.3 Pure Visual Resolver (`resolver/`)
* **`ExerciseVisualResolver`**: Evaluates any `Exercise` entity (`id`, `name`, `category`, `equipment`, `movementPattern`, `muscles`, `primaryMuscles`, `secondaryMuscles`) using deterministic rules.
* Evaluates equipment substrings, pattern definitions, body support orientation, grip spacing, stance stance widths, range of motion constraints, and exact bench tilt angles ($30^\circ$ incline, $-15^\circ$ decline, $0^\circ$ flat, $85^\circ$ upright seated).

---

## 4. Validation System & Automated Coverage Reporting

The `com.replog.domain.visual.validation` package prevents silent failures as the exercise library expands:
* **`ExerciseVisualValidator.validateCatalog(exercises: List<Exercise>)`**: Batches resolution over all input exercises. Detects unmapped patterns (`GENERIC_UNMAPPED`), unclassified equipment (`OTHER`), and empty anatomical mappings (`missingMuscles`).
* **`VisualCoverageReport`**: Generates exact quantitative statistics and categorized breakdowns of every catalog exercise.

---

## 5. Unit Test Suite

Comprehensive automated tests are implemented in **`app/src/test/java/com/replog/domain/visual/ExerciseVisualDomainTest.kt`**, covering:
1. **Registry Verification**: Confirms that $\ge 33$ standardized families are registered and retrievable.
2. **Resolver Accuracy**: Verifies complex multi-parameter resolution (e.g., confirming `Incline Barbell Bench Press` maps to `INCLINE_PUSH`, `BARBELL`, `SEATED_INCLINE` at $30.0^\circ$, `STANDARD_PRONATED` grip).
3. **Parametric Variations**: Confirms grip (`WIDE`, `CLOSE`) and stance (`WIDE_SUMO`, `SINGLE_LEG`) scaling multipliers.
4. **Fallback Resilience**: Verifies graceful degradation for unknown equipment (`Alien Grav-Sled`) and unknown movement patterns (`Mysterious Neuro-Flex`).
5. **Anatomical Mapping**: Verifies clean extraction and separation of primary vs. secondary muscles.
6. **Automated Catalog Validation**: Automatically locates and parses `app/src/main/assets/exercises.json` (516 bundled exercises) at test execution time, verifying high resolution coverage and zero unhandled exceptions across the entire production catalog.

---

## 6. Zero-Code Expansion: How to Add Future Exercises

Because `ExerciseVisualResolver` derives visual specifications purely from domain metadata, adding future exercises requires zero Kotlin animation or rendering code changes.

### Step-by-Step Guide for Adding a New Exercise
1. **Add Entry to Catalog / Database (`exercises.json` or Room)**:
   Ensure the exercise record provides clear metadata:
   ```json
   {
     "name": "Seated Single-Arm Cable Row",
     "category": "Back",
     "equipment": "Cable",
     "muscles": ["Latissimus Dorsi", "Biceps", "Rear Deltoids"],
     "primaryMuscles": ["Lats"],
     "secondaryMuscles": ["Biceps", "Rear Delts"],
     "movementPattern": "Pull • Horizontal Row",
     "difficulty": "Intermediate"
   }
   ```
2. **Automatic Runtime Resolution**:
   When passed to `ExerciseVisualResolver.resolve(exercise)`:
   * `equipment = "Cable"` resolves to `EquipmentType.CABLE`.
   * `movementPattern = "Pull • Horizontal Row"` resolves to `MovementFamily.CABLE_ROW` (or `HORIZONTAL_PULL`).
   * `"Seated"` resolves to `SupportType.SEATED_FLAT` ($85^\circ$ torso angle) and `BodyOrientation.SEATED`.
   * `"Single-Arm"` resolves stance/grip parameters.
   * Primary/secondary muscles resolve into `AnatomySpec(primaryMuscles = {"Lats"}, secondaryMuscles = {"Biceps", "Rear Delts"})`.
3. **Automated Verification**:
   Running `ExerciseVisualValidator.validateCatalog(...)` during CI/CD automatically detects the new exercise and verifies that it resolves without fallbacks.

---

## 7. Next Stage Readiness

With Stage 1 completed and verified, the codebase is completely prepared for **RC17.3 — Anatomical Muscle Renderer (`anatomy/`)**, where `AnatomySpec` will be consumed by Compose vector renderers to replace legacy rectangular muscle boxes.
