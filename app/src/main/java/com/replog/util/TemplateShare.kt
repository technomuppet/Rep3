package com.replog.util

import com.google.gson.GsonBuilder
import com.replog.data.model.TemplateWithExercises

/**
 * Portable, account-free workout template sharing.
 *
 * A shared template references exercises by NAME (plus enough metadata to
 * recreate a custom exercise if the recipient does not have it), so a template
 * exported on one device imports cleanly on another. Files use the
 * ".rpltemplate" extension and are plain JSON, so they can be shared over any
 * channel (chat, email, GitHub, cloud drive) with no server involved.
 */
data class SharedTemplate(
    val schemaVersion: Int = 1,
    val type: String = "replog.template",
    val name: String,
    val exportedAt: Long = System.currentTimeMillis(),
    val exercises: List<SharedTemplateExercise> = emptyList()
)

data class SharedTemplateExercise(
    val exerciseName: String,
    val sets: Int = 3,
    val reps: Int = 8,
    val targetWeight: Double? = null,
    val orderIndex: Int = 0,
    // Metadata so an exercise the recipient lacks can be recreated as custom.
    val category: String = "Imported",
    val equipment: String = "Other",
    val muscles: String = "",
    val primaryMuscles: String = "",
    val secondaryMuscles: String = "",
    val movementPattern: String = "",
    val difficulty: String = "Intermediate"
)

object TemplateShare {

    const val FILE_EXTENSION = "rpltemplate"
    const val MIME_TYPE = "application/json"

    private val gson = GsonBuilder().setPrettyPrinting().create()

    fun encode(template: TemplateWithExercises): String {
        val shared = SharedTemplate(
            name = template.template.name,
            exercises = template.exercises
                .sortedBy { it.templateExercise.orderIndex }
                .mapIndexed { index, te ->
                    SharedTemplateExercise(
                        exerciseName = te.exercise.name,
                        sets = te.templateExercise.defaultSets,
                        reps = te.templateExercise.targetReps,
                        targetWeight = te.templateExercise.targetWeight,
                        orderIndex = index,
                        category = te.exercise.category,
                        equipment = te.exercise.equipment,
                        muscles = te.exercise.muscles,
                        primaryMuscles = te.exercise.primaryMuscles,
                        secondaryMuscles = te.exercise.secondaryMuscles,
                        movementPattern = te.exercise.movementPattern,
                        difficulty = te.exercise.difficulty
                    )
                }
        )
        return gson.toJson(shared)
    }

    /** Parse a shared-template JSON string. Throws if it is not a valid template. */
    fun decode(json: String): SharedTemplate {
        val parsed = gson.fromJson(json, SharedTemplate::class.java)
            ?: throw IllegalArgumentException("Not a RepLog template file")
        if (parsed.name.isNullOrBlank() || parsed.exercises.isNullOrEmpty()) {
            throw IllegalArgumentException("Template file is empty or malformed")
        }
        return parsed
    }

    /** A safe, OS-friendly file name for a shared template. */
    fun fileNameFor(templateName: String): String {
        val safe = templateName.trim()
            .replace(Regex("[^A-Za-z0-9 _-]"), "")
            .replace(Regex("\\s+"), "_")
            .ifBlank { "template" }
        return "$safe.$FILE_EXTENSION"
    }
}
