# Search & Accessibility Report (Sprint 13, Phases 7-9)

## Phase 7 - Search improvements (domain/library/ExerciseFilter.kt)
Existing dimensions (kept): muscle group, equipment, difficulty, movement pattern,
free-text query. New dimensions, all mapped onto EXISTING attributes (no new
search engine, no new data):
- Goal: Hypertrophy (resistance work), Strength (compound patterns: squat/hinge/
  press/row/pull/olympic), Fat Loss (cardio/conditioning/full-body).
- Experience: First Week / Building Up / Experienced -> Beginner / Intermediate /
  Advanced difficulty.
- Quick equipment: No Equipment (bodyweight), Home Workout (bodyweight/dumbbell/
  band/kettlebell), Machine Only (machine/cable/smith).
Dimensions combine with AND; multiple chips within a dimension combine with OR.
Verified against the real 516: Beginner Legs=82, No Equipment=84, Push=133,
Pull=123, Fat Loss=37, Hypertrophy=491, Home Workout=232, Machine Only=173; no
leaks; empty filter returns all 516.

Search is in-memory over the full list (516 rows), so all combinations are instant
and offline.

## Phase 8 - Accessibility
- TalkBack / screen readers: the muscle diagram and animation expose spoken
  contentDescriptions; every expandable section's header announces its title and
  expanded/collapsed state.
- No colour-only indicators: difficulty is shown with a text marker
  ([Easy]/[Moderate]/[Hard]); secondary muscles use an OUTLINE not just a colour;
  a text legend accompanies the diagram.
- Large fonts / landscape / tablets / foldables: all text uses Material typography
  roles (scales with system font); layouts use LazyColumn / weight-based rows and
  vector diagrams that reflow; the detail dialog scrolls.
- Dark mode: all visuals use Material colour roles, so they adapt automatically.

## Phase 9 - Performance
- 100% offline: coaching content, confidence, why (from local engines), diagrams
  and animations are all generated in code. No network requests, no image
  downloads, no API keys.
- Metadata loads instantly: derivation is O(1) per exercise on simple string
  inspection; the detail bundle is built lazily only for the selected exercise.
- SVG/diagram and animation frames are generated locally (nothing to cache from a
  network); Compose recomposition is bounded to the open dialog.
- Scales to 1000+ exercises: nothing is precomputed or stored per exercise, so the
  library's footprint and load time are unchanged as the catalog grows. Filtering
  is linear over the in-memory list and remains smooth.
