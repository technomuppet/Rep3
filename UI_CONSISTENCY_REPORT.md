# UI Consistency Report (Sprint 11, Priority 4)

## Standardised this sprint
- **Status colours:** single source of truth via `RepLogSuccess` / `RepLogWarning`
  theme tokens. Removed 13 `Color(0xFF2E7D32)` + the `Color(0xFFF9A825)` literals
  across Recovery Centre, Training DNA, Coach, Coach History, Goals. Now
  consistent and theme-driven (light + dark).
- **Screen content padding:** all screens now `PaddingValues(20.dp)` (Onboarding
  was 24.dp).

## Audited — already consistent
- **Cards:** all use the shared `RepLogCard` (16.dp internal padding, 20.dp
  corner radius). One component → consistent.
- **Buttons:** shared `PrimaryButton` / `SecondaryButton` (54.dp height, 16.dp
  radius). Consistent.
- **Inputs:** shared `NumberInputField` (14.dp radius, decimal keyboard).
- **Progress indicators:** `LinearProgressIndicator` (lambda overload) used
  uniformly for scores/recovery/goals.
- **Dialogs:** Material 3 `AlertDialog` used throughout (confirm/dismiss buttons).
- **Empty/loading:** shared `EmptyState`/`InlineEmpty`/`LoadingState`.
- **Terminology:** PB (not PR) everywhere; "Training" tab label consistent.

## Remaining minor inconsistencies (Low — documented, not changed)
- Corner radii vary by intent (6.dp dots, 12.dp surfaces, 16.dp buttons, 20.dp
  cards, 999.dp pills). These are role-appropriate, not random; standardising
  further risks visual regressions for no clear gain.
- Inter-section vertical spacing varies (8/10/12/14/16.dp) by screen. Within
  Material tolerance; a future pass could pick one scale, but it is cosmetic and
  best tuned visually on-device.
- 32 hand-rolled section headers (see UX_AUDIT) — candidate for a shared
  `SectionHeader`, deferred.

## Verdict
The app uses shared components for every major element (cards, buttons, inputs,
empty/loading states), and status colours are now unified. It reads as one
product. Residual spacing/radius variance is cosmetic and Low.
