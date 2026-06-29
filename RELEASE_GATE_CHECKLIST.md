# Release Gate Checklist (Sprint 13.1, Priority 6)

Distinguishes what is VERIFIED at source level (done here) from what REMAINS for
human/CI device validation. No item is marked done on the basis of device testing,
because no device testing was performed in this environment.

## Verified at source level (DONE here)
- [x] Sprint 13 features wired (composables/ViewModels/routes all referenced;
      19=19 routes, 0 duplicates).
- [x] No dead code, no orphaned assets, no TODO/FIXME/HACK, no placeholder code.
- [x] Unused import removed; defensive guards added (animation empty-frame guard;
      debug-only coaching-contract asserts passing for all 516).
- [x] Domain compiles clean under kotlinc 1.9.22 (38 classes, 0 warnings).
- [x] All 516 exercises have complete coaching/confidence/why/animation/diagram;
      0 duplicates; 0 orphaned muscle mappings (re-verified).
- [x] Search dimensions correct against the real catalogue; reset returns all 516.
- [x] No Room schema change (DB v15); buildConfig enabled; no new dependency.
- [x] Offline preserved: no INTERNET permission, no network code.
- [x] Build config coherent (AGP 8.3.2 / Kotlin 1.9.22 / KSP matched / Compose
      compiler 1.5.9 / Gradle 8.7 / JDK 17); no likely assembleDebug breakers.

## REMAINING - human/CI on an Android SDK host (NOT done here)
- [ ] ./gradlew :app:assembleDebug succeeds; APK installs.
- [ ] ./gradlew :app:assembleRelease (signed) succeeds; (optional) Play pre-launch
      report clean.
- [ ] MANUAL_QA_CHECKLIST passes on small phone, large phone, and tablet/foldable.
- [ ] DEVICE_TEST_PLAN matrix passes across Android 13/14/15, portrait/landscape,
      light/dark, 100%/200% font, TalkBack, gesture nav.
- [ ] Runtime risks R1-R10 (RUNTIME_RISK_REPORT) each confirmed OK on-device.
- [ ] Offline test (airplane mode) confirmed.
- [ ] No regressions per REGRESSION_REPORT confirmed on-device.

## Release decision
RELEASE IS NOT YET CLEARED. The source/architecture/build configuration is ready
and no source-level blocker remains, but the mandatory on-device validation
(assembleDebug + the device matrix) has not been performed in this environment and
is a genuine open gate. Clear it on an SDK host using the plan/checklists in this
sprint, then sign off here.
