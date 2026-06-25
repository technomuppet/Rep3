# RepLog - Optimisation Opportunities

Companion to `AUDIT_PROJECT_REVIEW.md`. Ranked by impact/effort. None are
release blockers; items 1-2 are the highest leverage. All preserve the
offline-first, no-cloud architecture.

## 1. Reduce full-session fan-out in hot ViewModels (HIGH impact)
**Problem.** `HomeViewModel` and `ActiveWorkoutViewModel` both include
`workouts.getAllSessions()` in their `combine`. That DAO query is a Room
`@Transaction` that materialises **every** session with **all** of its child
session-exercises and **all** set rows, and it re-emits on any write to those
tables. As workout history grows, every set logged triggers a full reload of the
entire training history on the Home and active-workout flows.

**Where it is used today.**
- `HomeViewModel`: only to derive weekly stats, day streak, genome headline and
  the "recent" list - none of which need the full per-set object graph.
- `ActiveWorkoutViewModel`: only to build the (offline) adaptive plan when no
  workout is active.

**Recommended fixes (offline, no new deps):**
- Add lightweight aggregate DAO queries for the dashboard numbers, e.g.
  `SELECT COUNT(*)`, weekly volume via a `SUM(...) ... WHERE startTime >= :weekStart`,
  and a streak helper - rather than summing in Kotlin over the full graph.
- Keep `getRecentSessions(5)` (already limited) for the recent list; drop the
  unbounded `getAllSessions()` subscription from `HomeViewModel`.
- In `ActiveWorkoutViewModel`, the adaptive plan only matters when `activeId ==
  null`; gate the `getAllSessions()` collection behind that (e.g. flatMapLatest
  on `activeId`) so it is not resubscribed during active logging.

**Why it matters for Sprint 5's goal.** The sprint target is "logging a repeated
set under one second". Each `addSet`/`repeatLastSet` writes to `set_logs`, which
currently invalidates the full-history flow on Home and (transitively) recomputes
heavy state. Trimming the fan-out directly protects logging latency.

## 2. Finish P1 auto-focus + add the Settings toggle (MEDIUM impact)
The `autoFocusField` preference is plumbed into `ActiveWorkoutUiState` but inert.
To complete it:
- In the set-entry dialog (`AddOrEditSetDialog`), add a `FocusRequester` on the
  weight and reps `NumberInputField`s and `LaunchedEffect(Unit)` to request focus
  on the field named by `autoFocusField`, plus `keyboardController?.show()` so the
  numeric keyboard opens immediately (the field already uses a Decimal/Number
  keyboard, so cursor placement is instant).
- Add a Settings row ("After completing a set, focus: Weight / Reps") bound to
  `PreferencesManager.setAutoFocusField`.
This is the remaining piece of the "single-tap, keyboard-ready" logging flow.

## 3. One-tap "Tap set to complete" for planned sets (MEDIUM impact)
Sprint 5 P1 describes tapping a set card to complete it. The engine already has
the building blocks: `repeatLastSet` (Smart Repeat, one tap, no dialog) and the
one-tap progression "Complete WxR" button. The remaining enhancement is making an
*upcoming/planned* set row (from a template/quick-workout prescription) directly
tappable to log the prescribed weight x reps without opening the dialog. Reuse
`quickCompleteSet`; no new logic required.

## 4. Swipe gestures - evaluate, likely skip (LOW impact)
The spec says "only implement if it genuinely improves speed". The current model
(prominent Repeat button + tappable rows + edit/delete icons) is already
one-to-two taps. Swipe-to-complete/edit/delete would add gesture-discovery cost
and accessibility complexity for marginal speed gain. Recommendation: do not add
swipe actions; the tap model is faster and more discoverable.

## 5. Test coverage for the two newest migrations (LOW impact, good hygiene)
`AppDatabaseMigrationTest` stops at v13. Add `migrate13To14` (rest_day_overrides)
and `migrate14To15` (isFavorite column) cases on an SDK host. Per project policy
tests are not committed from this environment; this is a maintainer task.

## 6. Minor consistency polish (LOW impact)
- Quick Workout cards render the full exercise list inline; for long sessions
  consider truncating with "+N more" to keep cards compact.
- Consider surfacing the recommendation engine's suggested Quick Workout at the
  top of the Quick Workouts screen (P4 deepening) - the recommendation engine and
  recovery analyzer already exist and could rank the catalogue by recovery/muscle
  gaps.

## Non-issues confirmed (no action needed)
- No deprecated Compose/Room/Hilt APIs.
- No accessibility violations on interactive icons.
- No networking / cloud coupling.
- DB schema/migration/DI all consistent.
