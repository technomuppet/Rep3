package com.replog.domain.visual.anatomy

enum class BodySide {
    FRONT,
    BACK
}

/**
 * Independent anatomical vector regions representing every major muscle group
 * required for precise front and back body highlighting.
 */
enum class MuscleRegion(val id: String, val displayName: String, val side: BodySide) {
    // Front Body Regions
    CHEST("CHEST", "Chest / Pectoralis Major", BodySide.FRONT),
    UPPER_CHEST("UPPER_CHEST", "Upper Chest / Clavicular Pectoral", BodySide.FRONT),
    ANTERIOR_DELTOID("ANTERIOR_DELTOID", "Anterior Deltoid / Front Shoulder", BodySide.FRONT),
    LATERAL_DELTOID("LATERAL_DELTOID", "Lateral Deltoid / Side Shoulder", BodySide.FRONT),
    BICEPS("BICEPS", "Biceps Brachii", BodySide.FRONT),
    FOREARMS_ANTERIOR("FOREARMS_ANTERIOR", "Anterior Forearm Flexors", BodySide.FRONT),
    RECTUS_ABDOMINIS("RECTUS_ABDOMINIS", "Rectus Abdominis / Abs", BodySide.FRONT),
    OBLIQUES("OBLIQUES", "External & Internal Obliques", BodySide.FRONT),
    HIP_FLEXORS("HIP_FLEXORS", "Iliopsoas / Hip Flexors", BodySide.FRONT),
    QUADRICEPS("QUADRICEPS", "Quadriceps Femoris", BodySide.FRONT),
    ADDUCTORS("ADDUCTORS", "Thigh Adductors / Inner Thighs", BodySide.FRONT),
    ABDUCTORS("ABDUCTORS", "Thigh Abductors / Outer Thighs", BodySide.FRONT),
    TIBIALIS_ANTERIOR("TIBIALIS_ANTERIOR", "Tibialis Anterior / Shin", BodySide.FRONT),

    // Back Body Regions
    POSTERIOR_DELTOID("POSTERIOR_DELTOID", "Posterior Deltoid / Rear Shoulder", BodySide.BACK),
    TRICEPS("TRICEPS", "Triceps Brachii", BodySide.BACK),
    FOREARMS_POSTERIOR("FOREARMS_POSTERIOR", "Posterior Forearm Extensors", BodySide.BACK),
    UPPER_TRAPEZIUS("UPPER_TRAPEZIUS", "Upper Trapezius", BodySide.BACK),
    MIDDLE_TRAPEZIUS("MIDDLE_TRAPEZIUS", "Middle Trapezius", BodySide.BACK),
    LOWER_TRAPEZIUS("LOWER_TRAPEZIUS", "Lower Trapezius", BodySide.BACK),
    LATISSIMUS_DORSI("LATISSIMUS_DORSI", "Latissimus Dorsi / Lats", BodySide.BACK),
    RHOMBOIDS("RHOMBOIDS", "Rhomboids Major & Minor", BodySide.BACK),
    TERES_MAJOR("TERES_MAJOR", "Teres Major", BodySide.BACK),
    SPINAL_ERECTORS("SPINAL_ERECTORS", "Erector Spinae / Lower Back", BodySide.BACK),
    GLUTE_MAXIMUS("GLUTE_MAXIMUS", "Gluteus Maximus", BodySide.BACK),
    GLUTE_MEDIUS("GLUTE_MEDIUS", "Gluteus Medius / Upper Hip", BodySide.BACK),
    HAMSTRINGS("HAMSTRINGS", "Hamstrings", BodySide.BACK),
    CALVES("CALVES", "Gastrocnemius & Soleus / Calves", BodySide.BACK);

    companion object {
        val FRONT_REGIONS: List<MuscleRegion> = entries.filter { it.side == BodySide.FRONT }
        val BACK_REGIONS: List<MuscleRegion> = entries.filter { it.side == BodySide.BACK }
    }
}
