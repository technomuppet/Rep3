package com.replog.domain.library

import com.replog.data.model.Exercise

/**
 * Beginner coaching for an exercise. Sprint 14 rewrite: content is generated from
 * the GRANULAR movement pattern + equipment + category + difficulty + primary
 * muscles, so individual exercises (e.g. Barbell vs Machine vs Dumbbell bench
 * press) read differently and accurately. Everything is plain English, generated
 * offline, with zero stored content and no media.
 */
data class CoachingInfo(
    val description: String,        // one plain-English sentence: "do this"
    val purpose: String,            // <= 150 chars, why you'd do it
    val steps: List<String>,        // 4 simple steps (setup is equipment-specific)
    val cues: List<String>,         // 3 memorable cues
    val mistakes: List<String>,     // up to 4
    val breathing: String,
    val tempo: String,
    val tempoExplanation: String,
    val rangeOfMotion: String,
    val safety: List<String>,       // up to 3
    val feel: List<String>,         // what you SHOULD feel
    val notFeel: List<String>,      // what you should NOT feel
    val reassurance: String         // a confidence-building sentence
)

object ExerciseCoach {

    fun coach(ex: Exercise): CoachingInfo {
        val pat = ExercisePattern.of(ex)
        val eq = EquipmentKind.of(ex)
        val primaryList = muscleList(ex.primaryMuscles.ifBlank { ex.muscles })
        // High pulls are catalogued under the "Lateral Raise" pattern but the weight
        // is pulled UP the body (elbows leading), not raised out to the sides. Give
        // them accurate description + first steps without adding a whole pattern for
        // two exercises. Muscle/diagram (traps) is already correct.
        val isHighPull = ex.name.lowercase().contains("high pull")
        val baseSteps = steps(pat, eq)
        val info = CoachingInfo(
            description = if (isHighPull)
                "Pull the weight straight up the front of your body, leading with your elbows, until it reaches chest height."
            else describe(pat, eq),
            purpose = purpose(pat, primaryList).take(150),
            steps = if (isHighPull)
                listOf(baseSteps[0], "Pull the weight up your body, leading with your elbows.", "Bring it to about chest height.", "Lower it under control and repeat.")
            else baseSteps,
            cues = if (isHighPull) listOf("Lead with your elbows", "Keep the weight close to your body", "Don't swing") else cues(pat),
            mistakes = mistakes(pat),
            breathing = breathing(pat),
            tempo = tempo(pat).first,
            tempoExplanation = tempo(pat).second,
            rangeOfMotion = rom(pat),
            safety = safety(pat, ex),
            feel = feel(pat, primaryList),
            notFeel = notFeel(pat),
            reassurance = reassurance(ex, pat, eq)
        )
        assert(info.steps.size == 4) { "coach(${ex.name}): expected 4 steps" }
        assert(info.cues.size == 3) { "coach(${ex.name}): expected 3 cues" }
        assert(info.description.isNotBlank()) { "coach(${ex.name}): blank description" }
        assert(info.feel.isNotEmpty()) { "coach(${ex.name}): no feel" }
        return info
    }

    /** Kept for callers that still ask for the coarse family (e.g. easier-alt grouping). */
    fun familyOf(ex: Exercise): ExercisePattern = ExercisePattern.of(ex)

    // ---- helpers ----

    private fun muscleList(csv: String): List<String> =
        csv.split(",").map { it.trim() }.filter { it.isNotBlank() }
            .map { it.lowercase() }.distinct()

    /** Human-friendly, de-duplicated muscle phrase, e.g. "quads, glutes and hamstrings". */
    private fun musclePhrase(muscles: List<String>): String {
        val friendly = muscles.map { friendlyMuscle(it) }.distinct()
        return when (friendly.size) {
            0 -> "the target muscles"
            1 -> friendly[0]
            2 -> "${friendly[0]} and ${friendly[1]}"
            else -> friendly.dropLast(1).joinToString(", ") + " and " + friendly.last()
        }
    }

    private fun friendlyMuscle(m: String): String = when {
        m.contains("quad") -> "quads"
        m.contains("pectoral") || m == "chest" -> "chest"
        m.contains("hamstring") -> "hamstrings"
        m.contains("glute") -> "glutes"
        m.contains("calf") || m.contains("calves") -> "calves"
        m.contains("front delt") -> "front shoulders"
        m.contains("side delt") -> "side shoulders"
        m.contains("rear delt") -> "rear shoulders"
        m.contains("delt") || m == "shoulders" -> "shoulders"
        m.contains("bicep") -> "biceps"
        m.contains("tricep") -> "triceps"
        m.contains("forearm") -> "forearms"
        m.contains("oblique") -> "obliques"
        m.contains("abdom") || m == "core" || m == "abs" -> "core"
        m.contains("lat") -> "lats"
        m.contains("trap") -> "traps"
        m.contains("upper back") -> "upper back"
        m.contains("lower back") || m.contains("erector") -> "lower back"
        m == "back" -> "back"
        m.contains("abductor") -> "outer hips"
        m.contains("adductor") -> "inner thighs"
        m.contains("cardio") -> "heart and lungs"
        m.contains("full body") -> "whole body"
        else -> m
    }

    // ---- one-sentence description (Priority 2): pattern + equipment specific ----
    private fun describe(p: ExercisePattern, eq: EquipmentKind): String = when (p) {
        ExercisePattern.HORIZONTAL_PRESS -> when (eq) {
            EquipmentKind.BARBELL, EquipmentKind.SMITH -> "Lie on a flat bench and push the bar straight up until your arms are almost straight."
            EquipmentKind.DUMBBELL -> "Lie on a flat bench and push two dumbbells straight up until your arms are almost straight."
            EquipmentKind.MACHINE -> "Sit on the machine and push the handles away from your chest until your arms are almost straight."
            EquipmentKind.CABLE -> "Push the cable handles forward and together until your arms are almost straight."
            EquipmentKind.BODYWEIGHT -> "Lower your chest toward the floor, then push your body back up until your arms are straight."
            else -> "Push the weight away from your chest until your arms are almost straight, then lower it slowly."
        }
        ExercisePattern.INCLINE_PRESS -> "Lie back on an angled bench and push the weight up and slightly back until your arms are almost straight."
        ExercisePattern.DECLINE_PRESS -> "Lie on a downward-angled bench and push the weight up until your arms are almost straight."
        ExercisePattern.VERTICAL_PRESS -> when (eq) {
            EquipmentKind.DUMBBELL -> "Hold a dumbbell in each hand at shoulder height and press them straight overhead."
            EquipmentKind.MACHINE -> "Sit on the machine and press the handles straight overhead until your arms are almost straight."
            else -> "Press the weight straight overhead until your arms are almost straight, then lower it to your shoulders."
        }
        ExercisePattern.CHEST_FLY -> "With a slight bend in your elbows, open your arms out wide, then bring them together in front of your chest."
        ExercisePattern.ELBOW_EXTENSION -> when (eq) {
            EquipmentKind.CABLE -> "Keep your elbows still and push the cable down until your arms are straight."
            else -> "Keep your elbows still and straighten your arms against the weight, then bend them back slowly."
        }
        ExercisePattern.ELBOW_FLEXION -> when (eq) {
            EquipmentKind.CABLE -> "Keep your elbows by your sides and curl the cable up toward your shoulders."
            EquipmentKind.BARBELL, EquipmentKind.EZ_BAR -> "Keep your elbows by your sides and curl the bar up toward your shoulders."
            else -> "Keep your elbows by your sides and curl the weight up toward your shoulders."
        }
        ExercisePattern.VERTICAL_PULL -> when (eq) {
            EquipmentKind.CABLE, EquipmentKind.MACHINE -> "Pull the bar down toward your upper chest, then let it rise back up slowly."
            EquipmentKind.BODYWEIGHT -> "Hang from the bar and pull your chest up toward it until your chin clears the bar."
            else -> "Pull the weight down toward your chest, then let it return slowly."
        }
        ExercisePattern.HORIZONTAL_ROW -> when (eq) {
            EquipmentKind.CABLE, EquipmentKind.MACHINE -> "Pull the handle toward your stomach while keeping your chest up, then return slowly."
            EquipmentKind.DUMBBELL -> "Pull the dumbbell up toward your hip while keeping your back flat, then lower it slowly."
            else -> "Pull the weight toward your stomach while keeping your back flat, then lower it slowly."
        }
        ExercisePattern.FACE_PULL -> "Pull the rope toward your face, spreading your hands apart as you reach your forehead."
        ExercisePattern.REAR_DELT -> "With a slight bend in your elbows, raise the weight out to your sides and slightly back."
        ExercisePattern.SHRUG -> "Hold the weight at your sides and lift your shoulders straight up toward your ears, then lower them."
        ExercisePattern.WRIST_FLEXION -> "Rest your forearms down and curl the weight up using only your wrists."
        ExercisePattern.OLYMPIC -> "Explosively pull the weight from the floor up to your shoulders in one smooth move."
        ExercisePattern.LATERAL_RAISE -> "With a slight bend in your elbows, raise the weights out to your sides up to shoulder height."
        ExercisePattern.FRONT_RAISE -> "With a slight bend in your elbows, raise the weight straight out in front of you up to shoulder height, then lower it slowly."
        ExercisePattern.SQUAT -> when (eq) {
            EquipmentKind.DUMBBELL, EquipmentKind.KETTLEBELL -> "Hold the weight against your chest and squat down as though sitting into a chair, then stand back up."
            EquipmentKind.BARBELL, EquipmentKind.SMITH -> "With the bar on your upper back, squat down as though sitting into a chair, then stand back up."
            EquipmentKind.MACHINE -> "Sit or stand in the machine and bend your knees to lower the weight, then push back up."
            EquipmentKind.BODYWEIGHT -> "Squat down as though sitting into a chair, then stand back up."
            else -> "Squat down as though sitting into a chair, then stand back up."
        }
        ExercisePattern.LUNGE -> "Step forward and lower your back knee toward the floor, then push back up to standing."
        ExercisePattern.HINGE -> "Keeping your back straight, push your hips back to lower the weight down your legs, then stand tall."
        ExercisePattern.HIP_EXTENSION -> "Drive your hips up until your body is in a straight line, squeezing your glutes at the top."
        ExercisePattern.HIP_ABDUCTION -> "Push your legs outward against the resistance, then return slowly."
        ExercisePattern.HIP_ADDUCTION -> "Squeeze your legs together against the resistance, then return slowly."
        ExercisePattern.KNEE_EXTENSION -> when (eq) {
            EquipmentKind.MACHINE, EquipmentKind.CABLE -> "Sitting down, straighten your knees to lift the pad, then lower it slowly."
            else -> "Keeping your hips and torso straight, bend at the knees to lower your body, then straighten your knees to come back up."
        }
        ExercisePattern.KNEE_FLEXION -> when (eq) {
            EquipmentKind.MACHINE, EquipmentKind.CABLE -> "Curl your heels toward your bottom against the resistance, then return slowly."
            else -> "Anchor your feet, then lower your body forward as slowly as you can by letting your knees straighten, and pull yourself back up."
        }
        ExercisePattern.CALF_RAISE -> "Push up onto your toes as high as you can, then lower your heels back down slowly."
        ExercisePattern.CORE_ANTI_EXTENSION -> "Hold your body in a straight line and keep your stomach tight so your hips do not sag."
        ExercisePattern.CORE_ROTATION -> "Keeping your stomach tight, turn your upper body from side to side under control."
        ExercisePattern.CORE_TRUNK_FLEXION -> "Curl your shoulders up toward your knees using your stomach muscles, then lower slowly."
        ExercisePattern.CORE_HIP_FLEXION -> "Keeping your stomach tight, raise your legs up, then lower them slowly without arching your back."
        ExercisePattern.CORE_CARRY -> "Hold the weight and walk with short, steady steps while keeping your body tall and tight."
        ExercisePattern.CONDITIONING -> when (eq) {
            EquipmentKind.MACHINE -> "Keep moving at a steady, comfortable pace to raise your heart rate."
            else -> "Work at a steady, repeatable effort to raise your heart rate and keep moving."
        }
        ExercisePattern.GENERIC -> "Move the weight smoothly through its full range and back under control."
    }

    private fun purpose(p: ExercisePattern, primary: List<String>): String {
        val m = musclePhrase(primary)
        return when (p) {
            ExercisePattern.HORIZONTAL_PRESS, ExercisePattern.INCLINE_PRESS, ExercisePattern.DECLINE_PRESS ->
                "Builds chest strength and pushing power."
            ExercisePattern.VERTICAL_PRESS -> "Builds strong shoulders and overhead pushing power."
            ExercisePattern.CHEST_FLY -> "Shapes and strengthens the chest through a wide, controlled stretch."
            ExercisePattern.ELBOW_EXTENSION -> "Builds bigger, stronger arms by training your $m."
            ExercisePattern.ELBOW_FLEXION -> "Builds bigger, stronger arms by training your $m."
            ExercisePattern.VERTICAL_PULL -> "Builds a wider, stronger back and improves your posture."
            ExercisePattern.HORIZONTAL_ROW -> "Builds a thicker, stronger back and improves your posture."
            ExercisePattern.FACE_PULL -> "Strengthens the rear shoulders and upper back to improve posture and shoulder health."
            ExercisePattern.REAR_DELT -> "Builds the rear shoulders for balanced, healthy shoulders, working your $m."
            ExercisePattern.SHRUG -> "Builds the upper traps for a stronger neck and shoulders, working your $m."
            ExercisePattern.WRIST_FLEXION -> "Builds forearm and grip strength, working your $m."
            ExercisePattern.OLYMPIC -> "Builds full-body power and explosiveness, working your $m."
            ExercisePattern.LATERAL_RAISE -> "Builds wider, rounder shoulders."
            ExercisePattern.FRONT_RAISE -> "Builds the front of your shoulders and your pressing strength."
            ExercisePattern.SQUAT -> "Builds strong legs and teaches good squatting technique."
            ExercisePattern.LUNGE -> "Builds single-leg strength and balance, working your $m."
            ExercisePattern.HINGE -> "Strengthens the back of your body and protects your back, working your $m."
            ExercisePattern.HIP_EXTENSION -> "Builds strong, powerful glutes and hips."
            ExercisePattern.HIP_ABDUCTION -> "Strengthens your outer hips for stronger, more stable legs."
            ExercisePattern.HIP_ADDUCTION -> "Strengthens your inner thighs for stronger, more stable legs."
            ExercisePattern.KNEE_EXTENSION -> "Builds the front of your thighs by training your $m."
            ExercisePattern.KNEE_FLEXION -> "Builds the back of your thighs by training your $m."
            ExercisePattern.CALF_RAISE -> "Builds stronger lower legs by training your $m."
            ExercisePattern.CORE_ANTI_EXTENSION, ExercisePattern.CORE_HIP_FLEXION ->
                "Builds a strong core that supports your back and steadies every lift."
            ExercisePattern.CORE_ROTATION -> "Builds a strong, athletic core and stronger obliques."
            ExercisePattern.CORE_TRUNK_FLEXION -> "Strengthens your stomach muscles for a stronger, more stable core."
            ExercisePattern.CORE_CARRY -> "Builds full-body and grip strength while keeping your core strong and steady."
            ExercisePattern.CONDITIONING -> "Raises your heart rate to improve fitness, stamina and calorie burn."
            ExercisePattern.GENERIC -> "Builds strength and confidence by training your $m."
        }
    }

    // ---- steps: setup is equipment-specific, then pattern-specific action ----
    private fun steps(p: ExercisePattern, eq: EquipmentKind): List<String> {
        val setup = setupStep(p, eq)
        val action = actionSteps(p, eq)
        return (listOf(setup) + action).take(4).let {
            if (it.size == 4) it else it + List(4 - it.size) { "Repeat for the planned number of reps." }
        }
    }

    private fun setupStep(p: ExercisePattern, eq: EquipmentKind): String {
        // Equipment-specific setup wording (Priority 3).
        // Pattern-specific overrides first - these matter more than equipment.
        when (p) {
            ExercisePattern.CONDITIONING -> return when (eq) {
                EquipmentKind.MACHINE -> "Step onto the machine and start at an easy, comfortable pace."
                else -> "Take a stable stance with the equipment ready, and start at an easy, comfortable pace."
            }
            ExercisePattern.KNEE_EXTENSION -> if (eq == EquipmentKind.BODYWEIGHT)
                return "Kneel or stand tall with your hips locked and something to hold for balance."
            ExercisePattern.KNEE_FLEXION -> if (eq == EquipmentKind.BODYWEIGHT)
                return "Kneel down and anchor your feet (under a pad or a partner) with your body upright."
            ExercisePattern.CALF_RAISE -> return "Place the balls of your feet on the platform with your heels free to drop."
            ExercisePattern.VERTICAL_PRESS -> return when (eq) {
                EquipmentKind.DUMBBELL -> "Stand or sit tall and hold a dumbbell at each shoulder."
                EquipmentKind.MACHINE -> "Sit on the machine and take hold of the handles at shoulder height."
                EquipmentKind.SMITH -> "Sit or stand under the bar with it resting at shoulder height."
                else -> "Stand tall and hold the weight at shoulder height."
            }
            ExercisePattern.CORE_CARRY -> return "Pick up the weight with a flat back and stand up tall."
            else -> {}
        }
        // Lying presses use a flat/angled bench; overhead is handled above.
        val benchPress = p in setOf(ExercisePattern.HORIZONTAL_PRESS, ExercisePattern.INCLINE_PRESS, ExercisePattern.DECLINE_PRESS, ExercisePattern.CHEST_FLY)
        val seatedMachine = "Sit on the machine, adjust the seat so the handles line up with the right height, and take hold of them."
        return when (eq) {
            EquipmentKind.MACHINE -> when {
                p == ExercisePattern.HIP_ABDUCTION || p == ExercisePattern.HIP_ADDUCTION -> "Sit on the machine and place your legs against the pads."
                p == ExercisePattern.KNEE_EXTENSION -> "Sit on the machine with your shins behind the lower pad."
                p == ExercisePattern.KNEE_FLEXION -> "Sit or lie on the machine with the pad against the back of your ankles."
                else -> seatedMachine
            }
            EquipmentKind.CABLE -> "Set the cable to the right height and take hold of the handle with a comfortable grip."
            EquipmentKind.BARBELL -> if (benchPress) "Lie back evenly and grip the bar a little wider than your shoulders." else "Stand tall and grip the bar with your hands about shoulder-width apart."
            EquipmentKind.EZ_BAR -> "Grip the angled bar where it feels comfortable on your wrists and stand or sit tall."
            EquipmentKind.SMITH -> "Position yourself under the bar and twist it off the safety hooks to start."
            EquipmentKind.DUMBBELL -> "Pick up a dumbbell in each hand and get into a stable, balanced position."
            EquipmentKind.KETTLEBELL -> "Hold the kettlebell securely and set your feet about shoulder-width apart."
            EquipmentKind.BAND -> "Anchor the band securely and take up the slack so there is gentle tension to start."
            EquipmentKind.PLATE -> "Hold the weight plate firmly with both hands and set a stable stance."
            EquipmentKind.BODYWEIGHT -> when {
                p == ExercisePattern.VERTICAL_PULL -> "Reach up and take a firm overhand grip on the bar, then hang with straight arms."
                p == ExercisePattern.HORIZONTAL_PRESS -> "Get into a straight-body position with your hands under your shoulders."
                else -> "Get into a stable starting position with your stomach tight."
            }
            EquipmentKind.OTHER -> "Get set up in a stable, balanced starting position."
        }
    }

    private fun actionSteps(p: ExercisePattern, eq: EquipmentKind): List<String> = when (p) {
        ExercisePattern.HORIZONTAL_PRESS, ExercisePattern.INCLINE_PRESS, ExercisePattern.DECLINE_PRESS ->
            listOf("Lower the weight slowly toward your chest.", "Push it back up until your arms are almost straight.", "Pause, then repeat with control.")
        ExercisePattern.VERTICAL_PRESS ->
            listOf("Press the weight straight up overhead.", "Lower it slowly back to your shoulders.", "Repeat without leaning back.")
        ExercisePattern.CHEST_FLY ->
            listOf("Open your arms out wide with a slight elbow bend.", "Bring them together in front of your chest.", "Lower slowly and repeat.")
        ExercisePattern.ELBOW_EXTENSION ->
            listOf("Keep your elbows still and straighten your arms.", "Squeeze, then bend your arms back slowly.", "Repeat with control.")
        ExercisePattern.ELBOW_FLEXION ->
            listOf("Keep your elbows by your sides and curl the weight up.", "Squeeze at the top.", "Lower slowly and repeat.")
        ExercisePattern.VERTICAL_PULL ->
            listOf("Pull down (or up) until your chest is near the bar.", "Squeeze your back muscles.", "Return slowly under control.")
        ExercisePattern.HORIZONTAL_ROW ->
            listOf("Pull the weight toward your stomach, leading with your elbows.", "Squeeze your shoulder blades together.", "Lower slowly and repeat.")
        ExercisePattern.FACE_PULL ->
            listOf("Pull the rope toward your face, hands spreading apart.", "Squeeze your rear shoulders.", "Return slowly and repeat.")
        ExercisePattern.REAR_DELT ->
            listOf("Raise the weight out and slightly back.", "Pause at the top without shrugging.", "Lower slowly and repeat.")
        ExercisePattern.SHRUG ->
            listOf("Lift your shoulders straight up toward your ears.", "Pause at the top.", "Lower slowly and repeat.")
        ExercisePattern.WRIST_FLEXION ->
            listOf("Curl the weight up using only your wrists.", "Pause at the top.", "Lower slowly and repeat.")
        ExercisePattern.OLYMPIC ->
            listOf("Pull the weight up powerfully.", "Catch it at your shoulders.", "Lower it under control and reset.")
        ExercisePattern.LATERAL_RAISE ->
            listOf("Raise the weights out to your sides to shoulder height.", "Pause briefly without shrugging.", "Lower slowly and repeat.")
        ExercisePattern.FRONT_RAISE ->
            listOf("Raise the weight straight out in front to shoulder height.", "Pause briefly without shrugging.", "Lower slowly and repeat.")
        ExercisePattern.SQUAT ->
            listOf("Tighten your stomach and lower down like sitting into a chair.", "Go as deep as is comfortable.", "Push through your feet to stand back up.")
        ExercisePattern.LUNGE ->
            listOf("Step into the lunge and lower your back knee.", "Keep your front knee over your foot.", "Push back up and switch sides.")
        ExercisePattern.HINGE ->
            listOf("Push your hips back to lower the weight down your legs.", "Keep your back straight throughout.", "Drive your hips forward to stand tall.")
        ExercisePattern.HIP_EXTENSION ->
            listOf("Drive your hips up until your body is in a straight line.", "Squeeze your glutes hard at the top.", "Lower slowly and repeat.")
        ExercisePattern.HIP_ABDUCTION ->
            listOf("Push your legs apart against the resistance.", "Pause at the widest point.", "Return slowly and repeat.")
        ExercisePattern.HIP_ADDUCTION ->
            listOf("Squeeze your legs together against the resistance.", "Pause when they meet.", "Return slowly and repeat.")
        ExercisePattern.KNEE_EXTENSION ->
            if (eq == EquipmentKind.BODYWEIGHT)
                listOf("Bend at the knees to lower your body, keeping your hips straight.", "Go only as low as is comfortable.", "Straighten your knees to come back up.")
            else
                listOf("Straighten your knees to lift the pad.", "Pause at the top.", "Lower slowly and repeat.")
        ExercisePattern.KNEE_FLEXION ->
            if (eq == EquipmentKind.BODYWEIGHT)
                listOf("Lower your body forward slowly by letting your knees straighten.", "Keep your hips and back straight.", "Pull yourself back up using the backs of your legs.")
            else
                listOf("Curl your heels toward your bottom.", "Squeeze the back of your thighs.", "Return slowly and repeat.")
        ExercisePattern.CALF_RAISE ->
            listOf("Push up onto your toes as high as you can.", "Pause at the top.", "Lower your heels slowly for a stretch.")
        ExercisePattern.CORE_ANTI_EXTENSION ->
            listOf("Tighten your stomach and hold your body in a straight line.", "Keep your hips from sagging or rising.", "Breathe steadily and hold or repeat.")
        ExercisePattern.CORE_ROTATION ->
            listOf("Tighten your stomach and turn your upper body to one side.", "Control the turn - do not swing.", "Return to the middle and repeat the other way.")
        ExercisePattern.CORE_TRUNK_FLEXION ->
            listOf("Curl your shoulders up toward your knees.", "Squeeze your stomach muscles at the top.", "Lower slowly and repeat.")
        ExercisePattern.CORE_HIP_FLEXION ->
            listOf("Raise your legs while keeping your stomach tight.", "Do not let your lower back arch.", "Lower slowly and repeat.")
        ExercisePattern.CORE_CARRY ->
            listOf("Stand tall with your shoulders back.", "Walk with short, steady steps.", "Set the weight down safely when done.")
        ExercisePattern.CONDITIONING ->
            listOf("Start at an easy pace to warm up.", "Settle into a steady rhythm you can keep.", "Keep your breathing controlled throughout.")
        ExercisePattern.GENERIC ->
            listOf("Move the weight smoothly through its range.", "Squeeze the working muscle.", "Return slowly and repeat.")
    }

    private fun cues(p: ExercisePattern): List<String> = when (p) {
        ExercisePattern.HORIZONTAL_PRESS, ExercisePattern.INCLINE_PRESS, ExercisePattern.DECLINE_PRESS ->
            listOf("Keep your shoulders pinned back", "Lower the weight slowly", "Push with your chest")
        ExercisePattern.VERTICAL_PRESS -> listOf("Tighten your stomach", "Don't lean back", "Press straight up")
        ExercisePattern.CHEST_FLY -> listOf("Keep a slight bend in your elbows", "Feel the stretch across your chest", "Squeeze at the top")
        ExercisePattern.ELBOW_EXTENSION -> listOf("Keep your elbows still", "Only move your forearms", "Squeeze at the bottom")
        ExercisePattern.ELBOW_FLEXION -> listOf("Keep your elbows by your sides", "Don't swing the weight", "Squeeze at the top")
        ExercisePattern.VERTICAL_PULL -> listOf("Lead with your elbows", "Squeeze your back", "Don't swing")
        ExercisePattern.HORIZONTAL_ROW -> listOf("Pull with your elbows", "Squeeze your shoulder blades", "Keep your back straight")
        ExercisePattern.FACE_PULL -> listOf("Aim for your forehead", "Spread your hands apart", "Squeeze your rear shoulders")
        ExercisePattern.REAR_DELT -> listOf("Lead with your elbows", "Don't shrug", "Move slowly")
        ExercisePattern.SHRUG -> listOf("Lift straight up", "Pause at the top", "Don't roll your shoulders")
        ExercisePattern.WRIST_FLEXION -> listOf("Move only your wrists", "Go slowly", "Full range each rep")
        ExercisePattern.OLYMPIC -> listOf("Be explosive", "Keep the weight close", "Catch it softly")
        ExercisePattern.LATERAL_RAISE -> listOf("Lead with your elbows", "Don't shrug your shoulders", "Raise only to shoulder height")
        ExercisePattern.FRONT_RAISE -> listOf("Keep your arms straight in front", "Don't swing or lean back", "Raise only to shoulder height")
        ExercisePattern.SQUAT -> listOf("Push through your heels", "Keep your chest up", "Knees in line with your toes")
        ExercisePattern.LUNGE -> listOf("Keep your body tall", "Control the way down", "Push through your front foot")
        ExercisePattern.HINGE -> listOf("Keep your back straight", "Push your hips back", "Feel it in your hamstrings")
        ExercisePattern.HIP_EXTENSION -> listOf("Squeeze your glutes hard", "Don't arch your back", "Chin slightly tucked")
        ExercisePattern.HIP_ABDUCTION -> listOf("Move slowly", "Push from your outer hips", "Don't lean back")
        ExercisePattern.HIP_ADDUCTION -> listOf("Move slowly", "Squeeze your inner thighs", "Stay upright")
        ExercisePattern.KNEE_EXTENSION -> listOf("Squeeze your thighs at the top", "Lower slowly", "Don't slam the weight")
        ExercisePattern.KNEE_FLEXION -> listOf("Squeeze the back of your thighs", "Move slowly", "Keep your hips down")
        ExercisePattern.CALF_RAISE -> listOf("Go all the way up", "Pause at the top", "Lower for a full stretch")
        ExercisePattern.CORE_ANTI_EXTENSION -> listOf("Tighten your stomach", "Keep a straight line", "Breathe steadily")
        ExercisePattern.CORE_ROTATION -> listOf("Move from your waist", "Go slowly", "Don't swing")
        ExercisePattern.CORE_TRUNK_FLEXION -> listOf("Use your stomach, not your neck", "Go slowly", "Breathe out as you curl up")
        ExercisePattern.CORE_HIP_FLEXION -> listOf("Keep your stomach tight", "Don't arch your back", "Lower slowly")
        ExercisePattern.CORE_CARRY -> listOf("Stand tall", "Tighten your stomach", "Short steady steps")
        ExercisePattern.CONDITIONING -> listOf("Keep a steady pace", "Breathe in a rhythm", "Stay relaxed")
        ExercisePattern.GENERIC -> listOf("Move with control", "Tighten your stomach", "Focus on the working muscle")
    }

    private fun mistakes(p: ExercisePattern): List<String> = when (p) {
        ExercisePattern.HORIZONTAL_PRESS, ExercisePattern.INCLINE_PRESS, ExercisePattern.DECLINE_PRESS ->
            listOf("Bouncing the weight off your chest", "Flaring your elbows out too wide", "Lifting your hips off the bench", "Rushing the reps")
        ExercisePattern.VERTICAL_PRESS -> listOf("Leaning too far back", "Using your legs to push", "Going too heavy too soon")
        ExercisePattern.CHEST_FLY -> listOf("Bending your elbows too much (turning it into a press)", "Using too much weight", "Rushing the stretch")
        ExercisePattern.ELBOW_EXTENSION -> listOf("Letting your elbows drift forward", "Using your shoulders", "Going too heavy")
        ExercisePattern.ELBOW_FLEXION -> listOf("Swinging the weight up", "Moving your elbows", "Using your back")
        ExercisePattern.VERTICAL_PULL -> listOf("Swinging your body", "Pulling with your arms only", "Not going through the full range")
        ExercisePattern.HORIZONTAL_ROW -> listOf("Rounding your back", "Swinging the weight up", "Shrugging your shoulders")
        ExercisePattern.FACE_PULL -> listOf("Going too heavy", "Shrugging your shoulders", "Rushing the movement")
        ExercisePattern.REAR_DELT -> listOf("Using momentum", "Going too heavy", "Shrugging instead of raising")
        ExercisePattern.SHRUG -> listOf("Rolling your shoulders", "Bending your arms", "Using too little range")
        ExercisePattern.WRIST_FLEXION -> listOf("Moving your whole arm", "Going too fast", "Using too much weight")
        ExercisePattern.OLYMPIC -> listOf("Rounding your back", "Pulling too early with the arms", "Letting the weight drift away")
        ExercisePattern.LATERAL_RAISE -> listOf("Swinging the weights up", "Shrugging your shoulders", "Going too heavy", "Raising above shoulder height")
        ExercisePattern.FRONT_RAISE -> listOf("Swinging the weight up", "Leaning back to cheat", "Going too heavy", "Raising above shoulder height")
        ExercisePattern.SQUAT -> listOf("Letting your knees cave inward", "Rounding your back", "Coming up onto your toes", "Not going deep enough")
        ExercisePattern.LUNGE -> listOf("Letting your front knee cave in", "Leaning too far forward", "Taking too small a step")
        ExercisePattern.HINGE -> listOf("Rounding your lower back", "Squatting instead of hingeing", "Jerking the weight up")
        ExercisePattern.HIP_EXTENSION -> listOf("Arching your lower back", "Not squeezing your glutes", "Pushing through your toes instead of heels")
        ExercisePattern.HIP_ABDUCTION -> listOf("Using momentum", "Leaning back too far", "Rushing the reps")
        ExercisePattern.HIP_ADDUCTION -> listOf("Using momentum", "Going too heavy", "Rushing the reps")
        ExercisePattern.KNEE_EXTENSION -> listOf("Slamming the weight down", "Going too heavy", "Using momentum")
        ExercisePattern.KNEE_FLEXION -> listOf("Lifting your hips", "Rushing the reps", "Using momentum")
        ExercisePattern.CALF_RAISE -> listOf("Bouncing at the bottom", "Using a short range", "Going too fast")
        ExercisePattern.CORE_ANTI_EXTENSION -> listOf("Letting your hips sag", "Holding your breath", "Lifting your hips too high")
        ExercisePattern.CORE_ROTATION -> listOf("Swinging instead of controlling", "Going too fast", "Holding your breath")
        ExercisePattern.CORE_TRUNK_FLEXION -> listOf("Pulling on your neck", "Using momentum", "Holding your breath")
        ExercisePattern.CORE_HIP_FLEXION -> listOf("Arching your lower back", "Swinging your legs", "Rushing the reps")
        ExercisePattern.CORE_CARRY -> listOf("Leaning to one side", "Rounding your back", "Taking long uneven steps")
        ExercisePattern.CONDITIONING -> listOf("Starting too fast", "Holding your breath", "Skipping a warm-up")
        ExercisePattern.GENERIC -> listOf("Using momentum", "Rushing the reps", "Holding your breath")
    }

    private fun breathing(p: ExercisePattern): String = when (p) {
        ExercisePattern.HORIZONTAL_PRESS, ExercisePattern.INCLINE_PRESS, ExercisePattern.DECLINE_PRESS, ExercisePattern.VERTICAL_PRESS, ExercisePattern.CHEST_FLY ->
            "Breathe in as you lower the weight, and breathe out as you push."
        ExercisePattern.VERTICAL_PULL, ExercisePattern.HORIZONTAL_ROW, ExercisePattern.ELBOW_FLEXION, ExercisePattern.FACE_PULL, ExercisePattern.REAR_DELT, ExercisePattern.SHRUG ->
            "Breathe out as you pull, and breathe in as you return."
        ExercisePattern.ELBOW_EXTENSION -> "Breathe out as you straighten your arms, and breathe in as you bend them."
        ExercisePattern.LATERAL_RAISE, ExercisePattern.FRONT_RAISE -> "Breathe out as you raise the weight, and breathe in as you lower it."
        ExercisePattern.SQUAT, ExercisePattern.LUNGE, ExercisePattern.HIP_EXTENSION, ExercisePattern.KNEE_EXTENSION ->
            "Breathe in as you lower, and breathe out as you push up."
        ExercisePattern.HINGE -> "Take a breath in at the top, then breathe out as you stand up."
        ExercisePattern.CALF_RAISE -> "Breathe out as you rise onto your toes, and breathe in as you lower."
        ExercisePattern.KNEE_FLEXION, ExercisePattern.HIP_ABDUCTION, ExercisePattern.HIP_ADDUCTION, ExercisePattern.WRIST_FLEXION ->
            "Breathe out as you work against the resistance, and breathe in as you return."
        ExercisePattern.OLYMPIC -> "Breathe out sharply as you pull the weight up."
        ExercisePattern.CORE_TRUNK_FLEXION, ExercisePattern.CORE_ROTATION, ExercisePattern.CORE_HIP_FLEXION ->
            "Breathe out as you tighten your stomach, and in as you return."
        ExercisePattern.CORE_ANTI_EXTENSION, ExercisePattern.CORE_CARRY -> "Keep breathing steadily - do not hold your breath."
        ExercisePattern.CONDITIONING -> "Keep a steady, rhythmic breathing pattern throughout."
        ExercisePattern.GENERIC -> "Breathe out on the effort, and breathe in as you return."
    }

    private fun tempo(p: ExercisePattern): Pair<String, String> = when (p) {
        ExercisePattern.CONDITIONING -> "Steady pace" to "Keep a continuous, comfortable pace rather than counting seconds."
        ExercisePattern.CORE_ANTI_EXTENSION, ExercisePattern.CORE_CARRY -> "Slow and steady" to "Hold a strong, steady position - there is no fast part."
        ExercisePattern.OLYMPIC -> "Fast up, slow down" to "Lift the weight quickly and powerfully, then lower it under control."
        else -> "Slow down, quick up" to "Take about 2 seconds to lower, pause for 1, then lift smoothly over about 2 seconds."
    }

    private fun rom(p: ExercisePattern): String = when (p) {
        ExercisePattern.HORIZONTAL_PRESS, ExercisePattern.INCLINE_PRESS, ExercisePattern.DECLINE_PRESS ->
            "Lower until the weight is near your chest, then push up until your arms are almost straight (don't lock hard)."
        ExercisePattern.VERTICAL_PRESS -> "Lower to about your shoulders, then press up until your arms are almost straight."
        ExercisePattern.CHEST_FLY -> "Open until you feel a gentle stretch across your chest, then bring the weights together."
        ExercisePattern.VERTICAL_PULL -> "Start from a full stretch with arms straight, then pull until your chest is near the bar."
        ExercisePattern.HORIZONTAL_ROW -> "Start with your arms straight, then pull until your hands reach your stomach."
        ExercisePattern.ELBOW_FLEXION -> "Start with your arms straight, then curl all the way up."
        ExercisePattern.ELBOW_EXTENSION -> "Start with your arms bent, then straighten them fully."
        ExercisePattern.LATERAL_RAISE, ExercisePattern.FRONT_RAISE, ExercisePattern.REAR_DELT -> "Raise to about shoulder height - no higher - then lower all the way down."
        ExercisePattern.SQUAT -> "Lower until your thighs are at least parallel to the floor if comfortable, then stand all the way up."
        ExercisePattern.LUNGE -> "Lower until your back knee is just above the floor, then stand back up."
        ExercisePattern.HINGE -> "Lower until you feel a stretch in your hamstrings with a flat back, then stand tall."
        ExercisePattern.HIP_EXTENSION -> "Lift until your body is in a straight line - don't over-arch - then lower fully."
        ExercisePattern.CALF_RAISE -> "Rise as high onto your toes as possible, then lower your heels for a full stretch."
        ExercisePattern.HIP_ABDUCTION, ExercisePattern.HIP_ADDUCTION -> "Use a comfortable range with no pinching, moving fully in and out."
        ExercisePattern.KNEE_EXTENSION -> "Straighten fully without snapping your knees, then lower under control."
        ExercisePattern.KNEE_FLEXION -> "Curl as far as is comfortable, then return fully."
        ExercisePattern.CONDITIONING -> "Use a comfortable, repeatable range you can keep up."
        ExercisePattern.CORE_ANTI_EXTENSION, ExercisePattern.CORE_CARRY -> "Hold your position; there is no big range of motion - quality over movement."
        else -> "Move through a full, controlled range on every rep without forcing it."
    }

    private fun safety(p: ExercisePattern, ex: Exercise): List<String> {
        val base = when (p) {
            ExercisePattern.HORIZONTAL_PRESS, ExercisePattern.INCLINE_PRESS, ExercisePattern.DECLINE_PRESS ->
                mutableListOf("Use a spotter or safety bars when lifting heavy.", "Warm up your shoulders first.")
            ExercisePattern.VERTICAL_PRESS, ExercisePattern.LATERAL_RAISE, ExercisePattern.FRONT_RAISE, ExercisePattern.REAR_DELT ->
                mutableListOf("Start light to protect your shoulders.", "Stop if you feel any pinching.")
            ExercisePattern.HINGE, ExercisePattern.HIP_EXTENSION ->
                mutableListOf("Keep your back straight to protect it.", "Start light and add weight slowly.")
            ExercisePattern.SQUAT, ExercisePattern.LUNGE ->
                mutableListOf("Use safety bars or a rack when going heavy.", "Reduce the range if your knees feel uncomfortable.")
            ExercisePattern.CORE_ANTI_EXTENSION, ExercisePattern.CORE_TRUNK_FLEXION, ExercisePattern.CORE_HIP_FLEXION, ExercisePattern.CORE_ROTATION ->
                mutableListOf("Stop if you feel lower-back discomfort.", "Don't pull on your neck.")
            ExercisePattern.CONDITIONING ->
                mutableListOf("Warm up before pushing hard.", "Stop if you feel dizzy or unwell.")
            else -> mutableListOf("Start with a light, manageable weight.", "Stop if you feel sharp pain.")
        }
        if (ex.difficulty.equals("Advanced", true)) {
            base.add(0, "This is an advanced exercise - try an easier version first.")
        }
        return base.take(3)
    }

    // ---- what you should / should not feel (Priority 6) ----
    private fun feel(p: ExercisePattern, primary: List<String>): List<String> {
        // A couple of patterns are whole-body efforts where the named primary muscle
        // alone undersells what the user actually feels.
        when (p) {
            ExercisePattern.CONDITIONING -> return listOf("your heart rate rising", "your legs working", "your breathing speed up")
            ExercisePattern.CORE_CARRY -> return listOf("your core staying tight", "your grip", "your upper back")
            else -> {}
        }
        val muscles = primary.map { friendlyMuscle(it) }.distinct().take(3)
        return if (muscles.isNotEmpty()) muscles else listOf("the working muscle", "a steady effort")
    }

    private fun notFeel(p: ExercisePattern): List<String> = when (p) {
        ExercisePattern.SQUAT, ExercisePattern.LUNGE, ExercisePattern.KNEE_EXTENSION ->
            listOf("sharp knee pain", "lower-back pain")
        ExercisePattern.HINGE, ExercisePattern.HIP_EXTENSION, ExercisePattern.HORIZONTAL_ROW ->
            listOf("lower-back pain", "pain in your spine")
        ExercisePattern.HORIZONTAL_PRESS, ExercisePattern.INCLINE_PRESS, ExercisePattern.DECLINE_PRESS,
        ExercisePattern.VERTICAL_PRESS, ExercisePattern.LATERAL_RAISE, ExercisePattern.FRONT_RAISE, ExercisePattern.REAR_DELT, ExercisePattern.CHEST_FLY ->
            listOf("sharp shoulder pain", "pain in your joints")
        ExercisePattern.ELBOW_FLEXION, ExercisePattern.ELBOW_EXTENSION, ExercisePattern.WRIST_FLEXION ->
            listOf("pain in your elbows", "pain in your wrists")
        ExercisePattern.CORE_ANTI_EXTENSION, ExercisePattern.CORE_TRUNK_FLEXION, ExercisePattern.CORE_HIP_FLEXION, ExercisePattern.CORE_ROTATION ->
            listOf("lower-back pain", "strain in your neck")
        ExercisePattern.CONDITIONING -> listOf("chest pain", "dizziness")
        else -> listOf("sharp or joint pain", "pain in your back")
    }

    // ---- reassurance (Priority 5/9), generated from difficulty + equipment + pattern ----
    private fun reassurance(ex: Exercise, p: ExercisePattern, eq: EquipmentKind): String {
        val d = ex.difficulty.lowercase()
        val easyPattern = p in setOf(
            ExercisePattern.KNEE_EXTENSION, ExercisePattern.KNEE_FLEXION, ExercisePattern.HIP_ABDUCTION,
            ExercisePattern.HIP_ADDUCTION, ExercisePattern.LATERAL_RAISE, ExercisePattern.FRONT_RAISE, ExercisePattern.CALF_RAISE,
            ExercisePattern.CORE_TRUNK_FLEXION, ExercisePattern.SHRUG
        )
        return when {
            d == "beginner" && eq == EquipmentKind.MACHINE ->
                "This is one of the easiest exercises to learn - the machine guides the movement for you, so just relax and follow it."
            d == "beginner" && eq == EquipmentKind.BODYWEIGHT ->
                "You can practise this using only your bodyweight, at your own pace. Most people feel comfortable with it within a session or two."
            d == "beginner" && easyPattern ->
                "This is a beginner-friendly exercise that's easy to pick up. Start light and you'll have it after a couple of tries."
            d == "beginner" ->
                "This is beginner-friendly. Start light, focus on the movement, and most people get comfortable with it quickly."
            d == "intermediate" ->
                "Take your time with this one - start lighter than you think and build up. If it feels awkward, try the easier variation below."
            d == "advanced" ->
                "This is an advanced exercise, so there's no rush. Build up to it with an easier variation first - that's the smart way to progress."
            else -> "Start light, move with control, and you'll build confidence quickly. If it feels off, try the easier variation below."
        }
    }
}
