# RC20.3 — Centre of Mass System

## Objective
Implement COM calculations, body naturally balances, examples squat hips back knees forward torso incline COM over mid-foot, deadlift bar close hips rise correctly shoulders over bar, overhead press head through torso stable COM balanced.

## Implementation
**File:** `biomechanics/CentreOfMassCalculator.kt`

### Segment Mass Fractions (Dempster)
- Head+neck 8.1%, upper trunk 16%, lower trunk 27%, upper arm 2.8% each, forearm 1.6% each, hand 0.6% each, thigh 10% each, shank 4.65% each, foot 1.45% each.

### COM Fraction From Parent
- Thigh COM 43.3% from hip, shank 43.3% from knee, upper arm 43.6% from shoulder, forearm 43% from elbow, etc.

### Calculation
```
weightedX += comX * massFraction
com = weighted / totalMass
midFoot = (leftFoot + rightFoot)/2
dist = hypot(com - midFoot)
isBalanced = abs(com.x - midFoot.x) <0.15
```

### Balancing Corrections
- `calculateSquatTorsoCorrection(skeleton, targetMidFootX 0.5)`: errorX = com.x - target, correction = -errorX * (100/0.15) clamped -20..30 degrees. If COM behind, lean forward.
- `calculateDeadliftShoulderOverBarCorrection(skeleton, barX)`: midShoulderX = (leftShoulder+rightShoulder)/2, error = midShoulder - barX, correction -error*80.

### Integration
- Evaluated in `ExerciseAnimationView` after hybrid solve: ComResult comWorld, midFootWorld, comOverMidFootDistance, isBalanced.
- Drawn as coaching overlay: green dot COM, yellow dot mid-foot, red line if not balanced.
- StabilisationEngine uses COM result for footPressureBalanced.
- Motion templates designed with COM in mind: squat bottom chest 12 deg forward incline, hips -100, knees 125, ankle 18 dorsiflexion ensures COM over mid-foot; deadlift floor pelvis 20 hip -95 knee 70 chest 20 ensures shoulders over bar.
- HybridSolver.applyComBalancing currently logs but does not auto-correct drastically; balancing ensured during template creation to avoid instability. Future could adjust chest flexion.

### Commercial Examples
- Squat: hips move back (hip -100), knees forward (ankle 18 dorsiflexion), torso incline 12 deg, COM over mid-foot.
- Deadlift: bar stays close shins X variance <0.04, hips rise correctly (knee 70->5, hip -95->0), shoulders remain over bar (shoulder X near bar X).
- Overhead press: head moves through (neck -10 at top), torso stable chest 0, COM balanced.

### Performance
- 15 segments loop, 15 hypot? No, just lerp and multiply, cheap.
- No allocations except ComResult data class.

### Validation
- ExerciseMotionValidator checks COM.isBalanced for standing lifts, fail if not balanced (except hanging).
- RenderingValidationSuite already checks feet planted.

