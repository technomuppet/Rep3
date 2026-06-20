# Phase 1 Release Hardening Report

Date: 2026-06-19

## Scope

Phase 1 release hardening is in progress. This phase prioritises stability, validation, migration safety, recovery logic, export/import correctness and release readiness over new product features.

## Completed Blocks

### 1. Database Optimization

Room database version increased from 6 to 7.

Added migration:

```kotlin
MIGRATION_6_7
```

Indexes added:

```sql
index_workout_sessions_startTime
index_workout_sessions_endTime
index_set_logs_timestamp
index_set_logs_sessionExerciseId_setNumber
index_exercises_name
```

Purpose:

- faster session ordering
- faster completed/in-progress filtering
- faster set ordering within exercises
- faster exercise name lookup
- better analytics scalability

### 2. Entity Index Alignment

Updated Room entities so fresh v7 schemas match migration-created indexes:

- `Exercise` declares `Index("name")`
- `WorkoutSession` declares `Index("startTime")` and `Index("endTime")`
- `SetLog` declares `Index("timestamp")` and composite `Index("sessionExerciseId", "setNumber")`

This is required for Room schema validation because migrations and fresh schema generation must agree.

### 3. Widget Framework Foundation

Added package:

```text
app/src/main/java/com/replog/widget
```

Files:

```text
WidgetContracts.kt
WidgetStateFactory.kt
```

This is architecture only. No widget provider is registered yet, so there is no runtime widget risk.

Prepared widget concepts:

- Quick Start Widget
- Today's Workout Widget
- Active Workout Widget action

### 4. Active Workout Recovery Logic Extraction

Added:

```text
app/src/main/java/com/replog/util/ActiveWorkoutRecovery.kt
```

`ActiveWorkoutViewModel` now uses a small deterministic resolver for active workout recovery decisions:

- none
- resume
- clear stale ID

Added tests:

```text
app/src/test/java/com/replog/util/ActiveWorkoutRecoveryTest.kt
```

### 5. Restore Merge Planner

Added:

```text
app/src/main/java/com/replog/util/RestoreMergePlanner.kt
```

`SettingsViewModel` now uses it to filter duplicate sessions and bodyweight logs during restore.

Added tests:

```text
app/src/test/java/com/replog/util/RestoreMergePlannerTest.kt
```

### 6. Validation Test Scaffolding

Added JVM test files:

```text
app/src/test/java/com/replog/util/PlateCalculatorTest.kt
app/src/test/java/com/replog/util/ProgressionEngineTest.kt
app/src/test/java/com/replog/util/BackupJsonTest.kt
app/src/test/java/com/replog/util/BackupJsonFullRoundTripTest.kt
app/src/test/java/com/replog/util/WorkoutCsvExporterTest.kt
app/src/test/java/com/replog/util/AdaptiveProgramEngineSmokeTest.kt
app/src/test/java/com/replog/widget/WidgetStateFactoryTest.kt
```

Coverage:

- custom plate parsing and load calculation
- progression engine hit/miss behavior
- JSON backup encode/decode basic roundtrip
- JSON backup preserves advanced set, exercise and prescription fields
- CSV export includes prescription columns and target-hit status
- adaptive engine smoke/performance test over synthetic history
- widget state factory behavior

### 7. Migration Test Scaffolding

Added Room testing dependency:

```kotlin
androidTestImplementation("androidx.room:room-testing:$roomVersion")
```

Added instrumentation migration test:

```text
app/src/androidTest/java/com/replog/data/db/AppDatabaseMigrationTest.kt
```

Current test covers:

- v6 → v7 migration
- performance indexes created
- Room migration validation helper integration

### 8. Documentation Updated

Updated:

```text
MIGRATIONS.md
RELEASE_CHECKLIST.md
FILE_LIST.txt
```

## Validation Attempt

A full Gradle test/build cannot complete in this sandbox because Maven downloads fail with TLS handshake errors. These tests are ready to run locally with:

```bash
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
./gradlew clean assembleDebug
```

## Static Validation Completed

- File inventory updated.
- Brace-balance checks passed on changed files.
- New utilities referenced from production code where applicable.
- Migration v7 and entity index alignment reviewed.
- Test inventory verified.
- No paid API/cloud dependency introduced.

## Risks

- Migration v7 must be validated by Room in a local Android build.
- Instrumentation migration test may require minor constructor/signature adjustment depending on exact Room testing API resolution.
- Widget contracts are intentionally non-runtime until provider implementation is approved.
- JVM tests require local dependency resolution, unavailable in the sandbox due TLS issues.

## Next Phase 1 Tasks

1. Run local Android Studio build.
2. Run JVM unit tests.
3. Run migration instrumentation tests.
4. Add true repository/database integration tests after local Room test harness is confirmed.
5. Benchmark Progress screen with generated demo data and larger synthetic data.
6. Fix compile errors discovered locally.
