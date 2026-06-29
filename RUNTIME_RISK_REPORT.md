# Runtime Risk Report (Sprint 13.1, Priority 1)

Features that can only be confirmed at runtime, why each could fail, how to
reproduce, expected behaviour, and the likely fix if it fails. Source-derived
risk assessment; none claimed as tested on a device.

## R1 - Animation Play/Pause/Restart (ExerciseAnimationView)
- Why it could fail: the clock is a manual withFrameNanos loop in a LaunchedEffect
  keyed on (exercise.id, playing, cycleMillis). If the key set is wrong the loop
  could keep running while paused, or fail to resume.
- Reproduce: open any exercise; tap Pause (figure must stop instantly); tap Play
  (resumes from same phase); tap Restart (phase resets to 0 and plays).
- Expected: pause stops immediately; play resumes; restart returns to start; loops
  smoothly; never freezes; never advances while paused.
- Likely fix if wrong: ensure the LaunchedEffect returns early when !playing
  (it does) and that `phase` is remembered keyed on exercise.id (it is). If resume
  jumps, capture `last` after the gate rather than before.

## R2 - Detail dialog overflow / clipped text / no empty sections
- Why: the detail is an AlertDialog with a scrollable Column capped at 580.dp;
  long content at 200% font could clip or the dialog could feel cramped on small
  phones.
- Reproduce: open an exercise on a small phone at 200% font; expand every section.
- Expected: content scrolls; no clipping; every section has content (Alternatives
  always explained).
- Likely fix: if cramped, convert the detail from AlertDialog to a full-screen
  route/bottom sheet. (Documented as a candidate, not a confirmed defect.)

## R3 - Body diagram correctness (MuscleBodyDiagram + MuscleMap)
- Why: region rectangles are hand-placed normalised boxes; a muscle could map to
  the wrong box or none.
- Reproduce: open exercises covering each region (see EXERCISE_LIBRARY_RUNTIME_
  REPORT) and confirm the highlight matches the named muscle.
- Expected: every exercise highlights >=1 region; primary filled, secondary
  outlined; front/back correct.
- Likely fix: adjust REGION_BOXES coordinates or the keywordToRegion map.

## R4 - Touch targets (animation control IconButtons, filter chips)
- Why: IconButton defaults to 48.dp (OK); chips must remain >=48.dp tappable.
- Reproduce: tap Play/Pause/Restart and filter chips on a small phone.
- Expected: all comfortably tappable.
- Likely fix: wrap with minimumInteractiveComponentSize if any are small.

## R5 - Search recomposition / scroll smoothness with 516 rows
- Why: filtering runs in-memory on every filter/query change; the list is a
  LazyColumn but rebuilds the filtered list each emission.
- Reproduce: type quickly in search; toggle several chips; fling the list.
- Expected: instant, smooth.
- Likely fix: already bounded (WhileSubscribed); if needed, debounce the query or
  remember the filtered list. No defect expected at 516.

## R6 - TalkBack / focus order (detail + diagram + animation)
- Why: the diagram and animation expose contentDescriptions; expandable headers
  announce state; focus order must be logical top-to-bottom.
- Reproduce: enable TalkBack; swipe through the detail.
- Expected: name -> summary -> confidence -> diagram (muscles read) -> animation
  (+controls) -> each section header (with expanded/collapsed) -> analytics.
- Likely fix: add semantics ordering / mergeDescendants on a card if focus is
  fragmented.

## R7 - Landscape / tablet / foldable reflow
- Why: the detail dialog and library use weight-based rows + vector diagrams that
  should reflow, but a fixed 580.dp cap or 180/220.dp canvas heights could look
  off on very wide/short screens.
- Reproduce: rotate to landscape; open on a tablet/foldable.
- Expected: no clipping; diagram/animation scale; controls reachable.
- Likely fix: make heights adaptive or use BoxWithConstraints.

## R8 - Dark mode / contrast
- Why: new components use Material colour roles (primary/tertiary/onSurface), so
  they should adapt; secondary-muscle outline must stay visible in both themes.
- Reproduce: toggle dark/light while viewing a diagram.
- Expected: regions and stick figure visible in both; no colour-only reliance
  (legend + outline + text markers present).
- Likely fix: bump alpha or pick a higher-contrast role.

## R9 - State restoration / lifecycle (animation + selected exercise)
- Why: rotation recreates the composable; `playing`/`phase` are remembered keyed
  on exercise.id (not rememberSaveable), so they reset on rotation. The selected
  exercise is held in the ViewModel (survives rotation).
- Reproduce: open an exercise, rotate.
- Expected: dialog stays open (VM-held), animation restarts playing (acceptable).
- Likely fix: only if product wants pause-state to survive rotation, use
  rememberSaveable for `playing`.

## R10 - IME / keyboard (search field, add-exercise dialog)
- Why: search OutlinedTextField + Add Exercise dialog fields.
- Reproduce: focus search; type; open Add Exercise.
- Expected: keyboard opens, no layout jump that hides the field.
- Likely fix: imePadding / windowInsets if the field is obscured.

## Summary
No defect is asserted; these are the runtime-only surfaces a human/CI must
confirm. R1, R2 and R3 are the highest-value checks (the Sprint 13 headline
features). Fixes listed are pre-scoped so any failure can be addressed quickly.
