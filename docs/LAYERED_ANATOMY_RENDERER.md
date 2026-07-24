# Layered Anatomy Renderer

This document describes the RepLog layered anatomy renderer and the verification rules that protect it from regressions.

## Purpose

The anatomy renderer displays exercise target muscles by compositing complete SVG assets. It does not parse SVG path IDs and does not generate procedural muscle geometry.

## Rendering pipeline

```text
Exercise
  -> ExerciseVisualResolver
  -> AnatomySpec
  -> MuscleMap
  -> MuscleRegion
  -> MuscleAssetRegistry
  -> LayeredAnatomyRenderer
  -> SVGCache
  -> AnatomyAssetLoader
  -> AndroidSVG
  -> Compose Canvas
```

## Source of truth

The source of truth is the layered SVG asset library under:

```text
app/src/main/assets/anatomy/front/
app/src/main/assets/anatomy/back/
```

The legacy monolithic files under `app/src/main/assets/anatomy/front_anatomy.svg` and `back_anatomy.svg` are not part of the active renderer.

## Asset canvas contract

Every layered SVG must use the same canvas:

```xml
width="768"
height="1536"
viewBox="0 0 768 1536"
```

The renderer assumes this shared canvas for alignment and scaling. Do not add an asset with a different canvas unless the registry and verification tests are updated deliberately.

## Overlay transparency rules

Overlay SVG files must have transparent backgrounds.

Rules:

- Do not include a full-canvas opaque rectangle.
- Do not use a background fill layer.
- Keep only the visible muscle artwork in the overlay.
- Base body artwork belongs only in `anatomy_front_base.svg` or `anatomy_back_base.svg`.

## Naming conventions

Use lower snake case for asset filenames:

```text
upper_chest.svg
front_delts.svg
traps_upper.svg
erector_spinae.svg
```

Use `anatomy_front_base.svg` and `anatomy_back_base.svg` only for base layers.

## Registry rules

All asset filenames must be centralised in `MuscleAssetRegistry`.

Do not hardcode anatomy SVG filenames elsewhere in production code.

Every `MuscleRegion` must map to at least one `AnatomyLayerAsset`.

Alias mappings are allowed only when a fine-grained muscle region intentionally uses an aggregate SVG, for example:

- `RECTUS_FEMORIS` -> `quads.svg`
- `GLUTE_MEDIUS` -> `glutes.svg`
- `SOLEUS` -> `calves_back.svg`

Aliases must use `MuscleAssetMappingType.ALIAS` and must document the source aggregate region through `mappedFrom`.

## Layer ordering

Layer order is deterministic and defined by the integer `order` in `MuscleAssetRegistry`.

Rendering order is:

1. base layer
2. sorted active overlays
3. duplicate overlay asset paths removed after sorting

Inactive muscles are never rendered.

## Cache rules

`SVGCache` caches parsed SVG documents and failures.

Properties:

- process-local lifetime
- LRU bounded
- default max entries: 64
- caches successful parses
- caches failures
- shares duplicate concurrent loads for the same asset
- does not hold the cache lock during asset I/O or SVG parsing

## Adding a new SVG asset

1. Add the SVG to the appropriate folder:
   - `app/src/main/assets/anatomy/front/`
   - `app/src/main/assets/anatomy/back/`
2. Confirm it has:
   - `width="768"`
   - `height="1536"`
   - `viewBox="0 0 768 1536"`
   - transparent background if it is an overlay
3. Add a constant in `MuscleAssetRegistry.AssetPath`.
4. Add the path to `expectedLibraryAssetPaths`.
5. Add a direct or alias mapping in `entries`.
6. Run the renderer verification tests.

## Adding a new muscle region

1. Add the enum value to `MuscleRegion` with the correct `BodySide`.
2. Add string resolution in `MuscleMap` if user/exercise data may reference it by name.
3. Add a direct or alias mapping in `MuscleAssetRegistry`.
4. Add/adjust tests if the region has special alias behaviour.
5. Run the verification tests.

## Common failure modes

| Failure | Likely cause | Test that should fail |
| --- | --- | --- |
| Missing artwork | asset not present or path typo | registered asset existence test |
| Orphan SVG | asset added but not registered | orphan asset test |
| Misalignment | wrong width/height/viewBox | shared canvas contract test |
| Opaque overlay | background rect in overlay | transparency contract test |
| Unmapped muscle | new `MuscleRegion` without registry entry | registry coverage test |
| Duplicate alias rendering | multiple regions share same SVG but dedupe breaks | duplicate overlay test |
| Cache growth | eviction broken | cache max-size test |
| Concurrent repeated parsing | in-flight sharing broken | concurrent cache test |

## Verification workflow

Before submitting renderer or asset changes, run:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest
```

If CI/build dependency resolution is unavailable locally, at minimum inspect the static tests and run them in CI before merge.

## Screenshot testing

No screenshot framework is currently configured in this project. If screenshot regression testing is introduced later, prefer a JVM-based Compose screenshot tool such as Paparazzi only after confirming compatibility with the Android Gradle Plugin and Kotlin versions used by the project.

Suggested golden cases:

- empty anatomy diagram
- bench press
- squat
- deadlift
- shoulder press
- pull-up
- isolated overlay examples

## Non-goals

Do not reintroduce:

- SVG path-ID parsing
- procedural muscle geometry
- inactive muscle drawing
- scattered hardcoded asset filenames
- multiple active anatomy renderers
