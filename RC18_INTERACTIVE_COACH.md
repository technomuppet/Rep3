# RC18 — Interactive Exercise Coach (`RC18_INTERACTIVE_COACH.md`)

**Date:** July 4, 2026  
**Status:** Complete Implementation (`RC18.Final`)  
**Scope:** Transformation of the Exercise Visualisation Engine into a 100% offline, interactive commercial coaching system.

---

## 1. Executive Summary

Building upon the RC17 rotational Skeletal Animation Engine and Anatomical Muscle Renderer, RC18 transforms passive exercise viewing into an interactive strength coaching experience. Without adding network APIs, video streaming, or cloud AI dependencies, the presentation layer now empowers users to inspect step-by-step technique breakdowns, toggle range of motion (ROM) overlays, scrub animations frame-by-frame, and explore biomechanical muscle roles directly within Jetpack Compose.

---

## 2. Features Implemented across 7 Coaching Phases

### 2.1 7-Phase Execution Breakdown (`MovementCoachingProfile`)
Every supported movement family now provides a structured 7-phase technique execution guide:
1. **Setup:** Foot rooting, equipment spacing, and initial scapular retraction.
2. **Start Position:** Unracking and 360° diaphragmatic core bracing.
3. **Descent (Eccentric):** Controlled lowering tempo and limb tracking.
4. **Bottom Position:** Muscle stretch, hole stability, and pause control.
5. **Concentric Drive:** Explosive vertical/horizontal drive mechanics.
6. **Lockout:** Peak contraction and joint alignment without hyperextension.
7. **Finish & Recovery:** Safe reracking and structural reset.

### 2.2 Concise Coaching Cues & Common Mistakes Accordion
* **Live Cues Banner:** Displays memorable actionable prompts alongside the animation (`"Brace your core"`, `"Drive through your heels"`, `"Keep wrists neutral"`, `"Tuck elbows at 45°"`).
* **Mistakes Accordion:** Details family-specific technical errors (`Bouncing bar off chest`, `Knees collapsing inward/valgus`, `Rounding lumbar spine`), diagnosing root causes and providing exact physical corrections.

### 2.3 Interactive Biomechanical Muscle Explanation (`InteractiveMuscleExplanation`)
* Users can tap interactive filter chips representing active prime movers and secondary synergists to inspect their biomechanical function directly under the anatomical vector diagram.

### 2.4 Equipment Setup Information (`InteractiveEquipmentCard`)
* Details the physical purpose of the selected implement (`Barbell`, `Dumbbell`, `Cable`, `Machine`, `EZ Bar`) and recommends immediate suitable substitutions if equipment is busy in the gym.

### 2.5 Optional Range of Motion (ROM) Guide Overlays (`RomGuideRenderer`)
* Users can toggle `"Show ROM Guide"` to overlay dashed ideal bar trajectories, top lockout target coordinates, and bottom depth indicators directly over the moving skeleton Canvas.

### 2.6 Granular Speed & Scrubbing Controls (`InteractiveAnimationControls`)
* Provides transport controls including Pause/Resume, frame-by-frame step forward/backward ($\pm 0.15\text{s}$), variable slow-motion speeds (`1.0x`, `0.5x`, `0.25x`), instant restart, and continuous loop toggling.

---

## 3. Screens & Files Modified

* **`com.replog.domain.visual.coach` Package Created:**
  * `MovementCoachingProfile.kt`: Houses `ExercisePhase`, `CoachingRepository`, and structured profiles.
  * `RomGuideRenderer.kt`: Canvas overlay renderer for bar trajectory and depth targets.
* **`com.replog.ui.exercise.coach` Package Created:**
  * `InteractiveAnimationControls.kt`: Compose transport bar and speed chips.
  * `InteractiveAnimationCoachView.kt`: Master animated coach canvas and controls wrapper.
  * `InteractiveMuscleExplanation.kt`: Interactive role inspection widget.
  * `InteractiveEquipmentCard.kt`: Equipment setup and substitution card.
  * `InteractiveCoachingBreakdown.kt`: Expandable 7-phase breakdown and mistakes accordions.
* **Public Facades Adapted:**
  * `ExerciseAnimationView.kt` and `MuscleBodyDiagram.kt` were updated to render the interactive RC18 coaching experience automatically while preserving 100% backward compatibility.

---

## 4. Architecture & Performance Impact

* **Architecture:** Unidirectional data flow is maintained. The interactive coach presentation layer strictly consumes immutable specifications (`ExerciseVisualSpec`, `MovementCoachingProfile`) resolved deterministically from local models.
* **Performance:** **Locked 60 FPS.** All ROM overlay guides (`RomGuideRenderer`) and interactive transport state machines operate with zero draw loop allocations inside Compose Canvas.

---

## 5. Future Opportunities (RC19+)

* **Audio Coaching Narration:** Synthesize spoken audio cues using offline text-to-speech so users can listen to technique guidance during active rest timers.
* **Camera Angle Switching:** Allow users to switch between sagittal (side profile) and coronal (front view) kinematic perspectives during playback.
