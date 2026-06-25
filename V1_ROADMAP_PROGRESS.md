# RepLog V1.0 Roadmap - Progress

Branch: `sync/apply-improvements`. Highest-priority phases implemented first.

## Phase 1 - Logging Speed - DONE (`235f066`)
- Automatic keyboard focus + automatic keyboard opening on the Log Set dialog.
- Settings: "Fast logging focus" -> Focus Weight / Focus Reps (persisted via
  `PreferencesManager.autoFocusField`, threaded into the set-entry dialog).
- Weight field -> IME Next moves to reps; reps -> IME Done saves when valid.
- Tap a planned set to instantly log it (one-tap "Complete WxR" on the
  "Today's target" card; no dialog) reusing `quickCompleteSet` (auto-save +
  rest timer).
- Smart Repeat (one-tap "Repeat WxR") and one-tap progression complete were
  already present and remain the fastest path.
- `NumberInputField` extended (FocusRequester / imeAction / keyboardActions),
  backward compatible.

## Phase 2 - Performance - DONE (`<this commit>`)
- New `SessionSummaryRow` projection + `getCompletedSessionSummaries()`:
  volume aggregated in SQL (LEFT JOIN + GROUP BY), no set objects loaded.
- New `getRecentCompletedSessions(limit)`: bounded full-graph load.
- HomeViewModel no longer subscribes to all sessions: cheap stats from
  summaries; genome + insight from a bounded recent window (30).
- ActiveWorkoutViewModel adaptive plan from recent 8 (buildPlan only uses the
  last + recent 4); cache keyed on latest completed session id.
- CoachViewModel recovery analysis bounded to recent 30.
- Result: logging a set no longer triggers a full-history reload on the hot
  paths (Home / active workout / coach).
- Note: History list, export/backup, and the Progress / Training DNA analytics
  screens still read full history by design (they analyse all of it and are not
  on the set-logging hot path).

## Phase 3 - Home Dashboard 2.0 - PARTIALLY DONE (carried from Sprint 5)
Already on Home: Recommended Workout (Coach card), Favourite Quick Workouts
(Quick Start row), Recovery Score (Coach card), Today's Recommendation, Weekly
Progress, Current Streak, Last Personal Best, Quick Workouts entry, Recent
workouts (one-tap repeat). REMAINING: an explicit "Continue Workout" resume card
when a session is in progress, a Muscle Gap tile, and a "Next Recovery Window"
tile - all backed by existing engines (RecoveryCalendar, MuscleGapAnalyzer).

## Phases 4-8 - NOT STARTED (scoping notes)
- **Phase 4 RepLog Intelligence**: unify Training DNA / Genome / Recovery / Goal
  / Forecast / Coach / Muscle Gap into one recommendation surface. The engines
  all exist; this is an aggregation + presentation layer (a new domain
  "CoachBriefing v2" + Home card). Largest pure-logic phase.
- **Phase 5 Workout System Evolution**: expand the Quick Workout catalogue
  (hotel gym, more time-limited, more programmes) + custom sharing (sharing
  already exists via `.replogtemplate`). Mostly curated content.
- **Phase 6 Professional Polish**: warm-up generator, deload detection, injury
  tracking, session-duration prediction, animations, onboarding. Mix of new
  domain logic + UI.
- **Phase 7 Platform Readiness**: accessibility audit, Material 3 polish,
  tablet/foldable layouts; prepare for future KMP business-logic extraction
  (the `domain` layer is already pure Kotlin, which is the right foundation).
- **Phase 8 Launch Prep**: privacy policy, store listing, graphics/screenshots,
  testing tracks - non-code deliverables + release config.

## Verification status
- Pure domain+model compiles clean (kotlinc 1.9.22): 220 classes.
- DB consistent (17 entities, v15, 14 migrations defined = registered).
- No conflict markers; brace/paren balance holds across touched files.
- Outstanding gate (unchanged): `:app:assembleDebug` on an Android SDK host -
  not runnable in this environment. That is where the focus/keyboard behaviour,
  the new aggregate query, and overall Compose/Hilt/Room build are confirmed.
