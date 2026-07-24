package com.replog.domain.visual.anatomy

/**
 * Single source of truth for the layered anatomy SVG library.
 *
 * This registry is deliberately data-only: it does not read assets, parse SVGs,
 * or draw UI. That keeps file names centralised and allows renderer, tests and
 * diagnostics to reason about the same canonical mapping.
 */
object MuscleAssetRegistry {

    const val CANVAS_WIDTH: Float = 768f
    const val CANVAS_HEIGHT: Float = 1536f

    private const val FRONT_BASE = "anatomy/front/anatomy_front_base.svg"
    private const val BACK_BASE = "anatomy/back/anatomy_back_base.svg"


    private object AssetPath {
        const val FRONT_UPPER_CHEST = "anatomy/front/upper_chest.svg"
        const val FRONT_CHEST = "anatomy/front/chest.svg"
        const val FRONT_DELTS = "anatomy/front/front_delts.svg"
        const val FRONT_SIDE_DELTS = "anatomy/front/side_delts.svg"
        const val FRONT_BICEPS = "anatomy/front/biceps.svg"
        const val FRONT_FOREARMS = "anatomy/front/forearms.svg"
        const val FRONT_ABS = "anatomy/front/abs.svg"
        const val FRONT_OBLIQUES = "anatomy/front/obliques.svg"
        const val FRONT_SERRATUS = "anatomy/front/serratus.svg"
        const val FRONT_HIP_FLEXORS = "anatomy/front/hip_flexors.svg"
        const val FRONT_QUADS = "anatomy/front/quads.svg"
        const val FRONT_ADDUCTORS = "anatomy/front/adductors.svg"
        const val FRONT_TIBIALIS = "anatomy/front/tibialis.svg"
        const val FRONT_CALVES = "anatomy/front/calves.svg"
        const val BACK_TRAPS_UPPER = "anatomy/back/traps_upper.svg"
        const val BACK_TRAPS_MIDDLE = "anatomy/back/traps_middle.svg"
        const val BACK_REAR_DELTS = "anatomy/back/rear_delts.svg"
        const val BACK_LATS = "anatomy/back/lats.svg"
        const val BACK_RHOMBOIDS = "anatomy/back/rhomboids.svg"
        const val BACK_TERES_MAJOR = "anatomy/back/teres_major.svg"
        const val BACK_TRICEPS = "anatomy/back/triceps.svg"
        const val BACK_ERECTOR_SPINAE = "anatomy/back/erector_spinae.svg"
        const val BACK_GLUTES = "anatomy/back/glutes.svg"
        const val BACK_HAMSTRINGS = "anatomy/back/hamstrings.svg"
        const val BACK_FOREARMS = "anatomy/back/forearms_back.svg"
        const val BACK_CALVES = "anatomy/back/calves_back.svg"
    }

    /** Every SVG that belongs to the layered anatomy library. Legacy monolithic
     * SVGs at anatomy/front_anatomy.svg and anatomy/back_anatomy.svg are not part
     * of this source-of-truth set. */
    val expectedLibraryAssetPaths: Set<String> = setOf(
        FRONT_BASE,
        AssetPath.FRONT_UPPER_CHEST,
        AssetPath.FRONT_CHEST,
        AssetPath.FRONT_DELTS,
        AssetPath.FRONT_SIDE_DELTS,
        AssetPath.FRONT_BICEPS,
        AssetPath.FRONT_FOREARMS,
        AssetPath.FRONT_ABS,
        AssetPath.FRONT_OBLIQUES,
        AssetPath.FRONT_SERRATUS,
        AssetPath.FRONT_HIP_FLEXORS,
        AssetPath.FRONT_QUADS,
        AssetPath.FRONT_ADDUCTORS,
        AssetPath.FRONT_TIBIALIS,
        AssetPath.FRONT_CALVES,
        BACK_BASE,
        AssetPath.BACK_TRAPS_UPPER,
        AssetPath.BACK_TRAPS_MIDDLE,
        AssetPath.BACK_REAR_DELTS,
        AssetPath.BACK_LATS,
        AssetPath.BACK_RHOMBOIDS,
        AssetPath.BACK_TERES_MAJOR,
        AssetPath.BACK_TRICEPS,
        AssetPath.BACK_ERECTOR_SPINAE,
        AssetPath.BACK_GLUTES,
        AssetPath.BACK_HAMSTRINGS,
        AssetPath.BACK_FOREARMS,
        AssetPath.BACK_CALVES
    )

    val frontBase: AnatomyLayerAsset = AnatomyLayerAsset(
        id = "front_base",
        side = BodySide.FRONT,
        assetPath = FRONT_BASE,
        role = AnatomyLayerRole.BASE,
        order = 0
    )

    val backBase: AnatomyLayerAsset = AnatomyLayerAsset(
        id = "back_base",
        side = BodySide.BACK,
        assetPath = BACK_BASE,
        role = AnatomyLayerRole.BASE,
        order = 0
    )

    private data class Entry(
        val region: MuscleRegion,
        val side: BodySide,
        val assetPath: String,
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
        direct(MuscleRegion.UPPER_CHEST, BodySide.FRONT, AssetPath.FRONT_UPPER_CHEST, 10),
        direct(MuscleRegion.CHEST, BodySide.FRONT, AssetPath.FRONT_CHEST, 11),
        alias(MuscleRegion.MIDDLE_CHEST, BodySide.FRONT, AssetPath.FRONT_CHEST, 12, MuscleRegion.CHEST),
        alias(MuscleRegion.LOWER_CHEST, BodySide.FRONT, AssetPath.FRONT_CHEST, 13, MuscleRegion.CHEST),
        direct(MuscleRegion.SERRATUS_ANTERIOR, BodySide.FRONT, AssetPath.FRONT_SERRATUS, 14),
        direct(MuscleRegion.RECTUS_ABDOMINIS, BodySide.FRONT, AssetPath.FRONT_ABS, 15),
        alias(MuscleRegion.TRANSVERSE_ABDOMINIS, BodySide.FRONT, AssetPath.FRONT_ABS, 16, MuscleRegion.RECTUS_ABDOMINIS),
        direct(MuscleRegion.OBLIQUES, BodySide.FRONT, AssetPath.FRONT_OBLIQUES, 17),
        direct(MuscleRegion.HIP_FLEXORS, BodySide.FRONT, AssetPath.FRONT_HIP_FLEXORS, 18),

        // Front shoulders and arms
        direct(MuscleRegion.ANTERIOR_DELTOID, BodySide.FRONT, AssetPath.FRONT_DELTS, 30),
        direct(MuscleRegion.LATERAL_DELTOID, BodySide.FRONT, AssetPath.FRONT_SIDE_DELTS, 31),
        direct(MuscleRegion.BICEPS, BodySide.FRONT, AssetPath.FRONT_BICEPS, 32),
        alias(MuscleRegion.BRACHIALIS, BodySide.FRONT, AssetPath.FRONT_BICEPS, 33, MuscleRegion.BICEPS),
        direct(MuscleRegion.FOREARMS_ANTERIOR, BodySide.FRONT, AssetPath.FRONT_FOREARMS, 34),

        // Front legs
        direct(MuscleRegion.QUADRICEPS, BodySide.FRONT, AssetPath.FRONT_QUADS, 50),
        alias(MuscleRegion.RECTUS_FEMORIS, BodySide.FRONT, AssetPath.FRONT_QUADS, 51, MuscleRegion.QUADRICEPS),
        alias(MuscleRegion.VASTUS_LATERALIS, BodySide.FRONT, AssetPath.FRONT_QUADS, 52, MuscleRegion.QUADRICEPS),
        alias(MuscleRegion.VASTUS_MEDIALIS, BodySide.FRONT, AssetPath.FRONT_QUADS, 53, MuscleRegion.QUADRICEPS),
        alias(MuscleRegion.VASTUS_INTERMEDIUS, BodySide.FRONT, AssetPath.FRONT_QUADS, 54, MuscleRegion.QUADRICEPS),
        direct(MuscleRegion.ADDUCTORS, BodySide.FRONT, AssetPath.FRONT_ADDUCTORS, 55),
        alias(MuscleRegion.ABDUCTORS, BodySide.BACK, AssetPath.BACK_GLUTES, 56, MuscleRegion.GLUTE_MEDIUS),
        direct(MuscleRegion.TIBIALIS_ANTERIOR, BodySide.FRONT, AssetPath.FRONT_TIBIALIS, 57),
        // Calves have both anterior and posterior overlay assets.
        direct(MuscleRegion.CALVES, BodySide.FRONT, AssetPath.FRONT_CALVES, 58),

        // Back upper body
        direct(MuscleRegion.UPPER_TRAPEZIUS, BodySide.BACK, AssetPath.BACK_TRAPS_UPPER, 100),
        direct(MuscleRegion.MIDDLE_TRAPEZIUS, BodySide.BACK, AssetPath.BACK_TRAPS_MIDDLE, 101),
        alias(MuscleRegion.LOWER_TRAPEZIUS, BodySide.BACK, AssetPath.BACK_TRAPS_MIDDLE, 102, MuscleRegion.MIDDLE_TRAPEZIUS),
        direct(MuscleRegion.POSTERIOR_DELTOID, BodySide.BACK, AssetPath.BACK_REAR_DELTS, 103),
        direct(MuscleRegion.LATISSIMUS_DORSI, BodySide.BACK, AssetPath.BACK_LATS, 104),
        direct(MuscleRegion.RHOMBOIDS, BodySide.BACK, AssetPath.BACK_RHOMBOIDS, 105),
        direct(MuscleRegion.TERES_MAJOR, BodySide.BACK, AssetPath.BACK_TERES_MAJOR, 106),
        alias(MuscleRegion.TERES_MINOR, BodySide.BACK, AssetPath.BACK_TERES_MAJOR, 107, MuscleRegion.TERES_MAJOR),
        direct(MuscleRegion.TRICEPS, BodySide.BACK, AssetPath.BACK_TRICEPS, 108),
        direct(MuscleRegion.FOREARMS_POSTERIOR, BodySide.BACK, AssetPath.BACK_FOREARMS, 109),
        direct(MuscleRegion.SPINAL_ERECTORS, BodySide.BACK, AssetPath.BACK_ERECTOR_SPINAE, 110),

        // Back lower body
        direct(MuscleRegion.GLUTE_MAXIMUS, BodySide.BACK, AssetPath.BACK_GLUTES, 130),
        alias(MuscleRegion.GLUTE_MEDIUS, BodySide.BACK, AssetPath.BACK_GLUTES, 131, MuscleRegion.GLUTE_MAXIMUS),
        alias(MuscleRegion.GLUTE_MINIMUS, BodySide.BACK, AssetPath.BACK_GLUTES, 132, MuscleRegion.GLUTE_MAXIMUS),
        direct(MuscleRegion.HAMSTRINGS, BodySide.BACK, AssetPath.BACK_HAMSTRINGS, 133),
        alias(MuscleRegion.BICEPS_FEMORIS, BodySide.BACK, AssetPath.BACK_HAMSTRINGS, 134, MuscleRegion.HAMSTRINGS),
        alias(MuscleRegion.SEMITENDINOSUS, BodySide.BACK, AssetPath.BACK_HAMSTRINGS, 135, MuscleRegion.HAMSTRINGS),
        alias(MuscleRegion.SEMIMEMBRANOSUS, BodySide.BACK, AssetPath.BACK_HAMSTRINGS, 136, MuscleRegion.HAMSTRINGS),
        direct(MuscleRegion.CALVES, BodySide.BACK, AssetPath.BACK_CALVES, 137),
        alias(MuscleRegion.GASTROCNEMIUS, BodySide.BACK, AssetPath.BACK_CALVES, 138, MuscleRegion.CALVES),
        alias(MuscleRegion.SOLEUS, BodySide.BACK, AssetPath.BACK_CALVES, 139, MuscleRegion.CALVES),
        alias(MuscleRegion.PERONEALS, BodySide.BACK, AssetPath.BACK_CALVES, 140, MuscleRegion.CALVES)
    )

    private val layerComparator = compareBy<AnatomyLayerAsset> { it.side.ordinal }
        .thenBy { it.order }
        .thenBy { it.assetPath }

    private val mapping: Map<MuscleRegion, List<AnatomyLayerAsset>> = entries
        .groupBy { it.region }
        .mapValues { (_, regionEntries) ->
            regionEntries
                .map { entry -> entry.toAsset() }
                .sortedWith(layerComparator)
        }

    val allOverlayAssets: List<AnatomyLayerAsset> = mapping.values.flatten().sortedWith(layerComparator)

    fun baseFor(side: BodySide): AnatomyLayerAsset = when (side) {
        BodySide.FRONT -> frontBase
        BodySide.BACK -> backBase
    }

    fun assetsFor(region: MuscleRegion): List<AnatomyLayerAsset> = mapping[region].orEmpty()

    fun overlayAssetsFor(side: BodySide, regions: Set<MuscleRegion>): List<AnatomyLayerAsset> {
        if (regions.isEmpty()) {
            RendererDiagnostics.emptyActivationList(side)
            return emptyList()
        }

        val layers = regions.flatMap { region ->
            val assets = assetsFor(region)
            if (assets.isEmpty()) RendererDiagnostics.missingMuscleRegionMapping(region)
            assets
        }.filter { it.side == side }

        return layers.sortedWith(layerComparator).distinctBy { it.assetPath }
    }

    fun allAssetPaths(): Set<String> = buildSet {
        add(frontBase.assetPath)
        add(backBase.assetPath)
        addAll(allOverlayAssets.map { it.assetPath })
    }

    fun orphanLibraryAssets(availableAssetPaths: Set<String>): Set<String> =
        availableAssetPaths.filter { it.startsWith("anatomy/front/") || it.startsWith("anatomy/back/") }.toSet() - allAssetPaths()

    fun missingLibraryAssets(availableAssetPaths: Set<String>): Set<String> =
        expectedLibraryAssetPaths - availableAssetPaths

    fun unreferencedExpectedAssets(): Set<String> = expectedLibraryAssetPaths - allAssetPaths()

    fun duplicateRegionMappings(): Map<MuscleRegion, List<AnatomyLayerAsset>> = mapping.filterValues { layers ->
        layers.map { it.assetPath }.distinct().size != layers.size
    }

    fun unmappedRegions(): Set<MuscleRegion> = MuscleRegion.entries.toSet() - mapping.keys

    fun validate(availableAssetPaths: Set<String> = expectedLibraryAssetPaths): RegistryValidationReport {
        val duplicates = duplicateRegionMappings()
        val unmapped = unmappedRegions()
        val missing = missingLibraryAssets(availableAssetPaths)
        val orphaned = orphanLibraryAssets(availableAssetPaths)
        val unreferenced = unreferencedExpectedAssets()

        if (duplicates.isNotEmpty()) RendererDiagnostics.duplicateMappings(duplicates)
        unmapped.forEach(RendererDiagnostics::missingMuscleRegionMapping)
        missing.forEach { RendererDiagnostics.missingSvgAsset(it) }
        orphaned.forEach { RendererDiagnostics.renderFailure(it, "Orphan layered anatomy asset") }
        unreferenced.forEach { RendererDiagnostics.renderFailure(it, "Expected layered anatomy asset is not referenced") }

        return RegistryValidationReport(
            totalRegions = MuscleRegion.entries.size,
            mappedRegions = mapping.keys.size,
            unmappedRegions = unmapped,
            duplicateMappings = duplicates,
            missingAssetPaths = missing,
            orphanAssetPaths = orphaned,
            unreferencedExpectedAssetPaths = unreferenced
        )
    }

    private fun direct(region: MuscleRegion, side: BodySide, assetPath: String, order: Int): Entry =
        Entry(region = region, side = side, assetPath = assetPath, order = order)

    private fun alias(
        region: MuscleRegion,
        side: BodySide,
        assetPath: String,
        order: Int,
        mappedFrom: MuscleRegion
    ): Entry = Entry(
        region = region,
        side = side,
        assetPath = assetPath,
        order = order,
        type = MuscleAssetMappingType.ALIAS,
        mappedFrom = mappedFrom
    )

    private fun Entry.toAsset(): AnatomyLayerAsset = AnatomyLayerAsset(
        id = "${side.name.lowercase()}_${region.name.lowercase()}_${assetPath.substringAfterLast('/').substringBeforeLast('.')}",
        side = side,
        assetPath = assetPath,
        role = AnatomyLayerRole.OVERLAY,
        order = order,
        region = region,
        mappingType = type,
        mappedFrom = mappedFrom
    )

}

data class RegistryValidationReport(
    val totalRegions: Int,
    val mappedRegions: Int,
    val unmappedRegions: Set<MuscleRegion>,
    val duplicateMappings: Map<MuscleRegion, List<AnatomyLayerAsset>>,
    val missingAssetPaths: Set<String> = emptySet(),
    val orphanAssetPaths: Set<String> = emptySet(),
    val unreferencedExpectedAssetPaths: Set<String> = emptySet()
) {
    val isValid: Boolean
        get() = unmappedRegions.isEmpty() &&
            duplicateMappings.isEmpty() &&
            missingAssetPaths.isEmpty() &&
            orphanAssetPaths.isEmpty() &&
            unreferencedExpectedAssetPaths.isEmpty()
}
