# Manual QA Checklist (Sprint 13.1, Priority 2)

Execute on the installed debug APK. Mark [P] Pass or [F] Fail. None pre-checked;
nothing here has been run in the build environment.

## Onboarding + Legal Acceptance
1. Fresh install, first launch.
   - Steps: launch app with no prior data.
   - Expected: Welcome -> Create Profile -> Training Preferences -> Disclaimer ->
     Terms -> Privacy -> Final Confirmation; Home is NOT reachable until finished.
   - [ ] P  [ ] F
2. Legal gating.
   - Steps: on each legal doc, try Continue before scrolling/checkbox.
   - Expected: Continue disabled until scrolled to bottom AND checkbox ticked;
     Decline cancels (no profile created); Final Confirmation requires typing the
     exact display name.
   - [ ] P  [ ] F
3. Re-acceptance on version bump (if testable): bump a legal version -> on next
   launch user must re-accept; existing workouts/templates preserved.
   - [ ] P  [ ] F

## Home
4. Personalised greeting.
   - Steps: complete onboarding with name "David".
   - Expected: time-of-day greeting "Good morning, David." + "David's training
     dashboard"; briefing/score cards render.
   - [ ] P  [ ] F

## Exercise Library (Sprint 13 focus)
5. Open Beginner / Intermediate / Advanced exercises.
   - Expected: each shows name, difficulty, equipment, primary + secondary
     muscles, purpose, 3 cues, confidence card, muscle diagram, animation, and all
     coaching sections; NO empty section, NO clipped text.
   - [ ] P  [ ] F
6. Coaching sections content.
   - Steps: expand How to Perform, Common Mistakes, Breathing, Tempo, Safety, Why,
     Alternatives.
   - Expected: exactly 4 numbered steps; exactly 3 cues; <=4 mistakes; breathing
     sentence; tempo "2-1-2" + explanation; ROM; <=3 safety; Why mentions goal +
     experience (+ recovery if data); Alternatives shows an easier option OR an
     explanation why none exists.
   - [ ] P  [ ] F
7. Animation controls.
   - Steps: tap Pause, then Play, then Restart.
   - Expected: pauses instantly; resumes; restarts to start; loops; no freeze.
   - [ ] P  [ ] F
8. Body diagram per region: open exercises for Chest, Back, Shoulders, Arms, Core,
   Glutes, Quads, Hamstrings, Calves, Adductors, Abductors, a full-body/cardio move.
   - Expected: each highlights the correct region(s); cardio/full-body uses the
     major-mover fallback; primary filled, secondary outlined; legend present.
   - [ ] P  [ ] F
9. Search filters individually: Muscle, Equipment, Difficulty, Experience, Goal,
   Movement Pattern, No Equipment, Home Workout, Machine Only.
   - Expected: correct, non-empty (except genuinely-empty combos), no duplicates.
   - [ ] P  [ ] F
10. Search combinations + reset: Beginner+Legs, Hypertrophy+Push, Strength+Pull;
    then Clear.
    - Expected: sensible results; Clear restores all 516.
    - [ ] P  [ ] F

## Workout Logging + Active Workout
11. Start a workout (quick start / template / recommendation).
    - Expected: active workout opens; can log sets; weight/reps keyboard focuses
      the chosen field; Smart Repeat works.
    - [ ] P  [ ] F
12. Rest timer.
    - Expected: starts on set completion (if enabled); notification shows; sound/
      vibrate per settings; survives backgrounding.
    - [ ] P  [ ] F
13. Finish workout.
    - Expected: completion summary + PB celebration if any; data persists.
    - [ ] P  [ ] F

## Progress / Training DNA / Recovery / Intelligence / Muscle Balance / Goals
14. Open each screen.
    - Expected: loads (loading -> content or empty state); no crash; analytics
      reflect logged data; Recovery Centre title personalised ("David's Recovery
      Centre"); navigation back works (back arrow on secondary screens).
    - [ ] P  [ ] F

## Settings + Export/Import + Legal Centre
15. Settings -> Legal: open Disclaimer, Terms, Privacy, Acceptance History,
    Open Source Licences, App Version.
    - Expected: each opens read-only; Acceptance History shows date/time/versions/
      app version; App Version shows the build.
    - [ ] P  [ ] F
16. Export CSV + JSON backup.
    - Expected: file written to Downloads/RepLog or chosen folder; "Open Folder"
      works.
    - [ ] P  [ ] F
17. Import / restore JSON.
    - Expected: confirm dialog; data merged; no crash.
    - [ ] P  [ ] F

## Navigation + Notifications
18. Bottom tabs (Home, Training, Progress, History, Exercises, Settings).
    - Expected: switching tabs never stacks/loops; Home always reachable; back from
      a secondary screen returns correctly.
    - [ ] P  [ ] F
19. POST_NOTIFICATIONS prompt (Android 13+).
    - Expected: requested appropriately; denying does not crash.
    - [ ] P  [ ] F

## Sign-off
- [ ] All items Pass on the device matrix in DEVICE_TEST_PLAN.md.
