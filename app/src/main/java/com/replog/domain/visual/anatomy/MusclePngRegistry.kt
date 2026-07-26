package com.replog.domain.visual.anatomy

import androidx.annotation.DrawableRes
import com.replog.R

/**
 * Single source of truth for the layered PNG anatomy library.
 *
 * This registry mirrors the existing muscle-to-layer mapping, aliases and draw
 * ordering from the SVG registry, but references packaged drawable-nodpi PNG
 * resources instead of asset paths. It is deliberately data-only: it does not
 * read resources or draw UI.
 */
object MusclePngRegistry {

    const val CANVAS_WIDTH: Float = 768f
    const val CANVAS_HEIGHT: Float = 1536f

    private object DrawableName {
        const val FRONT_BASE = "anatomy_front_base"
        const val BACK_BASE = "anatomy_back_base"
        const val FRONT_UPPER_CHEST = "upper_chest"
        const val FRONT_CHEST = "chest"
        const val FRONT_DELTS = "front_delts"
        const val FRONT_SIDE_DELTS = "side_delts"
        const val FRONT_BICEPS = "biceps"
        const val FRONT_FOREARMS = "forearms"
        const val FRONT_ABS = "abs"
        const val FRONT_OBLIQUES = "obliques"
        const val FRONT_SERRATUS = "serratus"
        const val FRONT_HIP_FLEXORS = "hip_flexors"
        const val FRONT_QUADS = "quads"
        const val FRONT_ADDUCTORS = "adductors"
        const val FRONT_ABDUCTORS = "abductors"
        const val FRONT_TIBIALIS = "tibialis"
        const val FRONT_CALVES = "calves"
        const val BACK_TRAPS_UPPER = "traps_upper"
        const val BACK_TRAPS_MIDDLE = "traps_middle"
        const val BACK_REAR_DELTS = "rear_delts"
        const val BACK_LATS = "lats"
        const val BACK_RHOMBOIDS = "rhomboids"
        const val BACK_TERES_MAJOR = "teres_major"
        const val BACK_TRICEPS = "triceps"
        const val BACK_ERECTOR_SPINAE = "erector_spinae"
        const val BACK_GLUTES = "glutes"
        const val BACK_ABDUCTORS = "abductors_back"
        const val BACK_HAMSTRINGS = "hamstrings"
        const val BACK_FOREARMS = "forearms_back"
        const val BACK_CALVES = "calves_back"
    }

    private object DrawableId {
        @DrawableRes val FRONT_BASE: Int = R.drawable.anatomy_front_base
        @DrawableRes val BACK_BASE: Int = R.drawable.anatomy_back_base
        @DrawableRes val FRONT_UPPER_CHEST: Int = R.drawable.upper_chest
        @DrawableRes val FRONT_CHEST: Int = R.drawable.chest
        @DrawableRes val FRONT_DELTS: Int = R.drawable.front_delts
        @DrawableRes val FRONT_SIDE_DELTS: Int = R.drawable.side_delts
        @DrawableRes val FRONT_BICEPS: Int = R.drawable.biceps
        @DrawableRes val FRONT_FOREARMS: Int = R.drawable.forearms
        @DrawableRes val FRONT_ABS: Int = R.drawable.abs
        @DrawableRes val FRONT_OBLIQUES: Int = R.drawable.obliques
        @DrawableRes val FRONT_SERRATUS: Int = R.drawable.serratus
        @DrawableRes val FRONT_HIP_FLEXORS: Int = R.drawable.hip_flexors
        @DrawableRes val FRONT_QUADS: Int = R.drawable.quads
        @DrawableRes val FRONT_ADDUCTORS: Int = R.drawable.adductors
        @DrawableRes val FRONT_ABDUCTORS: Int = R.drawable.abductors
        @DrawableRes val FRONT_TIBIALIS: Int = R.drawable.tibialis
        @DrawableRes val FRONT_CALVES: Int = R.drawable.calves
        @DrawableRes val BACK_TRAPS_UPPER: Int = R.drawable.traps_upper
        @DrawableRes val BACK_TRAPS_MIDDLE: Int = R.drawable.traps_middle
        @DrawableRes val BACK_REAR_DELTS: Int = R.drawable.rear_delts
        @DrawableRes val BACK_LATS: Int = R.drawable.lats
        @DrawableRes val BACK_RHOMBOIDS: Int = R.drawable.rhomboids
        @DrawableRes val BACK_TERES_MAJOR: Int = R.drawable.teres_major
        @DrawableRes val BACK_TRICEPS: Int = R.drawable.triceps
        @DrawableRes val BACK_ERECTOR_SPINAE: Int = R.drawable.erector_spinae
        @DrawableRes val BACK_GLUTES: Int = R.drawable.glutes
        @DrawableRes val BACK_ABDUCTORS: Int = R.drawable.abductors_back
        @DrawableRes val BACK_HAMSTRINGS: Int = R.drawable.hamstrings
        @DrawableRes val BACK_FOREARMS: Int = R.drawable.forearms_back
        @DrawableRes val BACK_CALVES: Int = R.drawable.calves_back
    }

    /** Every drawable-nodpi PNG that belongs to the layered anatomy library. */
    val expectedDrawableResourceNames: Set<String> = setOf(
        DrawableName.FRONT_BASE,
        DrawableName.BACK_BASE,
        DrawableName.FRONT_UPPER_CHEST,
        DrawableName.FRONT_CHEST,
        DrawableName.FRONT_DELTS,
        DrawableName.FRONT_SIDE_DELTS,
        DrawableName.FRONT_BICEPS,
        DrawableName.FRONT_FOREARMS,
        DrawableName.FRONT_ABS,
        DrawableName.FRONT_OBLIQUES,
        DrawableName.FRONT_SERRATUS,
        DrawableName.FRONT_HIP_FLEXORS,
        DrawableName.FRONT_QUADS,
        DrawableName.FRONT_ADDUCTORS,
        DrawableName.FRONT_ABDUCTORS,
        DrawableName.FRONT_TIBIALIS,
        DrawableName.FRONT_CALVES,
        DrawableName.BACK_TRAPS_UPPER,
        DrawableName.BACK_TRAPS_MIDDLE,
        DrawableName.BACK_REAR_DELTS,
        DrawableName.BACK_LATS,
        DrawableName.BACK_RHOMBOIDS,
        DrawableName.BACK_TERES_MAJOR,
        DrawableName.BACK_TRICEPS,
        DrawableName.BACK_ERECTOR_SPINAE,
        DrawableName.BACK_GLUTES,
        DrawableName.BACK_ABDUCTORS,
        DrawableName.BACK_HAMSTRINGS,
        DrawableName.BACK_FOREARMS,
        DrawableName.BACK_CALVES
    )

    val frontBase: PngAnatomyLayerAsset = PngAnatomyLayerAsset(
        id = "front_base",
        side = BodySide.FRONT,
        drawableId = DrawableId.FRONT_BASE,
        drawableName = DrawableName.FRONT_BASE,
        role = AnatomyLayerRole.BASE,
        order = 0
    )

    val backBase: PngAnatomyLayerAsset = PngAnatomyLayerAsset(
        id = "back_base",
        side = BodySide.BACK,
        drawableId = DrawableId.BACK_BASE,
        drawableName = DrawableName.BACK_BASE,
        role = AnatomyLayerRole.BASE,
        order = 0
    )

    private data class Entry(
        val region: MuscleRegion,
        val side: BodySide,
        @DrawableRes val drawableId: Int,
        val drawableName: String,
        val order: Int,
        val type: MuscleAssetMappingType = MuscleAssetMappingType.DIRECT,
        val mappedFrom: MuscleRegion? = null
    )

    /**
     * Ordered by anatomical depth and visual readability. The order is stable so
     * identical activation sets always produce identical layer stacks.
     */
    private val entries: List<Entry> = listOf(
        // Front torso
        direct(MuscleRegion.UPPER_CHEST, BodySide.FRONT, DrawableId.FRONT_UPPER_CHEST, DrawableName.FRONT_UPPER_CHEST, 10),
        direct(MuscleRegion.CHEST, BodySide.FRONT, DrawableId.FRONT_CHEST, DrawableName.FRONT_CHEST, 11),
        alias(MuscleRegion.MIDDLE_CHEST, BodySide.FRONT, DrawableId.FRONT_CHEST, DrawableName.FRONT_CHEST, 12, MuscleRegion.CHEST),
        alias(MuscleRegion.LOWER_CHEST, BodySide.FRONT, DrawableId.FRONT_CHEST, DrawableName.FRONT_CHEST, 13, MuscleRegion.CHEST),
        direct(MuscleRegion.SERRATUS_ANTERIOR, BodySide.FRONT, DrawableId.FRONT_SERRATUS, DrawableName.FRONT_SERRATUS, 14),
        direct(MuscleRegion.RECTUS_ABDOMINIS, BodySide.FRONT, DrawableId.FRONT_ABS, DrawableName.FRONT_ABS, 15),
        alias(MuscleRegion.TRANSVERSE_ABDOMINIS, BodySide.FRONT, DrawableId.FRONT_ABS, DrawableName.FRONT_ABS, 16, MuscleRegion.RECTUS_ABDOMINIS),
        direct(MuscleRegion.OBLIQUES, BodySide.FRONT, DrawableId.FRONT_OBLIQUES, DrawableName.FRONT_OBLIQUES, 17),
        direct(MuscleRegion.HIP_FLEXORS, BodySide.FRONT, DrawableId.FRONT_HIP_FLEXORS, DrawableName.FRONT_HIP_FLEXORS, 18),

        // Front shoulders and arms
        direct(MuscleRegion.ANTERIOR_DELTOID, BodySide.FRONT, DrawableId.FRONT_DELTS, DrawableName.FRONT_DELTS, 30),
        direct(MuscleRegion.LATERAL_DELTOID, BodySide.FRONT, DrawableId.FRONT_SIDE_DELTS, DrawableName.FRONT_SIDE_DELTS, 31),
        direct(MuscleRegion.BICEPS, BodySide.FRONT, DrawableId.FRONT_BICEPS, DrawableName.FRONT_BICEPS, 32),
        alias(MuscleRegion.BRACHIALIS, BodySide.FRONT, DrawableId.FRONT_BICEPS, DrawableName.FRONT_BICEPS, 33, MuscleRegion.BICEPS),
        direct(MuscleRegion.FOREARMS_ANTERIOR, BodySide.FRONT, DrawableId.FRONT_FOREARMS, DrawableName.FRONT_FOREARMS, 34),

        // Front legs
        direct(MuscleRegion.QUADRICEPS, BodySide.FRONT, DrawableId.FRONT_QUADS, DrawableName.FRONT_QUADS, 50),
        alias(MuscleRegion.RECTUS_FEMORIS, BodySide.FRONT, DrawableId.FRONT_QUADS, DrawableName.FRONT_QUADS, 51, MuscleRegion.QUADRICEPS),
        alias(MuscleRegion.VASTUS_LATERALIS, BodySide.FRONT, DrawableId.FRONT_QUADS, DrawableName.FRONT_QUADS, 52, MuscleRegion.QUADRICEPS),
        alias(MuscleRegion.VASTUS_MEDIALIS, BodySide.FRONT, DrawableId.FRONT_QUADS, DrawableName.FRONT_QUADS, 53, MuscleRegion.QUADRICEPS),
        alias(MuscleRegion.VASTUS_INTERMEDIUS, BodySide.FRONT, DrawableId.FRONT_QUADS, DrawableName.FRONT_QUADS, 54, MuscleRegion.QUADRICEPS),
        direct(MuscleRegion.ADDUCTORS, BodySide.FRONT, DrawableId.FRONT_ADDUCTORS, DrawableName.FRONT_ADDUCTORS, 55),
        direct(MuscleRegion.ABDUCTORS, BodySide.FRONT, DrawableId.FRONT_ABDUCTORS, DrawableName.FRONT_ABDUCTORS, 56),
        direct(MuscleRegion.ABDUCTORS, BodySide.BACK, DrawableId.BACK_ABDUCTORS, DrawableName.BACK_ABDUCTORS, 57),
        direct(MuscleRegion.TIBIALIS_ANTERIOR, BodySide.FRONT, DrawableId.FRONT_TIBIALIS, DrawableName.FRONT_TIBIALIS, 58),
        // Calves have both anterior and posterior overlay assets.
        direct(MuscleRegion.CALVES, BodySide.FRONT, DrawableId.FRONT_CALVES, DrawableName.FRONT_CALVES, 59),

        // Back upper body
        direct(MuscleRegion.UPPER_TRAPEZIUS, BodySide.BACK, DrawableId.BACK_TRAPS_UPPER, DrawableName.BACK_TRAPS_UPPER, 100),
        direct(MuscleRegion.MIDDLE_TRAPEZIUS, BodySide.BACK, DrawableId.BACK_TRAPS_MIDDLE, DrawableName.BACK_TRAPS_MIDDLE, 101),
        alias(MuscleRegion.LOWER_TRAPEZIUS, BodySide.BACK, DrawableId.BACK_TRAPS_MIDDLE, DrawableName.BACK_TRAPS_MIDDLE, 102, MuscleRegion.MIDDLE_TRAPEZIUS),
        direct(MuscleRegion.POSTERIOR_DELTOID, BodySide.BACK, DrawableId.BACK_REAR_DELTS, DrawableName.BACK_REAR_DELTS, 103),
        direct(MuscleRegion.LATISSIMUS_DORSI, BodySide.BACK, DrawableId.BACK_LATS, DrawableName.BACK_LATS, 104),
        direct(MuscleRegion.RHOMBOIDS, BodySide.BACK, DrawableId.BACK_RHOMBOIDS, DrawableName.BACK_RHOMBOIDS, 105),
        direct(MuscleRegion.TERES_MAJOR, BodySide.BACK, DrawableId.BACK_TERES_MAJOR, DrawableName.BACK_TERES_MAJOR, 106),
        alias(MuscleRegion.TERES_MINOR, BodySide.BACK, DrawableId.BACK_TERES_MAJOR, DrawableName.BACK_TERES_MAJOR, 107, MuscleRegion.TERES_MAJOR),
        direct(MuscleRegion.TRICEPS, BodySide.BACK, DrawableId.BACK_TRICEPS, DrawableName.BACK_TRICEPS, 108),
        direct(MuscleRegion.FOREARMS_POSTERIOR, BodySide.BACK, DrawableId.BACK_FOREARMS, DrawableName.BACK_FOREARMS, 109),
        direct(MuscleRegion.SPINAL_ERECTORS, BodySide.BACK, DrawableId.BACK_ERECTOR_SPINAE, DrawableName.BACK_ERECTOR_SPINAE, 110),

        // Back lower body
        direct(MuscleRegion.GLUTE_MAXIMUS, BodySide.BACK, DrawableId.BACK_GLUTES, DrawableName.BACK_GLUTES, 130),
        alias(MuscleRegion.GLUTE_MEDIUS, BodySide.BACK, DrawableId.BACK_GLUTES, DrawableName.BACK_GLUTES, 131, MuscleRegion.GLUTE_MAXIMUS),
        alias(MuscleRegion.GLUTE_MINIMUS, BodySide.BACK, DrawableId.BACK_GLUTES, DrawableName.BACK_GLUTES, 132, MuscleRegion.GLUTE_MAXIMUS),
        direct(MuscleRegion.HAMSTRINGS, BodySide.BACK, DrawableId.BACK_HAMSTRINGS, DrawableName.BACK_HAMSTRINGS, 133),
        alias(MuscleRegion.BICEPS_FEMORIS, BodySide.BACK, DrawableId.BACK_HAMSTRINGS, DrawableName.BACK_HAMSTRINGS, 134, MuscleRegion.HAMSTRINGS),
        alias(MuscleRegion.SEMITENDINOSUS, BodySide.BACK, DrawableId.BACK_HAMSTRINGS, DrawableName.BACK_HAMSTRINGS, 135, MuscleRegion.HAMSTRINGS),
        alias(MuscleRegion.SEMIMEMBRANOSUS, BodySide.BACK, DrawableId.BACK_HAMSTRINGS, DrawableName.BACK_HAMSTRINGS, 136, MuscleRegion.HAMSTRINGS),
        direct(MuscleRegion.CALVES, BodySide.BACK, DrawableId.BACK_CALVES, DrawableName.BACK_CALVES, 137),
        alias(MuscleRegion.GASTROCNEMIUS, BodySide.BACK, DrawableId.BACK_CALVES, DrawableName.BACK_CALVES, 138, MuscleRegion.CALVES),
        alias(MuscleRegion.SOLEUS, BodySide.BACK, DrawableId.BACK_CALVES, DrawableName.BACK_CALVES, 139, MuscleRegion.CALVES),
        alias(MuscleRegion.PERONEALS, BodySide.BACK, DrawableId.BACK_CALVES, DrawableName.BACK_CALVES, 140, MuscleRegion.CALVES)
    )

    private val layerComparator = compareBy<PngAnatomyLayerAsset> { it.side.ordinal }
        .thenBy { it.order }
        .thenBy { it.drawableName }

    private val mapping: Map<MuscleRegion, List<PngAnatomyLayerAsset>> = entries
        .groupBy { it.region }
        .mapValues { (_, regionEntries) ->
            regionEntries
                .map { entry -> entry.toAsset() }
                .sortedWith(layerComparator)
        }

    val allOverlayAssets: List<PngAnatomyLayerAsset> = mapping.values.flatten().sortedWith(layerComparator)

    fun baseFor(side: BodySide): PngAnatomyLayerAsset = when (side) {
        BodySide.FRONT -> frontBase
        BodySide.BACK -> backBase
    }

    fun assetsFor(region: MuscleRegion): List<PngAnatomyLayerAsset> = mapping[region].orEmpty()

    fun overlayAssetsFor(side: BodySide, regions: Set<MuscleRegion>): List<PngAnatomyLayerAsset> {
        if (regions.isEmpty()) {
            RendererDiagnostics.emptyActivationList(side)
            return emptyList()
        }

        val layers = regions.flatMap { region ->
            val assets = assetsFor(region)
            if (assets.isEmpty()) RendererDiagnostics.missingMuscleRegionMapping(region)
            assets
        }.filter { it.side == side }

        return layers.sortedWith(layerComparator).distinctBy { it.drawableId }
    }

    fun allDrawableIds(): Set<Int> = buildSet {
        add(frontBase.drawableId)
        add(backBase.drawableId)
        addAll(allOverlayAssets.map { it.drawableId })
    }

    fun allDrawableResourceNames(): Set<String> = buildSet {
        add(frontBase.drawableName)
        add(backBase.drawableName)
        addAll(allOverlayAssets.map { it.drawableName })
    }

    fun missingDrawableResources(availableDrawableResourceNames: Set<String>): Set<String> =
        expectedDrawableResourceNames - availableDrawableResourceNames

    fun unreferencedExpectedDrawables(): Set<String> = expectedDrawableResourceNames - allDrawableResourceNames()

    fun duplicateRegionMappings(): Map<MuscleRegion, List<PngAnatomyLayerAsset>> = mapping.filterValues { layers ->
        layers.map { it.drawableId }.distinct().size != layers.size
    }

    fun unmappedRegions(): Set<MuscleRegion> = MuscleRegion.entries.toSet() - mapping.keys

    fun validate(availableDrawableResourceNames: Set<String> = expectedDrawableResourceNames): PngRegistryValidationReport {
        val duplicates = duplicateRegionMappings()
        val unmapped = unmappedRegions()
        val missing = missingDrawableResources(availableDrawableResourceNames)
        val unreferenced = unreferencedExpectedDrawables()

        if (duplicates.isNotEmpty()) RendererDiagnostics.duplicatePngMappings(duplicates)
        unmapped.forEach(RendererDiagnostics::missingMuscleRegionMapping)
        missing.forEach { RendererDiagnostics.missingPngDrawable(it) }
        unreferenced.forEach { RendererDiagnostics.renderFailure("drawable/$it", "Expected layered anatomy PNG is not referenced") }

        return PngRegistryValidationReport(
            totalRegions = MuscleRegion.entries.size,
            mappedRegions = mapping.keys.size,
            unmappedRegions = unmapped,
            duplicateMappings = duplicates,
            missingDrawableResourceNames = missing,
            unreferencedExpectedDrawableResourceNames = unreferenced
        )
    }

    private fun direct(
        region: MuscleRegion,
        side: BodySide,
        @DrawableRes drawableId: Int,
        drawableName: String,
        order: Int
    ): Entry = Entry(region = region, side = side, drawableId = drawableId, drawableName = drawableName, order = order)

    private fun alias(
        region: MuscleRegion,
        side: BodySide,
        @DrawableRes drawableId: Int,
        drawableName: String,
        order: Int,
        mappedFrom: MuscleRegion
    ): Entry = Entry(
        region = region,
        side = side,
        drawableId = drawableId,
        drawableName = drawableName,
        order = order,
        type = MuscleAssetMappingType.ALIAS,
        mappedFrom = mappedFrom
    )

    private fun Entry.toAsset(): PngAnatomyLayerAsset = PngAnatomyLayerAsset(
        id = "${side.name.lowercase()}_${region.name.lowercase()}_$drawableName",
        side = side,
        drawableId = drawableId,
        drawableName = drawableName,
        role = AnatomyLayerRole.OVERLAY,
        order = order,
        region = region,
        mappingType = type,
        mappedFrom = mappedFrom
    )
}

data class PngAnatomyLayerAsset(
    val id: String,
    val side: BodySide,
    @DrawableRes val drawableId: Int,
    val drawableName: String,
    val role: AnatomyLayerRole,
    val order: Int,
    val region: MuscleRegion? = null,
    val mappingType: MuscleAssetMappingType = MuscleAssetMappingType.DIRECT,
    val mappedFrom: MuscleRegion? = null
)

data class PngRegistryValidationReport(
    val totalRegions: Int,
    val mappedRegions: Int,
    val unmappedRegions: Set<MuscleRegion>,
    val duplicateMappings: Map<MuscleRegion, List<PngAnatomyLayerAsset>>,
    val missingDrawableResourceNames: Set<String> = emptySet(),
    val unreferencedExpectedDrawableResourceNames: Set<String> = emptySet()
) {
    val isValid: Boolean
        get() = unmappedRegions.isEmpty() &&
            duplicateMappings.isEmpty() &&
            missingDrawableResourceNames.isEmpty() &&
            unreferencedExpectedDrawableResourceNames.isEmpty()
}
