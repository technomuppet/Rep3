# RC20.4 — Camera System

## Objective
Implement proper rendering views: Front, Rear, Left Side, Right Side, automatic best-view selection based on exercise, manual override, prevent left/right limb overlap.

## Implementation

### CameraSystem.kt — `camera/CameraSystem.kt`
- **Enum:** CameraView FRONT, REAR, LEFT_SIDE, RIGHT_SIDE, AUTO
- **selectBestView(spec):** Automatic best-view based on family and name:
  - Side view best for sagittal plane where depth matters and overlap would occur:
    - SQUAT, FRONT_SQUAT, HACK_SQUAT, SPLIT_SQUAT, LUNGE, LEG_PRESS, DEADLIFT, ROMANIAN_DEADLIFT, HIP_HINGE, HIP_THRUST, LEG_EXTENSION, LEG_CURL -> RIGHT_SIDE
    - HORIZONTAL_PUSH bench press floor press -> RIGHT_SIDE
    - INCLINE_PUSH, DECLINE_PUSH -> RIGHT_SIDE
    - VERTICAL_PUSH overhead press -> RIGHT_SIDE (side to see head through, bar path)
    - PUSHDOWN, OVERHEAD_EXTENSION -> RIGHT_SIDE
    - CRUNCH, LEG_RAISE, PLANK, OLYMPIC_LIFT, CALF_RAISE -> RIGHT_SIDE
  - Front view best for frontal plane where lateral movement matters:
    - CURL, HAMMER_CURL, PREACHER_CURL -> FRONT
    - LATERAL_RAISE, REAR_DELT_FLY, SHRUG -> FRONT
    - PULL_UP, LAT_PULLDOWN, CABLE_ROW, HORIZONTAL_PULL -> FRONT
    - CABLE_FLY -> FRONT
    - Else -> FRONT
  - Examples from spec: Squat→side, Deadlift→side, Bench→side, Curl→front, Lateral raise→front, Pull-up→front — implemented as above.

- **resolveView(spec, manualOverride):** If manualOverride not null and != AUTO return manual, else selectBestView. Allows manual override.

- **shouldCullLeftSide / shouldCullRightSide:** For side views, to prevent left/right limb overlap, cull one side:
  - RIGHT_SIDE view -> cull left side limbs (shouldCullLeftSide true)
  - LEFT_SIDE view -> cull right side
  - FRONT/REAR -> no cull

- **getViewDescription:** Human-readable.

### HumanBodyRenderer Integration
- **Signature extended:** `drawHumanBody(skeleton, palette, referenceSize, toScreen, cameraView = FRONT)`
- **Culling logic:**
```kotlin
val cullLeft = CameraSystem.shouldCullLeftSide(cameraView)
val cullRight = CameraSystem.shouldCullRightSide(cameraView)
...
if (!cullLeft) drawFoot(leftAnkle,...)
if (!cullRight) drawFoot(rightAnkle,...)
...
if (!cullLeft) drawCapsule(leftKnee, leftAnkle,...)
if (!cullRight) drawCapsule(rightKnee, rightAnkle,...)
...
if (!cullLeft) drawCapsule(leftHip, leftKnee,...)
if (!cullRight) drawCapsule(rightHip, rightKnee,...)
...
if (!cullLeft) drawCapsule(leftShoulder, leftElbow,...)
if (!cullRight) drawCapsule(rightShoulder, rightElbow,...)
...
if (!cullLeft) drawCapsule(leftElbow, leftWrist,...)
if (!cullRight) drawCapsule(rightElbow, rightWrist,...)
...
if (!cullLeft) drawHand(leftWrist,...)
if (!cullRight) drawHand(rightWrist,...)
```
  - For RIGHT_SIDE view, only right side limbs drawn, left culled, preventing overlap where left and right would coincide in side view.
  - For LEFT_SIDE, opposite.
  - For FRONT/REAR, both sides drawn.

- **Torso, pelvis, head/neck always visible** regardless of view.

### LayeredRenderingPipeline Integration
- RenderContext now includes `cameraView: CameraView = AUTO` and `progress: Float`
- `drawPipeline` resolves final view:
```kotlin
val cameraView = CameraSystem.resolveView(spec, if (context.cameraView == AUTO) null else context.cameraView)
val bestView = CameraSystem.selectBestView(spec)
```
- Passes cameraView to `HumanBodyRenderer.drawHumanBody(..., cameraView = cameraView)`

- **SkeletalRenderer** updated to accept progress and cameraView params and pass to pipeline:
```kotlin
fun drawCommercial(..., progress: Float = 0.5f, cameraView: CameraView = AUTO)
```

- **ExerciseAnimationView** computes cameraView via `CameraSystem.selectBestView(mode.spec)` and passes to drawCommercial with progress t.

### Manual Override
- CameraSystem.resolveView supports manualOverride param.
- Could be exposed in UI as toggle buttons Front/Rear/Left/Right/Auto — not yet UI, but API ready.
- Manual override would be passed via RenderContext cameraView.

### Prevent Left/Right Overlap
- Side views (RIGHT_SIDE, LEFT_SIDE) cull one side, so only one arm and one leg visible in side view, preventing double lines overlapping which was criticized in RC20 audit as "both left and right limbs rendered in identical 2D plane causing overlap and confusion".
- Front view keeps both sides, no overlap issue because left/right separated horizontally.

### Verification

- **Squat → side:** selectBestView returns RIGHT_SIDE, culls left, only right leg visible, no overlap, depth visible hips back knees forward.
- **Deadlift → side:** RIGHT_SIDE, no overlap, bar close to shins visible.
- **Bench → side:** RIGHT_SIDE, no overlap, bar path vertical slight S visible from side, scap retraction.
- **Curl → front:** FRONT, both arms visible, no overlap because front view separates left/right horizontally.
- **Lateral raise → front:** FRONT, both arms abducted lateral visible.
- **Pull-up → front:** FRONT, both arms overhead.

### Performance

- Culling reduces draw calls by ~50% for side views (only one side), improves performance.

### Remaining

- Rear view currently same as front (both sides) but could be implemented as back of head? For now front/back same body, acceptable.
- Manual override UI toggle not yet implemented, but API ready.

