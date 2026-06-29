# Exercise Library Audit (Sprint 13, Phase 0)

Audit-first. Verified from source at HEAD 7c16747 before any code is written.

## 1. Current exercise model
- Room entity `data/model/Exercise.kt`, table `exercises`, index on `name`.
- Fields: id, name, category, equipment, type ("Strength"/"Cardio"), muscles
  (comma string), primaryMuscles, secondaryMuscles, movementPattern, difficulty
  ("Beginner"/"Intermediate"/"Advanced"/"Custom"), mediaAsset (a .gif path),
  isCustom.
- KEY FINDING: difficulty, primaryMuscles, secondaryMuscles, movementPattern and
  equipment ALREADY EXIST. Sprint 13 needs NO new entity columns -> NO Room
  migration. Coaching metadata can be DERIVED at runtime from these attributes.

## 2. Catalog / storage architecture
- Source of truth: `app/src/main/assets/exercises.json` (~215 KB, 516 exercises).
  JSON shape per item: name, category, equipment, type, muscles[], primaryMuscles[],
  secondaryMuscles[], movementPattern, difficulty, mediaAsset.
- Seeded by `util/DataSeeder.kt`: seedExercises() on first launch;
  upgradeExerciseMetadataIfNeeded() re-reads JSON and (a) inserts bundled
  exercises missing from the user library, (b) enriches rows with blank
  movementPattern/primaryMuscles/mediaAsset - WITHOUT touching custom exercises.
  This is the existing, safe hook for any shipped metadata changes.
- Distributions (516): difficulty Beginner 330 / Intermediate 137 / Advanced 49;
  equipment Dumbbell 104, Cable 91, Barbell 90, Bodyweight 84, Machine 68,
  Band 24, Kettlebell 20, Smith Machine 14, Plate 14, EZ Bar 7; category Legs 121,
  Back 89, Arms 85, Chest 70, Shoulders 61, Core 53, Cardio 25, Full Body 12.
- movementPattern is granular and well-structured (30 families), e.g.
  "Push - Horizontal Press", "Legs - Squat", "Hinge", "Pull - Vertical Pull",
  "Shoulders - Lateral Raise", "Core - Anti-Extension". This is the ideal key for
  a rules-based coaching generator.

## 3. Search architecture
- In-memory, reactive: `ExerciseViewModel.uiState` combines
  exerciseRepository.getAllExercises() (Flow) + filter + useKg, applying
  `ExerciseFilter.apply` on the full 516-row list. 516 rows in memory is trivial;
  this already scales and is offline. (A SQL search query also exists in
  ExerciseDao.searchExercises but the library screen uses the in-memory filter.)

## 4. Filtering / categories
- `domain/library/ExerciseFilter.kt`: multi-select chips for muscle group,
  equipment, difficulty, movement-pattern family + free-text query. Muscle chips
  combine OR; dimensions combine AND; query is a further AND. Muscle-group chips
  map to fine-grained muscle vocab (e.g. Shoulders -> front/side/rear deltoids).
- MUSCLE_GROUPS (11) and PATTERNS (7 families) are defined here.

## 5. Muscle mapping / equipment mapping
- Muscle mapping: groupToMuscles in ExerciseFilter (group label -> muscle set).
  primary/secondary/muscles strings are comma-delimited.
- Equipment: a single string per exercise; equipmentOptions derived distinctly in
  the ViewModel for the filter chips.

## 6. Existing descriptions / detail view
- Detail = `ExerciseDetailDialog` in ExerciseLibraryScreen, fed by
  `ExerciseViewModel.selectedInsight` (history/PR analytics) and `swapSuggestions`
  (alternatives via `ExerciseSwapEngine.alternatives`, already implemented).
- Currently shows: pattern, primary/secondary muscles, a progress chart, set
  history, PR count, and swap suggestions. NO coaching content yet.

## 7. Missing beginner information (the Sprint 13 gap)
- No exercise purpose, step-by-step instructions, coaching cues, common mistakes,
  breathing, tempo, range of motion, safety notes, learning-time/confidence card,
  "why this exercise" rationale, muscle diagram, or movement animation.
- mediaAsset references .gif files (against the Sprint 13 rule of no GIFs/videos/
  photos). We will NOT use mediaAsset for visuals; instead generate SVG body
  diagrams + a keyframe animation engine from movement pattern.

## 8. Reuse opportunities (engineering rule: reuse, do not duplicate)
- ExerciseSwapEngine -> alternatives ("Recommended first" for Advanced moves).
- IntelligenceRepository / RecoveryAnalyzer / TrainingGenome -> "Why this
  exercise" (recovery + goal + DNA), with the user's display name from profile.
- ExerciseFilter -> extend the searchable dimensions (experience/goal/body part)
  by mapping onto existing attributes; no new search engine.

## Plan (no new storage, no migration)
- Phase 1: `domain/library/ExerciseCoach.kt` - a PURE rules engine producing
  CoachingInfo(purpose, 4 steps, 3 cues, <=4 mistakes, breathing, tempo, ROM,
  <=3 safety) from (movementPattern, equipment, difficulty, muscles, category).
  All 516 covered with sensible per-pattern content; zero per-exercise authoring.
- Phase 2: BeginnerConfidence (difficulty badge, equipment, estimated learning
  time, "first week friendly" label) + easier-alternative suggestion for Advanced.
- Phase 3: WhyThisExercise - derive from goal/recovery/DNA; never invent.
- Phase 4: `ui/exercise/MuscleDiagram.kt` - code-drawn SVG-style Compose body
  diagram (front/back, male/female silhouette) highlighting primary/secondary;
  pure vector, ~0 KB asset footprint.
- Phase 5: `domain/library/ExerciseAnimation.kt` - reusable keyframe engine
  (<=8 keyframes of joint positions per pattern, generated in code, <5 KB each).
- Phase 6: progressive-disclosure detail (collapsed sections).
- Phase 7: extend search dimensions (experience/goal/body part) onto existing
  attributes.
- Phases 8-10: accessibility (contentDescription, no colour-only, scalable type),
  performance (all in-memory/offline), completeness QA over all 516.
