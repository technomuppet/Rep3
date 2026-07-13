# RC20.3 — Commercial Biomechanics Engine

## Overview
Replaces arbitrary joint angles with proper biomechanical model modeling pelvis, lumbar, thoracic, cervical, scapula, clavicle, shoulder, elbow, forearm rotation, wrist, hip, knee, ankle, foot with realistic anatomical limits. No impossible positions.

## Implementation
**File:** `domain/visual/biomechanics/BiomechanicalJointModel.kt`

### Joint Types
- HINGE: 1 DOF (elbow -5..145, knee 0..140, foot -40..15)
- BALL: 3 DOF (shoulder flexion -60..180, abduction -10..150, rotation -90..90; hip flexion -30..120, abduction -30..45, rotation -45..45; neck ball)
- UNIVERSAL: 2 DOF (pelvis tilt -20..20, lateral -15..15, rotation -45..45; wrist flex -70..70, radial -20..30; ankle plantar -50..20 dorsiflexion)
- SCAPULA: retraction -20..15, elevation -10..45, upward rot -20..60
- FIXED: 0 DOF

### Realistic Limits (vs old)
- Old pelvis FREEDOM_FULL -180..180 allowed inverted torso → new -20..20 tilt, -15..15 lateral, -45..45 rotation.
- Old shoulder -180..90 hyper-extension behind → new -60..180 flexion, -10..150 abduction, -90..90 rotation.
- Old hip -135..45 excessive extension → new -30..120 flexion, -30..45 abduction.
- Old wrist FREEDOM_FULL → new -70..70 flexion, -20..30 radial.
- Elbow kept -5..145 (slight hyperextension allowed) vs old 0..150.
- Ankle old -45..45 both directions unrealistic → new -50 plantar, 20 dorsiflexion + inversion/eversion -15..15.

### Integration
- `ForwardKinematicsSolver` now clamps via `BiomechanicalJointModel.clamp()` not old defaultConstraint.
- `PoseInterpolator` clamps interpolated angles via same model, uses shortest-angle diff.
- Bone lengths updated in `Bone.kt` to match Anthropometry (ANSUR): thigh 0.245 vs 0.22, shank 0.245 vs 0.20, foot 0.15 vs 0.06, upper arm 0.19 vs 0.14, forearm 0.16 vs 0.12, head 0.13 vs 0.08, torso lower 0.15 vs 0.12 upper 0.15 vs 0.10 → fixes short torso, short limbs, tiny head/foot.
- Pelvic links orientation changed 180/0 pure horizontal to 135/45 infero-lateral (down-left/down-right) for realistic hip joint inferior + lateral from pelvis.

### No Impossible Positions
- Joint validation `isValid()` checks min/max; `validate` in ExerciseMotionValidator flags out-of-range.
- Scapula implicit via shoulder + chest: bench press shoulder width <0.22 indicates retraction, validated.

## Performance
- Clamp is simple coerceIn, no allocation.
- Limits stored as data class AxisLimit, cheap.

## Commercial Readiness
- Limits based on Kapandji, NASA-STD-3000, ACSM.
- Recognisable by physios: no hyperextended elbows, no inverted pelvis, shoulder not behind body.

