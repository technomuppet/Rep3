# RC22 — Performance Measurements — Actual Measurements Attempt

## Disclaimer
**No physical Android device or emulator available in this sandbox, no APK generated due to build plugin resolution failure, so no actual measurements possible.** All numbers below are **theoretical estimates based on code analysis** (allocation count, draw ops, hypot, cos/sin per frame) NOT measured via Android Studio Profiler, Systrace, Macrobenchmark. Actual measurements must be done on physical device. We explicitly distinguish measured vs estimated and never invent benchmark numbers.

## Attempted Measurements

- **FPS:** Attempted via `dumpsys gfxinfo` — no device, no APK, cannot.
- **Frame time:** Attempted via FrameMetrics — no device.
- **CPU:** Attempted via Profiler — no device.
- **GPU:** Attempted via GPU rendering — no device.
- **Memory:** Attempted via Memory Profiler — no device.
- **GC:** Attempted via logcat — no device.
- **Startup:** Attempted via `adb shell am start -W` — no device.
- **Draw calls:** Attempted via GPU debugger — no device.
- **Recomposition count:** Attempted via Layout Inspector — no device.
- **Allocation count:** Attempted via allocation tracking — no device.
- **Battery impact:** Attempted via Battery Historian — no device.

**Result:** All measurements UNVERIFIED in this environment.

## What Must Be Measured on Physical Android Device

1. **FPS:** Use `adb shell dumpsys gfxinfo com.replog.debug` or Android Studio Profiler GPU, target 60 FPS stable.
2. **Frame time:** Use FrameMetrics API or `withFrameNanos` delta histogram, target <16ms per frame.
3. **CPU:** Use Android Studio CPU Profiler, record method tracing for `ForwardKinematicsSolver.solve`, `IKSolver.solveFABRIK`, `HybridSolver.solve`, `BarPathEngine.calculateBarPosition`, `CentreOfMassCalculator.calculate`, `StabilisationEngine.evaluate`, `HumanBodyRenderer.drawHumanBody`, `EquipmentEngine`. Target <5ms CPU per frame.
4. **GPU:** Use GPU rendering profiler, target <2ms GPU per frame, 60 draw ops (10 capsules 40 ops, torso 4 path draws pooled, equipment 7, floor 3, hands 2, coaching 5 dots).
5. **Memory:** Use Memory Profiler PSS, heap, track Path pooling effectiveness (should be 0 Path allocations per frame after pooling, verified via code: chestPath.reset() etc).
6. **GC:** Use logcat GC logs, watch for GC every few seconds, target GC every >10 sec with 70 objects/frame.
7. **Startup:** Use `adb shell am start -W` or Macrobenchmark cold startup, target <1 sec.
8. **Draw calls:** Use GPU debugger, count drawLine, drawCircle, drawPath, drawRect per frame, target ~60.
9. **Recomposition:** Use Layout Inspector recomposition counts, verify only AnimationCanvasContent recomposes each frame, not outer Column with Row controls (isolated Canvas fix).
10. **Allocation count:** Use allocation tracking, target ~70 objects/frame after optimizations (was 176 before RC20.4), with topBar/bottomBar cached (saves 76) and pre-baked timeline O(1) lookup (saves 38) and path pooling (saves 6 Path).
11. **Battery impact:** Use Battery Historian, target low.

## Theoretical Estimates Based on Code Analysis (Not Measured)

- **Allocation count per frame after RC20.4 optimizations:**
  - 1 FK: mutableMap 19 entries + 19 SolvedJoint = 38 objects
  - HybridSolver: 8 hypot no alloc, 4*FABRIK mutableList 3 Offsets (12 Offsets) + map copy 19 = ~31 objects
  - COM: ComResult 1 object
  - Stabilisation: 1 object + messages list
  - Body renderer: 0 Path (pooled chestPath, abdomenPath, pelvisPath, ezPathPool, kettlebellHandlePathPool, cableLoopPathPool reset() reuse)
  - Equipment: 0 Path (pooled)
  - Timeline: pre-baked lookup O(1) no map alloc (was mutableMap+Pose per evaluation)
  - RenderContext 1 object
  - Total ~70 objects/frame vs 176 before RC20.4 (cache top/bottom + pre-bake + pooling), reduction 60%.

- **Draw operations:** 10 capsules 40 ops (line+2 circles each), torso 4 path draws pooled, equipment 7, floor 3, hands 2, coaching 5 dots = ~60 draw ops.

- **FK cost:** 19 joints cos/sin per solve, 1 solve per frame after cache (was 3), 19 trig ops <0.1ms.

- **IK cost:** FABRIK 3 joints 10 iter 30 hypot per limb *4 limbs 120 hypot <0.3ms.

- **Frame time:** <5ms CPU + <1ms GPU = <6ms <16ms budget, 60fps theoretical.

## Optimisation Until Consistently Smooth

- **Implemented in RC20.4:**
  - Cache topBar/bottomBar via remember(timeline) once
  - Pre-bake timeline at 60fps List<BakedFrame> via remember(timeline)
  - Path pooling via reset()
  - Isolate Canvas recomposition via separate composable AnimationCanvasContent with own elapsedSeconds State, outer Row not recomposing

- **Remaining for low-end solid 60fps:**
  - Pre-bake solved skeletons too (not just poses) to eliminate FK 38 objects/frame, bringing total to ~10 objects/frame
  - Object pooling for SolvedSkeleton joints map via MutableMap reuse

## Actual Measurements Table

| Metric | Target | Measured | Status |
|--------|--------|----------|--------|
| FPS | 60 | UNVERIFIED — must measure via dumpsys gfxinfo on device | Unverified |
| Frame time | <16ms | UNVERIFIED — must measure via FrameMetrics | Unverified |
| CPU | <5ms per frame | UNVERIFIED — must measure via CPU Profiler | Unverified |
| GPU | <2ms | UNVERIFIED | Unverified |
| Memory PSS | <200MB | UNVERIFIED | Unverified |
| GC frequency | >10 sec per GC | UNVERIFIED | Unverified |
| Startup cold | <1 sec | UNVERIFIED | Unverified |
| Draw calls | ~60 | UNVERIFIED — estimated 60 via code analysis | Estimated |
| Recomposition | Only CanvasContent per frame | Verified via code split (outer not recomposing) but not measured via Layout Inspector | Code verified, measurement unverified |
| Allocation count | ~70/frame | Estimated via code analysis, not measured via allocation tracking | Estimated |
| Path allocations | 0 per frame | Verified via code pooling reset() reuse, no Path() new per frame after fix | Code verified |
| Timeline allocations | 0 per frame | Verified via pre-baked lookup O(1) no map alloc after fix | Code verified |
| FK cost | 19 trig | Estimated | Unverified |
| IK cost | 120 hypot | Estimated | Unverified |
| Cache effectiveness topBar/bottomBar | 100% hit after first | Verified via remember(timeline) caching | Code verified |
| Battery | Low | UNVERIFIED | Unverified |

## Summary

- **No actual measurements possible in this sandbox** without device/emulator and without APK due to build plugin resolution failure.
- **Theoretical estimates** based on code analysis: 70 objects/frame, 60 draw ops, <5ms CPU, <16ms frame time, 60fps mid-range theoretical after RC20.4 optimizations.
- **Must be measured on physical Android device** with Android Studio Profiler, Systrace, Macrobenchmark, dumpsys gfxinfo, Layout Inspector, allocation tracking.
- **Never invent benchmark numbers** — all above estimates clearly marked as estimates, not measured, with exact measurement methods specified.

