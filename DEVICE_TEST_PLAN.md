# Device Test Plan (Sprint 13.1, Priority 3)

To be executed by a human (or CI emulator matrix) after installing the debug APK.
NOTHING below has been run in this environment.

## Build the APK (on an SDK host with JDK 17)
1. Set local.properties sdk.dir, or ANDROID_HOME.
2. ./gradlew :app:assembleDebug  ->  app/build/outputs/apk/debug/app-debug.apk
3. adb install -r app-debug.apk

## Device matrix
| Device class       | Examples                         | Priority |
|--------------------|----------------------------------|----------|
| Small phone        | ~5.0-5.4in, 720p (or emulator)   | High     |
| Large phone        | 6.5in+, 1080p+                   | High     |
| Tablet             | 10in, sw720dp                    | Medium   |
| Foldable           | inner/outer display, fold/unfold | Medium   |

## OS versions
- Android 13 (API 33) - POST_NOTIFICATIONS runtime prompt path.
- Android 14 (API 34) - target SDK; foreground-service shortService behaviour.
- Android 15 (API 35) - forward-compat / edge-to-edge.

## Per-configuration sweep (run on each device class)
For each, run the MANUAL_QA_CHECKLIST end-to-end under:
- [ ] Portrait, light mode, 100% font
- [ ] Portrait, dark mode, 100% font
- [ ] Landscape, 100% font
- [ ] 200% font scale (Settings > Display > Font size: largest)
- [ ] TalkBack ON (focus order, labels, control reachability)
- [ ] Gesture navigation ON (no back-gesture conflicts; dialogs dismiss)
- [ ] Tablet/foldable layout (reflow; fold/unfold continuity)

## Focus areas for Sprint 13 (Exercise Library 2.0)
- [ ] Open >=1 Beginner, >=1 Intermediate, >=1 Advanced exercise; confirm every
      coaching section renders with content and no clipping (see Priority 1 list).
- [ ] Animation: Play, Pause (stops instantly), Resume, Restart; loops smoothly;
      no jank on a low-end device.
- [ ] Body diagram highlights match the named muscles for each region group.
- [ ] All search filters individually + combinations (see SEARCH cases below).
- [ ] TalkBack reads the diagram muscles, animation, and section headers.

## Search combinations to verify (expected counts from the 516 catalogue)
| Filter                | Expected (approx) | Pass/Fail |
|-----------------------|-------------------|-----------|
| Beginner + Legs       | ~82               | [ ]       |
| Beginner + No Equipment | subset of 84    | [ ]       |
| Hypertrophy + Push    | subset of 133     | [ ]       |
| Fat Loss + Cardio     | ~ up to 37        | [ ]       |
| Machine Only          | ~173              | [ ]       |
| Home Workout          | ~232              | [ ]       |
| Strength + Pull       | subset            | [ ]       |
| (Clear/reset)         | all 516           | [ ]       |

## Offline test
- [ ] Enable airplane mode / disable network; exercise library, coaching,
      diagrams, animations and search all continue to work (no network is used).

## Exit criteria
All checklist items Pass on small phone, large phone, and at least one of
tablet/foldable, across the OS versions available, with no clipped content, no
crashes, and smooth animation/scrolling.
