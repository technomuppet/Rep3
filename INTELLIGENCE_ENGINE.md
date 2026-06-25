# RepLog Intelligence Engine - Documentation

## Purpose
Turn the seven existing analysis engines into ONE explainable, offline coaching
briefing, without recomputing anything or inventing advice.

## Layering
```
UI:    HomeScreen (TodaysBriefingCard, ContinueWorkoutCard)
         |  observes
VM:    HomeViewModel.briefing : StateFlow<TodaysBriefing?>   (cached on demand)
         |  calls once / on session-count change
DATA:  IntelligenceRepository.buildBriefing()                (Hilt @Singleton)
         |  gathers inputs (bounded queries) + reuses engines
DOMAIN: IntelligenceEngine.build(IntelligenceInputs) -> TodaysBriefing  (pure)
```

## Source engines reused (no duplication)
| Signal | Source engine | Input field(s) |
|---|---|---|
| Recovery score / label / directive / factors | `RecoveryAnalyzer.overallRecovery` + `RecoveryDashboard.from` | recoveryScore, recoveryStatusLabel, recoveryDirective, recoveryFactors |
| Ready / fatigued muscle groups | `RecoveryAnalyzer.muscleRecovery` (status FRESH/RECOVERED vs FATIGUED/VERY_FATIGUED) | readyMuscleGroups, fatiguedMuscleGroups |
| Recommended focus | `RecommendationEngine.generate().title` | recommendedFocus |
| Best rep range | `TrainingGenomeEngine.analyze` (trait "Rep range") | genomeBestRepRange |
| Under-target volume | `VolumeLandmarks.analyze` (status UNDER) | underVolumeGroups |
| Neglected muscles | `MuscleGapAnalyzer.analyze` (weak list from latest DNA) | neglectedMuscles |
| PB projection | `ProgressionForecaster.forecast` (best progression score) | topForecastLabel, topForecastConfidenceHigh |
| Goal forecast | `GoalRepository.forecastFor` | goalSummary |
| Strongest day / PB-rest gap / slow leg recovery | session history (computed in repo) | strongestDayOfWeek, prsAfterRestDays, slowRecoveryAfterHighVolume |

## Output: `TodaysBriefing`
- `recoveryScore: Int?`
- `recommendation: String` (rest if recovery < 45; else the recommended focus /
  most-recovered muscle / balanced full body)
- `reasons: List<String>` - one line per present signal (recovered/fatigued
  groups, low volume, projection, goal, rest directive). Falls back to a real
  recovery factor, never to fabricated text.
- `confidence: BriefingConfidence` (LOW/MEDIUM/HIGH) - scales with how many
  independent signals are present (a high-confidence forecast counts double).
- `coachInsights: List<CoachInsight>` - personalised statements, each tagged with
  its data `source`, emitted only when the underlying signal exists.

## Explainability guarantee
`IntelligenceEngine` only ever reads fields of `IntelligenceInputs`. A reason or
insight is appended solely when its source field is non-null/non-empty. There is
no random text, no model, no network. The `noInventedInsights` unit test asserts
that with no genome/day/gap/PR signals, zero coach insights are produced.

## Performance characteristics
- One bounded read of recent completed sessions (`getRecentCompletedSessions(60)`)
  feeds recovery, genome and volume; DNA snapshot, progression scores, goals and
  exercise library are small/aggregate reads.
- `HomeViewModel` caches the result and recomputes only when the completed
  session count changes, so logging a set never triggers the engine pass.

## Extending
Add a new signal by adding a field to `IntelligenceInputs`, populating it in
`IntelligenceRepository` from an existing engine, and appending a guarded line in
`buildReasons`/`buildCoachInsights`. Keep the engine pure and never recompute an
analysis another engine already owns.
