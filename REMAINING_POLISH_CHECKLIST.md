# Remaining Polish Checklist

These are best validated/finished on a device with the Android SDK; none are
High severity and none block the architecture being release-candidate ready.

## On-device visual QA (cannot be done without a device)
- [ ] TalkBack pass on Home, Active Workout, Recovery Centre, Settings.
- [ ] Font scale 200%: Log Set dialog, Home cards, Briefing/Score cards.
- [ ] Landscape + tablet + foldable reflow; verify the widest cards do not clip.
- [ ] Dark mode contrast on RepLogSuccess / RepLogWarning and on all status text.
- [ ] Ripple / press feedback feel on cards and chips.
- [ ] Rest-timer notification + completion haptic on a physical device.
- [ ] Export/import round-trip to Downloads/RepLog; "Open Folder" / "Share File".

## Low-severity code polish (optional, post-RC)
- [ ] Introduce a shared `SectionHeader` composable and replace the ~32
      hand-rolled headers (cosmetic consistency).
- [ ] Pick one inter-section vertical spacing scale (8/12/16) and apply.
- [ ] Convert `BackupJson` to Gson streaming (CSV already streams).
- [ ] Replace positional `combine(Array<Any?>)` casts with typed wrappers in the
      large ViewModels (brittleness, not a current bug).
- [ ] Extract the largest composables (ActiveWorkoutScreen, HomeScreen) for
      recomposition isolation/testability.
- [ ] Add debug-only logging around broad `runCatching{}.getOrNull()` calls.

## Store preparation (non-code)
- [ ] Privacy policy + Data Safety form (declare: no data collected/shared,
      on-device only, user can export/delete).
- [ ] Feature graphic, screenshots (the demo-data generator helps here).
- [ ] Store listing copy; closed-beta track setup.
- [ ] Replace placeholder launcher artwork if any; clean signed release build.
