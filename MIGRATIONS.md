# RepLog Database Migrations

Current Room database version: **8**

## Version 1

Initial schema:

- exercises
- workout_sessions
- session_exercises
- set_logs
- workout_templates
- template_exercises

## Migration 1 → 2

Adds bodyweight tracking:

```sql
CREATE TABLE bodyweight_logs
```

## Migration 2 → 3

Adds smart exercise metadata, advanced set metadata and supersets.

Exercises:

```sql
primaryMuscles TEXT NOT NULL DEFAULT ''
secondaryMuscles TEXT NOT NULL DEFAULT ''
movementPattern TEXT NOT NULL DEFAULT ''
difficulty TEXT NOT NULL DEFAULT 'Intermediate'
```

Set logs:

```sql
setType TEXT NOT NULL DEFAULT 'Working'
rpe REAL
tempo TEXT
```

Session exercises:

```sql
supersetGroup TEXT
```

Index:

```sql
index_session_exercises_supersetGroup
```

## Migration 3 → 4

Adds local exercise media path:

```sql
mediaAsset TEXT NOT NULL DEFAULT ''
```

## Migration 4 → 5

Adds target fields to template exercises:

```sql
targetReps INTEGER NOT NULL DEFAULT 8
targetWeight REAL
```

## Migration 5 → 6

Adds persisted workout prescriptions:

```sql
CREATE TABLE workout_prescriptions
```

Fields:

- sessionId
- exerciseId
- source
- targetSets
- targetReps
- targetWeight
- adjustment
- reason
- createdAt

Indexes:

- sessionId
- exerciseId
- unique sessionId + exerciseId

## Migration 6 → 7

Adds performance indexes for release hardening:

```sql
CREATE INDEX index_workout_sessions_startTime ON workout_sessions(startTime)
CREATE INDEX index_workout_sessions_endTime ON workout_sessions(endTime)
CREATE INDEX index_set_logs_timestamp ON set_logs(timestamp)
CREATE INDEX index_set_logs_sessionExerciseId_setNumber ON set_logs(sessionExerciseId, setNumber)
CREATE INDEX index_exercises_name ON exercises(name)
```

## Migration 7 → 8

Adds architecture foundation storage for future Training DNA and knowledge graph systems:

```sql
CREATE TABLE training_dna_metrics
CREATE TABLE knowledge_graph_relations
```

Training DNA indexes:

```sql
index_training_dna_metrics_dimension
index_training_dna_metrics_subjectType
index_training_dna_metrics_subjectId
index_training_dna_metrics_dimension_subjectType_subjectId
```

Knowledge graph indexes:

```sql
index_knowledge_graph_relations_sourceType
index_knowledge_graph_relations_sourceId
index_knowledge_graph_relations_targetType
index_knowledge_graph_relations_targetId
index_knowledge_graph_relations_relationType
index_knowledge_graph_relations_sourceType_sourceId_targetType_targetId_relationType
```

## Required QA

Test:

- fresh install creates v6 schema
- upgrade v1 → v6
- upgrade v2 → v6
- upgrade v3 → v6
- upgrade v4 → v6
- upgrade v5 → v6
- existing workouts remain readable
- existing templates remain readable
- exercise metadata upgrade runs
- prescriptions are created for new adaptive/template workouts
