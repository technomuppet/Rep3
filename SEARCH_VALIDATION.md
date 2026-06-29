# Search Validation (Sprint 13 hardening, Phase 7)

Verified against the real 516 catalogue via the production ExerciseFilter.

## Dimensions (all present, all wired to UI chip groups + VM toggles)
- Muscle (11 groups), Equipment (distinct from catalog), Difficulty
  (Beginner/Intermediate/Advanced), Movement Pattern (7 families),
  Experience (First Week/Building Up/Experienced -> difficulty),
  Goal (Hypertrophy/Strength/Fat Loss), and quick equipment presets
  (No Equipment / Home Workout / Machine Only), plus free-text query.
- Combine semantics: chips within a dimension OR; dimensions AND; query AND.

## Representative results (no empty/broken states)
- Beginner Legs = 82, No Equipment = 84 (all bodyweight), Push = 133, Pull = 123,
  Fat Loss = 37 (cardio/conditioning/full-body), Hypertrophy = 491, Home Workout
  = 232, Machine Only = 173. Empty filter returns all 516.
- Leak checks pass: No Equipment yields only bodyweight; First Week yields only
  Beginner; Fat Loss yields only cardio/conditioning/full-body.

## Chip state
8 chip groups render in the expanded filter; the active-count badge and Clear
action reflect all dimensions. No broken chip states.
