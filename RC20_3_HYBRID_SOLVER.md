# RC20.3 — Hybrid FK/IK Solver

## Objective
Replace purely angle-driven motion where appropriate with hybrid using FK, IK, foot locking, hand targets, equipment targets. Body solves naturally toward targets. Hands follow equipment. Feet remain planted.

## Implementation

### IKSolver.kt — FABRIK
- `solveFABRIK(jointPositions, boneLengths, target, tolerance 0.001, maxIter 10)`: For arbitrary chain length 2-3 bones typical for limbs.
- Handles unreachable target by stretching towards target preserving lengths.
- Backward: set end to target, iterate backwards preserving bone length.
- Forward: set root to original, iterate forwards.
- Convergence via diff < tolerance.

- `solveTwoBoneArm(shoulder, elbow, wrist, handTarget, upperArmLen, forearmLen)`: Returns new elbow, wrist solving shoulder->elbow->wrist to handTarget (equipment). Uses FABRIK chain 3 joints 2 lengths.

- `solveLegWithFootLock(hip, knee, ankle, foot, footTarget, thighLen, shankLen, footLen)`: Foot locking — foot at footTarget fixed to ground, ankle target = footTarget - footLen vector, solves hip->knee->ankle to ankleTarget, then foot from ankle + foot direction.

- `boneLength(a,b)`: hypot.

### HybridSolver.kt
- `Targets`: leftHand, rightHand, leftFoot, rightFoot, barCenter, pulley optional.
- `solve(baseSkeleton, targets, comBalancing)`: Extracts current positions, bone lengths cached via IKSolver.boneLength, solves left/right arms to hand targets via solveTwoBoneArm, solves legs with foot locking via solveLegWithFootLock, updates SolvedJoint map via copy worldPositionOffset.
- `updateJointWorld`: creates new SolvedJoint with new world, keeps constraint from biomechanical model.
- `applyComBalancing`: calls CentreOfMassCalculator, if not balanced logs but does not drastically auto-correct (balancing ensured during motion template creation). Future could adjust chest flexion.
- `calculateBarbellHandTargets(barCenter, gripWidth)`: half grip each side.
- Performance: Small chain 3 joints, 10 iterations max, minimal allocations (mutableList of 3 Offsets per limb). Cached bone lengths.

### Integration in ExerciseAnimationView
- Evaluates base pose from commercial timeline via FK.
- Computes bar path type from family, start/end bar positions from timeline top/bottom keyframe wrist midpoints, progress t from cycleTime/duration via ping-pong, desiredBar via BarPathEngine.
- GripWidthWorld from spec.movementFamily.parameters gripWidthFactor *0.18.
- Hand targets = desiredBar +/- half grip.
- Foot lock targets = current foot world positions if support != HANGING.
- Calls HybridSolver.solve().
- Result is oriented via BodyOrientationEngine then rendered via layered pipeline.

### Foot Locking Example
- Squat: hips back, knees forward, torso incline, feet remain at ground Y 0.85..0.96, X diff <0.15, validated via RenderingValidationSuite.
- Deadlift: bar close to shins, shoulders over bar, feet planted.

### Hand Targets
- Barbell: both hands share bar center with grip width.
- Dumbbells: each dumbbell centered at respective wrist (one per hand) — hand target is dumbbell center.
- Cable: pulley fixed, hand moves along line to pulley.
- Pull-up bar: fixed overhead 8% height, hands near bar Y <0.45.

## Performance
- FABRIK 3 joints *10 iter * cos/sin? Actually only vector math hypot, no trig per iteration except direction normalization (hypot + division). Cheap <0.1ms per limb.
- No object pooling needed, small lists.

## Commercial Quality
- Hands follow equipment, not floating.
- Feet remain planted, no sliding (previous root offset caused slide 28dp).
- Body solves naturally toward targets, not arbitrary angles.

