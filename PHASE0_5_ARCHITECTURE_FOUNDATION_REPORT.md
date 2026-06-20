# RepLog Phase 0.5 Architecture Foundation Report

Date: 2026-06-19

## Objective

Transform RepLog from a feature-based app structure into a platform architecture that can support future roadmap systems without major refactors.

No user-facing behavior was intentionally changed in this phase.

## Architecture Diagrams

### High-Level Architecture

```text
UI layer
  ↓
ViewModels
  ↓
Repositories
  ↓
Room / DataStore

Domain engines and contracts sit beside ViewModels and repositories:

UI/ViewModels → domain/* contracts and engines → repositories/data stores
```

### New Domain Package Structure

```text
domain/
├── analytics/
│   ├── AnalyticsEngine
│   ├── DefaultAnalyticsEngine
│   ├── AnalyticsContext
│   ├── VolumeSummary
│   └── TrendSummary
├── recovery/
│   ├── RecoveryAnalyzer
│   ├── RecoveryState
│   └── MuscleRecoverySignal
├── trainingdna/
│   ├── TrainingDnaDimension
│   ├── TrainingDnaSignal
│   └── TrainingDnaStore
├── coach/
│   ├── Recommendation
│   ├── RecommendationType
│   ├── RecommendationPriority
│   ├── RecommendationGenerator
│   └── CoachEngine
├── programs/
│   ├── ProgramBuilderEngine
│   ├── ProgramGoal
│   ├── ProgramBuilderRequest
│   └── ProgramDraft
├── discoveries/
│   ├── KnowledgeEntityType
│   ├── KnowledgeRelation
│   ├── KnowledgeGraphStore
│   └── DiscoveryEngine
├── forecasting/
│   ├── ForecastingEngine
│   ├── ForecastType
│   ├── ForecastRequest
│   └── ForecastResult
└── insights/
    ├── Insight
    ├── InsightCategory
    ├── InsightSeverity
    ├── InsightGenerator
    └── InsightsEngine
```

### Dependency Diagram

```text
ui/*
  ↓
ViewModels
  ↓                         ↘
data/repository/*            domain/*
  ↓                             ↓
data/db/*                    pure contracts/models/engines
  ↓
Room database
```

Domain contracts do not depend on UI. Most domain contracts depend only on data models or other domain models.

## Implemented Foundations

### Analytics Core

Added:

```text
app/src/main/java/com/replog/domain/analytics
```

Includes:

- `AnalyticsEngine`
- `DefaultAnalyticsEngine`
- volume summary calculation
- estimated 1RM trend extraction
- generic trend calculation

This is deterministic and offline.

### Insights Framework

Added:

```text
app/src/main/java/com/replog/domain/insights
```

Includes:

- `Insight`
- `InsightCategory`
- `InsightSeverity`
- `InsightContext`
- `InsightGenerator`
- `InsightsEngine`
- `CompositeInsightsEngine`

No UI was added.

### Training DNA Foundation

Added domain contracts:

```text
app/src/main/java/com/replog/domain/trainingdna
```

Added database model:

```text
TrainingDnaMetric
```

Stored dimensions can represent:

- recovery speed
- volume tolerance
- frequency tolerance
- preferred rep ranges
- exercise responsiveness
- fatigue sensitivity

No calculations were added.

### Knowledge Graph Foundation

Added domain contracts:

```text
app/src/main/java/com/replog/domain/discoveries
```

Added database model:

```text
KnowledgeGraphRelation
```

Supports relationships between:

- exercises
- muscles
- bodyweight
- volume
- frequency
- performance
- recovery
- programs

No discovery generation was added.

### Coaching Framework

Added:

```text
app/src/main/java/com/replog/domain/coach
```

Includes:

- `Recommendation`
- `RecommendationType`
- `RecommendationPriority`
- `RecommendationGenerator`
- `CoachEngine`

No recommendation logic was added.

### Program Builder Foundation

Added:

```text
app/src/main/java/com/replog/domain/programs
```

Supports future goals:

- strength
- hypertrophy
- powerlifting
- fat loss
- general

No program generation logic was added.

### Forecasting Framework

Added:

```text
app/src/main/java/com/replog/domain/forecasting
```

Supports future forecast types:

- strength
- bodyweight
- volume

No forecast calculations were added in this framework.

## Database Migration Report

Database version increased from 7 to 8.

Added:

```kotlin
MIGRATION_7_8
```

New tables:

```text
training_dna_metrics
knowledge_graph_relations
```

New DAOs:

```text
TrainingDnaDao
KnowledgeGraphDao
```

New repositories:

```text
TrainingDnaRepository
KnowledgeGraphRepository
```

Hilt providers added for both DAOs.

## Testing Foundation

Added test fixtures:

```text
app/src/test/java/com/replog/testing/TestFixtures.kt
```

Added analytics engine tests:

```text
app/src/test/java/com/replog/domain/analytics/DefaultAnalyticsEngineTest.kt
```

These are meaningful tests for the new analytics foundation rather than fake placeholder tests.

## Validation Performed

Static validation completed:

- File inventory updated.
- Brace-balance checks passed on changed files.
- Domain package structure verified.
- New DAOs wired into AppDatabase.
- Hilt DAO providers added.
- Migration documentation updated.

Full Gradle validation is still blocked in the sandbox by Maven TLS handshake issues and must be run locally.

## Technical Debt Report

Still present:

- Existing analytics logic remains in `ProgressViewModel` for behavior preservation.
- Large UI files remain large.
- Backup/restore remains in `SettingsViewModel`.
- No full domain migration of existing production logic yet.

Intentional choice:

- This phase establishes foundations without changing user behavior.
- Existing logic should be migrated behind domain engines in a later approved phase after compile baseline passes.

## Risks Introduced

| Risk | Severity | Mitigation |
|---|---:|---|
| Room schema mismatch from new tables | Medium | Local Room migration tests required |
| Added interfaces unused initially | Low | They are foundation contracts for future phases |
| More database migrations | Medium | Migration docs updated; test locally |
| Domain naming overlap with data repositories | Low | Domain interfaces use `Store` where repository names could conflict |

## Success Criteria Status

| Criteria | Status |
|---|---|
| No user-facing behavior changes | Met |
| Offline-first preserved | Met |
| No paid APIs | Met |
| No cloud dependencies | Met |
| Domain structure created | Met |
| Analytics core foundation created | Met |
| Insights framework created | Met |
| Training DNA storage created | Met |
| Knowledge graph foundation created | Met |
| Coach framework created | Met |
| Program builder foundation created | Met |
| Forecasting framework created | Met |
| Test scaffolding added | Met |
| Full local compile validation | Pending local environment |

## Next Recommended Step

Do not add new feature behavior yet.

Next should be local validation:

```bash
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
./gradlew clean assembleDebug
```

After compile validation, begin migrating existing `ProgressViewModel` analytics logic into the new `domain.analytics`, `domain.recovery`, `domain.forecasting`, and `domain.insights` packages incrementally.
