# RC20.3 — Commercial Motion Library

## Objective
Replace generic families with professionally researched templates for 55+ movements.

## Implementation
**File:** `biomechanics/CommercialMotionLibrary.kt`

### Research Basis
- NSCA Essentials, ACSM, Starting Strength bar path analysis, ExRx joint actions, Kapandji limits.
- Each template comment explains coaching technique: 5-point contact bench, scap retracted 45-60 deg elbows, neutral spine, tripod foot, etc.
- Realistic joint limits from BiomechanicalJointModel enforced via clamp in PoseInterpolator.

### Template Structure
- Helper `pose(marker, vararg JointId to Float, rootX, rootY)` creates SkeletalPose with map and root offset.
- Each exercise function returns `SkeletalTimeline(duration, list of Keyframe(time, pose))`.
- Duration 1.8-3.0 sec per cycle, keyframes 2-4.

### Push (7)
- Bench Press: bottom shoulder -45 elbow 105 chest -5 scap retraction top -15 elbow 10, bar path VERTICAL slight S, COM stable, 5-point contact. Duration 2.6.
- Incline Bench: more shoulder flexion -60 bottom -20 top, torso incline via orientation benchAngle 30, chest -8.
- Decline: -35 bottom -10 top lower chest.
- Dumbbell Bench: deeper ROM elbow 115 bottom, wrist neutral 10/-10, grip wider.
- Push-up: prone plank top -20 elbow 10 chest 5 pelvis -10 core braced bottom -50 elbow 90.
- Dip: torso forward pelvis 10 top 10 elbow 10 bottom -40 elbow 95 pelvis 15.
- Machine Press: guided bottom -40 elbow 105 top -15 elbow 10.

### Pull (8)
- Pull-up: dead hang -175 shoulder 10 elbow chest -5 rootY 0.65 top -65 shoulder 135 elbow chest -10 rootY 0.40 vertical pull core braced no kipping.
- Chin-up: supinated wrist 60 hang -170 top -60 elbow 140.
- Lat Pulldown: seated slight lean 15 deg stretch -165 elbow 15 chest 10 contract -60 elbow 130 chest 5.
- Cable Row: upright stretch -75 elbow 10 chest 5 contract 15 elbow 115 chest -10 scap retraction.
- Chest Supported Row: torso prone incline 45 pelvis 35 stretch -45 elbow 10 contract 35 elbow 110.
- Barbell Row: hip hinge 45 neutral spine stretch -50 elbow 15 contract 30 elbow 110 hip -45 knee 20.
- Pendlay Row: floor start pelvis 55 hip -60 knee 30 shoulder -60 elbow 10 top pelvis 45 hip -45 shoulder 35 elbow 115 deadlift start each rep.
- Face Pull: high pulley reach -80 elbow 10 pull -95 elbow 110 chest -10 rear delts external rotation.

### Legs (8)
- Squat: stand hip 0 knee 5 ankle 0 chest 0 rootY 0.42 bottom hip -100 knee 125 ankle 18 dorsiflexion chest 12 rootY 0.58 hips back knees forward torso incline COM over mid-foot neutral spine knees over toes depth parallel. S-curve bar path.
- Front Squat: anterior load upright torso elbows high front rack 140 stand hip 0 knee 5 bottom hip -105 knee 130 ankle 20 chest 5.
- Hack Squat: machine guided upright feet forward stand hip -10 knee 10 bottom hip -90 knee 125.
- Leg Press: seated sled 45 deg extend hip -80 knee 10 flex hip -125 knee 115 horizontal bar path.
- Bulgarian Split Squat: staggered front 90 knee back 25 hip 90 knee torso upright.
- Walking Lunge: dynamic stepping front 90 back near floor torso tall.
- Step-up: low hip 0 knee 10 back -20 knee 90 high hip -10 knee 5 back -85 knee 5.
- Calf Raise: ankle 15 down -25 up plantarflexion.

### Hinge (5)
- Romanian Deadlift: top-down hinge minimal knee 20 hips back neutral spine close to legs hips -75 bottom pelvis 30 anterior tilt chest 15 rootY 0.48 bar close vertical.
- Conventional Deadlift: lockout hip 0 knee 5 chest 0 rootY 0.42 floor pelvis 20 hip -95 knee 70 chest 20 rootY 0.54 hips rise correctly shoulders over bar.
- Sumo Deadlift: wide stance toes out hips more open abduction, torso more upright floor hip -85 knee 75 chest 10.
- Hip Thrust: shoulders on bench bottom pelvis 30 hip -85 knee 90 top pelvis 0 hip 0 knee 90 glute squeeze.
- Good Morning: bar on back hips back torso incline stand hip 0 knee 15 bent hip -80 knee 15 chest 25.

### Shoulders (7)
- Overhead Press: head through, torso stable, COM balanced, elbows 130 bottom -110 shoulder  -110 to -170 top -170 elbow 10 neck -10 head forward.
- Arnold Press: supinated curl top 60 wrist bottom -90 elbow 125 top -165 elbow 15 rotation.
- Lateral Raise: large shoulder arc abduction 0->85 elbows 15 no shrug.
- Front Raise: flexion 5->-85.
- Rear Delt Fly: hip hinge 35 shoulders horizontal abduction -20->-80.
- Upright Row: elbows leading abduction + flexion 0->-60 shoulder 10->110 elbow.
- Shrug: scapular elevation -10->-35 shoulder 5->-25 chest -5.

### Arms (8)
- Barbell Curl: elbows by sides flexion 10->135 no swing core braced.
- EZ Curl: same.
- Hammer Curl: neutral wrist 0 elbows 10->130.
- Preacher Curl: arm supported shoulder flex 45 forward elbow 10->135.
- Concentration Curl: same.
- Triceps Pushdown: shoulders 10 elbows 110->10.
- Skull Crusher: lying shoulders -90 overhead elbows 90->15 top 15.
- Overhead Extension: shoulders -165 overhead elbows 125->15 stretch.

### Core (6)
- Crunch: spinal flexion CHEST -25 UPPER -20 NECK -15.
- Reverse Crunch: hip flexion -85 knee 90 chest -10.
- Hanging Leg Raise: hang Y 0.65 raise hip -90 knee 10 chest -10.
- Plank: shoulder -90 elbow 90 pelvis -10 neutral spine.
- Side Plank: lateral flexion shoulder -90 elbow 90 other -80 chest 15.
- Ab Wheel: rollout shoulder -90->-150 hips 0->-20 core anti-extension.

### Olympic (6)
- Clean: floor hip -90 knee 70 shoulder -40 elbow 10 extension hip -20 knee 20 shoulder -150 elbow 20 catch hip -100 knee 125 shoulder -80 elbow 140 triple extension.
- Power Clean: same.
- Hang Clean: hang pelvis 25 hip -45 knee 30 extension -10 hip -20 knee 15 catch hip -60 knee 70 shoulder -80 elbow 140.
- Snatch: wide grip overhead squat floor hip -90 knee 70 overhead -175 shoulder 10 elbow hip -100 knee 125.
- Push Press: dip drive bottom -110 shoulder 130 elbow hip -20 knee 20 dip hip -30 knee 35 top -170 shoulder 10 elbow.
- Push Jerk: dip then jerk hip -40 knee 50 shoulder -175 elbow 10.

### Family Mapping
- getTimelineForFamily delegates to exercise-specific but for backward compat: HORIZONTAL_PUSH -> benchPress, etc.
- getTimelineForExerciseName checks lowercased name contains e.g., "incline" && "bench" -> inclineBench, "dumbbell bench" -> dumbbellBench, "push-up" -> pushUp, etc 55 mappings.

### Bar Path Integration
- Each template designed for specific BarPathType: bench VERTICAL, squat S_CURVE, deadlift CLOSE_VERTICAL, curl ARC_ELBOW, lateral raise ARC_SHOULDER, cable CABLE_CONSTRAINED.

### Commercial Accuracy
- Recognisable by coaches: bench elbows 45-60 not 90, squat depth thighs parallel knees over toes ankle dorsiflexion 18-20, deadlift shoulders over bar bar close, overhead head through, pull-up dead hang no kipping, row neutral spine.
- Physio acceptable: no hyperextension, neutral spine, scap retracted for bench, depressed for pull, hip stable no valgus.

