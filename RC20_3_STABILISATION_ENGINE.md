# RC20.3 — Body Stabilisation System

## Objective
Implement automatic stabilisation: core bracing, scapular retraction, shoulder depression, neutral spine, hip stability, foot pressure, balance correction.

## Implementation
**File:** `biomechanics/StabilisationEngine.kt`

### StabilisationCues Data
- coreBraced Boolean
- scapulaRetracted Boolean
- shoulderDepressed Boolean
- neutralSpine Boolean
- hipStable Boolean
- footPressureBalanced Boolean
- balanced Boolean (all)
- messages List<String>

### Evaluation Logic
- **Core bracing:** pelvis tilt within realistic, pelvis.y - chest.y <0.3, neutral spine. If not, "Brace core, neutral pelvis".
- **Scapular retraction:** For bench families HORIZONTAL_PUSH etc shoulderWidth <0.22 indicates retracted (narrower than max). If not, "Retract scapula".
- **Shoulder depression:** For pull-up, lat pulldown, deadlift leftShoulder.y > upperChest.y -0.05 not shrugged. If not, "Depress shoulders".
- **Neutral spine:** abs(chest.x - pelvis.x) <0.15 for standing. If not, "Maintain neutral spine".
- **Hip stability:** Knee over toe via leftKnee.x - leftAnkle.x absolute <0.08 and right same. If not, "Knees tracking over toes, avoid valgus".
- **Foot pressure:** COM.isBalanced from CentreOfMassCalculator (COM x within 0.15 of mid-foot). If not, "COM over mid-foot, tripod foot".
- **Balanced:** neutralSpine && hipStable && footPressureBalanced.

### Stabilise Method
- Currently returns same skeleton with cues (no auto-correction to avoid instability). Future could adjust chest flexion slightly via torso inclination correction from COM calculator.
- `stabilise(skeleton, familyId)` -> Pair(skeleton, cues)

### Coaching Overlay
- `generateCoachingOverlay(cues)` returns messages list.
- In ExerciseAnimationView, overlay draws COM green dot, mid-foot yellow dot, red line if not balanced, bar path trace blue dots.

### Commercial Use
- Bench: scap retracted & depressed, 5-point contact, core braced, feet flat tripod, neutral spine slight arch.
- Squat: neutral spine, knees tracking over toes no valgus, tripod foot, COM over mid-foot, core braced.
- Deadlift: neutral spine, lats engaged (shoulder depressed), core braced, foot pressure mid-foot.
- Pull-up: depressed shoulders, core braced, no kipping, scap depressed at bottom.

### Integration
- Called per frame after hybrid solve, comResult and stabilisation computed.
- Messages could be shown in UI as coaching cues synced to animation (future: show "Retract scapula" when bench bottom).

### Performance
- Cheap: few hypot, abs checks, COM calculation already done.

