# RepLog Accessibility Notes

This document tracks accessibility expectations for the app.

## Current improvements

- Destructive icon buttons generally include content descriptions.
- Bottom navigation uses visible labels.
- Text-first cards are used instead of icon-only navigation.
- Confirmation dialogs protect destructive actions.
- Exercise media placeholder has a content description in exercise detail.

## Required QA

Test with:

- Android font scale 1.3x and 1.5x
- TalkBack enabled
- Dark and light system modes
- One-handed use during workout logging

## Areas to watch

- Charts are visual-only. Each chart should have nearby text summary, which most current chart cards include.
- Some icons are decorative and intentionally use null content descriptions.
- Six bottom-nav items may be cramped on smaller devices.
- Large dialogs such as template editor and exercise detail may need full-screen layouts for accessibility.

## Future improvements

- Add explicit semantic descriptions to chart components.
- Split large dialogs into dedicated screens on small/large devices.
- Increase touch target spacing for dense set rows.
- Add haptic or visual feedback for completed targets.
