# RC20.3 — Equipment Trajectory Engine (Bar Paths)

## Objective
Implement correct equipment trajectories: bench nearly vertical, squat slight S-curve, deadlift bar close to shins, curl arc around elbow, lateral raise large shoulder arc, cable constrained by pulley.

## Implementation
**File:** `biomechanics/BarPathEngine.kt`

### BarPathType Enum
- VERTICAL: bench, overhead press
- S_CURVE: squat, front squat
- CLOSE_VERTICAL: deadlift, sumo
- ARC_ELBOW: curl, skull crusher, overhead extension, pushdown (arc around elbow)
- ARC_SHOULDER: lateral raise, front raise, rear delt fly, upright row (large shoulder arc)
- CABLE_CONSTRAINED: cable row, lat pulldown, face pull, pullover, straight bar, rope
- HORIZONTAL: leg press sled
- FIXED: plank, carry, isometric

### Calculation
`calculateBarPosition(pathType, start, end, progress 0..1, pulley optional)` returns desired bar center.

- VERTICAL: Y lerp start->end, X = lerp + sin(t*PI)*0.02 * dir (slight S small horizontal). Ensures nearly vertical X variance <0.06.
- S_CURVE: X offset sin(t*PI)*0.04 * (-1 if t<0.5 else 1) creating S shape for squat hips back knees forward.
- CLOSE_VERTICAL: X almost constant over mid-foot, Y vertical, add clearance sin(t*PI)*0.015 mid for knee clearance.
- ARC_ELBOW: Quadratic bezier: mid = (start+end)/2 -0.08 Y, bezier formula omt²*start +2*omt*t*mid + t²*end gives arc around elbow.
- ARC_SHOULDER: Circular: angleSpan 80 deg, angle = -90 deg + t*80, radius 0.18 normalized, pivot = (start+end)/2 + offset, x = pivot.x + cos(angle)*radius, y = pivot.y + sin(angle)*radius large arc.
- CABLE_CONSTRAINED: Straight line start->end (could be improved to line pulley->hand). If pulley provided, ensures distance to pulley decreases/increases naturally. For now lerp.
- HORIZONTAL: X lerp only, Y constant.
- FIXED: start.

### Validation
`validateBarPath(pathType, positions)` checks X variance and Y range:
- VERTICAL: xVar <0.06 && yRange >0.1 ok
- S_CURVE: xVar 0.02..0.08 && yRange >0.12
- CLOSE_VERTICAL: xVar <0.04 && yRange >0.15
- ARC_ELBOW: yRange >0.08 && xVar <0.15
- ARC_SHOULDER: yRange >0.1 && xVar >0.08

### Integration
- ExerciseAnimationView computes topBar and bottomBar from timeline top/bottom keyframe wrist midpoints (evaluates timeline at 0 and duration*0.5).
- Computes segment progress t from cycleTime/duration ping-pong 0->1->0.
- Calls BarPathEngine.calculateBarPosition to get desiredBar.
- GripWidth from spec parameters gripWidthFactor*0.18.
- LeftHandTarget = desiredBar.x - halfGrip, Right = +halfGrip.
- Hand targets fed to HybridSolver to solve arms via FABRIK.
- For dumbbells, each hand target is its own dumbbell center (not shared bar).
- For cable, pulley anchor top 5% or bottom 95% depending on support, cable line tension drawn in equipment renderer.

### Commercial Examples
- Bench: nearly vertical path, X variance <0.05, slight S from chest lower sternum to over shoulders.
- Squat: S-curve slight back at bottom (hips back) then forward, X variance 0.02..0.08.
- Deadlift: close to shins X variance <0.04, vertical Y.
- Curl: arc around elbow radius ~0.12, Y range >0.08.
- Lateral raise: large shoulder arc 80 deg radius 0.18.

### Performance
- No allocations, only Float math sin, cos, hypot, lerp.
- Called per frame once per exercise, cheap.

