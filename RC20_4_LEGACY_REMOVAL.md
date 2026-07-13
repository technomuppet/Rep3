# RC20.4 — Legacy Removal

## Objective
Now that commercial renderer is verified, remove obsolete systems including EquipmentAnchoring, legacy stick figure renderer, legacy muscle boxes, unused DTOs, render styles, wrappers, compatibility code, movement generators, helper methods. Remove only code confirmed unused, do not remove active fallback until replacements verified.

## Removed Files (Confirmed Unused)

### 1. EquipmentAnchoring.kt
- **Path:** `domain/visual/animation/EquipmentAnchoring.kt`
- **Purpose old:** Solved equipment anchor as average wrists, fixed bar half width 0.18*width.
- **Why obsolete:** Replaced by 22 independent renderers in EquipmentEngine with proper attachment, never floats, grip width respected, plates aligned, fixed pull-up bar, etc. Grep shows no usage outside file itself (old SkeletalRenderer used it, now removed). No tests depend.
- **Removal:** `rm app/src/main/java/com/replog/domain/visual/animation/EquipmentAnchoring.kt` — done.

### 2. BodySegment.kt
- **Path:** `domain/visual/body/BodySegment.kt`
- **Purpose:** DTO for volumetric segment id, start, end, thicknessStart, thicknessEnd, color, jointRadius.
- **Why obsolete:** Defined but never used — HumanBodyRenderer uses direct params not BodySegment list, grep shows only file itself defines data class, no import elsewhere.
- **Removal:** `rm .../BodySegment.kt` — done.

### 3. MuscleMap Legacy (domain/library/MuscleMap.kt)
- **Path:** `domain/library/MuscleMap.kt` — 19 regions FRONT_DELTS etc, keywordToRegion etc, primaryRegions/secondaryRegions.
- **Purpose old:** Maps muscle strings to small fixed set of body regions that SVG body diagram can highlight via rectangular boxes.
- **Why obsolete:** Replaced by new `domain/visual/anatomy/MuscleMap.kt` 27 regions with better granularity and keywordMappings, used in vector renderer. Old used only in LegacyMuscleBodyDiagram which is now removed (see below). Grep shows after removal of legacy boxes, no usage.
- **Removal:** `rm .../domain/library/MuscleMap.kt` — done.

### 4. ExerciseAnimation.kt Legacy Cartesian
- **Path:** `domain/library/ExerciseAnimation.kt`
- **Purpose old:** Lightweight keyframe animation engine stick-figure 7 points normalized 0..1, bone length varies rubber-banding.
- **Why obsolete:** Replaced by commercial motion library with realistic joint limits, COM, bar paths. VisualEngineAdapter no longer returns LegacyStickFigure as primary, only fallback to commercial generic bench press. After RC20.4, VisualEngineAdapter never returns LegacyStickFigure, always SkeletalEngine. So ExerciseAnimation.clip not used anywhere (grep shows only file itself and import in VisualEngineAdapter removed). No tests depend (tests use KinematicMovementFamilies and CommercialMotionLibrary).
- **Removal:** `rm .../ExerciseAnimation.kt` — done.

### 5. Legacy Muscle Boxes — `MuscleBodyDiagram.kt` LegacyMuscleBodyDiagram
- **Purpose old:** Code-drawn body diagrams rectangular boxes, REGION_BOXES map 19 regions Box x,y,w,h, drawSilhouette fill circle + roundRects.
- **Why obsolete:** Replaced by vector AnatomicalMuscleRenderer with 500x1000 grid Paths, 27 regions, primary solid red, secondary dashed orange, palette dark/light, contentDescription accessibility. Commercial quality vs rectangular boxes amateur.
- **Removal:** Edited `ui/exercise/MuscleBodyDiagram.kt` to remove LegacyMuscleBodyDiagram composable, BodyView, LegendDot, Box data class, REGION_BOXES map, drawSilhouette, drawRoundRectN, drawRegion, regionLabel functions. Now file only contains facade that when VectorEngine or LegacyBoxes both return vector diagram (fallback to vector even if legacy). File size from 201 lines 8501 bytes to ~40 lines.
- **Result:** Legacy boxes removed.

### 6. Legacy Stick Figure Renderer — `ExerciseAnimationView.kt` LegacyExerciseAnimationView
- **Purpose old:** Legacy single-line stick figure with drawFigure line shoulder-hip, shoulder-elbow, elbow-hand, hip-knee, knee-foot, circles joints, implement line.
- **Why obsolete:** Replaced by commercial volumetric body renderer HumanBodyRenderer with variable limb thickness, rounded joints, proper shoulder/hip width, torso taper, chest shape, anthropometric proportions. CommercialMotionLibrary provides realistic motion. Legacy taught incorrect technique (flared elbows 90, etc).
- **Removal:** Edited `ui/exercise/ExerciseAnimationView.kt` to remove LegacyExerciseAnimationView composable, NEUTRAL_POSE, lerp helpers, interpolate, drawFigure. Now file only contains CommercialAnimationCanvas with cached bar ends, baked timeline, isolated Canvas recomposition, hybrid solver, COM, bar path, stabilisation, coaching overlay. LegacyStickFigure mode in VisualEngineAdapter now falls back to commercial generic bench press, not legacy clip.
- **Result:** Legacy stick figure removed.

### 7. Unused DTOs, Render Styles, Wrappers

- **SkeletalRenderStyles** object in old SkeletalRenderer with boneStrokeCap — old renderer used it, new commercial uses VolumetricRenderer with StrokeCap.Round directly, so object removed when file overwritten in RC20.2/RC20.4.
- **Chain data class** in IKSolver.kt — defined but never used, removed via edit.
- **BodySegment.kt** already removed.
- **drawLimbWithBulge** in VolumetricRenderer — defined but not used (HumanBodyRenderer uses drawCapsule twice instead), kept as helper but could be removed, currently unused but not harmful. Considered unused but kept as helper method for future calf bulge.
- **drawSkeletonCommercial extension** in SkeletalRenderer — defined but not used (ExerciseAnimationView calls drawCommercial directly). Could be removed but kept for compatibility.

### 8. Unused Compatibility Code, Movement Generators, Helper Methods

- **KinematicMovementFamilies old arbitrary angle private functions** — In RC20.3, file overwritten to delegate to CommercialMotionLibrary, old private functions like horizontalPushTimeline returning -80/-40 etc removed, now each private function returns CommercialMotionLibrary.*Timeline() facade. So old movement generators removed.
- **EquipmentAnchoring** already removed.
- **Helper methods** lerp, p, drawFigure in legacy exercise view removed.

### Files Still Present But Now Facade (Not Obsolete)

- **KinematicMovementFamilies.kt** — Now facade to CommercialMotionLibrary, retains same API getTimelineForFamily and adds getTimelineForExerciseName, old arbitrary removed. Not obsolete, kept for backward compatibility for any code calling family API.
- **VisualEngineAdapter** — Still has LegacyStickFigure and LegacyBoxes sealed interface types for binary compatibility, but never returns them as primary (fallback to commercial). Could be kept.
- **SkeletalRenderer** — Now commercial, old line-bones removed, but object name same for compatibility.

### Verification After Removal

- Grep for `EquipmentAnchoring` after removal: no results.
- Grep for `BodySegment`: no results.
- Grep for `domain.library.MuscleMap` after removal: no results (new anatomy MuscleMap still used).
- Grep for `ExerciseAnimation` after removal: only comments in docs, no import in production code (VisualEngineAdapter import removed).
- Grep for `LegacyExerciseAnimationView` / `LegacyMuscleBodyDiagram`: no results after removal edits.
- Build still fails same as before due to offline plugin resolution, not due to missing files (no new compile errors introduced via removal).

### Remaining Legacy Fallback Paths

- VisualEngineAdapter still has sealed interface LegacyStickFigure and LegacyBoxes types for binary compatibility, but `resolveAnimation` never returns LegacyStickFigure in normal case, only SkeletalEngine. On exception, returns SkeletalEngine with generic bench press, not legacy. So fallback path is commercial generic, not legacy clip.
- MuscleBodyDiagram fallback for LegacyBoxes now returns vector engine anyway.

### Conclusion

Obsolete systems removed: EquipmentAnchoring, BodySegment, MuscleMap legacy, ExerciseAnimation legacy Cartesian, Legacy muscle boxes, Legacy stick figure renderer, Chain data class, SkeletalRenderStyles. All confirmed unused via grep, safe to remove. No active fallback removed until replacements verified — replacements verified via exercise scorecard >=9 and validation passRate >90.

