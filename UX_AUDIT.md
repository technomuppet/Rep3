# UX Audit (Sprint 11, Phase 0)

Audited from source, per-screen, for friction and consistency. "OK" = meets the
release-candidate bar; "Fixed" = changed this sprint; "Note" = documented, no
change.

## Workout logging (the heart of RepLog) — OK
- One-tap paths exist: Smart Repeat ("Repeat WxR", no dialog), one-tap
  progression/target Complete, tap planned set to log. Add Set opens a dialog
  pre-filled from the suggested set.
- Keyboard: the Log Set dialog auto-focuses the user's chosen field (weight/reps,
  Settings) and shows the numeric keyboard; weight→Next→reps→Done saves.
- Haptics on Repeat, progression-complete, and all rest-timer controls.
- Rest timer auto-starts on set completion when enabled; foreground service.
- Verdict: minimal taps; no unnecessary friction found.

## Home — OK
- Priority-ordered control centre: Continue Workout (when active), Today's
  Briefing (recommendation + recovery% + Why + tap→Recovery Centre), RepLog
  Score, intelligence hubs, Quick Start (favourites), Recommended, Recent,
  goal/weekly/streak/last-PB. The "what to do next / recovery / progress / goal"
  questions are answerable at a glance.

## Intelligence (Briefing / Recovery / Muscle Balance / DNA / Coach / Score) — OK
- Explainability: Briefing has narrative + expandable per-engine "Why?" with
  per-section confidence. Recovery/Muscle Balance/DNA each have factors/reasons.
- No conflicting recommendations: all surfaces derive from the one
  IntelligenceRepository, so recovery/recommendation are consistent.

## Visual consistency — Fixed
- Status colours unified to theme tokens (was 13 duplicated greens + amber).
- Onboarding content padding standardised to 20.dp.

## Empty / loading / error states — OK
- Shared `LoadingState` / `EmptyState` / `InlineEmpty`. Intelligence screens and
  analytics screens show loading then empty-or-content; export/import surface
  status messages; restore/delete use confirmation dialogs (typed DELETE for
  destructive reset).

## Section headers — Note (no change)
- 32 hand-rolled headers across screens. Visually similar (titleMedium/titleLarge
  Bold). A shared `SectionHeader` is deferred: broad churn, low benefit, and the
  brief says do not refactor unnecessarily.

## Accessibility / keyboard — see ACCESSIBILITY_REPORT.md

## Friction summary
No unnecessary taps were identified on the core logging path. The polish gap was
visual-consistency (status colours, one padding value), now resolved. Remaining
polish is subjective/visual and best validated on-device (see
REMAINING_POLISH_CHECKLIST.md).
