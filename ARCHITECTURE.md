# RepLog Architecture

## Overview

RepLog is a local-first Android app built with:

- Kotlin
- Jetpack Compose
- Room
- Hilt
- DataStore
- Navigation Compose

The app intentionally avoids accounts, mandatory cloud sync and subscriptions. All core data lives locally and can be exported.

## Layers

### Domain Layer

Path:

```text
app/src/main/java/com/replog/domain
```

Domain packages define platform contracts for future systems:

- analytics
- recovery
- trainingdna
- coach
- programs
- discoveries
- forecasting
- insights

These are intentionally UI-free and deterministic. They are designed for offline/on-device intelligence.

### UI Layer

Path:

```text
app/src/main/java/com/replog/ui
```

Screens:

- Home
- Workout
- Progress
- History
- Exercise Library
- Settings
- Onboarding

State is exposed from ViewModels through `StateFlow` and consumed by Compose.

### ViewModel Layer

ViewModels coordinate repositories and UI state. Some ViewModels currently contain analytics/business logic. Long term, extract these into dedicated domain engines.

High-priority extraction candidates:

- `ProgressViewModel` analytics logic
- `ActiveWorkoutViewModel` active workout/template/prescription logic

### Data Layer

Path:

```text
app/src/main/java/com/replog/data
```

Room entities include:

- Exercise
- WorkoutSession
- SessionExercise
- SetLog
- WorkoutTemplate
- TemplateExercise
- BodyweightLog
- WorkoutPrescription

### Repository Layer

Repositories wrap DAOs and provide a stable API to ViewModels.

### Utility / Domain Engines

Path:

```text
app/src/main/java/com/replog/util
```

Current engines/tools:

- AdaptiveProgramEngine
- BackupJson
- DataSeeder
- PlateCalculator
- PreferencesManager
- WorkoutCsvExporter

## Key Design Choices

### Local-first

Room is the source of truth for training data.

### Data ownership

CSV and JSON export are first-class features.

### Prescriptions

`WorkoutPrescription` stores exact targets for workouts generated from adaptive plans or templates. This prevents target evaluation from changing when history changes later.

### Structured exercise metadata

Exercise metadata powers analytics:

- movementPattern
- primaryMuscles
- secondaryMuscles
- difficulty
- mediaAsset

## Known Technical Debt

- Large Compose files should be split.
- Analytics logic should be moved out of `ProgressViewModel`.
- Active workout/template logic should be split out of `ActiveWorkoutViewModel`.
- JSON restore currently merges data and skips simple duplicates, but a full merge/replace UX would be better.
