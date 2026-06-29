# Beginner Guidance Verification (Sprint 13 hardening)

Verified against the actual app + the real 516-exercise catalogue.

## Exercise Detail (Phase 1) - beginner-first
The detail dialog answers "What is this exercise?" immediately, showing (top,
always visible): name, "category - equipment - difficulty", primary muscles,
secondary muscles, a one-sentence purpose, three coaching cues, and the
ConfidenceCard (difficulty + equipment + estimated learning time + ideal
experience). The old GIF/media placeholder has been removed entirely (drawable
deleted; no references remain). Detail then shows the muscle diagram + animation,
then progressive-disclosure coaching, then analytics.

## Coaching (Phase 2) - all generated offline
Every exercise (516/516): purpose (<=150 chars), exactly 4 numbered steps,
exactly 3 cues, up to 4 common mistakes, breathing sentence, tempo "2-1-2" with
explanation (or "steady"/"controlled" for cardio/carries), range-of-motion, up to
3 safety reminders. Verified: 0 failures.

## Confidence (Phase 3) - never blank
Every exercise shows a confidence card (difficulty explanation via a text marker,
estimated learning time, ideal experience). The Alternatives section is ALWAYS
rendered: it recommends an easier variation when one exists (39/49 advanced,
131/137 intermediate) and otherwise EXPLAINS why none is suggested (already
beginner-friendly / already the most accessible option in its family / custom).
No blank section.

## Why This Exercise (Phase 6) - derived, never invented
Each rationale now states up to four derived facts: (1) goal fit (profile goal +
primary muscle), (2) experience-level appropriateness (exercise difficulty vs the
profile's stored experience level), (3) recovery context (ONLY when the Recovery
Centre reports the primary muscle FRESH - supplied by IntelligenceRepository,
never computed here), and (4) why it appears in recommendations (goal/level match,
reusing ExerciseFilter's existing goal logic). Verified non-blank across all 516.

## Result
All 516 exercises display complete beginner guidance with no empty states.
