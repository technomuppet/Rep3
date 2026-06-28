# Code Quality Audit (Phase 7)

## Verified clean
- 0 conflict markers; 0 TODO/FIXME/STOPSHIP/XXX; 0 placeholder/stub/"not
  implemented" in production code.
- No unused ViewModels (all 12 are referenced/`hiltViewModel()`'d).
- No unused screens (every `*Screen` composable is registered in the NavGraph).
- No duplicate models or duplicate navigation graphs (single `NavHost`).
- Pure domain+model compiles standalone (kotlinc 1.9.22): 231 classes.

## Risks / smells (no critical defects)
- MEDIUM — Positional `combine(...)` with `Array<Any?>` + index casts in
  `ActiveWorkoutViewModel`, `HomeViewModel`, `SettingsViewModel`. Adding/reordering
  a flow without updating the `args[i] as T` casts would throw at runtime
  (ClassCastException / index drift). Mitigated by co-location in one block, but
  brittle. Recommendation: a typed wrapper data class or `combine` overloads.
- LOW — Several very large composables/files (`ActiveWorkoutScreen` ~1k lines,
  `HomeScreen`, `SettingsScreen`) mix many private composables. Functional but
  harder to test/recompose in isolation; candidate for extraction.
- LOW — Tomorrow/48h recovery projections in `buildRecoveryCentre` are derived
  presentation values (clearly documented), not a second recovery model — fine,
  but should stay labelled as projections so they are not mistaken for measured.
- LOW — `runCatching{}.getOrNull()` is used widely for engine calls (good for
  resilience) but can mask real errors silently in development. Consider logging
  in debug builds.

## Coroutines / threading / nullability
- All DB access is `suspend`/`Flow` off the main thread via repositories; no
  obvious main-thread DB calls.
- Flows are scoped to `viewModelScope` with `WhileSubscribed` — no leaked
  collectors observed; no `GlobalScope` usage.
- Nullability handled with safe calls + defaults across the intelligence layer;
  no force-unwrap (`!!`) hotspots found in the new code (one `!!` patterns audit
  recommended in older screens as Low cleanup).

## Dead code
None material. The audit found no orphaned engines, repositories, screens or
routes. Documentation `.md` files accumulate per sprint (housekeeping, not code).

## Recommendation
Address the Medium positional-`combine` brittleness opportunistically (when next
touching those ViewModels) and continue extracting large composables. No code
quality issue blocks release.
