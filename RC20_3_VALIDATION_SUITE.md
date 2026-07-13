# RC20.3 — Exercise Validation Suite

## Objective
Audit every exercise, verify movement accuracy, joint limits, equipment attachment, foot placement, hand placement, body posture, ROM, coaching correctness. Generate validation report.

## Implementation
**File:** `biomechanics/ExerciseMotionValidator.kt`

### Checks per Exercise (9 checks)
1. **JointLimits:** For each joint in skeleton, localRotation within BiomechanicalJointModel realistic limits via isValid(). Fail if out of -20..20 pelvis, -60..180 shoulder, etc. Reports joint name + rot + limits.
2. **EquipmentAttachment:** GripWidth = hypot(rightWrist-leftWrist). For bodyweight true else 0.05..0.8. Ensures hands grip correctly, never floats.
3. **FootPlacement:** leftFoot.y, rightFoot.y in 0.3..0.98, Y diff <0.15 (feet not one floating), ensures feet planted, tripod.
4. **HandPlacement:** Wrist y in 0.0..0.9 (within canvas), not outside.
5. **Posture:** Uses StabilisationEngine: neutralSpine && hipStable. Checks knee over ankle <0.08 no valgus, chest over pelvis X <0.15 neutral spine.
6. **ROM:** For squat family knee 80..140 depth, deadlift knee 10..80, curl elbow 10..140. Ensures realistic range of motion matches exercise.
7. **Coaching:** coreBraced && balanced from stabilisation.
8. **COM:** CentreOfMassCalculator.isBalanced true || family PULL_UP/HANGING (hanging COM not over mid-foot ok). Checks COM over mid-foot dist.
9. **BarPath:** Wrist-chest dist 0.05..0.4 for bench ensures bar not too far, not penetrating chest.

### Report Structure
- total, passed, failed, passRate, isCommercialReady >=85% && jointLimitFails empty && equipmentFails empty
- Lists: jointLimitFails, equipmentFails, footPlacementFails, handPlacementFails, postureFails, romFails, coachingFails, comFails, barPathFails
- allChecks detailed.

### Text Report
`generateReportText()` prints:
```
=== RC20.3 Exercise Validation Report ===
Total Checks: ...
Passed: ...
Failed: ...
Pass Rate: 92.5%
Commercial Ready: true
--- Joint Limits Fails ---
...
```

### Integration
- Can be run in unit test with list of specs + skeletons (sample pose bottom).
- Requires toScreen lambda (width*x, height*y) for validation that uses screen? Actually most checks use world positions normalized 0..1, so toScreen not strictly needed but passed for consistency with RenderingValidationSuite.
- Combines with RenderingValidationSuite (RC20.2) which checks equipment floats, hands attached, feet planted, bench contact, layer order, clipping, penetration. Now extended with joint limits, COM, bar path, posture, ROM, coaching.

### Commercial Readiness Criteria
- PassRate >=85% (allows minor posture deviations for some families)
- JointLimitFails empty (no impossible positions — critical for physio)
- EquipmentFails empty (never floats)

### Example Failure Handling
- If squat knee 125 but ankle  -25 old unrealistic dorsiflexion sign invert would fail joint limits (ankle -25 out of -50..20? Actually -25 is within -50..20 plantar, so passes, but old -25 was sign invert). New ankle 18 dorsiflexion passes.
- If bench elbows flared 90 (shoulder -80) old would pass old limit -180..90 but new shoulder limit -60..180 for flexion, but horizontal abduction -10..150? -80 is flexion? Actually shoulder -80 is within -60..180, okay. But coaching check scap retraction would fail if shoulderWidth 0.25 >0.22, message "Retract scapula".

### Future
- Could add tempo validation: concentric faster than eccentric.
- Could add bar path validation using BarPathEngine.validateBarPath with sampled positions over full cycle.

