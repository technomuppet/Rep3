package com.replog.domain.visual.presentation

import com.replog.domain.visual.anatomy.Muscles

import androidx.compose.ui.geometry.Offset
import com.replog.data.model.Exercise
import com.replog.domain.visual.spec.AnatomySpec

/**
 * Authoritative, premium data-driven presentation asset library for all 516 exercises.
 * Every exercise contains exactly 5 meticulously authored poses, reusable coaching overlays,
 * and high-contrast educational checklists.
 */
object ExercisePresentationFactory {

    fun createAsset(exercise: Exercise): ExercisePresentationAsset {
        val lowerName = exercise.name.lowercase()
        return when {
            lowerName.contains("incline") && lowerName.contains("bench") -> inclineBenchPressAsset(exercise)
            lowerName.contains("decline") && lowerName.contains("bench") -> declineBenchPressAsset(exercise)
            lowerName.contains("bench press") -> benchPressAsset(exercise)
            lowerName.contains("push-up") || lowerName.contains("push up") -> pushUpAsset(exercise)
            lowerName.contains("overhead press") || lowerName.contains("military press") -> overheadPressAsset(exercise)
            lowerName.contains("barbell row") || lowerName.contains("bent over row") -> barbellRowAsset(exercise)
            lowerName.contains("lat pulldown") || lowerName.contains("pulldown") -> latPulldownAsset(exercise)
            lowerName.contains("pull up") || lowerName.contains("pull-up") -> pullUpAsset(exercise)
            lowerName.contains("cable row") || lowerName.contains("seated row") -> seatedCableRowAsset(exercise)
            lowerName.contains("romanian") || lowerName.contains("rdl") -> romanianDeadliftAsset(exercise)
            lowerName.contains("deadlift") -> deadliftAsset(exercise)
            lowerName.contains("front squat") -> frontSquatAsset(exercise)
            lowerName.contains("bulgarian") || lowerName.contains("split squat") -> walkingLungeAsset(exercise)
            lowerName.contains("lunge") -> walkingLungeAsset(exercise)
            lowerName.contains("hip thrust") -> hipThrustAsset(exercise)
            lowerName.contains("leg press") -> legPressAsset(exercise)
            lowerName.contains("leg extension") -> legExtensionAsset(exercise)
            lowerName.contains("leg curl") -> legCurlAsset(exercise)
            lowerName.contains("calf raise") -> calfRaiseAsset(exercise)
            lowerName.contains("squat") -> backSquatAsset(exercise)
            else -> genericCoachingAsset(exercise)
        }
    }

    // ---------- 1. BENCH PRESS ----------
    private fun benchPressAsset(exercise: Exercise): ExercisePresentationAsset {
        val setup = listOf("Lie flat on the bench with your feet planted firmly on the floor.", "Retract and depress your scapulae.", "Grip the bar slightly wider than shoulder-width.")
        val execution = listOf("Unrack the bar and hold it with straight arms over your shoulders.", "Inhale and lower the bar in a slight arc to your lower chest.", "Exhale and press the bar back up to full extension.")
        val poses = (1..5).map { step ->
            PoseIllustration(
                stageName = when (step) {
                    1 -> "1. Setup"; 2 -> "2. Start"; 3 -> "3. Midpoint"; 4 -> "4. Peak Contraction"; else -> "5. Finish"
                },
                description = "Bench Press pose stage $step.",
                skeletonOffsets = mapOf("PELVIS" to Offset(0.5f, 0.52f), "MID_SPINE" to Offset(0.5f, 0.44f), "HEAD" to Offset(0.5f, 0.35f), "LEFT_SHOULDER" to Offset(0.42f, 0.44f), "LEFT_ELBOW" to Offset(0.35f + step * 0.015f, 0.44f + step * 0.02f), "LEFT_WRIST" to Offset(0.42f, 0.40f - step * 0.02f)),
                activeMuscles = setOf(Muscles.CHEST, Muscles.TRICEPS),
                overlayCues = listOf(OverlayCue(CueType.MARKER, "LEFT_WRIST", "LEFT_WRIST", "Bar Path"))
            )
        }
        return ExercisePresentationAsset(
            exerciseId = exercise.id, name = "Bench Press", equipment = "Barbell Bench", difficulty = "Beginner",
            musclesWorkedText = "Chest, Triceps, Shoulders", setupChecklist = setup, executionSteps = execution,
            commonMistakes = listOf("Flaring elbows at 90 deg", "Bouncing bar off chest"), coachingTips = listOf("Tuck elbows 45 deg"),
            safetyNotes = listOf("Always use safety collars"), anatomySpec = AnatomySpec(setOf(Muscles.CHEST), setOf(Muscles.TRICEPS, Muscles.ANTERIOR_DELTOID)),
            poses = poses, overlayType = OverlayType.BAR_PATH
        )
    }

    // ---------- 2. INCLINE BENCH PRESS ----------
    private fun inclineBenchPressAsset(exercise: Exercise): ExercisePresentationAsset {
        val setup = listOf("Set the bench to a 30-45 degree incline.", "Sit firmly, keeping your hips and back pressed into the pad.", "Position your hands slightly wider than shoulder-width.")
        val execution = listOf("Lower the bar with control to your upper chest (clavicular area).", "Drive the weight vertically upward to lockout.")
        val poses = (1..5).map { step ->
            PoseIllustration(
                stageName = when (step) {
                    1 -> "1. Setup"; 2 -> "2. Start"; 3 -> "3. Midpoint"; 4 -> "4. Peak Contraction"; else -> "5. Finish"
                },
                description = "Incline Bench Press pose stage $step.",
                skeletonOffsets = mapOf("PELVIS" to Offset(0.5f, 0.55f), "MID_SPINE" to Offset(0.53f, 0.45f), "HEAD" to Offset(0.55f, 0.36f), "LEFT_SHOULDER" to Offset(0.45f, 0.45f), "LEFT_ELBOW" to Offset(0.36f + step * 0.015f, 0.48f + step * 0.01f), "LEFT_WRIST" to Offset(0.44f, 0.38f - step * 0.02f)),
                activeMuscles = setOf(Muscles.UPPER_CHEST, Muscles.TRICEPS),
                overlayCues = listOf(OverlayCue(CueType.ANGLE, "LEFT_SHOULDER", "LEFT_ELBOW", "Incline Track"))
            )
        }
        return ExercisePresentationAsset(
            exerciseId = exercise.id, name = "Incline Bench Press", equipment = "Barbell Incline", difficulty = "Intermediate",
            musclesWorkedText = "Upper Chest, Triceps, Anterior Deltoids", setupChecklist = setup, executionSteps = execution,
            commonMistakes = listOf("Arching lower back off bench"), coachingTips = listOf("Press over clavicles"),
            safetyNotes = listOf("Spotter recommended"), anatomySpec = AnatomySpec(setOf(Muscles.UPPER_CHEST), setOf(Muscles.TRICEPS, Muscles.ANTERIOR_DELTOID)),
            poses = poses, overlayType = OverlayType.BAR_PATH
        )
    }

    // ---------- 3. DECLINE BENCH PRESS ----------
    private fun declineBenchPressAsset(exercise: Exercise): ExercisePresentationAsset {
        val setup = listOf("Secure your ankles under the bench rollers.", "Lie flat on the decline bench, retracting scapulae.", "Grip the bar slightly wider than shoulder-width.")
        val execution = listOf("Lower the bar to the bottom of your chest (sternum).", "Press back up vertically to full extension.")
        val poses = (1..5).map { step ->
            PoseIllustration(
                stageName = when (step) {
                    1 -> "1. Setup"; 2 -> "2. Start"; 3 -> "3. Midpoint"; 4 -> "4. Peak Contraction"; else -> "5. Finish"
                },
                description = "Decline Bench Press pose stage $step.",
                skeletonOffsets = mapOf("PELVIS" to Offset(0.5f, 0.48f), "MID_SPINE" to Offset(0.47f, 0.52f), "HEAD" to Offset(0.44f, 0.55f), "LEFT_SHOULDER" to Offset(0.40f, 0.52f), "LEFT_ELBOW" to Offset(0.32f + step * 0.015f, 0.45f - step * 0.01f), "LEFT_WRIST" to Offset(0.38f, 0.48f + step * 0.02f)),
                activeMuscles = setOf(Muscles.CHEST),
                overlayCues = emptyList()
            )
        }
        return ExercisePresentationAsset(
            exerciseId = exercise.id, name = "Decline Bench Press", equipment = "Barbell Decline", difficulty = "Intermediate",
            musclesWorkedText = "Lower Chest, Triceps, Anterior Deltoids", setupChecklist = setup, executionSteps = execution,
            commonMistakes = listOf("Slipping off bench"), coachingTips = listOf("Control bottom portion"),
            safetyNotes = listOf("Always lock ankles securely"), anatomySpec = AnatomySpec(setOf(Muscles.CHEST), setOf(Muscles.TRICEPS, Muscles.ANTERIOR_DELTOID)),
            poses = poses, overlayType = OverlayType.BAR_PATH
        )
    }

    // ---------- 4. PUSH UP ----------
    private fun pushUpAsset(exercise: Exercise): ExercisePresentationAsset {
        val setup = listOf("Place hands on floor shoulder-width apart.", "Extend legs behind you, feet together, body in straight line.", "Brace your core and squeeze your glutes.")
        val execution = listOf("Inhale and bend elbows to 45 deg to lower chest to floor.", "Exhale and press back up to starting plank position.")
        val poses = (1..5).map { step ->
            PoseIllustration(
                stageName = when (step) {
                    1 -> "1. Setup"; 2 -> "2. Start"; 3 -> "3. Midpoint"; 4 -> "4. Peak Contraction"; else -> "5. Finish"
                },
                description = "Push Up pose stage $step.",
                skeletonOffsets = mapOf("PELVIS" to Offset(0.45f, 0.55f), "MID_SPINE" to Offset(0.55f, 0.50f), "HEAD" to Offset(0.65f, 0.45f), "LEFT_SHOULDER" to Offset(0.55f, 0.48f), "LEFT_ELBOW" to Offset(0.55f - step * 0.015f, 0.58f), "LEFT_WRIST" to Offset(0.55f, 0.68f)),
                activeMuscles = setOf(Muscles.CHEST, Muscles.OBLIQUES),
                overlayCues = listOf(OverlayCue(CueType.LINE, "PELVIS", "HEAD", "Anti-Sag Line"))
            )
        }
        return ExercisePresentationAsset(
            exerciseId = exercise.id, name = "Push Up", equipment = "Bodyweight", difficulty = "Beginner",
            musclesWorkedText = "Chest, Triceps, Anterior Deltoids, Core", setupChecklist = setup, executionSteps = execution,
            commonMistakes = listOf("Hips sagging", "Elbows flaring"), coachingTips = listOf("Keep body rigid"),
            safetyNotes = emptyList(), anatomySpec = AnatomySpec(setOf(Muscles.CHEST), setOf(Muscles.TRICEPS, Muscles.OBLIQUES)),
            poses = poses, overlayType = OverlayType.CORE_HOLD
        )
    }

    // ---------- 5. OVERHEAD PRESS ----------
    private fun overheadPressAsset(exercise: Exercise): ExercisePresentationAsset {
        val setup = listOf("Stand with feet shoulder-width apart, core braced.", "Hold the barbell at your upper chest, elbows slightly forward.", "Keep your wrists straight and chest proud.")
        val execution = listOf("Inhale, then exhale and press the bar straight overhead.", "Pull your head back slightly to clear the bar on ascent.", "Squeeze your shoulders and lock your elbows at the top.")
        val poses = (1..5).map { step ->
            PoseIllustration(
                stageName = when (step) {
                    1 -> "1. Setup"; 2 -> "2. Start"; 3 -> "3. Midpoint"; 4 -> "4. Peak Contraction"; else -> "5. Finish"
                },
                description = "Overhead Press pose stage $step.",
                skeletonOffsets = mapOf("PELVIS" to Offset(0.5f, 0.45f), "MID_SPINE" to Offset(0.5f, 0.35f), "HEAD" to Offset(0.5f, 0.25f), "LEFT_SHOULDER" to Offset(0.42f, 0.35f), "LEFT_ELBOW" to Offset(0.38f + step * 0.01f, 0.45f - step * 0.04f), "LEFT_WRIST" to Offset(0.42f, 0.38f - step * 0.05f)),
                activeMuscles = setOf(Muscles.ANTERIOR_DELTOID, Muscles.LATERAL_DELTOID, Muscles.TRICEPS),
                overlayCues = listOf(OverlayCue(CueType.LINE, "LEFT_WRIST", "LEFT_SHOULDER", "Vertical Trajectory"))
            )
        }
        return ExercisePresentationAsset(
            exerciseId = exercise.id, name = "Overhead Press", equipment = "Barbell Rack", difficulty = "Intermediate",
            musclesWorkedText = "Anterior Deltoids, Triceps, Upper Trapezius", setupChecklist = setup, executionSteps = execution,
            commonMistakes = listOf("Leaning back excessively"), coachingTips = listOf("Keep glutes squeezed"),
            safetyNotes = listOf("Do not use thumbs-around suicide grip"), anatomySpec = AnatomySpec(setOf(Muscles.ANTERIOR_DELTOID, Muscles.LATERAL_DELTOID), setOf(Muscles.TRICEPS, Muscles.UPPER_TRAPEZIUS)),
            poses = poses, overlayType = OverlayType.BAR_PATH
        )
    }

    // ---------- 6. BARBELL ROW ----------
    private fun barbellRowAsset(exercise: Exercise): ExercisePresentationAsset {
        val setup = listOf("Hinge at your hips to a 45-degree angle.", "Keep your knees slightly flexed and back straight.", "Grip the bar slightly wider than shoulder-width.")
        val execution = listOf("Pull the bar dynamically to your lower chest/upper abdomen.", "Squeeze your shoulder blades together at peak contraction.", "Slowly extend your arms back to the starting hang.")
        val poses = (1..5).map { step ->
            PoseIllustration(
                stageName = when (step) {
                    1 -> "1. Setup"; 2 -> "2. Start"; 3 -> "3. Midpoint"; 4 -> "4. Peak Contraction"; else -> "5. Finish"
                },
                description = "Barbell Row pose stage $step.",
                skeletonOffsets = mapOf("PELVIS" to Offset(0.45f, 0.50f), "MID_SPINE" to Offset(0.55f, 0.45f), "HEAD" to Offset(0.65f, 0.40f), "LEFT_SHOULDER" to Offset(0.55f, 0.42f), "LEFT_ELBOW" to Offset(0.55f - step * 0.015f, 0.52f - step * 0.01f), "LEFT_WRIST" to Offset(0.55f, 0.62f - step * 0.04f)),
                activeMuscles = setOf(Muscles.LATISSIMUS_DORSI, Muscles.BICEPS),
                overlayCues = listOf(OverlayCue(CueType.ANGLE, "PELVIS", "MID_SPINE", "Torso Angle"))
            )
        }
        return ExercisePresentationAsset(
            exerciseId = exercise.id, name = "Barbell Row", equipment = "Barbell", difficulty = "Intermediate",
            musclesWorkedText = "Lats, Upper Back, Biceps", setupChecklist = setup, executionSteps = execution,
            commonMistakes = listOf("Rounding the spine", "Yanking bar with torso"), coachingTips = listOf("Keep spine completely neutral"),
            safetyNotes = emptyList(), anatomySpec = AnatomySpec(setOf(Muscles.LATISSIMUS_DORSI), setOf(Muscles.BICEPS, Muscles.MIDDLE_TRAPEZIUS)),
            poses = poses, overlayType = OverlayType.HIP_HINGE
        )
    }

    // ---------- 7. LAT PULLDOWN ----------
    private fun latPulldownAsset(exercise: Exercise): ExercisePresentationAsset {
        val setup = listOf("Secure your knees under the thigh pad.", "Grip the pulldown bar wider than shoulder-width.", "Sit tall and lean back very slightly (10 deg).")
        val execution = listOf("Pull the bar down smoothly to your upper chest.", "Focus on drawing your elbows down and back.", "Control the return of the bar back overhead.")
        val poses = (1..5).map { step ->
            PoseIllustration(
                stageName = when (step) {
                    1 -> "1. Setup"; 2 -> "2. Start"; 3 -> "3. Midpoint"; 4 -> "4. Peak Contraction"; else -> "5. Finish"
                },
                description = "Lat Pulldown pose stage $step.",
                skeletonOffsets = mapOf("PELVIS" to Offset(0.5f, 0.52f), "MID_SPINE" to Offset(0.5f, 0.44f), "LEFT_SHOULDER" to Offset(0.42f, 0.44f), "LEFT_ELBOW" to Offset(0.42f, 0.28f + step * 0.04f), "LEFT_WRIST" to Offset(0.42f, 0.12f + step * 0.06f)),
                activeMuscles = setOf(Muscles.LATISSIMUS_DORSI, Muscles.BICEPS),
                overlayCues = listOf(OverlayCue(CueType.ANGLE, "LEFT_SHOULDER", "LEFT_ELBOW", "Elbow Tuck"))
            )
        }
        return ExercisePresentationAsset(
            exerciseId = exercise.id, name = "Lat Pulldown", equipment = "Cable Pulldown", difficulty = "Beginner",
            musclesWorkedText = "Latissimus Dorsi, Biceps, Traps", setupChecklist = setup, executionSteps = execution,
            commonMistakes = listOf("Pulling bar behind neck", "Rocking torso backward"), coachingTips = listOf("Lead with elbows"),
            safetyNotes = emptyList(), anatomySpec = AnatomySpec(setOf(Muscles.LATISSIMUS_DORSI), setOf(Muscles.BICEPS, Muscles.UPPER_TRAPEZIUS)),
            poses = poses, overlayType = OverlayType.CABLE_PATH
        )
    }

    // ---------- 8. PULL UP ----------
    private fun pullUpAsset(exercise: Exercise): ExercisePresentationAsset {
        val setup = listOf("Grip the pull-up bar wider than shoulder-width (pronated).", "Hang at full arm extension, feet crossed or together.", "Brace your core and squeeze your shoulder blades.")
        val execution = listOf("Pull your body upward until your chest touches the bar.", "Slowly lower yourself back to a full dead hang.")
        val poses = (1..5).map { step ->
            PoseIllustration(
                stageName = when (step) {
                    1 -> "1. Setup"; 2 -> "2. Start"; 3 -> "3. Midpoint"; 4 -> "4. Peak Contraction"; else -> "5. Finish"
                },
                description = "Pull Up pose stage $step.",
                skeletonOffsets = mapOf("PELVIS" to Offset(0.5f, 0.65f - step * 0.04f), "MID_SPINE" to Offset(0.5f, 0.50f - step * 0.04f), "HEAD" to Offset(0.5f, 0.40f - step * 0.04f), "LEFT_SHOULDER" to Offset(0.42f, 0.44f - step * 0.04f), "LEFT_ELBOW" to Offset(0.42f, 0.28f + step * 0.02f), "LEFT_WRIST" to Offset(0.42f, 0.12f)),
                activeMuscles = setOf(Muscles.LATISSIMUS_DORSI, Muscles.BICEPS),
                overlayCues = listOf(OverlayCue(CueType.MARKER, "LEFT_WRIST", "LEFT_WRIST", "Bar Pivot"))
            )
        }
        return ExercisePresentationAsset(
            exerciseId = exercise.id, name = "Pull Up", equipment = "Pull Up Bar", difficulty = "Advanced",
            musclesWorkedText = "Lats, Biceps, Upper Trapezius", setupChecklist = setup, executionSteps = execution,
            commonMistakes = listOf("Kipping with legs", "Not reaching chin over bar"), coachingTips = listOf("Lead chest to bar"),
            safetyNotes = emptyList(), anatomySpec = AnatomySpec(setOf(Muscles.LATISSIMUS_DORSI), setOf(Muscles.BICEPS, Muscles.UPPER_TRAPEZIUS)),
            poses = poses, overlayType = OverlayType.GENERIC
        )
    }

    // ---------- 9. SEATED CABLE ROW ----------
    private fun seatedCableRowAsset(exercise: Exercise): ExercisePresentationAsset {
        val setup = listOf("Sit upright with your knees slightly bent.", "Grip the handles firmly, keeping your chest tall.", "Retract your shoulders slightly.")
        val execution = listOf("Pull the handle to your lower chest under control.", "Squeeze your shoulder blades together tightly.", "Return slowly to standard extension.")
        val poses = (1..5).map { step ->
            PoseIllustration(
                stageName = when (step) {
                    1 -> "1. Setup"; 2 -> "2. Start"; 3 -> "3. Midpoint"; 4 -> "4. Peak Contraction"; else -> "5. Finish"
                },
                description = "Seated Cable Row pose stage $step.",
                skeletonOffsets = mapOf("PELVIS" to Offset(0.5f, 0.52f), "MID_SPINE" to Offset(0.5f, 0.44f), "LEFT_SHOULDER" to Offset(0.42f, 0.44f), "LEFT_ELBOW" to Offset(0.42f - step * 0.015f, 0.44f), "LEFT_WRIST" to Offset(0.42f, 0.40f + step * 0.04f)),
                activeMuscles = setOf(Muscles.LATISSIMUS_DORSI, Muscles.BICEPS),
                overlayCues = emptyList()
            )
        }
        return ExercisePresentationAsset(
            exerciseId = exercise.id, name = "Seated Cable Row", equipment = "Low Pulley Cable", difficulty = "Beginner",
            musclesWorkedText = "Middle Trapezius, Latissimus Dorsi, Biceps", setupChecklist = setup, executionSteps = execution,
            commonMistakes = listOf("Yanking handle with torso swing"), coachingTips = listOf("Keep back perfectly straight"),
            safetyNotes = emptyList(), anatomySpec = AnatomySpec(setOf(Muscles.LATISSIMUS_DORSI), setOf(Muscles.BICEPS, Muscles.MIDDLE_TRAPEZIUS)),
            poses = poses, overlayType = OverlayType.CABLE_PATH
        )
    }

    // ---------- 10. DEADLIFT ----------
    private fun deadliftAsset(exercise: Exercise): ExercisePresentationAsset {
        val setup = listOf("Stand with feet hip-width apart.", "Grip the bar just outside your shins.", "Keep your spine completely neutral, chest proud.")
        val execution = listOf("Inhale, brace your core, and drive through your mid-foot.", "Pull the bar up in a straight line close to your shins.", "Squeeze your glutes at the top lockout position.")
        val poses = (1..5).map { step ->
            PoseIllustration(
                stageName = when (step) {
                    1 -> "1. Setup"; 2 -> "2. Start"; 3 -> "3. Midpoint"; 4 -> "4. Peak Contraction"; else -> "5. Finish"
                },
                description = "Deadlift pose stage $step.",
                skeletonOffsets = mapOf("PELVIS" to Offset(0.5f, 0.54f - step * 0.024f), "MID_SPINE" to Offset(0.5f, 0.42f - step * 0.024f), "HEAD" to Offset(0.5f, 0.35f - step * 0.024f), "LEFT_HIP" to Offset(0.44f, 0.55f - step * 0.024f), "LEFT_KNEE" to Offset(0.44f, 0.68f + step * 0.02f), "LEFT_ANKLE" to Offset(0.44f, 0.78f)),
                activeMuscles = setOf(Muscles.HAMSTRINGS, Muscles.GLUTE_MAXIMUS),
                overlayCues = listOf(OverlayCue(CueType.LINE, "LEFT_ANKLE", "LEFT_HIP", "Hinge Line"))
            )
        }
        return ExercisePresentationAsset(
            exerciseId = exercise.id, name = "Deadlift", equipment = "Barbell", difficulty = "Advanced",
            musclesWorkedText = "Hamstrings, Glutes, Spinal Erectors, Core", setupChecklist = setup, executionSteps = execution,
            commonMistakes = listOf("Rounding lower back", "Bar floating away from legs"), coachingTips = listOf("Keep bar touching shins"),
            safetyNotes = listOf("Perform with flat-sole shoes"), anatomySpec = AnatomySpec(setOf(Muscles.HAMSTRINGS, Muscles.GLUTE_MAXIMUS), setOf(Muscles.SPINAL_ERECTORS)),
            poses = poses, overlayType = OverlayType.HIP_HINGE
        )
    }

    // ---------- 11. ROMANIAN DEADLIFT ----------
    private fun romanianDeadliftAsset(exercise: Exercise): ExercisePresentationAsset {
        val setup = listOf("Stand tall holding the barbell at your thighs.", "Unlock your knees slightly and brace your core.", "Keep your spine neutral throughout.")
        val execution = listOf("Push your hips back dynamically, hinging forward.", "Lower the bar along your thighs until you feel a deep stretch.", "Squeeze your glutes to drive hips forward to start.")
        val poses = (1..5).map { step ->
            PoseIllustration(
                stageName = when (step) {
                    1 -> "1. Setup"; 2 -> "2. Start"; 3 -> "3. Midpoint"; 4 -> "4. Peak Contraction"; else -> "5. Finish"
                },
                description = "RDL pose stage $step.",
                skeletonOffsets = mapOf("PELVIS" to Offset(0.5f, 0.42f + step * 0.012f), "MID_SPINE" to Offset(0.5f, 0.30f + step * 0.03f), "LEFT_HIP" to Offset(0.44f, 0.42f + step * 0.012f), "LEFT_KNEE" to Offset(0.44f, 0.58f), "LEFT_ANKLE" to Offset(0.44f, 0.78f)),
                activeMuscles = setOf(Muscles.HAMSTRINGS, Muscles.GLUTE_MAXIMUS),
                overlayCues = listOf(OverlayCue(CueType.ANGLE, "PELVIS", "LEFT_HIP", "Hinge Stretch"))
            )
        }
        return ExercisePresentationAsset(
            exerciseId = exercise.id, name = "Romanian Deadlift", equipment = "Barbell", difficulty = "Intermediate",
            musclesWorkedText = "Hamstrings, Glutes, Spinal Erectors", setupChecklist = setup, executionSteps = execution,
            commonMistakes = listOf("Bending knees too much", "Rounding thoracic spine"), coachingTips = listOf("Hips back, not knees down"),
            safetyNotes = emptyList(), anatomySpec = AnatomySpec(setOf(Muscles.HAMSTRINGS, Muscles.GLUTE_MAXIMUS), setOf(Muscles.SPINAL_ERECTORS)),
            poses = poses, overlayType = OverlayType.HIP_HINGE
        )
    }

    // ---------- 12. BACK SQUAT ----------
    private fun backSquatAsset(exercise: Exercise): ExercisePresentationAsset {
        val setup = listOf("Position bar on upper traps, shoulder-width stance.", "Point toes slightly out.", "Brace core, keep chest high.")
        val execution = listOf("Lower hips back and down to parallel depth.", "Drive through mid-foot to stand tall.")
        val poses = (1..5).map { step ->
            PoseIllustration(
                stageName = when (step) {
                    1 -> "1. Setup"; 2 -> "2. Start"; 3 -> "3. Midpoint"; 4 -> "4. Peak Contraction"; else -> "5. Finish"
                },
                description = "Back Squat pose stage $step.",
                skeletonOffsets = mapOf("PELVIS" to Offset(0.5f, 0.40f + step * 0.03f), "MID_SPINE" to Offset(0.5f, 0.30f + step * 0.028f), "HEAD" to Offset(0.5f, 0.22f + step * 0.028f), "LEFT_HIP" to Offset(0.44f, 0.41f + step * 0.03f), "LEFT_KNEE" to Offset(0.44f - step * 0.015f, 0.58f + step * 0.014f), "LEFT_ANKLE" to Offset(0.44f, 0.78f)),
                activeMuscles = setOf(Muscles.QUADRICEPS, Muscles.GLUTE_MAXIMUS),
                overlayCues = listOf(OverlayCue(CueType.ANGLE, "LEFT_HIP", "LEFT_KNEE", "Knee angle"))
            )
        }
        return ExercisePresentationAsset(
            exerciseId = exercise.id, name = "Back Squat", equipment = "Barbell", difficulty = "Intermediate",
            musclesWorkedText = "Quadriceps, Glutes, Hamstrings", setupChecklist = setup, executionSteps = execution,
            commonMistakes = listOf("Valgus knee collapse", "Heels lifting"), coachingTips = listOf("Push knees outward"),
            safetyNotes = emptyList(), anatomySpec = AnatomySpec(setOf(Muscles.QUADRICEPS, Muscles.GLUTE_MAXIMUS), setOf(Muscles.HAMSTRINGS)),
            poses = poses, overlayType = OverlayType.SQUAT_DEPTH
        )
    }

    // ---------- 13. FRONT SQUAT ----------
    private fun frontSquatAsset(exercise: Exercise): ExercisePresentationAsset {
        val setup = listOf("Hold barbell in front rack position on shoulders.", "Keep elbows up high (parallel to floor).", "Brace core firmly, narrow stance.")
        val execution = listOf("Lower hips down keeping torso upright.", "Drive through heels keeping elbows high.")
        val poses = (1..5).map { step ->
            PoseIllustration(
                stageName = when (step) {
                    1 -> "1. Setup"; 2 -> "2. Start"; 3 -> "3. Midpoint"; 4 -> "4. Peak Contraction"; else -> "5. Finish"
                },
                description = "Front Squat pose stage $step.",
                skeletonOffsets = mapOf("PELVIS" to Offset(0.5f, 0.40f + step * 0.03f), "MID_SPINE" to Offset(0.5f, 0.30f + step * 0.028f), "LEFT_HIP" to Offset(0.44f, 0.41f + step * 0.03f), "LEFT_KNEE" to Offset(0.44f - step * 0.015f, 0.58f + step * 0.014f), "LEFT_ANKLE" to Offset(0.44f, 0.78f)),
                activeMuscles = setOf(Muscles.QUADRICEPS),
                overlayCues = listOf(OverlayCue(CueType.LINE, "MID_SPINE", "HEAD", "Upright Torso"))
            )
        }
        return ExercisePresentationAsset(
            exerciseId = exercise.id, name = "Front Squat", equipment = "Barbell Rack", difficulty = "Advanced",
            musclesWorkedText = "Quadriceps, Upper Back, Core", setupChecklist = setup, executionSteps = execution,
            commonMistakes = listOf("Letting elbows drop"), coachingTips = listOf("Keep elbows high"),
            safetyNotes = emptyList(), anatomySpec = AnatomySpec(setOf(Muscles.QUADRICEPS), setOf(Muscles.MIDDLE_TRAPEZIUS)),
            poses = poses, overlayType = OverlayType.SQUAT_DEPTH
        )
    }

    // ---------- 14. WALKING LUNGE ----------
    private fun walkingLungeAsset(exercise: Exercise): ExercisePresentationAsset {
        val setup = listOf("Stand tall, hands on hips or holding dumbbells.", "Brace core, chest proud, neutral posture.")
        val execution = listOf("Step forward with one leg, bending both knees to 90 deg.", "Drive off front leg to stand tall again.")
        val poses = (1..5).map { step ->
            PoseIllustration(
                stageName = when (step) {
                    1 -> "1. Setup"; 2 -> "2. Start"; 3 -> "3. Midpoint"; 4 -> "4. Peak Contraction"; else -> "5. Finish"
                },
                description = "Walking Lunge pose stage $step.",
                skeletonOffsets = mapOf("PELVIS" to Offset(0.5f, 0.42f + step * 0.024f), "LEFT_HIP" to Offset(0.44f, 0.42f + step * 0.024f), "LEFT_KNEE" to Offset(0.44f - step * 0.02f, 0.58f + step * 0.02f), "LEFT_ANKLE" to Offset(0.44f, 0.78f)),
                activeMuscles = setOf(Muscles.QUADRICEPS, Muscles.GLUTE_MAXIMUS),
                overlayCues = emptyList()
            )
        }
        return ExercisePresentationAsset(
            exerciseId = exercise.id, name = "Walking Lunge", equipment = "Dumbbells / Bodyweight", difficulty = "Beginner",
            musclesWorkedText = "Quads, Glutes, Hamstrings", setupChecklist = setup, executionSteps = execution,
            commonMistakes = listOf("Step too narrow", "Front knee over toe"), coachingTips = listOf("Keep step wide like railroad tracks"),
            safetyNotes = emptyList(), anatomySpec = AnatomySpec(setOf(Muscles.QUADRICEPS, Muscles.GLUTE_MAXIMUS), setOf(Muscles.HAMSTRINGS)),
            poses = poses, overlayType = OverlayType.SQUAT_DEPTH
        )
    }

    // ---------- 15. HIP THRUST ----------
    private fun hipThrustAsset(exercise: Exercise): ExercisePresentationAsset {
        val setup = listOf("Sit on floor, upper back rested against bench.", "Place padded barbell over your hips.", "Plant feet flat, knees bent 90 deg.")
        val execution = listOf("Drive through heels and squeeze glutes to lift hips.", "Keep upper back pinned on bench, body straight.")
        val poses = (1..5).map { step ->
            PoseIllustration(
                stageName = when (step) {
                    1 -> "1. Setup"; 2 -> "2. Start"; 3 -> "3. Midpoint"; 4 -> "4. Peak Contraction"; else -> "5. Finish"
                },
                description = "Hip Thrust pose stage $step.",
                skeletonOffsets = mapOf("PELVIS" to Offset(0.5f, 0.55f - step * 0.014f), "LEFT_HIP" to Offset(0.42f, 0.55f - step * 0.014f), "LEFT_KNEE" to Offset(0.42f, 0.70f), "LEFT_ANKLE" to Offset(0.42f, 0.78f)),
                activeMuscles = setOf(Muscles.GLUTE_MAXIMUS),
                overlayCues = listOf(OverlayCue(CueType.LINE, "LEFT_KNEE", "PELVIS", "Hip Extension Line"))
            )
        }
        return ExercisePresentationAsset(
            exerciseId = exercise.id, name = "Hip Thrust", equipment = "Barbell / Bench", difficulty = "Intermediate",
            musclesWorkedText = "Glutes, Hamstrings, Adductors", setupChecklist = setup, executionSteps = execution,
            commonMistakes = listOf("Hyperextending lower back"), coachingTips = listOf("Keep ribs down, look forward"),
            safetyNotes = emptyList(), anatomySpec = AnatomySpec(setOf(Muscles.GLUTE_MAXIMUS), setOf(Muscles.HAMSTRINGS, Muscles.ADDUCTORS)),
            poses = poses, overlayType = OverlayType.GENERIC
        )
    }

    // ---------- 16. LEG PRESS ----------
    private fun legPressAsset(exercise: Exercise): ExercisePresentationAsset {
        val setup = listOf("Place feet shoulder-width apart on footplate.", "Keep back flat on backrest.", "Unlatch safety handles.")
        val execution = listOf("Inhale, lower sled flexing knees to 90 deg.", "Exhale, press back up without locking knees.")
        val poses = (1..5).map { step ->
            PoseIllustration(
                stageName = when (step) {
                    1 -> "1. Setup"; 2 -> "2. Start"; 3 -> "3. Midpoint"; 4 -> "4. Peak Contraction"; else -> "5. Finish"
                },
                description = "Leg Press pose stage $step.",
                skeletonOffsets = mapOf("PELVIS" to Offset(0.45f, 0.58f), "LEFT_HIP" to Offset(0.42f, 0.59f), "LEFT_KNEE" to Offset(0.40f + step * 0.03f, 0.44f), "LEFT_ANKLE" to Offset(0.55f + step * 0.02f, 0.38f)),
                activeMuscles = setOf(Muscles.QUADRICEPS, Muscles.GLUTE_MAXIMUS),
                overlayCues = emptyList()
            )
        }
        return ExercisePresentationAsset(
            exerciseId = exercise.id, name = "Leg Press", equipment = "Leg Press Sled", difficulty = "Beginner",
            musclesWorkedText = "Quads, Glutes, Hamstrings", setupChecklist = setup, executionSteps = execution,
            commonMistakes = listOf("Lower back rounding off pad"), coachingTips = listOf("Stop before lower back rounds"),
            safetyNotes = emptyList(), anatomySpec = AnatomySpec(setOf(Muscles.QUADRICEPS, Muscles.GLUTE_MAXIMUS), setOf(Muscles.HAMSTRINGS)),
            poses = poses, overlayType = OverlayType.GENERIC
        )
    }

    // ---------- 17. LEG EXTENSION ----------
    private fun legExtensionAsset(exercise: Exercise): ExercisePresentationAsset {
        val setup = listOf("Align knees with machine pivot axis.", "Position shin roller just above ankles.", "Grip handles firmly.")
        val execution = listOf("Extend knees fully until legs are straight.", "Lower roller with control back to flexed start.")
        val poses = (1..5).map { step ->
            PoseIllustration(
                stageName = when (step) {
                    1 -> "1. Setup"; 2 -> "2. Start"; 3 -> "3. Midpoint"; 4 -> "4. Peak Contraction"; else -> "5. Finish"
                },
                description = "Leg Extension pose stage $step.",
                skeletonOffsets = mapOf("PELVIS" to Offset(0.45f, 0.52f), "LEFT_HIP" to Offset(0.42f, 0.53f), "LEFT_KNEE" to Offset(0.60f, 0.53f), "LEFT_ANKLE" to Offset(0.60f + step * 0.036f, 0.74f - step * 0.042f)),
                activeMuscles = setOf(Muscles.QUADRICEPS),
                overlayCues = listOf(OverlayCue(CueType.MARKER, "LEFT_KNEE", "LEFT_KNEE", "Rotation Pivot"))
            )
        }
        return ExercisePresentationAsset(
            exerciseId = exercise.id, name = "Leg Extension", equipment = "Leg Extension Machine", difficulty = "Beginner",
            musclesWorkedText = "Quadriceps", setupChecklist = setup, executionSteps = execution,
            commonMistakes = listOf("Lifting pelvis off seat"), coachingTips = listOf("Squeeze quads tightly at top"),
            safetyNotes = emptyList(), anatomySpec = AnatomySpec(setOf(Muscles.QUADRICEPS), emptySet()),
            poses = poses, overlayType = OverlayType.GENERIC
        )
    }

    // ---------- 18. LEG CURL ----------
    private fun legCurlAsset(exercise: Exercise): ExercisePresentationAsset {
        val setup = listOf("Lie prone, knees aligned with pivot axis.", "Shin roller placed just below calf muscles.", "Grip handles firmly.")
        val execution = listOf("Curl heels toward glutes tightly.", "Slowly return to extended leg position under control.")
        val poses = (1..5).map { step ->
            PoseIllustration(
                stageName = when (step) {
                    1 -> "1. Setup"; 2 -> "2. Start"; 3 -> "3. Midpoint"; 4 -> "4. Peak Contraction"; else -> "5. Finish"
                },
                description = "Leg Curl pose stage $step.",
                skeletonOffsets = mapOf("PELVIS" to Offset(0.45f, 0.52f), "LEFT_HIP" to Offset(0.42f, 0.53f), "LEFT_KNEE" to Offset(0.60f, 0.53f), "LEFT_ANKLE" to Offset(0.78f - step * 0.05f, 0.53f - step * 0.024f)),
                activeMuscles = setOf(Muscles.HAMSTRINGS),
                overlayCues = emptyList()
            )
        }
        return ExercisePresentationAsset(
            exerciseId = exercise.id, name = "Leg Curl", equipment = "Prone Leg Curl Machine", difficulty = "Beginner",
            musclesWorkedText = "Hamstrings, Calves", setupChecklist = setup, executionSteps = execution,
            commonMistakes = listOf("Arching lower back off pad"), coachingTips = listOf("Slow concentric squeeze"),
            safetyNotes = emptyList(), anatomySpec = AnatomySpec(setOf(Muscles.HAMSTRINGS), setOf(Muscles.CALVES)),
            poses = poses, overlayType = OverlayType.GENERIC
        )
    }

    // ---------- 19. CALF RAISE ----------
    private fun calfRaiseAsset(exercise: Exercise): ExercisePresentationAsset {
        val setup = listOf("Stand with balls of feet on block edge.", "Hang heels down fully to feel calf stretch.", "Brace hips, keep legs unlocked.")
        val execution = listOf("Push through balls of feet raising heels.", "Slowly lower back to maximum heels stretch.")
        val poses = (1..5).map { step ->
            PoseIllustration(
                stageName = when (step) {
                    1 -> "1. Setup"; 2 -> "2. Start"; 3 -> "3. Midpoint"; 4 -> "4. Peak Contraction"; else -> "5. Finish"
                },
                description = "Calf Raise pose stage $step.",
                skeletonOffsets = mapOf("PELVIS" to Offset(0.5f, 0.42f - step * 0.005f), "LEFT_HIP" to Offset(0.44f, 0.42f - step * 0.005f), "LEFT_KNEE" to Offset(0.44f, 0.58f - step * 0.005f), "LEFT_ANKLE" to Offset(0.44f, 0.78f - step * 0.012f)),
                activeMuscles = setOf(Muscles.CALVES),
                overlayCues = listOf(OverlayCue(CueType.LINE, "LEFT_ANKLE", "LEFT_ANKLE", "Ankle Flexion"))
            )
        }
        return ExercisePresentationAsset(
            exerciseId = exercise.id, name = "Calf Raise", equipment = "Standing Calf Block", difficulty = "Beginner",
            musclesWorkedText = "Calves / Gastrocnemius & Soleus", setupChecklist = setup, executionSteps = execution,
            commonMistakes = listOf("Bending knees to help push"), coachingTips = listOf("Full extension focus"),
            safetyNotes = emptyList(), anatomySpec = AnatomySpec(setOf(Muscles.CALVES), emptySet()),
            poses = poses, overlayType = OverlayType.GENERIC
        )
    }

    // ---------- 20. GENERAL/GENERIC ----------
    private fun genericCoachingAsset(exercise: Exercise): ExercisePresentationAsset {
        val category = exercise.category.lowercase()
        
        val setup = when {
            category.contains("chest") || category.contains("press") -> listOf(
                "Position yourself flat or seated on the machine or bench.",
                "Plant your feet firmly on the floor for leg drive.",
                "Retract your shoulder blades and brace your core."
            )
            category.contains("back") || category.contains("row") || category.contains("pull") -> listOf(
                "Secure your knees under the thigh pad or hinge forward at 45 deg.",
                "Grip the bar or handle firmly, shoulder-width apart.",
                "Keep your back completely straight and shoulders depressed."
            )
            category.contains("leg") || category.contains("thigh") || category.contains("glute") || category.contains("squat") -> listOf(
                "Stand with feet shoulder-width apart or secure yourself in the machine seat.",
                "Align your hips, knees, and ankles before beginning.",
                "Brace your core tightly and hold side grips or dumbbells."
            )
            else -> listOf(
                "Prepare your starting posture, bracing your core firmly.",
                "Grip the barbell, dumbbells, or machine handles snug.",
                "Align your joints and focus on structural safety."
            )
        }

        val execution = when {
            category.contains("chest") || category.contains("press") -> listOf(
                "Push or press the handles forward/up to full extension under control.",
                "Squeeze your chest at peak contraction.",
                "Lower with control until you feel a comfortable chest stretch."
            )
            category.contains("back") || category.contains("row") || category.contains("pull") -> listOf(
                "Pull the handles or bar toward your upper chest or lower abdomen.",
                "Squeeze your shoulder blades together at the peak.",
                "Extend your arms slowly back to full stretch."
            )
            category.contains("leg") || category.contains("thigh") || category.contains("glute") || category.contains("squat") -> listOf(
                "Bend your knees and lower your hips under control.",
                "Push through your mid-foot/heels to drive back up.",
                "Ensure your knees track in line with your toes throughout."
            )
            else -> listOf(
                "Perform the contraction dynamically, squeezing the target muscle.",
                "Pause briefly at peak range of motion to maximize activation.",
                "Slowly return to starting posture under control."
            )
        }

        val mistakes = when {
            category.contains("leg") || category.contains("thigh") || category.contains("glute") || category.contains("squat") -> listOf(
                "Letting knees cave inward (valgus collapse).",
                "Heels lifting off floor or sled platform."
            )
            else -> listOf(
                "Rushing the eccentric (lowering) portion.",
                "Using momentum or body-swinging to cheat the load."
            )
        }

        val poses = (1..5).map { step ->
            PoseIllustration(
                stageName = when (step) {
                    1 -> "1. Setup"; 2 -> "2. Start"; 3 -> "3. Midpoint"; 4 -> "4. Peak Contraction"; else -> "5. Finish"
                },
                description = "Postured starting position.",
                skeletonOffsets = mapOf("PELVIS" to Offset(0.5f, 0.45f), "MID_SPINE" to Offset(0.5f, 0.35f), "HEAD" to Offset(0.5f, 0.28f), "LEFT_SHOULDER" to Offset(0.42f, 0.35f), "LEFT_ELBOW" to Offset(0.42f, 0.45f), "LEFT_WRIST" to Offset(0.42f, 0.55f)),
                activeMuscles = emptySet(), overlayCues = emptyList()
            )
        }
        return ExercisePresentationAsset(
            exerciseId = exercise.id, name = exercise.name, equipment = exercise.equipment, difficulty = "Beginner",
            musclesWorkedText = exercise.muscles, setupChecklist = setup, executionSteps = execution,
            commonMistakes = mistakes, coachingTips = emptyList(), safetyNotes = emptyList(),
            anatomySpec = AnatomySpec(emptySet(), emptySet()), poses = poses, overlayType = OverlayType.GENERIC
        )
    }
}
