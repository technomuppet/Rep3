package com.replog.domain.visual.equipment

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.replog.domain.visual.animation.SolvedSkeleton
import com.replog.domain.visual.spec.EquipmentSpec

/**
 * Modular equipment rendering interface.
 * Each equipment type must have its own renderer — no generic placeholders.
 */
interface EquipmentRenderer {
    fun draw(
        drawScope: DrawScope,
        skeleton: SolvedSkeleton,
        toScreen: (Offset) -> Offset,
        equipmentSpec: EquipmentSpec,
        implementColor: Color,
        secondaryColor: Color,
        referenceSize: Float
    )

    /**
     * Validates attachment: returns true if equipment is correctly attached to body.
     */
    fun validateAttachment(
        skeleton: SolvedSkeleton,
        toScreen: (Offset) -> Offset
    ): ValidationResult
}

data class ValidationResult(
    val isAttached: Boolean,
    val handsAttached: Boolean,
    val feetPlanted: Boolean,
    val floating: Boolean,
    val message: String = ""
)

enum class EquipmentLayer {
    BEHIND_BODY,
    IN_FRONT_OF_BODY,
    SUPPORT_SURFACE
}
