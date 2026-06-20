# V3 Analytics Notes

This block begins the analytics layer that will eventually power Training DNA™.

## Implemented Signals

### Lifetime Load
- Total workouts
- Total sets
- Total reps
- Total PRs
- Total volume
- Tonnes lifted

### Movement Balance
Uses `Exercise.movementPattern` and groups by the first segment before `•`.

Examples:
- `Push • Horizontal Press` -> Push
- `Pull • Vertical Pull` -> Pull
- `Legs • Squat` -> Legs

### Muscle Distribution
Uses:
- primary muscles at 100% set volume
- secondary muscles at 50% set volume

This is intentionally approximate but directionally useful.

### Recovery Signal
Uses local deterministic rules:
- training frequency over 7 and 14 days
- current weekly volume vs previous week
- recent bodyweight trend

Labels:
- Recovered
- Normal
- Fatigued

### Plateau Watch
Flags exercises where:
- the exercise has been trained enough recently
- recent 6-week estimated 1RM has not matched the all-time best

This is a first-pass watchlist, not a medical/training prescription.

## Why This Matters

Training DNA™ needs structured, longitudinal signals. This analytics block creates the first set of interpretable signals without cloud, accounts, or API cost.

## V3.1 Additions

- Strength forecast cards using recent estimated 1RM trend.
- Forecast confidence ranges around projected estimated 1RM.
- Next-best-action recommendations from recovery, plateau, balance and forecast signals.
- Training DNA™ first draft: preferred rep range, average RPE, hard sets/week, top movement pattern and top exercise.
- Plateau suggestions now consider recent hard sets and RPE.
- Home screen intelligence card summarises recent PR, volume and consistency signals.
- Adaptive vNext workout generation from the last completed workout.
- Adaptive targets now display inside active workout exercise cards.
- Workout completion summary evaluates adaptive targets hit/missed.
- PR forecast cards now include target-date estimates for the next rounded strength target.
- Muscle balance recommendations now include specific exercise suggestions.
- Template schema and editor now support target sets, reps and optional target load.
- Workout prescriptions are now persisted per generated/template workout for exact target comparison later.
- Progression engine recommends next action after prescribed target hit/miss.

## Next Analytics Upgrades

- More robust PR prediction with confidence intervals.
- Progression-rate calculation by exercise and movement pattern.
- Volume/frequency response by exercise.
- RPE-adjusted fatigue score.
- Muscle imbalance recommendations with specific exercise suggestions.
- Adaptive programming rules.
