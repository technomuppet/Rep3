# SVG Visual System (Sprint 13, Phase 4)

## Principle
No GIFs, no videos, no photographs. Muscle visualisation is a code-drawn vector
diagram (Compose Canvas), so the asset footprint is effectively 0 KB (well under
the 100 KB target) and there is nothing to download or cache from a network.

## Components
- domain/library/MuscleMap.kt: pure mapper from an exercise's muscle strings to a
  fixed set of body REGIONS (chest, front/rear delts, biceps, triceps, forearms,
  abs, obliques, lats, traps, upper/lower back, quads, glutes, hamstrings, calves),
  split by Side (FRONT/BACK). Adductors/abductors map to the quads region; cardio
  and full-body movements fall back to the major movers so EVERY exercise (516/516)
  highlights at least one region.
- ui/exercise/MuscleBodyDiagram.kt: draws a simple front and back silhouette and
  shades the targeted regions. Primary = filled tint; Secondary = outlined fill
  (a stroke), so the primary/secondary distinction does NOT rely on colour alone.

## Accessibility (Phase 8)
- The diagram exposes a single spoken contentDescription naming the primary and
  secondary muscles for TalkBack.
- A text legend ("Primary" / "Secondary (outlined)") reinforces the colour coding.
- Vector scaling means it reflows on large fonts, landscape, tablets and foldables.

## Why not store SVG files
Drawing in code avoids shipping/parsing SVG assets, keeps the diagram themable
(it uses Material colour roles, so dark mode is automatic), and adds no storage.
