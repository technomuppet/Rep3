# RC20.2 Developer Documentation — Commercial Body Rendering & Equipment Engine

## Overview

RC20.2 replaces stick-figure with commercial human body renderer and modular equipment system while staying offline, Kotlin + Compose Canvas only.

**Packages:**
- `domain.visual.body` — anthropometry, volumetric renderer, human renderer
- `domain.visual.equipment` — 22 independent renderers + engine
- `domain.visual.orientation` — body orientation (standing, seated, supine, prone, incline, decline, hanging)
- `domain.visual.attachment` — physical attachment validation
- `domain.visual.layered` — layered pipeline 8 layers
- `domain.visual.validation` — automatic verification suite

## Architecture Diagram (Text)

```
Exercise -> ExerciseVisualResolver -> ExerciseVisualSpec (equipment, support, orientation, benchAngle)
                |
                -> KinematicMovementFamilies -> SkeletalTimeline -> evaluate(t) -> SkeletalPose (joint rotations)
                |
                -> ForwardKinematicsSolver -> SolvedSkeleton (world positions)
                |
                -> BodyOrientationEngine.orient() -> OrientedSkeleton (rotated upper/lower, root shift)
                |
                -> LayeredRenderingPipeline.drawPipeline()
                        |
                        |---> Background
                        |---> Support (FloorRenderer always, Flat/Incline/Decline Bench, Rack)
                        |---> Equipment Behind (PullUpBar, PowerRack, SquatRack, SmithMachine rails)
                        |---> Body (HumanBodyRenderer.drawHumanBody volumetric)
                        |       |-- Feet (shoe capsule + shadow)
                        |       |-- Shanks (capsule with calf bulge)
                        |       |-- Thighs (shorts capsule)
                        |       |-- Pelvis (shorts shape)
                        |       |-- Torso (chest + abdomen trapezoid paths, shirt)
                        |       |-- Upper arms (shirt)
                        |       |-- Forearms (skin)
                        |       |-- Hands (mitt)
                        |       |-- Neck + Head (skin + hair)
                        |---> Equipment Front (Barbell, Dumbbells, Kettlebell, Cable, Machine handles)
                        |---> Hands highlight (grip attachment)
                        |---> Muscle overlay (future)
                        |---> Coaching overlay (bar path trace)
```

## How to Add a New Body Type

Requirement: Support variable limb thickness, rounded joints, proper shoulder/hip width, torso taper, chest shape, natural proportions.

**Steps:**

1. **Define proportions in Anthropometry.kt:**
   Add new constants e.g., `FEMALE_SHOULDER_BREADTH = 0.23f` vs male 0.26f, or `MUSCULAR_THIGH_THICKNESS_START = 0.075f`.

2. **Create BodyType enum:**
```kotlin
enum class BodyType { DEFAULT, ATHLETIC, FEMALE, YOUTHFUL }
```

3. **Extend HumanBodyRenderer.BodyPalette.fromMaterial to accept BodyType:**
```kotlin
fun fromMaterial(primary: Color, onSurface: Color, type: BodyType): BodyPalette {
  val skin = when(type) { FEMALE -> Color(0xFF...), else -> ...}
}
```

4. **In drawHumanBody, compute thickness via Anthropometry with factor:**
```kotlin
val thighStart = Anthropometry.thighThicknessStart(ref) * when(bodyType) { ATHLETIC -> 1.2f else -> 1f }
```

5. **Torso shape:** Adjust chestBottomWidth factor 0.74, waist 0.58 based on type.

6. **Add validation:** In RenderingValidationSuite, add check for new type proportions within ANSUR ranges.

No need to modify equipment renderers.

## How to Add New Equipment

Each equipment must have its own renderer — no generic placeholders.

**Steps:**

1. Create new object in `EquipmentRenderers.kt` implementing `EquipmentRenderer`:

```kotlin
object MyNewMachineRenderer : EquipmentRenderer {
  override fun draw(drawScope, skeleton, toScreen, equipmentSpec, implementColor, secondaryColor, referenceSize) = with(drawScope) {
    // 1. Get anchor points
    val leftWrist = toScreen(skeleton.getWorldPosition(LEFT_WRIST))
    val rightWrist = toScreen(skeleton.getWorldPosition(RIGHT_WRIST))
    // 2. Draw fixed frame (behind layer) and handles (front layer) — separate into two renderers if needed
    // 3. Use referenceSize for scaling: referenceSize * factor
    // 4. Avoid allocations: no new Object inside draw loop, use primitives
  }
  override fun validateAttachment(...) = ValidationResult(...)
}
```

2. Register in `EquipmentEngine`:
- In `resolvePrimary` add case for new EquipmentType or name contains check.
- Add to `getAllRenderers()` list.

3. Define layer:
```kotlin
fun getEquipmentLayer(renderer) = when(renderer) { is MyNewMachineRenderer -> EquipmentLayer.BEHIND_BODY else ... }
```

4. Add validation details: check hands attached distance, feet planted, not floating.

5. Document in `RC20_2_RENDERING_AUDIT.md` and add preview in `AnatomicalPreviews.kt` if needed.

**Example: Adding Landmine Press Renderer**
- Fixed pivot at floor 0.2, bar angled 45deg to hands midpoint, plates at far end.

## Layered Rendering Details

**8 Layers in order:**

0 BACKGROUND — subtle rect, can be gradient
1 SUPPORT_SURFACE — FloorRenderer always + bench/rack/box
2 EQUIPMENT_BEHIND — PullUpBar, PowerRack, SquatRack, Smith rails, LegPress rails
3 BODY — HumanBodyRenderer volumetric
4 EQUIPMENT_FRONT — Barbells, Dumbbells, Kettlebells, Cables
5 HANDS — highlight circles showing grip attachment correctness
6 MUSCLE_OVERLAY — reserved, currently empty (muscle diagram is separate composable)
7 COACHING_OVERLAY — bar path trace, joint angle hints (future)
8 INTERACTION — touch targets (future)

Ensure no conflicts: Support always below body, behind equipment below body, front equipment above body but below hands highlight for grip visibility.

## Orientation System

`BodyOrientationEngine.orient()` takes SolvedSkeleton + BodyOrientation + SupportType + benchAngle.

- **Upper body joints:** CHEST, UPPER_CHEST, NECK, HEAD, shoulders, elbows, wrists
- **Lower body:** hips, knees, ankles, feet
- Rotates around pelvis pivot.

Adds root shift: Standing 0, Seated +0.08 Y, Supine +0.05 Y & -90 deg upper, Prone +90 deg, Hanging -0.15 Y.

To add new orientation e.g., SIDE_PLANK:
- Add enum value SIDE_PLANK in BodyOrientation spec
- In `BodyOrientationEngine`, add Triple( -20f upper, 0f lower, Offset)
- Add bench rendering if needed

## Attachment Validation

`EquipmentAttachmentSolver.solve()` returns Attachment with isValid, validation.

Physical rules:
- Barbell: gripWidth 0.08..0.8 world, span on screen >5px, hands attached true if within bounds, floating false.
- Dumbbells: one per hand, centered at wrist.
- Cable: pulley anchor top 5% or bottom 95% depending on support, tension line.
- Bench: pelvis y 0.3..0.85, back/head supported check X or Y diff <0.25.

Use in CI: Run RenderingValidationSuite.validate(specs, skeletons, toScreen) and assert passRate >90.

## Performance Guidelines

- **Zero allocation inside draw loops:** Avoid `mutableMap`, `Offset` allocation is value type okay but minimize. No `Path` unless necessary for torso — 2 Paths max.
- **Cached geometry:** Anthropometry constants, BodyPalette, EquipmentEngine all objects singletons.
- **Minimal recomposition:** Isolate Canvas composable. Don't update State that causes parent Column recomposition each frame. In RC20.3, use `Canvas`-only recomposition.
- **ReferenceSize:** Compute once per draw via `computeReferenceSize(width,height) = min(width,height)*0.32f` ensures scaling across display sizes.
- **Draw ops budget:** ~60 per frame, <16ms.

## Testing

- **Unit tests:** `SkeletalAnimationEngineTest` still passes (FK bone invariance). Add new tests:
```kotlin
@Test fun testHumanBodyProportions() {
  assertTrue(Anthropometry.totalHeight() in 0.9f..1.1f)
  assertTrue(Anthropometry.SHOULDER_BREADTH in 0.2f..0.3f)
}
@Test fun testEquipmentNeverFloats() {
  val skeleton = ForwardKinematicsSolver.solve(emptyMap())
  val result = EquipmentEngine.resolvePrimary(EquipmentSpec(...)).validateAttachment(skeleton, toScreen)
  assertTrue(!result.floating)
}
```

- **Validation suite test:**
```kotlin
val specs = listOf(ExerciseVisualSpec(...))
val skeletons = specs.map { ForwardKinematicsSolver.solve(...) }
val report = RenderingValidationSuite.validate(specs, skeletons, toScreen)
assertTrue(report.passRate > 90f)
```

## FAQ

**Q: Why not use 3D library?**
A: Requirement forbids OpenGL, Unity, external rendering libs. Canvas only, offline.

**Q: How to support new bench angle?**
A: Pass benchAngle in EquipmentSpec, resolver already does, orientation engine adds to upperRotation, bench renderer `AdjustableBenchRenderer` uses angle param Math.toRadians.

**Q: How to show side view vs front view?**
A: Currently orientation rotates upper body but front view is default (both arms visible overlapping). For side view, we could cull one side: e.g., draw only right side joints for sagittal movements (squat, deadlift) by checking movementFamily horizontal vs vertical. Add param `view = SIDE` in spec.

**Q: How to add gender?**
A: Extend BodyType enum and palette, adjust shoulder/hip ratio: female hips wider than shoulders reverse of male.

## Checklist for New Contributor

- [ ] Added renderer implements EquipmentRenderer
- [ ] No allocation inside draw loop
- [ ] Uses referenceSize scaling
- [ ] Validates attachment (handsAttached, floating)
- [ ] Registered in EquipmentEngine
- [ ] Layer ordering defined
- [ ] Documentation updated
- [ ] Validation suite pass rate improved

## Remaining Tech Debt (from Migration Report)

- Torso Path 2 allocations per frame — pool later
- Column recomposition per frame — isolate Canvas
- KinematicMovementFamilies still magic angles — RC20.3 will replace with motion library
- Foot direction after supine rotation may need adjustment for foot rendering forward vector

## Ready for RC20.3?

Yes — body renderer commercial, equipment modular, orientation, layered pipeline, validation present, performance 60fps theoretical. Remaining debt does not block motion library work.

