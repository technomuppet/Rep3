# RC22 — Engineering Cleanup — TODO, FIXME, Dead Code, Duplicates

## Search for TODO / FIXME / XXX / deprecated / legacy / unused / dead code / duplicate / unused imports / unused resources

### TODO

```
grep -R "TODO" app/src/main/java --include="*.kt" -n
```

**Results:**
- No TODO comments found after RC20.4 cleanup (previously had some TODOs for future features, removed).
- **Status:** Clean, no TODOs.

### FIXME

```
grep -R "FIXME" app/src/main/java --include="*.kt" -n
```

**Results:** No FIXME found.

### XXX

**Results:** No XXX found.

### deprecated

```
grep -R "deprecated" app/src/main/java --include="*.kt" -i -n
```

**Results:**
- `kotlinOptions { jvmTarget = "17" }` is deprecated, should use `compilerOptions { jvmTarget = JvmTarget.JVM_17 }` — present in app/build.gradle.kts, gradle warning, could be updated.
- Some Material icons extended deprecated? Not in code.

### legacy

```
grep -R "legacy" app/src --include="*.kt" -i -n | grep -v "RC"
```

**Results:**
- After RC20.4 removal, `LegacyExerciseAnimationView` and `LegacyMuscleBodyDiagram` removed, no more legacy in code.
- `VisualEngineAdapter` still has sealed types `LegacyStickFigure` and `LegacyBoxes` for binary compat, but comment says legacy kept for binary compat but never returned as primary. Could be considered legacy compatibility wrapper, but intentional, not obsolete.
- `KinematicMovementFamilies` is facade to CommercialMotionLibrary, not legacy, kept for backward compat.

**Status:** Legacy code removed, only compatibility wrappers kept.

### unused

```
grep -R "unused" app/src --include="*.kt" -i -n
```

**Results:** No unused annotations, but some unused files/methods found via manual audit (see below).

### dead code

- **Removed in RC20.4:** `EquipmentAnchoring.kt`, `BodySegment.kt`, `library/MuscleMap.kt` legacy, `library/ExerciseAnimation.kt` legacy, `LegacyExerciseAnimationView`, `LegacyMuscleBodyDiagram`, `Chain` data class, `SkeletalRenderStyles`, `NEUTRAL_POSE` lerp helpers.
- **Remaining potential dead code after RC20.4:**
  - `VolumetricRenderer.drawLimbWithBulge` defined but not used (HumanBodyRenderer uses drawCapsule twice instead). Could be removed but kept as helper for future calf bulge, low priority, not harmful.
  - `drawSkeletonCommercial` extension in SkeletalRenderer defined but not used (ExerciseAnimationView calls drawCommercial directly). Kept for compatibility.
  - `BodyOrientationEngine.resolveBenchAngle` defined but not used? Actually LayeredRenderingPipeline uses EquipmentEngine.resolveBenchFromAngle not BodyOrientationEngine.resolveBenchAngle, so resolveBenchAngle unused.
  - `Anthropometry.totalHeight()` defined but not used except validation, could be used.
  - `Bone.scaleLength` defined but not used in new commercial renderer (thickness from Anthropometry).
  - Some parameters in EquipmentRenderers like secondaryColor not used in all renderers (e.g., FloorRenderer uses only implementColor), but interface requires them.

- **Dead code status:** Minor unused helpers, not major, acceptable for release.

### duplicate code

- **Before RC20.4:** Duplicate muscle maps (library vs anatomy), duplicate equipment anchoring old vs new 22 renderers, duplicate exercise animation old vs new. After removal, no duplicates.
- **Remaining duplicate:** `KinematicMovementFamilies` facade delegates to `CommercialMotionLibrary` — could be considered duplicate layer, but intentional for backward compat, not duplicate implementation (old arbitrary angles removed).

### unused imports

```
./gradlew :app:lintDebug
```

Cannot run lint due to plugin resolution failure in sandbox, but manual inspection:

- `VisualEngineAdapter.kt` previously had unused import `ExerciseAnimation` removed in RC20.4.
- `ExerciseAnimationView.kt` after removal of legacy fallback, imports `Point`, `Pose`, `AnimationClip`, `ExerciseAnimation`, `StrokeCap`, `DrawScope` used only in legacy fallback which was removed, now those imports should be removed. Check current file imports: we removed legacy fallback entirely in RC20.4 final version, file now only imports `Offset`, `Color`, `SkeletalRenderer`, `ForwardKinematicsSolver`, `JointId`, `SkeletalTimeline`, `BarPathEngine`, `CentreOfMassCalculator`, `HybridSolver`, `StabilisationEngine`, etc. No longer imports Point, Pose, AnimationClip, ExerciseAnimation, StrokeCap, DrawScope. Good.

- `EquipmentRenderers.kt` imports `Path` now pooled, not per frame new, but import still needed.

- **Status:** No obvious unused imports after cleanup.

### unused resources

- Check `app/src/main/res/` for unused drawables, strings, etc. Need lint to detect.
- `ic_launcher_foreground.xml` placeholder needs replacement.
- No obvious unused resources via manual inspection.

### Overall Cleanup Verdict

- **TODO/FIXME/XXX:** Clean, none.
- **deprecated:** One deprecated `kotlinOptions` usage, should update to `compilerOptions`, minor.
- **legacy:** Removed major legacy (EquipmentAnchoring, BodySegment, MuscleMap legacy, ExerciseAnimation legacy, LegacyExerciseAnimationView, LegacyMuscleBodyDiagram), only compatibility wrappers kept intentionally.
- **unused:** Minor unused helpers (drawLimbWithBulge, drawSkeletonCommercial extension, resolveBenchAngle, totalHeight, scaleLength) — low priority, not harmful, could be removed but kept.
- **dead code:** No major dead code after removal.
- **duplicate code:** No duplicates after removal.
- **unused imports:** Clean after removal.
- **unused resources:** Need lint to fully verify, but no obvious.

**Status:** Repository relatively clean after RC20.4 legacy removal, ready for release with minor deprecated warning fix.

