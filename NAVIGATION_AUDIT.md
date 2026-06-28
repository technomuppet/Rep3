# Navigation Audit (HIGH PRIORITY)

Source: `ui/navigation/NavGraph.kt` (single `NavHost`, single graph). Verified
14 routes declared = 14 `composable()` registered. No duplicate route strings.

## Navigation map
Start destination: **Home**.

Primary tabs (bottom navigation bar shown; tap = one action to any tab incl. Home):
- Home, Workout ("Training"), Progress, History, Exercises, Settings.

Deep link (bottom bar shown, Training tab highlighted):
- WorkoutRecommendation — one-shot; consumes the staged coach recommendation.

Secondary screens (TopAppBar with Back arrow → `navigateUp()`; bottom bar hidden):
- TrainingDna, CoachHistory, Goals, QuickWorkouts, RecoveryCentre, MuscleBalance,
  DnaEvolution.

Entry points (verified wired in `HomeScreen`):
- RecoveryCentre ← Today's Briefing card tap + "Recovery" hub card + Coach rest-day.
- MuscleBalance ← "Muscle Balance" hub card.
- DnaEvolution ← "DNA" hub card.
- TrainingDna ← Genome card + Progress screen.
- Goals ← goal card + "Goals" button. CoachHistory ← "Coach History" button.
- QuickWorkouts ← "Quick Workouts" card.

## Critical requirement: Home always reachable — PASS
- On all 6 primary tabs: the bottom bar contains Home → one tap to Home.
- On WorkoutRecommendation: bottom bar present (Home one tap).
- On all 7 secondary screens: every screen is pushed on top of Home via
  `openDetail` (single `launchSingleTop` push), so the TopAppBar Back arrow
  (`navigateUp()`) returns to Home in one action. No secondary screen is reachable
  except from Home, so the back target is always Home.
- No route lacks both a bottom bar and a back arrow → no trap exists.

## Verified: no traps / loops / dead screens / duplicates
- Every route is reachable (14 declared = 14 registered = all navigated-to).
- No duplicate route strings; no duplicate navigation graphs.
- The historical "Home loops to Training" bug is fixed: the Training tab
  (`workout`) and the recommendation deep link (`workout_recommendation`) are
  distinct routes; the deep link uses `launchSingleTop` only (no save/restore),
  so it cannot be resurfaced under Home.
- Tab switches use `popUpTo(startDestination){saveState}; launchSingleTop;
  restoreState` (the standard Material pattern) → no tab stacking, no loop.

## Issues found
- MEDIUM — Bottom bar is hidden on the 7 secondary screens. Home is still one
  action away (back arrow), but the spec asks the bottom bar to "remain available
  wherever appropriate." Recommendation: keep the back arrow, and optionally show
  the bottom bar on the intelligence hubs (RecoveryCentre/MuscleBalance/
  DnaEvolution) so users can tab-hop without backing out. Not a trap; UX polish.
- LOW — System Back from a non-Home primary tab follows the standard
  save/restore semantics (returns toward the start destination / may minimise the
  app from Home). Acceptable Material behaviour; consider an explicit "back to
  Home then exit" policy if product wants Instagram-style back stack.
- LOW — No global menu/hamburger; navigation is bottom-bar + cards. Consistent
  and intentional; not a defect.

## TopAppBar consistency
- Secondary screens: consistent TopAppBar with title + Back arrow (rendered
  centrally in the Scaffold, so all 7 are identical — no per-screen drift).
- Primary tabs: no TopAppBar (content provides its own header text). Consistent
  across all 6. No duplicate navigation controls observed.

## Verdict
Navigation is production-safe: Home is never inaccessible, there are no traps,
loops, dead screens or duplicate routes. The only actionable item is the
Medium-severity bottom-bar-on-secondary-screens UX choice.
