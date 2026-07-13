package com.replog.domain.visual.equipment

import com.replog.domain.visual.spec.EquipmentSpec
import com.replog.domain.visual.spec.EquipmentType
import com.replog.domain.visual.spec.SupportType

/**
 * Modular equipment rendering system with independent renderers per equipment type.
 * No generic placeholders — each type has dedicated renderer.
 */
object EquipmentEngine {

    /**
     * Resolves the primary equipment renderer for a given spec.
     */
    fun resolvePrimary(spec: EquipmentSpec): EquipmentRenderer {
        return when (spec.type) {
            EquipmentType.BARBELL -> {
                // Choose Olympic vs Standard based on name
                if (spec.implementType.contains("Olympic", ignoreCase = true)) OlympicBarbellRenderer
                else StandardBarbellRenderer
            }
            EquipmentType.EZ_BAR -> EZBarRenderer
            EquipmentType.DUMBBELL -> DumbbellsRenderer
            EquipmentType.KETTLEBELL -> KettlebellsRenderer
            EquipmentType.CABLE -> {
                val name = spec.implementType.lowercase()
                when {
                    name.contains("rope") -> RopeAttachmentRenderer
                    name.contains("straight") || name.contains("bar") -> StraightCableBarRenderer
                    else -> CableHandleRenderer
                }
            }
            EquipmentType.SMITH_MACHINE -> SmithMachineRenderer
            EquipmentType.MACHINE -> {
                val name = spec.implementType.lowercase()
                when {
                    name.contains("chest press") -> ChestPressMachineRenderer
                    name.contains("shoulder press") -> ShoulderPressMachineRenderer
                    name.contains("leg press") -> LegPressMachineRenderer
                    name.contains("power rack") -> PowerRackRenderer
                    name.contains("squat rack") -> SquatRackRenderer
                    name.contains("power") -> PowerRackRenderer
                    name.contains("rack") -> SquatRackRenderer
                    else -> ChestPressMachineRenderer // default machine
                }
            }
            EquipmentType.BODYWEIGHT -> {
                // Bodyweight may still need pull-up bar, dip bars based on movement family
                // Resolver will pass supportType HANGING for pull-up
                when (spec.supportType) {
                    SupportType.HANGING -> PullUpBarRenderer
                    SupportType.PRONE_LYING, SupportType.SUPINE_LYING -> FlatBenchRenderer // no equipment, but bench is support
                    else -> FloorRenderer // no primary equipment
                }
            }
            EquipmentType.PLATE -> DumbbellsRenderer // plate loaded as dumbbell-like
            EquipmentType.BAND -> CableHandleRenderer // band similar to cable for rendering
            EquipmentType.OTHER -> {
                when (spec.supportType) {
                    SupportType.HANGING -> PullUpBarRenderer
                    else -> FloorRenderer
                }
            }
        }
    }

    /**
     * Resolves support surface renderer (bench, box, floor, rack).
     */
    fun resolveSupport(spec: EquipmentSpec): EquipmentRenderer? {
        return when (spec.supportType) {
            SupportType.SUPINE_LYING -> FlatBenchRenderer
            SupportType.SEATED_INCLINE -> InclineBenchRenderer
            SupportType.SEATED_DECLINE -> DeclineBenchRenderer
            SupportType.SEATED_FLAT -> {
                // Could be machine seat or flat bench — use flat bench as fallback
                FlatBenchRenderer
            }
            SupportType.STANDING -> FloorRenderer
            SupportType.HANGING -> PullUpBarRenderer
            SupportType.PRONE_LYING -> FlatBenchRenderer
            SupportType.CHEST_SUPPORTED -> InclineBenchRenderer // chest supported on incline
            SupportType.NONE -> null
            else -> FloorRenderer
        }
    }

    /**
     * Special handling for explicit equipment names that require dedicated bench renderers even if supportType generic.
     */
    fun resolveBenchFromAngle(benchAngle: Float): EquipmentRenderer {
        return when {
            benchAngle > 20f -> InclineBenchRenderer
            benchAngle < -10f -> DeclineBenchRenderer
            benchAngle != 0f -> AdjustableBenchRenderer
            else -> FlatBenchRenderer
        }
    }

    /**
     * Returns layer ordering for equipment.
     */
    fun getEquipmentLayer(renderer: EquipmentRenderer): EquipmentLayer {
        return when (renderer) {
            is PullUpBarRenderer, is PowerRackRenderer, is SquatRackRenderer, is SmithMachineRenderer -> EquipmentLayer.BEHIND_BODY
            is FlatBenchRenderer, is InclineBenchRenderer, is DeclineBenchRenderer, is AdjustableBenchRenderer, is PlyoBoxRenderer, is FloorRenderer -> EquipmentLayer.SUPPORT_SURFACE
            else -> EquipmentLayer.IN_FRONT_OF_BODY
        }
    }

    /**
     * List all renderers for validation and documentation.
     */
    fun getAllRenderers(): List<EquipmentRenderer> = listOf(
        OlympicBarbellRenderer,
        StandardBarbellRenderer,
        EZBarRenderer,
        DumbbellsRenderer,
        KettlebellsRenderer,
        CableHandleRenderer,
        StraightCableBarRenderer,
        RopeAttachmentRenderer,
        PullUpBarRenderer,
        DipBarsRenderer,
        SmithMachineRenderer,
        ChestPressMachineRenderer,
        ShoulderPressMachineRenderer,
        LegPressMachineRenderer,
        PowerRackRenderer,
        SquatRackRenderer,
        FlatBenchRenderer,
        InclineBenchRenderer,
        DeclineBenchRenderer,
        AdjustableBenchRenderer,
        PlyoBoxRenderer,
        FloorRenderer
    )
}
