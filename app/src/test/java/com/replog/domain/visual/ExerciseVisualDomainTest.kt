package com.replog.domain.visual

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.replog.data.model.Exercise
import com.replog.domain.visual.registry.MovementCategory
import com.replog.domain.visual.registry.MovementFamily
import com.replog.domain.visual.registry.MovementRegistry
import com.replog.domain.visual.resolver.ExerciseVisualResolver
import com.replog.domain.visual.spec.BodyOrientation
import com.replog.domain.visual.spec.EquipmentType
import com.replog.domain.visual.spec.GripType
import com.replog.domain.visual.spec.RangeOfMotion
import com.replog.domain.visual.spec.StanceType
import com.replog.domain.visual.spec.SupportType
import com.replog.domain.visual.validation.ExerciseVisualValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ExerciseVisualDomainTest {

    @Test
    fun testMovementRegistryContainsStandardFamilies() {
        val families = MovementRegistry.getAllFamilies()
        assertTrue("Registry must contain at least 33 movement families", families.size >= 33)

        val horizontalPush = MovementRegistry.getById("HORIZONTAL_PUSH")
        assertEquals("Horizontal Push", horizontalPush.displayName)
        assertEquals(MovementCategory.PUSH, horizontalPush.category)
        assertTrue(MovementRegistry.isRegistered("HORIZONTAL_PUSH"))
        assertFalse(MovementRegistry.isRegistered("GENERIC_UNMAPPED"))
    }

    @Test
    fun testResolverDerivesVisualSpecCorrectly() {
        val ex = Exercise(
            id = 101,
            name = "Incline Barbell Bench Press",
            category = "Chest",
            equipment = "Barbell",
            muscles = "Pectorals, Anterior Deltoid, Triceps",
            primaryMuscles = "Upper Chest, Front Delts",
            secondaryMuscles = "Triceps",
            movementPattern = "Push • Incline Press",
            difficulty = "Intermediate"
        )

        val spec = ExerciseVisualResolver.resolve(ex)
        assertEquals(101, spec.exerciseId)
        assertEquals("Incline Barbell Bench Press", spec.exerciseName)
        assertEquals("INCLINE_PUSH", spec.movementFamily.familyId)
        assertEquals(EquipmentType.BARBELL, spec.equipment.type)
        assertEquals(SupportType.SEATED_INCLINE, spec.equipment.supportType)
        assertEquals(30.0f, spec.equipment.benchAngle, 0.01f)
        assertEquals(BodyOrientation.SEATED, spec.bodyOrientation)
        assertEquals(GripType.STANDARD_PRONATED, spec.gripType)
        assertEquals(RangeOfMotion.FULL, spec.rangeOfMotion)
        assertTrue(spec.anatomy.primaryMuscles.contains("Upper Chest"))
        assertTrue(spec.anatomy.primaryMuscles.contains("Front Delts"))
        assertTrue(spec.anatomy.secondaryMuscles.contains("Triceps"))
    }

    @Test
    fun testResolverDetectsGripAndStanceVariations() {
        val sumoEx = Exercise(
            id = 102,
            name = "Wide Grip Sumo Deadlift",
            category = "Legs",
            equipment = "Barbell",
            movementPattern = "Hinge • Deadlift"
        )
        val spec = ExerciseVisualResolver.resolve(sumoEx)
        assertEquals("DEADLIFT", spec.movementFamily.familyId)
        assertEquals(GripType.WIDE, spec.gripType)
        assertEquals(StanceType.WIDE_SUMO, spec.stance)
        assertEquals(1.3f, spec.movementFamily.parameters["gripWidthFactor"] ?: 0f, 0.01f)
        assertEquals(1.4f, spec.movementFamily.parameters["stanceWidthFactor"] ?: 0f, 0.01f)
    }

    @Test
    fun testUnknownEquipmentFallback() {
        val ex = Exercise(
            id = 103,
            name = "Alien Grav-Sled Squat",
            category = "Legs",
            equipment = "Grav-Sled",
            movementPattern = "Legs • Squat"
        )
        val spec = ExerciseVisualResolver.resolve(ex)
        assertEquals(EquipmentType.OTHER, spec.equipment.type)
        assertEquals("Grav-Sled", spec.equipment.implementType)
        assertEquals("SQUAT", spec.movementFamily.familyId)
    }

    @Test
    fun testUnknownMovementPatternFallback() {
        val ex = Exercise(
            id = 104,
            name = "Mysterious Neuro-Flex",
            category = "Unclassified",
            equipment = "None",
            movementPattern = "Weird Pattern"
        )
        val spec = ExerciseVisualResolver.resolve(ex)
        assertEquals("GENERIC_UNMAPPED", spec.movementFamily.familyId)
        assertEquals(EquipmentType.BODYWEIGHT, spec.equipment.type)
    }

    @Test
    fun testMuscleMappingAnatomySpec() {
        val ex = Exercise(
            name = "Dumbbell Lateral Raise",
            category = "Shoulders",
            equipment = "Dumbbell",
            muscles = "Lateral Deltoid",
            primaryMuscles = "Side Delts",
            secondaryMuscles = "Traps, Side Delts"
        )
        val spec = ExerciseVisualResolver.resolve(ex)
        assertEquals(setOf("Side Delts"), spec.anatomy.primaryMuscles)
        assertEquals(setOf("Traps"), spec.anatomy.secondaryMuscles)
    }

    @Test
    fun testValidatorAndCoverageReporting() {
        val list = listOf(
            Exercise(id = 1, name = "Barbell Bench Press", category = "Chest", equipment = "Barbell", movementPattern = "Push • Horizontal Press", primaryMuscles = "Chest"),
            Exercise(id = 2, name = "Lat Pulldown", category = "Back", equipment = "Cable", movementPattern = "Pull • Vertical Pull", primaryMuscles = "Lats"),
            Exercise(id = 3, name = "Weird Machine Exercise", category = "Unknown", equipment = "Alien Device", movementPattern = "Unknown", primaryMuscles = "")
        )

        val report = ExerciseVisualValidator.validateCatalog(list)
        assertEquals(3, report.totalExercises)
        assertEquals(2, report.successfullyResolvedCount)
        assertEquals(1, report.fallbackUsedCount)
        assertEquals(1, report.unknownEquipmentCount)
        assertEquals(1, report.unknownMovementPatternCount)
        assertEquals(1, report.missingMusclesCount)
        assertEquals(66.66f, report.coveragePercentage, 0.1f)
    }

    @Test
    fun testAutomatedValidationOfBundledExercisesJson() {
        val filePaths = listOf(
            "app/src/main/assets/exercises.json",
            "src/main/assets/exercises.json"
        )
        val assetFile = filePaths.map { File(it) }.firstOrNull { it.exists() }
        if (assetFile != null && assetFile.exists()) {
            val json = assetFile.readText()
            val type = object : TypeToken<List<ExerciseDataStub>>() {}.type
            val rawData: List<ExerciseDataStub> = Gson().fromJson(json, type)
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

            val report = ExerciseVisualValidator.validateCatalog(exercises)
            assertNotNull(report)
            assertTrue("Total exercises tested should match JSON count", report.totalExercises > 200)
            assertTrue("Resolution coverage percentage should be very high (> 85%)", report.coveragePercentage > 85.0f)
        }
    }

    private data class ExerciseDataStub(
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
