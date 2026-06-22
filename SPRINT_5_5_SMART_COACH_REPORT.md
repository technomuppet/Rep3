# RepLog V2 — Sprint 5.5: Smart Coach Experience

_Branch: `integration/sprint4-sprint5`. Goal: expose the existing intelligence systems to users. No new recommendation/analytics logic was written — only presentation, wiring, and persistence around the systems that already existed._

---

## Deliverables

| # | Deliverable | Status | Where |
|---|---|---|---|
| 1 | Smart Coach dashboard card | ✅ | `ui/coach/SmartCoachCard.kt`, wired into `ui/home/HomeScreen.kt` |
| 2 | Explainable recommendations | ✅ | `domain/recommendation/RecommendationExplanation.kt` + expandable "Why this recommendation?" in the card |
| 3 | Smart Start workflow | ✅ | `ui/coach/CoachHandoff.kt`, `ActiveWorkoutViewModel.consumePendingRecommendation()`, nav arg `?fromRecommendation` |
| 4 | Recommendation history screen | ✅ | `ui/coach/CoachHistoryScreen.kt`, route `coach_history` |
| 5 | DNA interpretation layer | ✅ | `domain/trainingdna/DnaInterpreter.kt`, "What your DNA means" card in `TrainingDnaInsightScreen` |
| 6 | Recommendation wiring audit | ✅ | this document, §Phase 6 |

All new logic helpers are pure Kotlin and **unit-tested** (6 tests, all passing under a standalone JUnit run).

---

## Phase 1 — Smart Coach Home Card

A new "Today's Recommendation" card is the first thing on the Home screen. It renders, straight from the existing `RecommendationRepository.generateRecommendationAndEnsureDNA()`:

- **Recommended workout** — `recommendation.title` (e.g. "Train Push").
- **Recovery status** — parsed from the engine's `dataUsed` "Recovery score…" line.
- **Plateau alerts** — the engine's "Stalled lifts" line.
- **Progression suggestion** — first planned exercise with a target (e.g. "Bench Press: 82.5 kg × 5").
- **Confidence score** — `recommendation.confidenceScore` with a progress bar.

**Recompute only when necessary:** `CoachViewModel` regenerates only when the calendar day changes *or* the completed-session count changes since the last generation (tracked in `PreferencesManager` via `coachLastGenDay` / `coachLastGenSessionCount`). Otherwise it reuses the in-memory recommendation.

**Persistence:** accepting writes a `RecommendationHistory` row ("Accepted"); completing the workout upgrades it to "Completed"; "Not today" writes "Rejected".

## Phase 2 — Recommendation Explanation Layer

`RecommendationExplanation.contributions()` re-buckets the engine's **already-produced** `dataUsed` + `reasoning` strings into the four required categories — **Recovery, Plateau, Training DNA, Frequency** (plus an "Other factors" catch-all so nothing is hidden). The card's expandable "Why this recommendation?" section lists each contribution and the expected outcome. No black box: every line shown is sourced from the engine output.

## Phase 3 — Smart Start Button

"Start Recommended Workout" maps the recommendation to one tap:
- **TRAIN / PROGRESS / REPEAT / DELOAD** (any plan present) → `CoachHandoff` stages the recommendation; navigation opens the Workout screen with `?fromRecommendation=true`; `ActiveWorkoutViewModel.consumePendingRecommendation()` builds the session from the plan's `PlannedExercise` list using the existing prescription pipeline (`source = "Coach"`).
- **REST** (or no plan) → routes to recovery guidance (Training DNA screen) instead of starting a session.

The accepted recommendation is tracked so finishing the workout marks it "Completed".

## Phase 4 — Recommendation History Screen ("Coach History")

`CoachHistoryScreen` reads the `recommendation_history` table via `RecommendationRepository.getRecommendationHistory()`. Each entry shows date, title, explanation, split, confidence, and outcome (Completed / Accepted / Rejected with reason). A header summarises Followed / Skipped / Total so users can see coaching consistency over time. Reachable from a "Coach History" button on Home.

## Phase 5 — DNA Integration

`DnaInterpreter.interpret(snapshot)` translates the existing `TrainingDnaSnapshot` into plain English with no raw numbers left unexplained, e.g.:
- "You respond best to: **4–6 reps**"
- "You recover fastest when training: **4x per week**"
- "Most responsive muscle: **Back**"
- "Slowest progressing lift: **Overhead Press**"

Placeholder values ("Not enough data", "—") are filtered out. Surfaced as a "What your DNA means" card near the top of the Training DNA screen.

---

## Phase 6 — Recommendation Wiring Audit

**Question:** are `RecommendationEngine`, `WorkoutPlanGenerator`, `RecoveryAnalyzer`, `ProgressionDecider` all actively used?

**Result: YES — all four are now live on a user-reachable path.**

```
Home / Coach History screen
        │
        ▼
CoachViewModel  ──┐         ActiveWorkoutViewModel (Smart Start + completion)
        │         │                 │
        ▼         ▼                 ▼
RecommendationRepository ───────────┘
        │  (engine = RecommendationEngine())
        ▼
RecommendationEngine.generate(context)
        ├── RecoveryAnalyzer.overallRecovery() / .muscleRecovery()   [lines 25–26]
        ├── WorkoutPlanGenerator.generate() / .splitName() / .repTargetFromDna()  [129+]
        └── ProgressionDecider.decide()                              [168, 226]
```

- `RecommendationEngine` — consumed by `RecommendationRepository` (+ referenced by the new `RecommendationExplanation` / `SmartCoachCard`). **Live.**
- `RecoveryAnalyzer` — called inside `RecommendationEngine.generate()`. **Live.**
- `WorkoutPlanGenerator` — called inside `RecommendationEngine`. **Live.**
- `ProgressionDecider` — called inside `RecommendationEngine.applyProgression()` and the repeat-plan builder. **Live.**

### Dead recommendation pathway removed
- **`domain/coach/CoachContracts.kt`** (`CoachEngine`, `RecommendationGenerator`, `CoachContext`, plus duplicate `Recommendation` / `RecommendationType`) — a Sprint-4 legacy contract set **superseded** by `domain/recommendation` and **consumed by nothing**. Removed. This also eliminates the duplicate-type-name situation flagged in the prior audit: there is now exactly one `Recommendation` and one `RecommendationType` (the active `domain/recommendation` versions).

### Retained (not dead pathways)
- `RecommendationRepository.generateRecommendation()` (non-DNA) — used internally by `generateRecommendationAndEnsureDNA()`. Kept.
- `RecommendationRepository.getSuccessRate()` — currently unused (the Coach History header derives counts from the history flow instead). Harmless public helper; left in place to avoid changing the repository's API surface.

---

## Verification

- **Pure-Kotlin compile** (kotlinc 1.9.22, Room annotations stubbed): full `domain/*` + `data/model/*` compile clean after the `domain/coach` removal (exit 0, 128 classes).
- **Unit tests:** `RecommendationExplanationTest` (3) + `DnaInterpreterTest` (3) — **all 6 pass**.
- **Reference integrity:** every repository/prefs/model method and `CoachUiState` field referenced by the new ViewModels/UI verified to exist; navigation routes consistent; bottom-bar selection updated to ignore the new query arg; no conflict markers.
- **Not verifiable here (no Android SDK):** full `:app:assembleDebug`, Hilt/KSP codegen, Compose UI compilation. The new `@HiltViewModel` `CoachViewModel` and `@Singleton @Inject CoachHandoff` need no new Hilt module (constructor bindings only). Confirm on an SDK host.

---

## Success criterion

A new user opening RepLog now sees **"Today's Recommendation"** at the top of Home with a one-tap **Start Recommended Workout** — answering *"What should I train today?"* without opening any analytics screen. The "Why this recommendation?" panel and the "What your DNA means" card make the intelligence transparent, turning RepLog from a logger into a coach.
