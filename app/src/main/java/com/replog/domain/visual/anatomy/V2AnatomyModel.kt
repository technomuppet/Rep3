package com.replog.domain.visual.anatomy

import android.content.Context
import androidx.compose.ui.graphics.Path

/**
 * Version 2 Visual Engine — Anatomically detailed vector geometry model.
 * RC47.6 OVERHAUL: Source geometry is loaded exclusively from external SVG assets
 * (assets/anatomy/front_anatomy.svg and back_anatomy.svg). The procedural buildPath
 * definitions have been removed. Only silhouette geometry remains procedural
 * (structural boundary, not independently addressable muscle regions).
 * The renderer is responsible only for loading, mapping, and colouring SVG assets.
 */
object V2AnatomyModel {

    private fun buildPath(block: Path.() -> Unit): Path = Path().apply {
        block()
        close()
    }

    val frontSilhouette: Path = buildPath {
        moveTo(250f, 30f)
        cubicTo(285f, 30f, 295f, 70f, 285f, 110f)
        cubicTo(275f, 130f, 275f, 150f, 280f, 150f)
        cubicTo(320f, 155f, 360f, 160f, 390f, 165f)
        cubicTo(415f, 185f, 420f, 240f, 415f, 350f)
        lineTo(435f, 510f)
        lineTo(440f, 565f)
        lineTo(410f, 565f)
        lineTo(395f, 510f)
        lineTo(375f, 355f)
        cubicTo(365f, 255f, 345f, 215f, 345f, 215f)
        cubicTo(335f, 300f, 325f, 380f, 325f, 440f)
        cubicTo(335f, 470f, 345f, 510f, 340f, 550f)
        cubicTo(345f, 620f, 340f, 690f, 330f, 715f)
        cubicTo(345f, 780f, 345f, 870f, 330f, 925f)
        lineTo(350f, 965f)
        lineTo(280f, 965f)
        lineTo(270f, 925f)
        lineTo(265f, 715f)
        lineTo(255f, 480f)
        lineTo(245f, 480f)
        lineTo(235f, 715f)
        lineTo(230f, 925f)
        lineTo(220f, 965f)
        lineTo(150f, 965f)
        lineTo(170f, 925f)
        cubicTo(155f, 870f, 155f, 780f, 170f, 715f)
        cubicTo(160f, 690f, 155f, 620f, 160f, 550f)
        cubicTo(155f, 510f, 165f, 470f, 175f, 440f)
        cubicTo(175f, 380f, 165f, 300f, 155f, 215f)
        cubicTo(155f, 215f, 135f, 255f, 125f, 355f)
        lineTo(105f, 510f)
        lineTo(90f, 565f)
        lineTo(60f, 565f)
        lineTo(65f, 510f)
        lineTo(85f, 350f)
        cubicTo(80f, 240f, 85f, 185f, 110f, 165f)
        cubicTo(140f, 160f, 180f, 155f, 220f, 150f)
        cubicTo(225f, 150f, 225f, 130f, 215f, 110f)
        cubicTo(205f, 70f, 215f, 30f, 250f, 30f)
    }

    val rearSilhouette: Path = frontSilhouette

    /**
     * RC47.6: Load anatomy regions exclusively from SVG assets.
     * Context is required (non-null). There is no procedural fallback.
     */
    fun loadRegionsForSide(isFront: Boolean, context: Context): List<BodyRegion> {
        val svgPaths = try {
            SVGAnatomyLoader.loadRegions(context, isFront)
        } catch (e: Exception) {
            AnatomyDiagnostics.auditSvgLoad(
                assetName = if (isFront) "front_anatomy.svg" else "back_anatomy.svg",
                expectedIds = if (isFront) 21 else 22,
                loadedIds = 0,
                missingIds = emptyList(),
                exceptionMsg = e.message ?: "V2AnatomyModel.loadRegions exception"
            )
            emptyMap()
        }
        val list = mutableListOf<BodyRegion>()
        val regions = if (isFront) MuscleRegion.FRONT_REGIONS else MuscleRegion.BACK_REGIONS
        for (r in regions) {
            val path = svgPaths[r]
            if (path != null) {
                list.add(BodyRegion(r, path, isFront))
            } else {
                AnatomyDiagnostics.auditSvgMissingIds(listOf(r.name))
                throw IllegalStateException(
                    "SVG path missing for region ${r.name} (side=${if (isFront) "FRONT" else "BACK"}). " +
                    "Every region must have its own independent SVG geometry."
                )
            }
        }
        return list
    }
}
