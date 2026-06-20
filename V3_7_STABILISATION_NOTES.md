# V3.7 Stabilisation Notes

This pass implements the safety and data-completeness work recommended by the project audit.

## Implemented

### Restore safety
- JSON import now asks for confirmation before merging data.
- Local JSON restore now asks for confirmation before merging data.
- User is warned that restores merge data and can create duplicates if repeated.
- Restore now skips simple duplicate sessions using start time, exercise count and set count.
- Restore now skips duplicate bodyweight logs using timestamp and weight.

### Destructive action confirmations
- Discard active workout requires confirmation.
- Delete custom template requires confirmation.
- Delete set requires confirmation.
- Delete workout from History requires confirmation.
- Delete custom exercise requires confirmation.
- Delete bodyweight entry requires confirmation.

### Backup completeness
- JSON backup schema upgraded to version 3.
- JSON backups now include workout prescriptions.
- JSON restore recreates prescriptions after restoring sessions/exercises.

### CSV completeness
- CSV export now includes prescription fields:
  - prescription source
  - target sets
  - target reps
  - target weight
  - target hit status

### Build configuration cleanup
- Removed custom KSP Gradle plugin resolution mapping from `settings.gradle.kts`.
- Kept Hilt plugin module mapping because the sandbox cannot resolve the marker artifact directly.

### Documentation
- Added `ARCHITECTURE.md`.
- Added `MIGRATIONS.md`.
- Added `QA_TEST_PLAN.md`.

## Still Required Locally

- Run Android Studio sync with JDK 17+.
- Run `./gradlew clean assembleDebug`.
- Fix any compile errors from Kotlin/Compose/Room/Hilt after dependency resolution succeeds.
- Test Room migrations through v6.
