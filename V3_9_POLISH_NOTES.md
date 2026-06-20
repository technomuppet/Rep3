# V3.9 Polish Notes

## Implemented

### Superset grouped layout

Active workout display now groups exercises sharing a superset group into a visible Superset block.

- Superset group header appears before linked exercises.
- A1/A2 labels are preserved on exercise cards.
- The layout communicates that linked movements should be performed together.

### Screenshot/demo tooling

Settings now includes a local demo-data generator for QA and store screenshots.

Generated demo data includes:

- completed workouts
- PR flags
- advanced set metadata
- supersets
- bodyweight logs

### Exercise media polish

Exercise detail now renders a bundled local media placeholder vector.

### Accessibility documentation

Added `ACCESSIBILITY_NOTES.md` with current status and QA checklist.

## Still Required

- Real bundled exercise GIFs/images.
- Shared superset rest timer logic.
- Full TalkBack QA pass.
- Dedicated large-screen layouts.
- Final Android Studio compile pass.
