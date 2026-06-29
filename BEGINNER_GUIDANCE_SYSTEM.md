# Beginner Guidance System (Sprint 13, Phases 1-3)

## Approach: derive, do not store
All coaching content is GENERATED at runtime from each exercise's existing
attributes (movementPattern, equipment, difficulty, primary/secondary muscles,
category). This adds ZERO storage, needs NO database migration, and scales to
1000+ exercises because it is keyed on a small fixed set of movement families.

## Phase 1 - Coaching model (domain/library/ExerciseCoach.kt)
CoachingInfo per exercise:
- purpose: one sentence, <= 150 chars.
- steps: exactly 4.
- cues: exactly 3.
- mistakes: up to 4.
- breathing: one sentence.
- tempo: "2-1-2" (or "steady"/"controlled" for cardio/carries) + explanation.
- rangeOfMotion: short explanation.
- safety: up to 3 (advanced movements get an extra "build up to it" note).
Keyed on 10 movement families (PUSH, PULL, HINGE, SQUAT, LUNGE, SHOULDERS, CORE,
CARRY, CALVES, CONDITIONING) resolved by familyOf(); GENERIC is a final fallback.
Verified across all 516 exercises: 0 failures, 0 GENERIC fallbacks.

## Phase 2 - Confidence system (domain/library/BeginnerGuidance.kt)
- ConfidenceCard: difficulty (with a text marker, not colour-only), equipment,
  estimated learning minutes (5-25 by difficulty/equipment), ideal experience
  ("First Week Friendly" / "A Few Weeks In" / "Experienced Lifters").
- easierAlternative(): for harder movements, finds a strictly-easier same-family
  variation sharing a primary muscle (e.g. Pistol Squat -> Bodyweight Squat,
  Barbell Back Squat -> Bodyweight Squat). 39/49 advanced + 131/137 intermediate
  get a suggestion; the rest are already the easiest in their family.

## Phase 3 - Why this exercise (domain/library/WhyThisExercise.kt)
- Goal-based sentence from the user's profile goal (Hypertrophy/Strength/Fat Loss/
  General), e.g. "This exercise develops the chest and matches your goal of muscle
  growth."
- When the existing Recovery Centre reports the exercise's primary muscle as FRESH,
  it personalises: "David, your Recovery Centre shows your chest is fully recovered
  today, making this an ideal exercise." The recovered-muscle input is supplied by
  the ViewModel from IntelligenceRepository.buildRecoveryCentre() - the engine
  NEVER invents recovery data; with no data it falls back to the goal sentence.

## UI
ConfidenceCardView is always visible; coaching detail is progressive disclosure
(Phase 6). All reuse the existing RepLogCard / Expand idiom.
