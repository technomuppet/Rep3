# Known Limitations (Sprint 13.1)

## Verification environment
- This environment has NO Android SDK, NO emulator, NO device, and only JDK 11.
  Therefore NO part of the app has been built into an APK or run here. All
  verification is source-level (static analysis + standalone kotlinc 1.9.22
  domain compilation + JVM logic tests). Nothing in this sprint claims on-device
  behaviour.
- The single standing release gate remains `:app:assembleDebug` (and a signed
  release build) plus on-device QA, to be performed by a human / CI on an SDK host.

## Feature limitations (by design)
- Muscle body diagram and movement animation are CODE-DRAWN approximations
  (stick figure + region rectangles), intentionally - no GIFs/photos/videos, ~0 KB
  assets. They illustrate the movement and target regions; they are not anatomical
  renders.
- Coaching content is GENERATED from movement family + equipment + difficulty, so
  it is correct and consistent but generic per family rather than hand-authored
  per exercise. This is the deliberate zero-storage, 516-exercise-scalable design.
- "Why this exercise" recovery line only appears when the Recovery Centre has
  enough data to report a FRESH muscle; with no history it shows goal/experience/
  recommendation reasoning only (never invents recovery data).
- 10 of 49 advanced exercises have no "easier alternative" because they are
  already the easiest in their movement family; the Alternatives section then
  explains why (never blank).

## Deferred / out of scope
- The debug-only content assertions in ExerciseCoach run under -ea / Android
  debuggable builds; release builds skip them (intended).
- Per-exercise hand-authored cues/animations, real anatomical SVGs, and localisation
  are not in scope for this offline, zero-storage approach.
