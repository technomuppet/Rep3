package com.replog.domain.visual.anatomy

/**
 * Canonical muscle vocabulary for the visual anatomy system.
 *
 * Every visual component should use these constants instead of raw strings.
 *
 * This object is the single source of truth for:
 * - ExercisePresentationFactory
 * - AnatomySpec
 * - AnatomicalPreviews
 * - MuscleMap
 * - AnatomyDiagnostics
 * - MuscleActivationEngine
 *
 * Rules:
 * - No underscores in values.
 * - Use title case.
 * - One canonical spelling only.
 * - Never duplicate synonyms.
 */
object Muscles {

    // -------------------------------------------------------------------------
    // Chest
    // -------------------------------------------------------------------------

    const val CHEST = "Chest"
    const val UPPER_CHEST = "Upper Chest"

    // -------------------------------------------------------------------------
    // Back
    // -------------------------------------------------------------------------

    const val LATISSIMUS_DORSI = "Latissimus Dorsi"
    const val TERES_MAJOR = "Teres Major"
    const val RHOMBOIDS = "Rhomboids"

    const val UPPER_TRAPEZIUS = "Upper Trapezius"
    const val MIDDLE_TRAPEZIUS = "Middle Trapezius"

    const val SPINAL_ERECTORS = "Spinal Erectors"

    // -------------------------------------------------------------------------
    // Shoulders
    // -------------------------------------------------------------------------

    const val ANTERIOR_DELTOID = "Anterior Deltoid"
    const val LATERAL_DELTOID = "Lateral Deltoid"
    const val POSTERIOR_DELTOID = "Posterior Deltoid"

    // -------------------------------------------------------------------------
    // Arms
    // -------------------------------------------------------------------------

    const val BICEPS = "Biceps"
    const val TRICEPS = "Triceps"
    const val FOREARMS = "Forearms"

    // -------------------------------------------------------------------------
    // Core
    // -------------------------------------------------------------------------

    const val OBLIQUES = "Obliques"

    // -------------------------------------------------------------------------
    // Lower Body
    // -------------------------------------------------------------------------

    const val GLUTE_MAXIMUS = "Glute Maximus"

    const val QUADRICEPS = "Quadriceps"

    const val HAMSTRINGS = "Hamstrings"

    const val ADDUCTORS = "Adductors"

    const val RECTUS_ABDOMINIS = "Rectus Abdominis"

    const val SERRATUS_ANTERIOR = "Serratus Anterior"


    const val HIP_FLEXORS = "Hip Flexors"

    const val TIBIALIS_ANTERIOR = "Tibialis Anterior"

    const val CALVES = "Calves"
}
