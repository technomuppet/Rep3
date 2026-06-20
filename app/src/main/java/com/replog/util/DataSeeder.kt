package com.replog.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.replog.data.model.Exercise
import com.replog.data.model.TemplateExercise
import com.replog.data.model.WorkoutTemplate
import com.replog.data.repository.ExerciseRepository
import com.replog.data.repository.WorkoutRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataSeeder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val exercises: ExerciseRepository,
    private val workouts: WorkoutRepository,
    private val prefs: PreferencesManager
) {
    suspend fun seedDataIfFirstLaunch() {
        val count = exercises.countExercises()
        if (!prefs.isFirstLaunch.first() && count > 0) {
            upgradeExerciseMetadataIfNeeded()
            return
        }
        seedExercises()
        seedTemplates()
        prefs.setFirstLaunchComplete()
    }

    private suspend fun seedExercises() {
        if (exercises.countExercises() > 0) return
        exercises.insertExercises(loadExerciseData().map { it.toExercise() })
    }

    private suspend fun upgradeExerciseMetadataIfNeeded() {
        loadExerciseData().forEach { data ->
            val existing = exercises.getExerciseByName(data.name) ?: return@forEach
            if (existing.movementPattern.isBlank() || existing.primaryMuscles.isBlank() || existing.mediaAsset.isBlank()) {
                val enriched = data.toExercise().copy(
                    id = existing.id,
                    isCustom = existing.isCustom
                )
                exercises.updateExercise(enriched)
            }
        }
    }

    private fun loadExerciseData(): List<ExerciseData> {
        val json = context.assets.open("exercises.json").bufferedReader().use { it.readText() }
        val type = object : TypeToken<List<ExerciseData>>() {}.type
        return Gson().fromJson(json, type)
    }

    private fun ExerciseData.toExercise(): Exercise = Exercise(
        name = name,
        category = category,
        equipment = equipment,
        type = type,
        muscles = muscles.joinToString(", "),
        primaryMuscles = primaryMuscles.ifEmpty { muscles.take(1) }.joinToString(", "),
        secondaryMuscles = secondaryMuscles.ifEmpty { muscles.drop(1) }.joinToString(", "),
        movementPattern = movementPattern,
        difficulty = difficulty,
        mediaAsset = mediaAsset
    )

    private suspend fun seedTemplates() {
        val templates = mapOf(
            "Full Body A" to listOf("Barbell Back Squat", "Barbell Bench Press", "Barbell Bent Over Row"),
            "Full Body B" to listOf("Barbell Deadlift", "Barbell Overhead Press", "Pull Ups"),
            "Push Day" to listOf("Barbell Bench Press", "Barbell Overhead Press", "Tricep Dips", "Cable Chest Fly"),
            "Pull Day" to listOf("Barbell Deadlift", "Pull Ups", "Lat Pulldown", "Face Pulls", "Dumbbell Bicep Curl"),
            "Leg Day" to listOf("Barbell Back Squat", "Barbell Romanian Deadlift", "Leg Press", "Calf Raises")
        )
        templates.forEach { (name, names) ->
            val templateId = workouts.insertTemplate(WorkoutTemplate(name = name, isBuiltIn = true)).toInt()
            names.forEachIndexed { index, exerciseName ->
                exercises.getExerciseByName(exerciseName)?.let { ex ->
                    workouts.insertTemplateExercise(TemplateExercise(templateId = templateId, exerciseId = ex.id, defaultSets = 3, orderIndex = index))
                }
            }
        }
    }

    data class ExerciseData(
        val name: String,
        val category: String,
        val equipment: String,
        val type: String = "Strength",
        val muscles: List<String> = emptyList(),
        val primaryMuscles: List<String> = emptyList(),
        val secondaryMuscles: List<String> = emptyList(),
        val movementPattern: String = "",
        val difficulty: String = "Intermediate",
        val mediaAsset: String = ""
    )
}
