# RepLog Score - Documentation

## What it is
A single 0-100 number summarising overall training quality, with a fully
transparent breakdown. Computed by the pure `RepLogScoreEngine`; it performs no
raw-session analysis and only weights seven already-measured component scores.

## Components and weights (sum = 100)
| Component | Weight | Source / meaning |
|---|---|---|
| Consistency | 20 | Sessions/week over the last 4 weeks vs ~3/week target |
| Progressive overload | 20 | Share of tracked lifts whose 30-day e1RM >= 90-day e1RM |
| Volume quality | 16 | Share of trained muscle groups inside the optimal weekly range (VolumeLandmarks) |
| Recovery | 12 | Current overall recovery score (RecoveryAnalyzer) |
| Goal adherence | 12 | Average progress percent across active goals (GoalRepository) |
| Muscle balance | 12 | 100 minus the share of trained groups below optimal volume |
| Recovery discipline | 8 | Penalty for high-RPE sessions repeated within 18h (overreaching) |

## Formula
`score = round( sum(componentValue * weightPercent) / 100 )`, clamped to 0-100.
Each component value is itself clamped to 0-100. The result is deterministic for
a given set of inputs.

## Trends
`weeklyTrend = score - previousWeekScore`, `monthlyTrend = score - previousMonthScore`
(null when the prior value is unknown). Prior values can be persisted to show
trend deltas once historical scores are recorded.

## Where component values come from (no duplication)
`IntelligenceRepository.buildRepLogScore()` reads a single bounded window of
recent completed sessions plus small aggregate reads (progression scores, goals,
volume landmarks, recovery) and maps them to the seven 0-100 component values,
then calls `RepLogScoreEngine.compute`. Nothing is recomputed that an existing
engine already owns.

## UI
The Home `RepLogScoreCard` shows the score, the week/month trend, and an
expandable "How is this calculated?" panel listing every component with its
value, weight and one-line explanation.
