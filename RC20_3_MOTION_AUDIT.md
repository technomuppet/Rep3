# RC20.3 — Commercial Motion Library & Biomechanics — Complete Motion Audit

**Fresh clone audit, no prior RC knowledge assumed, current implementation from RC20.2 commercial body renderer is present.**

## Purpose
Audit every file responsible for movement generation, timelines, pose, FK, joint constraints, interpolation, resolver, movement families, equipment attachment, playback, coaching overlays.

---

### File: `domain/visual/animation/KinematicMovementFamilies.kt`
- Purpose: Repository of parametric kinematic timelines, `getTimelineForFamily(familyId)` switch returns `SkeletalTimeline` with 2-3 keyframes for 40 families.
- Dependencies: `SkeletalPose`, `JointId`, `Offset`, `Keyframe`, `PoseMarker`
- Weaknesses: **Arbitrary joint angles** (-80, -40, 110 etc) with no biomechanical source, same flexion axis abused for abduction (lateral raise uses shoulder flexion 5->-85 which is forward raise not lateral), no scapula, no forearm rotation, no lumbar/thoracic distinction, rootPositionOffset hack moves whole skeleton breaking foot planting, timelines not data-driven, grip/stance params computed but unused, no tempo variation per joint, no bar path S-curve, no COM.
- Biomechanical errors: 
  - Squat: hip -110 knee 125 ankle -25 – ankle dorsiflexion sign invert, should be +20 dorsiflexion, hips -110 excessive vs realistic 100 flexion, torso no forward incline, COM not over mid-foot.
  - Deadlift floor: pelvis 50 hip -95 knee 70 – pelvis tilt 50 unrealistic, knee 70 too flexed for conventional deadlift (should be ~30-50), bar not close to shins.
  - Bench: shoulder -80->-40 elbow 15->110 – shoulder horizontal abduction missing, elbows flared 90 taught injury risk, no scapular retraction.
  - Lateral raise: shoulder 5->-85 – forward flexion not abduction, teaches front raise for lateral.
- Performance: Timeline allocation per call, keyframes list immutable but evaluated per frame allocates map via interpolator.
- Tech debt: 20KB file hard-coded magic numbers, no JSON asset pipeline, no validation against real coaching.
- Retain? No – interface `getTimelineForFamily` good, but implementation to be replaced by CommercialMotionLibrary.
- Verdict: **Rewrite completely** with researched templates.

### File: `domain/visual/animation/SkeletalTimeline.kt`
- Purpose: Frame-rate independent timeline, duration, List<Keyframe(time, pose, easing)>, PlaybackMode LOOP/PING_PONG/ONCE, evaluate(elapsed, playbackSpeed) finds surrounding keyframes and interpolates.
- Dependencies: `SkeletalPose`, `PoseInterpolator`, `EasingCurve`
- Weaknesses: Simple time modulo, ping-pong reversal causes velocity discontinuity if poses not symmetric, no speed variation per joint, no pause at bottom/top for tempo, playbackSpeed constant 1.0 always (spec field unused), no support for hold phases (isometric).
- Biomechanical errors: Does not enforce realistic velocity: concentric should be faster than eccentric for many lifts, but easing same both ways. PING_PONG naive reversal teaches same speed up/down.
- Performance: Finds surrounding keyframes via linear scan O(n) each frame (n<=3 so cheap), but allocates interpolated pose via PoseInterpolator.
- Tech debt: No caching of evaluated poses, no pre-bake.
- Retain? Retain interface and PlaybackMode, rewrite evaluation to support tempo phases and pre-bake.
- Verdict: **Retain structure, rewrite with bar path and stabilisation support**

### File: `domain/visual/animation/Pose.kt`
- Purpose: Immutable kinematic pose defining joint rotations for exercise state, marker enum IDLE/START/BOTTOM/MID/TOP/LOCKOUT/STRETCH/CONTRACTED, jointRotations Map<JointId, Float>, rootPositionOffset Offset.
- Dependencies: JointId, Offset
- Weaknesses: Single Float per joint insufficient for 3-DOF shoulder (needs flexion, abduction, rotation), no scapula, no forearm rotation, rootPositionOffset mixes translation with FK breaking foot planting, marker enum used but not for tempo.
- Biomechanical errors: No representation of scapular retraction, lumbar neutral, pelvic tilt – all critical for teaching correct technique. Root offset hack moves feet.
- Performance: Map allocation per pose, getRotation returns 0 if missing causing snap.
- Tech debt: Should be data class with typed fields per joint, not generic map.
- Verdict: **Rewrite** to BiomechanicalPose with multi-DOF and explicit pelvis targets.

### File: `domain/visual/animation/ForwardKinematics.kt`
- Purpose: Pure FK solver computing world coordinates from local rotations preserving bone lengths, enforcing anatomical rotation limits via clamp, hierarchical order pelvis->chest->...->feet.
- Dependencies: JointId, BoneCatalog, Offset, cos/sin
- Weaknesses: EvaluationOrder hardcoded duplicating JointId parent hierarchy, skips joints if parent missing, uses degrees not radians internal, no IK, no COM, no balance, no foot lock, scaleFactor fixed 1.0, no handling of scapula/clavicle special cases, no 3-DOF.
- Biomechanical errors: Preserves bone length correctly (good) but joint limits unrealistic (pelvis FREEDOM_FULL -180..180 allows inverted torso, shoulder -180..90 hyper-extension, wrist FREEDOM_FULL allows 180 wrist flip). No coupling: shoulder flexion >90 should involve scapular upward rotation – not modeled. No spine coupling: lumbar, thoracic move independently no rhythm.
- Performance: Allocates mutableMap 19 entries + 19 SolvedJoint per solve – 38 objects/frame – GC pressure.
- Tech debt: DEG_TO_RAD constant Math.PI/180 Float, uses kotlin.math cos/sin per joint per frame.
- Verdict: **Retain as FK component but rewrite as part of hybrid FK/IK solver** – introduce FABRIK IK for limbs, COM balancing.

### File: `domain/visual/animation/Joint.kt`
- Purpose: JointConstraint min/max clamp, JointId enum 19 joints with parentId and defaultConstraint, SolvedJoint stores local/world rotation and offsets.
- Dependencies: Offset
- Weaknesses: Single axis constraint per joint insufficient for ball joints, constraints unrealistic ranges (see above), no distinction flexion vs abduction vs rotation, pelvis full freedom, wrist full freedom.
- Biomechanical errors: Shoulder -180..90 includes impossible hyperextension -180 behind body, should be flexion 0..180, extension 0..60, abduction 0..150. Hip -135..45 excessive extension -135 vs real 30. Ankle -45..45 pitch only, no inversion/eversion. Spine -30..30 too limited for crunch.
- Performance: Enum entries, constraint clamp cheap.
- Tech debt: JointId parent hierarchy defines 19 joints but missing scapula, clavicle joints, foot ball, toe.
- Verdict: **Rewrite** to BiomechanicalJointModel with per-axis limits and 31+ joints.

### File: `domain/visual/animation/PoseInterpolator.kt`
- Purpose: Rotational pose interpolator evaluating intermediate joint angles and root positions using easing curves, guarantees 0 coordinate rubber-banding (fixes legacy).
- Dependencies: JointId, Offset, EasingCurve
- Weaknesses: Only interpolates joints where angleA !=0 or angleB !=0 – if both 0, omitted leading to snap to 0, should carry forward previous. Uses linear angle lerp not shortest path slerp (angles -170 to 170 would interpolate through 0 wrong). Root XY lerp not accounting for foot lock.
- Biomechanical errors: Linear interpolation does not mimic human acceleration: concentric faster, eccentric slower, pause at stretch. Easing same for all joints – real movement has different timing per joint (hips extend before knees in deadlift).
- Performance: Allocates mutableMap allJointIds loop, new SkeletalPose each call.
- Tech debt: EasingCurve LINEAR/EASE_IN/EASE_OUT/EASE_IN_OUT implemented with quad – okay but no cubic bezier, no per-joint easing.
- Verdict: **Rewrite** to preserve all joints, use shortest-angle, per-joint easing, add COM-aware root interpolation.

### File: `domain/visual/animation/Bone.kt`
- Purpose: Immutable bone segment connecting parent to child, normalized length relative to height, defaultOrientationDegrees, renderThickness, BoneCatalog 18 bones.
- Dependencies: JointId
- Weaknesses: renderThickness mixes rendering with domain, pelvic links horizontal orientation 180/0 unrealistic (should be inferior), thickness same for both sides, no left/right length difference, no mass or COM fraction.
- Biomechanical errors: Proportions from RC20.2 Anthropometry now corrects rendering but BoneCatalog lengths still used for FK world positions – should match Anthropometry totalHeight. Currently thigh 0.22 vs Anthropometry 0.245 slight mismatch. Foot bone 0.06 horizontal too small vs real 0.15. Clavicle 0.09 each okay but upper arm 0.14 vs real 0.19 short.
- Performance: ALL_BONES list static, getBoneToChild find linear search O(n) per joint per solve (18*19) – cheap but could be map.
- Tech debt: scaleLength method unused in new commercial renderer, thickness unused.
- Verdict: **Rewrite** BoneCatalog to match Anthropometry, add mass fractions for COM, remove renderThickness, add method to get bone mass.

### File: `domain/visual/resolver/ExerciseVisualResolver.kt`
- Purpose: Pure domain translator Exercise -> ExerciseVisualSpec, resolves equipmentType via string contains, movement family via pattern/name/category complex if-else, supportType/benchAngle, bodyOrientation, gripType, stanceType, ROM, anatomy.
- Dependencies: MovementFamily, EquipmentType, BodyOrientation, SupportType etc.
- Weaknesses: 150 lines if-else fragile string contains, grip detection only name.contains("wide grip") misses metadata, benchAngle hard-coded 30/-15 not from data, parameters map gripWidthFactor 1.3/0.7 and stanceWidthFactor unused in renderer (dead code), no handling of unilateral, no tempo.
- Biomechanical errors: Does not infer realistic joint limits or tempo from exercise, does not differentiate Dumbbell Bench vs Barbell Bench motion (should have different bar path and grip width), does not differentiate front squat vs back squat torso angle.
- Performance: parseCsv per resolve allocates sets, called per exercise per frame via remember caching okay.
- Tech debt: High cyclomatic complexity, should be split into rule engine.
- Verdict: **Retain concept, refactor with typed metadata, wire gripWidthFactor to IK hand targets**

### File: `domain/visual/registry/MovementFamily.kt` + `MovementRegistry.kt`
- Purpose: Standardized 41 families HORIZONTAL_PUSH, INCLINE_PUSH, DECLINE_PUSH, VERTICAL_PUSH, HORIZONTAL_PULL, CABLE_ROW, PULL_UP, LAT_PULLDOWN, DEADLIFT etc with displayName, category, description, metadata. Registry single source truth map id->metadata.
- Dependencies: None
- Weaknesses: Some families overlapping (FRONT_SQUAT vs SQUAT, HACK_SQUAT same timeline), no distinction Dumbbell vs Barbell bench, no tempo, no ROM, GENERIC_UNMAPPED fallback hides unmapped.
- Biomechanical errors: Categories PUSH/PULL/LEGS/HINGE/CORE/ISOLATION okay but does not define joint actions per family (e.g., Horizontal Push = shoulder horizontal adduction + elbow extension + scapular protraction).
- Performance: Simple map, cheap.
- Tech debt: No versioning for motion templates.
- Verdict: **Retain registry, extend with biomechanical action definitions** – add joint actions list per family for new motion library.

### File: `domain/visual/animation/EquipmentAnchoring.kt` (old) + `attachment/EquipmentAttachmentSolver.kt` + `equipment/EquipmentRenderers.kt` (new RC20.2)
- Purpose: Old anchoring averages wrists to get axis midpoint, fixed bar half width. New attachment solver validates hands attached, feet planted, bench contact. EquipmentRenderers 22 independent renderers with validation.
- Dependencies: SolvedSkeleton, JointId, EquipmentType, DrawScope
- Weaknesses (old): Averaging wrists fails dumbbells, kettlebell below wrists, cable pulley, fixed pull-up bar moving with wrists bug fixed in RC20.2 new renderer. New RC20.2 renderers good but still use wrist positions directly from FK which may be inaccurate if motion arbitrary.
- Biomechanical errors: Old bar path horizontal only, no vertical arc, no scapular movement affecting grip. New barbell renderer draws bar at wrist midpoint but should follow bar path engine (vertical path for bench nearly vertical, squat S-curve, curl arc around elbow).
- Performance: Old computes hypot per frame, new per equipment similar.
- Tech debt: Old file should be removed after migration, new renderers good.
- Verdict: Old **Remove**, new **Retain and extend with BarPathEngine** – hand targets should follow equipment trajectory, not vice versa.

### File: `ui/exercise/ExerciseAnimationView.kt`
- Purpose: Facade choosing SkeletalEngine vs Legacy, LaunchedEffect withFrameNanos loop deltaSeconds per frame, Canvas 220dp drawing via SkeletalRenderer.drawCommercial.
- Dependencies: VisualEngineAdapter, ForwardKinematicsSolver, SkeletalRenderer, MaterialTheme
- Weaknesses: Recomposition per frame via mutableFloatStateOf elapsedSeconds triggers Column recomposition, Row controls also recompose, no lifecycle pause, no caching of timeline evaluation, no COM or stabilisation overlay.
- Biomechanical errors: No coaching overlay showing bar path, joint angles, COM, foot pressure, scapular position – teaching value low even if motion correct.
- Performance: Per-frame allocations FK+interpolator, recomposition expensive.
- Tech debt: LegacyExerciseAnimationView still present fallback.
- Verdict: **Retain facade but refactor to isolate Canvas recomposition, add coaching overlay for COM and bar path**

### File: `domain/visual/body/HumanBodyRenderer.kt` + `VolumetricRenderer.kt` + `Anthropometry.kt` (RC20.2)
- Purpose: Commercial body rendering volumetric capsules, not pipe lines, proper proportions from ANSUR.
- Dependencies: SolvedSkeleton, DrawScope
- Weaknesses: Rendering is good but motion drives it via FK only – if motion is wrong, body looks wrong. No stabilisation visual (core bracing). ReferenceSize min(width,height)*0.32 good.
- Biomechanical errors: None in renderer itself, but depends on motion accuracy.
- Performance: 2 Path allocations per frame for torso, 10 capsules = 40 draw ops, 60 total – good 60fps theoretical. Could be pooled.
- Tech debt: Path allocation per frame could be cached.
- Verdict: **Retain** – do NOT redesign renderer unless required for motion (RC20.3 says not unless required). Renderer supports realistic motion.

### File: `domain/library/ExerciseCoach.kt` + `ui/exercise/ExerciseCoachingSections.kt`
- Purpose: Generates coaching text description, purpose, steps (setup equipment-specific), cues, mistakes, breathing, tempo, ROM, safety, feel/notFeel, reassurance based on ExercisePattern + EquipmentKind + muscles.
- Dependencies: ExercisePattern, EquipmentKind
- Weaknesses: Text coaching good but not synchronized with animation – steps not linked to timeline keyframes, no visual sync showing "squeeze at top" when animation at top.
- Biomechanical errors: Some coaching text may be accurate but animation contradictory (e.g., text says "keep shoulders pinned back" but animation shows elbows flared 90).
- Performance: Pure offline text generation cheap.
- Tech debt: None major.
- Verdict: **Retain**, enhance with motion-synced coaching overlay in RC20.3.

### File: `domain/visual/validation/RenderingValidationSuite.kt`
- Purpose: Validates equipment never floats, hands attached, feet planted, bench contact, layer ordering, clipping, penetration.
- Dependencies: AttachmentSolver, LayeredRenderingPipeline, EquipmentEngine
- Weaknesses: Simple checks Y within 0.75..0.98 for feet planted, but after orientation -90 supine Y check fails, needs orientation-aware validation. No joint limit validation, no COM validation, no bar path validation.
- Biomechanical errors: Does not validate joint limits, COM over base, bar path verticality, ROM.
- Performance: Runs per exercise per frame if used in loop – but intended offline validation.
- Verdict: **Retain and extend to ExerciseMotionValidator** – add joint limit, COM, bar path checks.

## Summary Table Motion Audit

| File | Purpose | Weakness | Biomech Error | Perf | Action |
|------|---------|----------|---------------|------|--------|
| KinematicMovementFamilies | Timelines | Arbitrary angles same axis | Lateral raise as front raise, squat no COM | Map alloc | Rewrite commercial |
| SkeletalTimeline | Evaluate time->pose | PING_PONG discontinuity, speed constant | Conc eccentric same speed | Linear scan | Retain interface, rewrite evaluation |
| Pose | Joint rotations map | Single float per joint, root hack | No scapula, no forearm rot | Map alloc snap | Rewrite biomech pose |
| ForwardKinematics | World positions preserving length | No IK, no foot lock, hardcoded order | Pelvis 180, shoulder -180 hyperext | 38 obj/frame | Rewrite hybrid FK/IK |
| Joint | Constraint + hierarchy | Single axis, unrealistic ranges | Shoulder -180, hip -135, wrist full | Cheap | Rewrite multi-DOF limits |
| PoseInterpolator | Angle + root lerp | Omits joints both 0 -> snap | No per-joint timing, no shortest path | Map alloc | Rewrite preserve all, shortest angle |
| Bone | Segment length + orientation | Thickness in domain, horizontal pelvis links | Thigh 0.22 vs 0.245, foot 0.06 small | Linear search | Rewrite match anthropometry + mass |
| ExerciseVisualResolver | Exercise->Spec | String contains fragile, dead params | No grip->hand target wiring | Csv alloc | Refactor wiring |
| MovementFamily/Registry | 41 families | Overlap, no joint actions | No action definition | Cheap | Retain + extend actions |
| EquipmentAnchoring old | Avg wrists line | Generic placeholder floating | Bar horizontal only | hypot | Remove |
| AttachmentSolver new | Validates attachment | Simple Y checks | No COM, no bar path | cheap | Retain + extend |
| EquipmentRenderers 22 | Independent per type | Uses FK wrist directly | Bar path not vertical S-curve | draw ops | Retain + add BarPathEngine |
| ExerciseAnimationView | Playback facade | Recompose per frame | No coaching overlay bar path | recompose | Retain refactor Canvas isolation |
| HumanBodyRenderer | Volumetric body | Path alloc 2/frame | Depends on motion | 60 ops | Retain |
| ExerciseCoach | Text coaching | Not sync with animation | Animation contradictory | cheap | Retain + sync |
| RenderingValidationSuite | Validates floats etc | Simple checks, no joint limit | No COM, no ROM | offline | Extend to motion validator |

## Critical Biomechanical Gaps

- No realistic joint limits multi-axis
- No scapula, clavicle, forearm rotation, foot articulation
- No IK foot locking, hand targets follow equipment trajectory (currently opposite)
- No COM balancing (squat hips back torso incline, deadlift shoulders over bar)
- No bar paths (bench nearly vertical, squat S-curve, deadlift close to shins, curl arc around elbow, lateral raise large shoulder arc, cable constrained by pulley)
- No stabilisation (core bracing, scapular retraction, neutral spine, hip stability)
- No exercise validation for joint limits, foot placement, hand placement, posture, ROM, coaching correctness

All to be addressed in RC20.3 implementation.

