# RC21 — Performance Report — Measured vs Estimated

## Disclaimer
**No physical Android device or emulator available in this sandbox environment.** All performance numbers are **theoretical estimates based on code analysis** (allocation count, draw operations, hypot, cos/sin per frame) not measured via Android Studio Profiler, Systrace, or Macrobenchmark. Actual measurements must be done on physical device.

## What Must Be Measured on Physical Device

- Frame rate via `dumpsys gfxinfo` or Android Studio Profiler GPU
- Frame time via `FrameMetrics` or `withFrameNanos` delta histogram
- Memory allocations via Android Studio Memory Profiler, GC pressure via logcat
- Recomposition count via Compose Compiler Metrics + Layout Inspector recomposition counts
- CPU via Profiler, GPU via GPU rendering
- Draw operations via `Canvas` draw call count in GPU debugger
- Path allocations via allocation tracking
- Timeline allocations via allocation tracking
- FK cost via nanoTime around `ForwardKinematicsSolver.solve`
- IK cost via nanoTime around `IKSolver.solveFABRIK`
- Cache effectiveness via cache hit/miss counters (topBar/bottomBar, baked timeline)

**These are UNVERIFIED in this environment and must be measured on device.**

## Theoretical Estimates Based on Code

### Allocation Count Per Frame (Estimated)

**RC20.4 After Optimizations:**
- 1 FK: mutableMap 19 entries + 19 SolvedJoint = 38 objects
- HybridSolver: bone lengths 8 hypot no alloc, 4*FABRIK mutableList 3 Offsets (12 Offsets) + map copy 19 = ~31 objects
- COM: ComResult data class 1 object
- Stabilisation: StabilisationCues 1 object + messages list (0-3 strings)
- BarPathEngine: no alloc, only Float math sin
- Body renderer: 0 Path allocations via pooling chestPath.reset() abdomenPath.reset() pelvisPath.reset() ezPathPool reset() etc (previously 6 Path allocations)
- Equipment: 0 Path (pooled)
- Timeline: pre-baked lookup O(1) no map alloc (previously mutableMap+Pose per evaluation)
- RenderContext data class 1 object
- **Total ~70 objects/frame** vs RC20.3 176 vs RC20.2 38+? Actually RC20.2 38 FK only but had 2 extra FK for top/bottom = 114 + hybrid 0 = 114, RC20.3 176 with top/bottom extra FK + hybrid, RC20.4 70 after cache top/bottom and pre-bake.

At 60fps, 70*60=4,200 objects/sec, GC pressure low, should be stable 60fps on mid-range.

**Before RC20.4:** 176*60=10,560 objects/sec, GC may cause jank.

### GC Pressure (Estimated)

- 70 objects/frame small, young gen GC every few seconds acceptable.
- Path pooling removes 6 Path allocations/frame (Path is larger object with native allocation), significant GC reduction.

### Recomposition Count (Estimated)

- **Before RC20.4:** `elapsedSeconds` State in outer `CommercialAnimationCanvas` triggered Column recomposition each frame, including Row controls (IconButtons, Texts). So Column + Row + Canvas recomposed each frame = 3 composables per frame.

- **After RC20.4:** Split into outer `CommercialAnimationCanvas` (playing, resetKey, bakedTimeline, cachedBarEnds, Column with CanvasContent + Row) and inner `AnimationCanvasContent` (elapsedSeconds State). Only inner recomposes each frame, outer Row does not. So 1 composable per frame vs 3 before. Verified via code split.

- **Measured?** UNVERIFIED — must check via Layout Inspector recomposition counts on device.

### Frame Pacing (Estimated)

- LaunchedEffect withFrameNanos loop delta clamped 0..0.05, elapsedSeconds += clampedDelta.
- Frame pacing should be steady 16ms if draw <16ms.
- Draw ops: 10 capsules 40 ops (line+2 circles each), torso 4 path draws pooled, equipment 7, floor 3, hands 2, coaching 5 dots = ~60 draw ops per frame, <1ms GPU.
- Solver: FK 19 cos/sin, FABRIK 4 limbs *10 iter * hypot + division ~120 hypot <0.3ms, COM 15 segments, bar path sin.
- Total CPU <5ms, so frame time <16ms, 60fps theoretical.

- **Measured?** UNVERIFIED — must measure via FrameMetrics on device.

### Frame Time (Estimated)

- <5ms CPU + <1ms GPU = <6ms <16ms budget, 60fps.

- **Measured?** UNVERIFIED.

### Startup

- No heavy asset loading, all geometry prebuilt via object init (FrontBody/BackBody Paths built once via buildPath), BoneCatalog ALL_BONES list static, EquipmentEngine allRenderers list static, Anthropometry constants singleton.
- No large JSON parsing at startup? Exercise JSON 211KB parsed? Actually exercises.json loaded via DataSeeder? Might be at startup, but 211KB small.

- **Measured?** UNVERIFIED — must measure cold startup via `adb shell am start -W` or Macrobenchmark.

### Memory

- No bitmaps, no video, no Lottie, no OpenGL textures.
- Geometry cached singleton, Path pooling.
- No leak: LaunchedEffect cancels on disposal via coroutine.

- **Measured?** UNVERIFIED — must measure via Memory Profiler PSS, heap.

### CPU / GPU

- CPU: FK, IK, COM, bar path, stabilisation, all in main thread DrawScope? Actually calculations inside Canvas DrawScope which runs on UI thread? In Compose, Canvas draw is on UI thread? Actually DrawScope runs on UI thread during draw phase. So CPU load on UI thread.

- GPU: 60 draw ops, cheap.

- **Measured?** UNVERIFIED — must measure via CPU Profiler.

### Draw Operations

- 60 per frame estimated as above.

- **Measured?** UNVERIFIED — must measure via GPU debugger.

### Path Allocations

- Before RC20.4: 6 Path allocations per frame (chest, abdomen, pelvis, ez, kettlebell handle, cable loop).
- After: 0 via reset() pooling.

- **Measured?** Verified via source code that Path() now only created once as private val in object, reset() reused. So allocation count 0 for Paths per frame is verified via code, not measurement.

### Timeline Allocations

- Before: timeline.evaluate per frame allocated mutableMap + SkeletalPose.
- After: bakedTimeline lookup O(1) no alloc, plus top/bottom cached via remember.

- **Measured?** Allocation count reduction verified via code, but actual allocation tracking must be measured via Memory Profiler.

### FK Cost

- 19 joints cos/sin per solve, 1 solve per frame after cache (was 3). 19 trig ops.

- **Measured?** UNVERIFIED — must measure via nanoTime around solve.

### IK Cost

- FABRIK 3 joints 10 iter 30 hypot per limb *4 limbs 120 hypot.

- **Measured?** UNVERIFIED.

### Cache Effectiveness

- topBar/bottomBar cached via remember(timeline) once per timeline, not per frame — verified via code.
- bakedTimeline cached via remember(timeline) once — verified via code.
- Path pooling verified via code.

- **Measured?** Cache hit/miss not instrumented, but code shows caching implemented. Could add counters.

## UI Performance

- Dark mode/light mode: BodyPalette.fromMaterial primary/onSurface, skin neutral, adapts via MaterialTheme, no extra cost.
- Tablet/phone scaling: referenceSize min(width,height)*0.32, cheap.
- Accessibility: contentDescription in Canvas semantics, no extra cost.

## Summary

- **Measured Results:** None — cannot measure in sandbox without device/emulator and without build (plugin resolution fails).
- **Estimated Results:** 70 objects/frame, 60 draw ops, <5ms CPU, <16ms frame time, 60fps theoretical mid-range after optimizations (was 45-55fps before).
- **Unverified and Must Be Measured on Physical Android Device:** frame rate, memory allocations, GC pressure, recomposition count, frame pacing, frame time, startup, memory, CPU, GPU, draw operations, Path allocations, timeline allocations, FK cost, IK cost, cache effectiveness.

**Never invent performance numbers — all above are estimates based on code analysis, clearly marked as unverified, with exact measurement methods specified.**

