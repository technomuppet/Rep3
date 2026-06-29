# Accessibility Validation (Sprint 13 hardening, Phase 8)

Verified from source against the wired-up screens.

## TalkBack / content descriptions
- Muscle diagram: a single spoken contentDescription naming primary + secondary
  muscles.
- Animation: contentDescription "Animated demonstration of <name>"; the
  Play/Pause and Restart buttons have spoken labels ("Play/Pause animation",
  "Restart animation").
- Expandable coaching sections: each header announces its title and
  expanded/collapsed state.

## No colour-only information
- Difficulty uses a text marker ([Easy]/[Moderate]/[Hard]), not colour alone.
- Body diagram distinguishes primary (filled) from secondary (OUTLINED) regions
  by shape as well as colour, with a text legend.
- Status uses text ("Playing"/"Paused") alongside the icon.

## Scaling / layout / theme
- All text uses Material typography roles, so it scales with the system font size.
- Layouts use LazyColumn / weight-based rows and vector (Canvas) diagrams that
  reflow on large screens, landscape and foldables; the detail dialog scrolls.
- All visuals use Material colour roles, so dark and light modes adapt
  automatically (no hardcoded colours in the new components).

## On-device confirmation (gate)
TalkBack sweep, 200% font, landscape/tablet/foldable reflow and dark-mode contrast
should be confirmed on an Android device, which requires the SDK host build.
