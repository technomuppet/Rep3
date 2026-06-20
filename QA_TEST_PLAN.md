# RepLog QA Test Plan

## Environment

Test on:

- Android Studio with JDK 17+
- Pixel emulator API 35
- at least one physical Android device

## Build Tests

```bash
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
./gradlew clean assembleDebug
./gradlew bundleRelease
```

## Automated Test Focus

Current test scaffolding covers:

- PlateCalculator
- ProgressionEngine
- BackupJson basic and full roundtrip
- WorkoutCsvExporter prescription columns
- ActiveWorkoutRecovery
- RestoreMergePlanner
- AdaptiveProgramEngine smoke test
- WidgetStateFactory
- Room migration v6 → v7 instrumentation scaffold

## First Launch

- Fresh install opens onboarding.
- Unit selection saves.
- Exercise data seeds.
- Built-in templates seed.
- App does not show onboarding after completion.

## Workout Logging

- Start empty workout.
- Add exercise.
- Add working set.
- Add warmup set.
- Add failure set.
- Add RPE.
- Add tempo.
- Edit set.
- Delete set with confirmation.
- Assign superset group.
- Remove superset group.
- Finish workout.
- Summary appears.

## Templates

- Start built-in template.
- Create custom template.
- Edit custom template name.
- Reorder exercises.
- Edit target sets/reps/weight.
- Start custom template.
- Verify targets appear in workout.
- Delete custom template with confirmation.

## Adaptive Workouts

- Complete a normal workout.
- Return to Workout screen.
- Verify vNext card appears.
- Start adaptive workout.
- Verify prescriptions appear in exercise cards.
- Hit one target and miss one target.
- Finish workout.
- Verify hit/miss summary.

## History

- Workout appears in history.
- Calendar highlights training day.
- Delete workout requires confirmation.
- Delete removes workout.

## Progress

- Bodyweight log works.
- Bodyweight delete requires confirmation.
- Goal line works.
- Lifetime stats update.
- Movement balance updates.
- Muscle balance updates.
- Recovery score appears.
- Forecast appears after enough data.
- Training DNA appears after enough data.

## Exercise Library

- Search by name.
- Search by movement pattern.
- Search by muscle.
- Add custom exercise.
- Delete custom exercise requires confirmation.
- Exercise detail analytics opens.

## Export / Import

- CSV export generates file.
- CSV share sheet opens.
- JSON backup generates file.
- JSON share sheet opens.
- JSON import asks for confirmation.
- Local restore asks for confirmation.
- Duplicate restore skips matching sessions/bodyweights.
- Prescriptions survive backup/restore.

## Rest Timer

- Rest timer appears after set.
- Skip works.
- Notification behaviour works on device.

## Accessibility

- Large font scale does not break main flows.
- TalkBack can identify core controls.
- Icon-only destructive actions have labels.
- Touch targets are usable.

## Regression Checklist

Run before every release candidate:

- Onboarding
- Start workout
- Finish workout
- Progress tab
- Export CSV
- Backup JSON
- Restore JSON
- Active workout recovery
