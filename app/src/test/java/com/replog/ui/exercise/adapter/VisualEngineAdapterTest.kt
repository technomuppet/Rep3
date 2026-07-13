package com.replog.ui.exercise.adapter

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.replog.data.model.Exercise
import com.replog.domain.visual.spec.EquipmentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class VisualEngineAdapterTest {

    @Test
    fun testResolveAnatomyPrefersVectorEngine() {
        val ex = Exercise(
            id = 1,
            name = "Barbell Bench Press",
            category = "Chest",
            equipment = "Barbell",
            muscles = "Pectorals",
            primaryMuscles = "Chest",
            secondaryMuscles = "Triceps"
        )
        val mode = VisualEngineAdapter.resolveAnatomy(ex)
        assertTrue("Standard exercise must resolve to VectorEngine", mode is VisualEngineAdapter.AnatomyRenderMode.VectorEngine)
        val vectorMode = mode as VisualEngineAdapter.AnatomyRenderMode.VectorEngine
        assertTrue(vectorMode.spec.primaryMuscles.contains("Chest"))
        assertTrue(vectorMode.spec.secondaryMuscles.contains("Triceps"))
    }

    @Test
    fun testResolveAnatomyFallsBackToLegacyWhenEmpty() {
        val ex = Exercise(
            id = 2,
            name = "Empty Muscle Exercise",
            category = "Other",
            equipment = "None",
            muscles = "",
            primaryMuscles = "",
            secondaryMuscles = ""
        )
        val mode = VisualEngineAdapter.resolveAnatomy(ex)
        assertTrue("Exercise with completely empty muscles must fall back to LegacyBoxes", mode is VisualEngineAdapter.AnatomyRenderMode.LegacyBoxes)
    }

    @Test
    fun testResolveAnimationPrefersSkeletalEngine() {
        val ex = Exercise(
            id = 3,
            name = "Lat Pulldown",
            category = "Back",
            equipment = "Cable",
            movementPattern = "Pull • Vertical Pull"
        )
        val mode = VisualEngineAdapter.resolveAnimation(ex)
        assertTrue("Standard exercise must resolve to SkeletalEngine", mode is VisualEngineAdapter.AnimationRenderMode.SkeletalEngine)
        val skeletalMode = mode as VisualEngineAdapter.AnimationRenderMode.SkeletalEngine
        assertEquals("LAT_PULLDOWN", skeletalMode.spec.movementFamily.familyId)
        assertEquals(EquipmentType.CABLE, skeletalMode.spec.equipment.type)
        assertNotNull(skeletalMode.timeline)
    }

    @Test
    fun testAutomatedCatalogAuditOverBundledExercisesJson() {
        val filePaths = listOf("app/src/main/assets/exercises.json", "src/main/assets/exercises.json")
        val assetFile = filePaths.map { File(it) }.firstOrNull { it.exists() }
        if (assetFile != null && assetFile.exists()) {
            val json = assetFile.readText()
            val type = object : TypeToken<List<ExerciseStub>>() {}.type
            val rawData: List<ExerciseStub> = Gson().fromJson(json, type)
            val exercises = rawData.mapIndexed { idx, d ->
                Exercise(
                    id = idx + 1,
                    name = d.name,
                    category = d.category,
                    equipment = d.equipment,
                    type = d.type,
                    muscles = d.muscles.joinToString(", "),
                    primaryMuscles = d.primaryMuscles.joinToString(", "),
                    secondaryMuscles = d.secondaryMuscles.joinToString(", "),
                    movementPattern = d.movementPattern,
                    difficulty = d.difficulty
                )
            }

            var vectorCount = 0
            var skeletalCount = 0

            for (ex in exercises) {
                if (VisualEngineAdapter.resolveAnatomy(ex) is VisualEngineAdapter.AnatomyRenderMode.VectorEngine) {
                    vectorCount++
                }
                if (VisualEngineAdapter.resolveAnimation(ex) is VisualEngineAdapter.AnimationRenderMode.SkeletalEngine) {
                    skeletalCount++
                }
            }

            assertTrue("At least 95% of catalog exercises should adapt to VectorEngine anatomy", vectorCount >= exercises.size * 0.95)
            assertTrue("At least 95% of catalog exercises should adapt to SkeletalEngine animations", skeletalCount >= exercises.size * 0.95)
        }
    }

    private data class ExerciseStub(
        val name: String = "",
        val category: String = "",
        val equipment: String = "",
        val type: String = "",
        val muscles: List<String> = emptyList(),
        val primaryMuscles: List<String> = emptyList(),
        val secondaryMuscles: List<String> = emptyList(),
        val movementPattern: String = "",
        val difficulty: String = ""
    )
}
