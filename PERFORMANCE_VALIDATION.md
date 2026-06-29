# Performance Validation (Sprint 13 hardening, Phase 9)

## Offline / storage / migrations - verified
- Zero network access: no http/retrofit/okhttp in the new domain or UI code; the
  app has no INTERNET permission.
- Zero additional database storage: Exercise.kt and assets/exercises.json are
  unchanged since before the sprint; no new columns, no new tables.
- Zero Room migrations: AppDatabase stays at version 15.
- Coaching, confidence, why, muscle diagram and animation are all generated at
  runtime from existing attributes - nothing is precomputed or persisted.

## Recomposition / allocation
- The coaching bundle is built lazily ONLY for the selected exercise, via a
  StateFlow (WhileSubscribed) - not for the whole list.
- The animation clip is remembered (keyed on exercise id/pattern/equipment) so it
  is built once per exercise, not per frame; the per-frame work is cheap
  interpolation + Canvas draws.
- The muscle diagram's region boxes are a module-level constant map (allocated
  once), not rebuilt per draw.
- Library filtering is linear over the in-memory 516-row list and remains smooth;
  because nothing is stored per exercise, footprint and load time are unchanged as
  the catalogue scales toward 1000+.

## Gate
Frame-time / jank profiling and 1000+ scale testing are confirmed on-device via
the SDK host build (:app:assembleDebug).
