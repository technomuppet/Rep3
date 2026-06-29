# Lightweight Animation Engine (Sprint 13, Phase 5)

## Principle
No embedded videos or GIFs. Each exercise's movement is shown as a stick-figure
animation generated from keyframes in code.

## Domain (domain/library/ExerciseAnimation.kt)
- Pose: normalised (0..1) joint positions (head, shoulder, elbow, hand, hip, knee,
  foot) plus an optional implement point. Resolution-independent.
- AnimationClip: an ordered list of <= 8 keyframes + a cycle time in millis.
- clip(exercise): returns a clip chosen by movement family (squat, hinge, press,
  pull, raise, calf, core, carry, conditioning). Bodyweight exercises omit the
  implement. Every one of the 516 exercises produces a valid clip (2-8 frames,
  500-4000 ms cycle).
- Footprint: each clip is a handful of small structs (< 5 KB per exercise target,
  comfortably met) generated on demand - nothing stored on disk.

## Renderer (ui/exercise/ExerciseAnimationView.kt)
- A Compose Canvas that linearly interpolates between keyframes driven by
  rememberInfiniteTransition + animateFloat (APIs already used elsewhere in the
  app). Draws the limbs, joints and implement; loops smoothly.
- Reusable for EVERY exercise via a single entry point; no per-exercise renderer.

## Accessibility / performance
- contentDescription "Animated demonstration of <name>" for screen readers.
- All frames generated locally; no network, no image decode, no caching needed.
