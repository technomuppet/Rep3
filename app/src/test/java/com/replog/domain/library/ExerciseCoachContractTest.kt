package com.replog.domain.library

import com.replog.data.model.Exercise
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * CI-enforced coaching content contract (Sprint 16 Phase 3). Runs the real
 * ExerciseCoach over a representative exercise from each equipment/pattern combo
 * and asserts the cardinality + non-contradiction rules that manual reading
 * verified. Pure JVM (no Android), so it runs in testDebugUnitTest.
 */
class ExerciseCoachContractTest {

    private fun ex(name: String, cat: String, equip: String, pm: String, sm: String, pattern: String, diff: String = "Beginner") =
        Exercise(
            id = 1, name = name, category = cat, equipment = equip, type = "Strength",
            muscles = "$pm, $sm", primaryMuscles = pm, secondaryMuscles = sm,
            movementPattern = pattern, difficulty = diff
        )

    private val samples = listOf(
        ex("Barbell Bench Press", "Chest", "Barbell", "Pectorals", "Triceps", "Push \u2022 Horizontal Press", "Intermediate"),
        ex("Dumbbell Bench Press", "Chest", "Dumbbell", "Pectorals", "Triceps", "Push \u2022 Horizontal Press"),
        ex("Machine Chest Press", "Chest", "Machine", "Pectorals", "Triceps", "Push \u2022 Horizontal Press"),
        ex("Goblet Squat", "Legs", "Dumbbell", "Quadriceps", "Glutes", "Legs \u2022 Squat"),
        ex("Sissy Squat", "Legs", "Bodyweight", "Quadriceps", "", "Legs \u2022 Knee Extension", "Advanced"),
        ex("Nordic Curl", "Legs", "Bodyweight", "Hamstrings", "", "Legs \u2022 Knee Flexion", "Advanced"),
        ex("Leg Extension", "Legs", "Machine", "Quadriceps", "", "Legs \u2022 Knee Extension"),
        ex("Battle Ropes", "Cardio", "Cable", "Cardiovascular", "", "Conditioning", "Intermediate"),
        ex("Treadmill Run", "Cardio", "Machine", "Cardiovascular", "", "Conditioning"),
        ex("Dumbbell Front Raise", "Shoulders", "Dumbbell", "Front Deltoids", "", "Shoulders \u2022 Lateral Raise"),
        ex("Dumbbell Lateral Raise", "Shoulders", "Dumbbell", "Side Deltoids", "", "Shoulders \u2022 Lateral Raise"),
        ex("High Pull", "Shoulders", "Barbell", "Traps", "", "Shoulders \u2022 Lateral Raise", "Intermediate"),
        ex("Farmers Carry", "Core", "Dumbbell", "Forearms", "", "Core \u2022 Carry"),
        ex("Hip Abduction Machine", "Legs", "Machine", "Abductors", "Glutes", "Legs \u2022 Hip Abduction")
    )

    @Test fun cardinality_contract_holds() {
        samples.forEach { e ->
            val c = ExerciseCoach.coach(e)
            assertEquals("${e.name} steps", 4, c.steps.size)
            assertEquals("${e.name} cues", 3, c.cues.size)
            assertTrue("${e.name} mistakes", c.mistakes.size in 1..4)
            assertTrue("${e.name} safety", c.safety.size in 1..3)
            assertTrue("${e.name} feel", c.feel.isNotEmpty())
            assertTrue("${e.name} desc", c.description.isNotBlank())
            assertTrue("${e.name} purpose<=150", c.purpose.length <= 150)
        }
    }

    @Test fun no_direction_contradictions() {
        fun descOf(e: Exercise) = ExerciseCoach.coach(e).description.lowercase()
        val front = samples.first { it.name == "Dumbbell Front Raise" }
        val lateral = samples.first { it.name == "Dumbbell Lateral Raise" }
        val highPull = samples.first { it.name == "High Pull" }
        assertTrue("front raise must say 'in front'", descOf(front).contains("in front"))
        assertTrue("front raise must NOT say 'out to your sides'", !descOf(front).contains("out to your sides"))
        assertTrue("lateral raise must say 'sides'", descOf(lateral).contains("sides"))
        assertTrue("high pull must say 'up'", descOf(highPull).contains("up"))
        assertTrue("high pull must NOT say 'out to your sides'", !descOf(highPull).contains("out to your sides"))
    }

    @Test fun bodyweight_knee_moves_are_not_machine_wording() {
        listOf("Sissy Squat", "Nordic Curl").forEach { name ->
            val e = samples.first { it.name == name }
            val c = ExerciseCoach.coach(e)
            val text = (c.description + " " + c.steps.joinToString(" ")).lowercase()
            assertTrue("$name must not use 'lift the pad'", !text.contains("lift the pad"))
            assertTrue("$name must not use 'sitting down'", !c.description.lowercase().startsWith("sitting down"))
        }
    }

    @Test fun conditioning_setup_matches_equipment() {
        val ropes = samples.first { it.name == "Battle Ropes" }
        val setup = ExerciseCoach.coach(ropes).steps.first().lowercase()
        assertTrue("battle ropes (cable) must not say 'step onto the machine'", !setup.contains("step onto the machine"))
    }
}
