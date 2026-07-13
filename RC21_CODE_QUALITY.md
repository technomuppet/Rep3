# RC21 — Code Quality Audit — SOLID, Clean Architecture, Compose Best Practices

## SOLID

### Single Responsibility

- **Good:** `BiomechanicalJointModel` only limits, `CentreOfMassCalculator` only COM, `IKSolver` only FABRIK, `HybridSolver` only hybrid, `BarPathEngine` only bar paths, `StabilisationEngine` only stabilisation cues, `CommercialMotionLibrary` only templates, `HumanBodyRenderer` only volumetric body, `EquipmentEngine` only registry, `EquipmentRenderers` each own renderer, `CameraSystem` only camera views, `BodyOrientationEngine` only orientation, `MuscleActivationEngine` only activation.
- **Violation:** `ExerciseAnimationView` does too much: evaluates timeline, FK, bar path, hybrid solver, COM, stabilisation, camera, draws coaching overlay — multiple responsibilities in one file. Could be split into `AnimationViewModel` + `CanvasContent`.

### Open/Closed

- **Good:** EquipmentEngine open for extension via new renderer object + register in getAllRenderers, closed for modification (resolvePrimary via when). CommercialMotionLibrary open for new exercise via new function + mapping.
- **Violation:** `ExerciseVisualResolver.resolveSupportAndAngle` long if-else chain with many conditions, not open/closed, would need modification for new family. Could be strategy pattern.

### Liskov Substitution

- **Good:** All EquipmentRenderer implementations substitutable via interface, same draw() signature.
- **No violation found.**

### Interface Segregation

- **Good:** EquipmentRenderer interface small draw() + validateAttachment(), not fat.
- **Violation:** `SkeletalRenderer` has two methods drawSkeleton legacy compat and drawCommercial with progress and cameraView defaults — could be split but acceptable.

### Dependency Inversion

- **Good:** Domain visual does not depend on UI, UI depends on domain via VisualEngineAdapter (abstraction). Good separation.

## Clean Architecture

- **Domain layer** `domain/visual/` pure Kotlin no Compose except renderers (Compose Canvas) — acceptable because renderers need DrawScope, but domain should ideally not depend on Compose UI. Currently renderers depend on `androidx.compose.ui.graphics.drawscope.DrawScope` which is UI, so domain depends on UI — architectural violation. Should be in `ui/` layer or `presentation` layer, not domain. However for performance and offline, it's common to have renderers in domain? Still violation.
- **UI layer** `ui/exercise/` depends on domain via adapter, good.
- **Data layer** `data/` independent, good.

## Separation of Concerns

- **Good:** Resolver pure translator Exercise -> Spec, no UI. Motion library pure timelines, no UI. Biomechanics pure calculations, no UI.
- **Violation:** `ExerciseAnimationView` mixes animation logic (FK, IK, COM) with UI (Canvas, MaterialTheme). Should move logic to ViewModel or use case.

## Naming

- **Good:** Classes named clearly: `BiomechanicalJointModel`, `CentreOfMassCalculator`, `IKSolver`, `HybridSolver`, `BarPathEngine`, `StabilisationEngine`, `CommercialMotionLibrary`, `HumanBodyRenderer`, `VolumetricRenderer`, `Anthropometry`, `EquipmentEngine`, `CameraSystem`, `BodyOrientationEngine`, `MuscleActivationEngine`.
- **File structure matches class names** — good.
- **Minor:** `BarPathEngine.BarPathType` vs `EquipmentType` — naming consistent.

## File Structure / Dependency Direction

- **Good:** Packages `biomechanics`, `body`, `equipment`, `camera`, `layered`, `orientation`, `anatomy`, `animation`, `resolver`, `registry`, `spec`, `validation` — logical grouping.
- **Dependency direction:** `animation` depends on `biomechanics` (BiomechanicalJointModel) — okay, biomechanics is lower level? Actually FK should depend on biomechanical model, so animation -> biomechanics okay. `body` depends on `animation` (JointId, SolvedSkeleton) — okay. `equipment` depends on `animation` (JointId, SolvedSkeleton) — okay. `layered` depends on `body`, `equipment`, `orientation`, `camera`, `anatomy` — higher level aggregating lower, okay. `ui` depends on all domain — okay. No circular dependencies found.

## Immutability / Thread Safety

- **Good:** Data classes `SkeletalPose`, `Keyframe`, `SkeletalTimeline`, `ExerciseVisualSpec`, `EquipmentSpec`, `ComResult`, `ValidationResult` are immutable (val).
- **Mutable:** `mutableMap` in `ForwardKinematicsSolver.solve` local mutable but not shared, thread-safe as local.
- **Thread safety:** No shared mutable state across threads, LaunchedEffect withFrameNanos runs on Main, no background threads for solvers, so thread-safe.

## Compose Best Practices

- **State hoisting:** `playing` and `resetKey` hoisted to `CommercialAnimationCanvas` outer, `elapsedSeconds` hoisted to inner `AnimationCanvasContent` with own State — good isolation, only inner recomposes.
- **Remember usage:** `remember(timeline) { bakeTimeline }` and `remember(timeline) { cachedBarEnds }` correctly keyed to timeline, not recomputed every frame. Good.
- **DerivedStateOf:** Could use `derivedStateOf` for progress derived from elapsedSeconds / duration, but currently computed directly in Canvas DrawScope via `cycleTime` calculation, okay.
- **Stability:** `SkeletalPose`, `JointId` enum stable, `SkeletalTimeline` class with List<Keyframe> stable? List is not stable by default but timeline is remembered, so okay. `Offset` is stable value type.
- **Allocation safety:** Path pooling via reset() avoids allocation, baked timeline lookup O(1) avoids map allocation, cached bar ends avoids FK per frame.
- **Recomposition safety:** Isolated Canvas recomposition fixed in RC20.4, only CanvasContent recomposes.

## Violations / Code Smells

- **Magic numbers:** Shoulder -45, elbow 105 etc in CommercialMotionLibrary are documented with coaching comments, not pure magic, acceptable.
- **Long file:** CommercialMotionLibrary 44KB 55 functions — could be split per family (pushPullLegs etc) but okay as single source of truth.
- **Long if-else chain:** `ExerciseVisualResolver.resolveMovementFamily` and `resolveSupportAndAngle` and `CommercialMotionLibrary.getTimelineForExerciseName` have long when with many contains checks — cyclomatic complexity high, could be strategy/rule engine, but pure function and offline.
- **Unused parameters:** `EquipmentEngine.resolvePrimary` uses EquipmentSpec but some renderers ignore secondaryColor, some ignore implementColor, but interface requires them — okay.
- **Duplicated calculations:** `hypot` for grip span calculated in OlympicBarbellRenderer and again in EquipmentAttachmentSolver and HybridSolver — could be extracted to util, but small duplication.
- **Unnecessary abstractions:** None major.

## Allocation Safety / Recomposition Safety

- **Allocation safety:** Path pooling implemented, baked timeline, cached bar ends, referenceSize computed once per draw, toScreen lambda cheap. Still 70 objects/frame via FK + hybrid map copy, but acceptable.
- **Recomposition safety:** Isolated Canvas fixed.

## Report Every Violation Summary

- **SOLID SRP violation:** ExerciseAnimationView does too much — should split logic to ViewModel.
- **Clean Architecture violation:** Domain visual renderers depend on Compose DrawScope (UI) — should be in ui layer.
- **Separation of concerns violation:** ExerciseAnimationView mixes animation logic with UI.
- **Magic numbers:** Some in CommercialMotionLibrary but documented.
- **Long file / long if-else chain:** CommercialMotionLibrary and ExerciseVisualResolver.
- **Unused methods/params:** BodyOrientationEngine.resolveBenchAngle unused, Anthropometry.totalHeight unused, Bone.scaleLength unused, VolumetricRenderer.drawLimbWithBulge unused, drawSkeletonCommercial extension unused, EquipmentRenderers secondaryColor not used in all.

**Overall Code Quality:** Good, SOLID mostly followed, Clean Architecture separation good except domain renderers depending on Compose, naming consistent, file structure logical, immutability good, Compose best practices now followed after RC20.4 fixes (remember, isolated Canvas). Minor violations not blocking release.

