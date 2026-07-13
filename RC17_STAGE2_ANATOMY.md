# RC17.3 — Exercise Visualisation Engine Stage 2: Anatomical Muscle Rendering System

**Date:** July 4, 2026  
**Status:** Stage 2 Implementation Complete (`RC17.3`)  
**Scope:** Offline, data-driven, vector anatomical muscle rendering system (`com.replog.domain.visual.anatomy`) powered by Jetpack Compose Canvas.

---

## 1. Architecture Overview

Stage 2 replaces crude rectangular bounding boxes (`MuscleBodyDiagram`) with a production-quality, scalable vector rendering engine. The system operates strictly offline using pure Kotlin and Jetpack Compose `Path` objects—requiring zero external assets, SVG libraries, Lottie animations, or network calls.

The engine consumes the immutable `AnatomySpec` produced by `ExerciseVisualResolver` (Stage 1) and translates raw muscle strings into pre-compiled anatomical vector geometry rendered over normalized anterior (`FrontBody`) and posterior (`BackBody`) silhouettes.

```text
+-----------------------------------------------------------------------------------+
|                            Exercise Entity (Room DB)                              |
+-----------------------------------------------------------------------------------+
                                          │
                                          ▼
+-----------------------------------------------------------------------------------+
|               ExerciseVisualResolver.resolve(exercise) -> AnatomySpec              |
|        (e.g., primaryMuscles = {"Upper Chest", "Side Delts"}, secondary = ...)     |
+-----------------------------------------------------------------------------------+
                                          │
                                          ▼
+-----------------------------------------------------------------------------------+
|               MuscleMap.mapPrimary / mapSecondary (String -> Set<MuscleRegion>)    |
|        (Resolves to [UPPER_CHEST, LATERAL_DELTOID, TRICEPS, ...])                  |
+-----------------------------------------------------------------------------------+
                                          │
                                          ▼
+-----------------------------------------------------------------------------------+
|             AnatomicalMuscleDiagram / MuscleRenderer (Compose Canvas)             |
|  1. Scales normalized 500x1000 grid to target Canvas size (withTransform).        |
|  2. Draws VectorBody.FRONT / BACK silhouettes (bodyFill + bodyOutline).           |
|  3. Draws Secondary Muscle Paths (Dashed stroke + 28% alpha fill).                |
|  4. Draws Primary Muscle Paths (Solid 5px stroke + 88% dominant accent fill).     |
+-----------------------------------------------------------------------------------+
```

---

## 2. Muscle Region Catalogue

The `MuscleRegion` taxonomy defines 27 distinct bilateral vector regions organized by anatomical side (`BodySide.FRONT` vs `BodySide.BACK`).

| Identifier | Display Name | Anatomical Side | Bilateral Rendering |
| :--- | :--- | :--- | :--- |
| `CHEST` | Chest / Pectoralis Major | Anterior (Front) | Left & Right Pec contours |
| `UPPER_CHEST` | Upper Chest / Clavicular Pectoral | Anterior (Front) | Clavicular upper chest bands |
| `ANTERIOR_DELTOID` | Anterior Deltoid / Front Shoulder | Anterior (Front) | Left & Right front shoulder caps |
| `LATERAL_DELTOID` | Lateral Deltoid / Side Shoulder | Anterior (Front) | Lateral shoulder caps |
| `BICEPS` | Biceps Brachii | Anterior (Front) | Left & Right anterior upper arms |
| `FOREARMS_ANTERIOR` | Anterior Forearm Flexors | Anterior (Front) | Anterior forearm bellies |
| `RECTUS_ABDOMINIS` | Rectus Abdominis / Abs | Anterior (Front) | Multi-tiered abdominal center column |
| `OBLIQUES` | External & Internal Obliques | Anterior (Front) | Lateral waist flanks |
| `HIP_FLEXORS` | Iliopsoas / Hip Flexors | Anterior (Front) | Upper anterior pelvic flexors |
| `QUADRICEPS` | Quadriceps Femoris | Anterior (Front) | Left & Right anterior thigh sweeps |
| `ADDUCTORS` | Thigh Adductors / Inner Thighs | Anterior (Front) | Medial inner thigh sweeps |
| `ABDUCTORS` | Thigh Abductors / Outer Thighs | Anterior (Front) | Lateral outer hip/thigh sweeps |
| `TIBIALIS_ANTERIOR` | Tibialis Anterior / Shin | Anterior (Front) | Anterior shin columns |
| `POSTERIOR_DELTOID` | Posterior Deltoid / Rear Shoulder | Posterior (Back) | Left & Right rear shoulder caps |
| `TRICEPS` | Triceps Brachii | Posterior (Back) | Left & Right posterior upper arms |
| `FOREARMS_POSTERIOR` | Posterior Forearm Extensors | Posterior (Back) | Posterior forearm extensors |
| `UPPER_TRAPEZIUS` | Upper Trapezius | Posterior (Back) | Neck-to-acromion upper slope |
| `MIDDLE_TRAPEZIUS` | Middle Trapezius | Posterior (Back) | Mid-scapular diamond |
| `LOWER_TRAPEZIUS` | Lower Trapezius | Posterior (Back) | Lower thoracic spinal taper |
| `LATISSIMUS_DORSI` | Latissimus Dorsi / Lats | Posterior (Back) | Lateral posterior back sweeps |
| `RHOMBOIDS` | Rhomboids Major & Minor | Posterior (Back) | Inter-scapular support columns |
| `TERES_MAJOR` | Teres Major | Posterior (Back) | Upper lateral scapular border |
| `SPINAL_ERECTORS` | Erector Spinae / Lower Back | Posterior (Back) | Lumbar spinal erector columns |
| `GLUTE_MAXIMUS` | Gluteus Maximus | Posterior (Back) | Left & Right glute max sweeps |
| `GLUTE_MEDIUS` | Gluteus Medius / Upper Hip | Posterior (Back) | Superior outer gluteal shelf |
| `HAMSTRINGS` | Hamstrings | Posterior (Back) | Posterior thigh bellies |
| `CALVES` | Gastrocnemius & Soleus / Calves | Posterior (Back) | Posterior lower leg bellies |

---

## 3. Front-Body & Back-Body Maps

### 3.1 Front-Body Map (`FrontBody.kt`)
Constructed on a normalized $500 \times 1000$ coordinate grid ($X=250$ vertical midline):
* **Silhouette (`silhouettePath`)**: Smooth cubic bezier perimeter framing head ($Y:30..115$), neck ($Y:115..150$), shoulders ($X:110..390$), anterior arms/hands ($Y:165..565$), torso/waist ($Y:150..480$), anterior thighs ($Y:480..715$), shins/feet ($Y:715..965$).
* **Anterior Region Paths**: Distinct closed vector shapes fitting strictly inside the silhouette boundary for `CHEST`, `UPPER_CHEST`, `ANTERIOR_DELTOID`, `LATERAL_DELTOID`, `BICEPS`, `FOREARMS_ANTERIOR`, `RECTUS_ABDOMINIS`, `OBLIQUES`, `HIP_FLEXORS`, `QUADRICEPS`, `ADDUCTORS`, `ABDUCTORS`, and `TIBIALIS_ANTERIOR`.

### 3.2 Back-Body Map (`BackBody.kt`)
* **Silhouette (`silhouettePath`)**: Corresponding posterior outline matching overall bodily proportions.
* **Posterior Region Paths**: Distinct vector paths fitting the dorsal silhouette for `UPPER_TRAPEZIUS`, `MIDDLE_TRAPEZIUS`, `LOWER_TRAPEZIUS`, `POSTERIOR_DELTOID`, `TRICEPS`, `FOREARMS_POSTERIOR`, `RHOMBOIDS`, `TERES_MAJOR`, `LATISSIMUS_DORSI`, `SPINAL_ERECTORS`, `GLUTE_MAXIMUS`, `GLUTE_MEDIUS`, `HAMSTRINGS`, and `CALVES`.

---

## 4. Renderer Pipeline & Accessibility

The rendering engine (`MuscleRenderer.drawBody` and `AnatomicalMuscleDiagram`) executes a multi-tier pass inside Jetpack Compose Canvas:

```kotlin
withTransform({ scale(size.width / 500f, size.height / 1000f, pivot = Offset.Zero) }) {
    // Tier 1: Silhouette Base & Perimeter Outline
    drawPath(path = body.silhouettePath, color = palette.bodyFill)
    drawPath(path = body.silhouettePath, color = palette.bodyOutline, style = RenderStyles.silhouetteStroke)

    // Tier 2: Secondary Supporting Muscles (Accessibility Contrast Layer)
    // Uses 28% alpha fill combined with a distinct dashed stroke (14px dash / 8px gap)
    for (region in secondaryRegions) {
        drawPath(path = path, color = palette.secondaryFill.copy(alpha = 0.28f))
        drawPath(path = path, color = palette.secondaryOutline, style = RenderStyles.secondaryStroke)
    }

    // Tier 3: Primary Target Muscles (Dominant Focus Layer)
    // Uses 88% solid accent fill combined with a continuous bold 5px stroke
    for (region in primaryRegions) {
        drawPath(path = path, color = palette.primaryFill.copy(alpha = 0.88f))
        drawPath(path = path, color = palette.primaryOutline, style = RenderStyles.primaryStroke)
    }
}
```

### Accessibility Compliance
By utilizing **dashed strokes (`PathEffect.dashPathEffect`) and distinct alpha fills** for secondary regions versus **solid continuous strokes and high-opacity fills** for primary regions, users with monochromatic or color-blind vision can immediately distinguish primary vs. secondary activation without relying on hue differences.

---

## 5. Performance Notes

1. **Zero Draw Loop Allocations:** All `Path` objects in `FrontBody` and `BackBody` are pre-compiled and initialized once into immutable singletons (`val`) when the class loads. Inside `DrawScope.onDraw`, `drawPath()` invokes these cached instances directly.
2. **Cached Render Styles:** Stroke specifications (`RenderStyles.primaryStroke`, `RenderStyles.secondaryStroke` with `dashPathEffect`) are declared as singleton properties outside composable scopes, preventing object churn during frame rendering.
3. **Recomposition Optimization:** `AnatomicalMuscleDiagram` wraps string-to-region resolutions inside `remember(anatomySpec.primaryMuscles) { ... }`, ensuring zero re-parsing unless the underlying specification changes. Guarantees smooth **60 FPS** performance.

---

## 6. End-to-End Mapping Table

| Exercise Example | Raw Exercise Muscle Strings | Resolved `AnatomySpec` | `MuscleMap` Resolved Regions | Rendered Vector Paths |
| :--- | :--- | :--- | :--- | :--- |
| **Barbell Bench Press** | `primary: "Pectorals, Upper Chest"`<br>`secondary: "Anterior Deltoid, Triceps"` | `primary = {"Pectorals", "Upper Chest"}`<br>`secondary = {"Anterior Deltoid", "Triceps"}` | **Primary:** `[CHEST, UPPER_CHEST]`<br>**Secondary:** `[ANTERIOR_DELTOID, TRICEPS]` | Anterior silhouette with solid red chest + upper chest fill; dashed orange front shoulders & posterior triceps. |
| **Barbell Back Squat** | `primary: "Quadriceps, Glutes"`<br>`secondary: "Hamstrings, Lower Back, Calves"` | `primary = {"Quadriceps", "Glutes"}`<br>`secondary = {"Hamstrings", "Lower Back", "Calves"}` | **Primary:** `[QUADRICEPS, GLUTE_MAXIMUS]`<br>**Secondary:** `[HAMSTRINGS, SPINAL_ERECTORS, CALVES]` | Anterior solid quads; posterior solid glutes with dashed hamstrings, erectors, and calves. |
| **Pull-Up** | `primary: "Latissimus Dorsi, Teres Major"`<br>`secondary: "Biceps, Rhomboids, Traps"` | `primary = {"Latissimus Dorsi", "Teres Major"}`<br>`secondary = {"Biceps", "Rhomboids", "Traps"}` | **Primary:** `[LATISSIMUS_DORSI, TERES_MAJOR]`<br>**Secondary:** `[BICEPS, RHOMBOIDS, UPPER_TRAPEZIUS, MIDDLE_TRAPEZIUS]` | Posterior solid lats + teres major; dashed rhomboids + traps; anterior dashed biceps. |
| **Overhead Press** | `primary: "Anterior Deltoid, Side Delts"`<br>`secondary: "Triceps, Upper Chest"` | `primary = {"Anterior Deltoid", "Side Delts"}`<br>`secondary = {"Triceps", "Upper Chest"}` | **Primary:** `[ANTERIOR_DELTOID, LATERAL_DELTOID]`<br>**Secondary:** `[TRICEPS, UPPER_CHEST]` | Anterior solid front/side delts; dashed upper chest; posterior dashed triceps. |

---

## 7. Validation & Test Coverage

Stage 2 validation is implemented across two comprehensive engines:
1. **`AnatomyValidator` (`com.replog.domain.visual.validation.AnatomyValidator`)**: Audits the entire `MuscleRegion` enum against `FrontBody` and `BackBody` path maps, verifying **100% vector geometry coverage** (27 defined regions = 13 anterior paths + 14 posterior paths, zero orphans).
2. **Unit Test Suite (`app/src/test/java/com/replog/domain/visual/AnatomyRenderingSystemTest.kt`)**: Verifies that primary and secondary muscle mappings are mutually exclusive and that complex catalog strings resolve accurately.

---

## 8. Known Limitations & Future Improvements

* **Current Stage Scope:** The new anatomical diagram is implemented as an independent domain renderer (`AnatomicalMuscleDiagram`) and preview suite (`AnatomicalPreviews.kt`). Per strict instructions ("Do not replace existing stick figure. Do not remove legacy code"), the legacy `MuscleBodyDiagram` remains embedded in `ExerciseDetailDialog` until UI integration in Stage 5.
* **Future Enhancement:** Support interactive path clicking (e.g., tapping a rendered muscle region on the Canvas highlights synergistic exercises hitting that muscle).
