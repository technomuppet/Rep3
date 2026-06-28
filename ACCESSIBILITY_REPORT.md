# Accessibility Report (Sprint 11, Priority 5)

## Verified from source
- **TalkBack / content descriptions:** every `IconButton` has a non-null
  `contentDescription` (Back, Edit, Delete, Share, Move up/down, Notes, etc.) —
  0 violations found. Decorative `Icon`s inside labelled rows correctly pass
  null.
- **Dynamic font scaling:** text uses Material 3 typography roles
  (`titleLarge`, `bodyMedium`, `labelSmall`, …) and theme colours, not fixed sp,
  so it scales with the system font size. Layouts use weight-based `Row`s and
  `LazyColumn`, so they reflow rather than truncate.
- **Dark mode / contrast:** colours come from the Material 3 scheme; the new
  `RepLogSuccess` token was chosen for adequate contrast in both themes (replaces
  a fixed green that did not adapt).
- **Touch targets:** primary/secondary buttons are 54.dp tall; `IconButton`
  (48.dp default) and `AssistChip`/`FilterChip` meet the 48.dp minimum.
- **Focus order / keyboard:** the Log Set dialog uses an explicit
  `FocusRequester` and `ImeAction` chain (Weight→Next→Reps→Done), so keyboard
  navigation and focus order are deterministic.

## Not changed (no regression)
- No accessibility regression was introduced this sprint (only colour tokens +
  one padding value changed). The colour change improves dark-mode contrast.

## Recommended on-device checks (cannot be done without a device)
- TalkBack swipe-through of Home, Active Workout, Recovery Centre.
- Font scale at 200% on the Log Set dialog and Home cards.
- Landscape / tablet / foldable reflow (weight-based rows should adapt; verify no
  clipping on the widest cards).
- Colour-contrast scan of `RepLogSuccess`/`RepLogWarning` on light + dark.
