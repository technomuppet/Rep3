package com.replog.domain.library

import com.replog.data.model.Exercise

/**
 * Beginner coaching information for an exercise. Every field is guaranteed
 * non-empty by [ExerciseCoach.coach], with the list cardinalities the Sprint 13
 * spec requires (4 steps, 3 cues, up to 4 mistakes, up to 3 safety notes).
 *
 * This content is GENERATED at runtime from the exercise's existing attributes
 * (movement pattern, equipment, difficulty, muscles). It adds ZERO storage and
 * needs no database migration, and it scales to any number of exercises because
 * it is keyed on the small, fixed set of movement-pattern families.
 */
data class CoachingInfo(
    val purpose: String,            // <= 150 chars, one sentence
    val steps: List<String>,        // exactly 4
    val cues: List<String>,         // exactly 3
    val mistakes: List<String>,     // up to 4
    val breathing: String,          // one sentence
    val tempo: String,              // e.g. "2-1-2"
    val tempoExplanation: String,
    val rangeOfMotion: String,
    val safety: List<String>        // up to 3
)

/**
 * Pure, offline coaching-content generator. No I/O, no Android, deterministic.
 */
object ExerciseCoach {

    /** The movement-pattern family used as the primary content key. */
    enum class Family { PUSH, PULL, HINGE, SQUAT, LUNGE, SHOULDERS, CORE, CARRY, CALVES, CONDITIONING, GENERIC }

    fun familyOf(ex: Exercise): Family {
        val p = ex.movementPattern.lowercase()
        val c = ex.category.lowercase()
        return when {
            p.contains("squat") -> Family.SQUAT
            p.contains("lunge") -> Family.LUNGE
            p.contains("hinge") || p.contains("hip extension") || p.contains("olympic") -> Family.HINGE
            p.contains("calf") -> Family.CALVES
            p.contains("carry") -> Family.CARRY
            p.startsWith("push") || p.contains("press") || p.contains("fly") -> Family.PUSH
            p.startsWith("pull") || p.contains("row") || p.contains("pull") || p.contains("shrug") || p.contains("face pull") -> Family.PULL
            p.startsWith("shoulders") || p.contains("lateral raise") || p.contains("rear delt") -> Family.SHOULDERS
            p.startsWith("core") || c == "core" -> Family.CORE
            p.contains("conditioning") || c == "cardio" -> Family.CONDITIONING
            p.contains("knee flexion") || p.contains("knee extension") || c == "legs" -> Family.SQUAT
            else -> Family.GENERIC
        }
    }

    private fun primaryMuscle(ex: Exercise): String {
        val raw = ex.primaryMuscles.split(",").map { it.trim() }.firstOrNull { it.isNotBlank() }
            ?: ex.muscles.split(",").map { it.trim() }.firstOrNull { it.isNotBlank() }
            ?: ex.category
        return raw.lowercase()
    }

    private fun equipmentNoun(ex: Exercise): String = when (ex.equipment.lowercase()) {
        "bodyweight", "none", "" -> "your bodyweight"
        "band" -> "the band"
        "cable" -> "the cable"
        "machine", "smith machine" -> "the machine"
        "barbell" -> "the bar"
        "ez bar" -> "the EZ bar"
        "dumbbell" -> "the dumbbells"
        "kettlebell" -> "the kettlebell"
        "plate" -> "the plate"
        else -> "the weight"
    }

    fun coach(ex: Exercise): CoachingInfo {
        val fam = familyOf(ex)
        val muscle = primaryMuscle(ex).ifBlank { "the target muscles" }
        val eq = equipmentNoun(ex)
        val tempo = tempoFor(fam)
        return CoachingInfo(
            purpose = purposeFor(fam, muscle).take(150),
            steps = stepsFor(fam, eq),
            cues = cuesFor(fam),
            mistakes = mistakesFor(fam),
            breathing = breathingFor(fam),
            tempo = tempo.first,
            tempoExplanation = tempo.second,
            rangeOfMotion = romFor(fam),
            safety = safetyFor(fam, ex)
        )
    }

    private fun purposeFor(f: Family, muscle: String): String = when (f) {
        Family.PUSH -> "Builds pressing strength and develops the $muscle and supporting pushing muscles."
        Family.PULL -> "Builds pulling strength and develops the $muscle for a stronger, balanced back."
        Family.HINGE -> "Strengthens the posterior chain, developing the $muscle, glutes and hamstrings."
        Family.SQUAT -> "Builds lower-body strength and develops the $muscle, glutes and overall leg power."
        Family.LUNGE -> "Builds single-leg strength and balance while developing the $muscle and glutes."
        Family.SHOULDERS -> "Develops shoulder strength and width by targeting the $muscle."
        Family.CORE -> "Strengthens the core and trunk, improving stability and protecting the spine."
        Family.CARRY -> "Builds full-body and grip strength while reinforcing a strong, braced core."
        Family.CALVES -> "Develops calf strength and size through the $muscle for stronger lower legs."
        Family.CONDITIONING -> "Raises your heart rate to improve conditioning, endurance and calorie burn."
        Family.GENERIC -> "Develops strength in the $muscle and improves overall training capacity."
    }

    private fun stepsFor(f: Family, eq: String): List<String> = when (f) {
        Family.PUSH -> listOf(
            "Set up with $eq and a stable, braced position.",
            "Lower under control to the working position.",
            "Press back until your arms are straight.",
            "Repeat with smooth, controlled reps."
        )
        Family.PULL -> listOf(
            "Set up with $eq and a tall, stable posture.",
            "Pull toward your body, leading with the elbows.",
            "Squeeze the target muscle at the top.",
            "Lower under control and repeat."
        )
        Family.HINGE -> listOf(
            "Stand with $eq and a soft bend in the knees.",
            "Hinge at the hips, keeping your back flat.",
            "Drive your hips forward to stand tall.",
            "Lower under control and repeat."
        )
        Family.SQUAT -> listOf(
            "Set up with $eq and feet about shoulder width.",
            "Brace your core and sit down between your hips.",
            "Drive through your feet to stand back up.",
            "Repeat with full, controlled depth."
        )
        Family.LUNGE -> listOf(
            "Stand tall holding $eq with a braced core.",
            "Step into the lunge and lower your back knee.",
            "Push through the front foot to return.",
            "Alternate or complete reps, then switch sides."
        )
        Family.SHOULDERS -> listOf(
            "Set up with $eq and a tall, stable torso.",
            "Raise the weight in a smooth, controlled arc.",
            "Pause briefly at the top without shrugging.",
            "Lower slowly and repeat."
        )
        Family.CORE -> listOf(
            "Set up in a stable position and brace your core.",
            "Move only the intended segment, keeping control.",
            "Hold or squeeze briefly at the hardest point.",
            "Return under control and repeat."
        )
        Family.CARRY -> listOf(
            "Pick up $eq with a flat back and braced core.",
            "Stand tall with shoulders back and chest up.",
            "Walk with short, controlled, even steps.",
            "Set the weight down safely when finished."
        )
        Family.CALVES -> listOf(
            "Set up with $eq and balls of the feet on a stable surface.",
            "Lower your heels for a full stretch.",
            "Drive up onto your toes as high as possible.",
            "Lower under control and repeat."
        )
        Family.CONDITIONING -> listOf(
            "Set up in a balanced, athletic stance.",
            "Move with a steady, repeatable rhythm.",
            "Keep your breathing controlled throughout.",
            "Maintain good form as you fatigue."
        )
        Family.GENERIC -> listOf(
            "Set up with $eq in a stable, braced position.",
            "Move through the working range under control.",
            "Contract the target muscle at the hardest point.",
            "Return slowly and repeat with good form."
        )
    }

    private fun cuesFor(f: Family): List<String> = when (f) {
        Family.PUSH -> listOf("Keep your shoulder blades set", "Control the lowering phase", "Keep your wrists stacked over your elbows")
        Family.PULL -> listOf("Lead with your elbows", "Squeeze at the top", "Avoid using momentum")
        Family.HINGE -> listOf("Keep your back flat", "Push your hips back", "Drive through your heels")
        Family.SQUAT -> listOf("Drive through your heels", "Keep your chest up", "Track knees over your toes")
        Family.LUNGE -> listOf("Keep your torso tall", "Control the descent", "Push through the front heel")
        Family.SHOULDERS -> listOf("Lead with your elbows", "Keep a slight bend in the arm", "Avoid shrugging your traps")
        Family.CORE -> listOf("Brace before you move", "Breathe steadily", "Move slowly and with control")
        Family.CARRY -> listOf("Stand tall and proud", "Keep your core braced", "Take short, even steps")
        Family.CALVES -> listOf("Use a full range of motion", "Pause at the top", "Control the stretch at the bottom")
        Family.CONDITIONING -> listOf("Keep a steady rhythm", "Stay light on your feet", "Breathe in a controlled pattern")
        Family.GENERIC -> listOf("Move with control", "Keep your core braced", "Focus on the target muscle")
    }

    private fun mistakesFor(f: Family): List<String> = when (f) {
        Family.PUSH -> listOf("Flaring the elbows too wide", "Bouncing the weight", "Losing tightness in the upper back", "Using momentum")
        Family.PULL -> listOf("Using momentum to swing the weight", "Rounding the upper back", "Shrugging the shoulders", "Cutting the range short")
        Family.HINGE -> listOf("Rounding the lower back", "Squatting instead of hinging", "Jerking the weight up", "Looking up sharply")
        Family.SQUAT -> listOf("Letting the knees cave inward", "Rounding the back", "Rising onto the toes", "Cutting depth short")
        Family.LUNGE -> listOf("Letting the front knee cave in", "Leaning too far forward", "Taking too short a step", "Rushing the reps")
        Family.SHOULDERS -> listOf("Swinging the weight up", "Shrugging the traps", "Using too heavy a load", "Cutting the range short")
        Family.CORE -> listOf("Holding your breath and straining", "Pulling on the neck", "Rushing the reps", "Arching the lower back")
        Family.CARRY -> listOf("Leaning to one side", "Rounding the back", "Taking long, uneven steps", "Letting the shoulders slump")
        Family.CALVES -> listOf("Bouncing at the bottom", "Using a partial range", "Going too fast", "Letting the ankles roll out")
        Family.CONDITIONING -> listOf("Starting too fast and burning out", "Holding your breath", "Sacrificing form for speed", "Skipping a warm-up")
        Family.GENERIC -> listOf("Using momentum", "Rushing the reps", "Cutting the range short", "Holding your breath")
    }

    private fun breathingFor(f: Family): String = when (f) {
        Family.PUSH -> "Inhale as you lower the weight, and exhale as you press."
        Family.PULL -> "Inhale as you lower the weight, and exhale as you pull."
        Family.HINGE -> "Take a breath and brace at the top, then exhale as you stand."
        Family.SQUAT -> "Breathe in and brace at the top, then exhale as you stand up."
        Family.LUNGE -> "Inhale as you lower, and exhale as you push back up."
        Family.SHOULDERS -> "Exhale as you raise the weight, and inhale as you lower it."
        Family.CORE -> "Exhale as you contract, and breathe steadily throughout."
        Family.CARRY -> "Breathe in a steady, controlled rhythm while keeping your core braced."
        Family.CALVES -> "Exhale as you rise onto your toes, and inhale as you lower."
        Family.CONDITIONING -> "Keep a steady, rhythmic breathing pattern throughout."
        Family.GENERIC -> "Exhale on the effort, and inhale as you return."
    }

    private fun tempoFor(f: Family): Pair<String, String> = when (f) {
        Family.CONDITIONING -> "steady" to "Keep a continuous, repeatable pace rather than counting a tempo."
        Family.CORE, Family.CARRY -> "controlled" to "Move slowly and hold tension; avoid rushing."
        else -> "2-1-2" to "Two seconds lowering, a one-second pause, two seconds lifting."
    }

    private fun romFor(f: Family): String = when (f) {
        Family.PUSH -> "Lower until you feel a comfortable stretch, then press to full arm extension without locking out hard."
        Family.PULL -> "Reach a full stretch at the start, then pull to a strong contraction of the target muscle."
        Family.HINGE -> "Lower until you feel a hamstring stretch with a flat back, then return to a tall, locked-out standing position."
        Family.SQUAT -> "Descend to at least thighs-parallel if your mobility allows, then stand all the way up."
        Family.LUNGE -> "Lower until the back knee is just above the floor, then return to standing."
        Family.SHOULDERS -> "Raise to about shoulder height, then lower fully under control."
        Family.CORE -> "Work through a controlled range without losing your braced position."
        Family.CARRY -> "There is no lifting range; focus on posture and distance or time."
        Family.CALVES -> "Lower the heels for a full stretch, then rise as high onto the toes as possible."
        Family.CONDITIONING -> "Use a comfortable, repeatable range that you can sustain."
        Family.GENERIC -> "Use a full, controlled range of motion on every rep."
    }

    private fun safetyFor(f: Family, ex: Exercise): List<String> {
        val base = when (f) {
            Family.PUSH -> mutableListOf("Use a spotter or safeties when pressing heavy.", "Warm up the shoulders before heavy sets.")
            Family.PULL -> mutableListOf("Avoid jerking the weight to protect the lower back.", "Keep the neck neutral.")
            Family.HINGE -> mutableListOf("Stop if you feel lower-back pain.", "Master the hinge with light load first.", "Keep the bar or weight close to your body.")
            Family.SQUAT -> mutableListOf("Use safeties or a rack when going heavy.", "Build depth gradually as mobility allows.")
            Family.LUNGE -> mutableListOf("Use a stable surface and watch your balance.", "Reduce range if you feel knee discomfort.")
            Family.SHOULDERS -> mutableListOf("Start light to protect the shoulder joint.", "Stop if you feel pinching at the top.")
            Family.CORE -> mutableListOf("Avoid straining your neck.", "Stop if you feel lower-back discomfort.")
            Family.CARRY -> mutableListOf("Lift and lower the weight with a flat back.", "Clear a safe walking path first.")
            Family.CALVES -> mutableListOf("Hold something stable if balance is a concern.", "Avoid bouncing at the bottom.")
            Family.CONDITIONING -> mutableListOf("Warm up before high-intensity efforts.", "Stop if you feel dizzy or unwell.")
            Family.GENERIC -> mutableListOf("Start with a light, manageable load.", "Stop if you feel sharp pain.")
        }
        if (ex.difficulty.equals("Advanced", true)) {
            base.add(0, "This is an advanced movement; build up to it with an easier variation first.")
        }
        return base.take(3)
    }
}
