# Exercise Library 2.0 - Implementation Verification Audit (Sprint 13 hardening)

Audit-first. Verified against the ACTUAL wired-up app (not just domain tests) at
HEAD 5b855c5 before any code change.

## Verified PRESENT and wired
- Exercise detail (ExerciseDetailDialog): shows category/equipment/difficulty,
  primary + secondary muscles, purpose, 3 coaching cues, ConfidenceCardView,
  MuscleBodyDiagram, ExerciseAnimationView, and progressive-disclosure
  CoachingSections (How to Perform, Common Mistakes, Breathing, Tempo, Safety,
  Why, Alternatives) + swap alternatives + analytics.
- Coaching engine (ExerciseCoach), BeginnerGuidance, WhyThisExercise, MuscleMap,
  ExerciseAnimation: all present; domain compiles; 516-exercise completeness
  passes.
- Search: ExerciseFilter has muscle/equipment/difficulty/pattern + goal/experience/
  equipment-presets; 7 VM toggles; 8 UI chip groups.
- Performance/offline: DB still v15; Exercise.kt and exercises.json unchanged
  since pre-sprint; zero network in new code.

## GAPS FOUND (must fix before claiming complete)
1. PHASE 5 - Animation has NO Play/Pause/Restart controls. It only auto-loops
   (infiniteRepeatable). Spec explicitly requires Play/Pause and Restart. MISSING.
2. PHASE 1/5 - The old media placeholder asset
   res/drawable/exercise_media_placeholder.xml STILL EXISTS (orphaned; no longer
   referenced by any .kt/.xml). Spec: remove the GIF/media placeholder COMPLETELY.
   Must delete the file.
3. PHASE 3 - The "Alternatives" section is OMITTED when no easier alternative
   exists. Spec: "If no easier alternative exists, explain why. Never leave the
   section blank." Must always render the section with an explanation.
4. PHASE 6 - "Why this exercise" covers goal + recovery only. Spec also wants:
   why it is appropriate for the user's EXPERIENCE LEVEL and why it appears in
   recommendations. Must extend (still derived, never invented).

## Non-issues (correct as-is)
- 10/49 advanced exercises have no easier alternative because they are already the
  easiest in their movement family - correct; will be EXPLAINED (gap 3 fix).
- No duplicate exercises; every exercise maps to >= 1 body region.

## Plan
Fix gaps 1-4, delete the orphaned drawable, re-verify all 516, then write the
remaining deliverables. No Room migration, no network, no new storage.
