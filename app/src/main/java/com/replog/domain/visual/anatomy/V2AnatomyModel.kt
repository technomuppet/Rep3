package com.replog.domain.visual.anatomy

import androidx.compose.ui.graphics.Path

/**
 * Version 2 Visual Engine — Anatomically detailed vector geometry model.
 * Each muscle is designed as an independent scalable path over a standard 500x1000 grid.
 * All paths are pre-compiled to guarantee zero heap allocations during the rendering loop.
 */
object V2AnatomyModel {

    // Helper to build and close Compose Paths cleanly
    private fun buildPath(block: Path.() -> Unit): Path = Path().apply {
        block()
        close()
    }

    // --- FRONT SILHOUETTE ---
    val frontSilhouette: Path = buildPath {
        moveTo(250f, 30f)
        cubicTo(290f, 30f, 300f, 75f, 290f, 115f)
        lineTo(280f, 150f)
        lineTo(390f, 165f)
        cubicTo(415f, 185f, 420f, 240f, 415f, 350f)
        lineTo(435f, 510f)
        lineTo(440f, 565f)
        lineTo(410f, 565f)
        lineTo(395f, 510f)
        lineTo(375f, 355f)
        lineTo(345f, 215f)
        lineTo(335f, 320f)
        lineTo(325f, 440f)
        lineTo(340f, 550f)
        cubicTo(345f, 620f, 340f, 690f, 330f, 715f)
        lineTo(330f, 925f)
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
        lineTo(170f, 715f)
        cubicTo(160f, 690f, 155f, 620f, 160f, 550f)
        lineTo(175f, 440f)
        lineTo(165f, 320f)
        lineTo(155f, 215f)
        lineTo(125f, 355f)
        lineTo(105f, 510f)
        lineTo(90f, 565f)
        lineTo(60f, 565f)
        lineTo(65f, 510f)
        lineTo(85f, 350f)
        cubicTo(80f, 240f, 85f, 185f, 110f, 165f)
        lineTo(220f, 150f)
        lineTo(210f, 115f)
        cubicTo(200f, 75f, 210f, 30f, 250f, 30f)
    }

    // --- REAR SILHOUETTE ---
    val rearSilhouette: Path = frontSilhouette // Symmetric projection for silhouette

    // --- NEW MODULAR FRONT BODY REGIONS ---
    val chestPath: Path = buildPath {
        moveTo(255f, 175f); lineTo(335f, 175f); lineTo(340f, 230f)
        cubicTo(325f, 255f, 285f, 270f, 255f, 260f)
        moveTo(165f, 175f); lineTo(245f, 175f); lineTo(245f, 260f)
        cubicTo(215f, 270f, 175f, 255f, 160f, 230f)
    }

    val upperChestPath: Path = buildPath {
        moveTo(170f, 155f); lineTo(245f, 155f); lineTo(245f, 185f); lineTo(165f, 185f)
        moveTo(255f, 155f); lineTo(330f, 155f); lineTo(335f, 185f); lineTo(255f, 185f)
    }

    val antDeltPath: Path = buildPath {
        moveTo(110f, 165f); lineTo(155f, 170f); lineTo(145f, 235f); lineTo(100f, 220f)
        moveTo(390f, 165f); lineTo(345f, 170f); lineTo(355f, 235f); lineTo(400f, 220f)
    }

    val latDeltPath: Path = buildPath {
        moveTo(85f, 175f); lineTo(105f, 170f); lineTo(95f, 245f); lineTo(80f, 235f)
        moveTo(415f, 175f); lineTo(395f, 170f); lineTo(405f, 245f); lineTo(420f, 235f)
    }

    val bicepsPath: Path = buildPath {
        moveTo(95f, 245f); lineTo(135f, 250f); lineTo(125f, 350f); lineTo(90f, 345f)
        moveTo(405f, 245f); lineTo(365f, 250f); lineTo(375f, 350f); lineTo(410f, 345f)
    }

    val forearmsAntPath: Path = buildPath {
        moveTo(85f, 365f); lineTo(120f, 365f); lineTo(105f, 500f); lineTo(75f, 495f)
        moveTo(415f, 365f); lineTo(380f, 365f); lineTo(395f, 500f); lineTo(425f, 495f)
    }

    val rectusAbsPath: Path = buildPath {
        moveTo(215f, 275f); lineTo(285f, 275f); lineTo(280f, 430f); lineTo(220f, 430f)
    }

    val obliquesPath: Path = buildPath {
        moveTo(165f, 275f); lineTo(210f, 275f); lineTo(215f, 430f); lineTo(175f, 430f)
        moveTo(335f, 275f); lineTo(290f, 275f); lineTo(285f, 430f); lineTo(325f, 430f)
    }

    val hipFlexorsPath: Path = buildPath {
        moveTo(180f, 440f); lineTo(240f, 440f); lineTo(235f, 485f); lineTo(175f, 480f)
        moveTo(320f, 440f); lineTo(260f, 440f); lineTo(265f, 485f); lineTo(325f, 480f)
    }

    val quadsPath: Path = buildPath {
        moveTo(175f, 495f); lineTo(235f, 495f); lineTo(225f, 700f); lineTo(165f, 700f)
        moveTo(325f, 495f); lineTo(265f, 495f); lineTo(275f, 700f); lineTo(335f, 700f)
    }

    val adductorsPath: Path = buildPath {
        moveTo(235f, 490f); lineTo(250f, 490f); lineTo(240f, 640f); lineTo(228f, 640f)
        moveTo(265f, 490f); lineTo(250f, 490f); lineTo(260f, 640f); lineTo(272f, 640f)
    }

    val abductorsPath: Path = buildPath {
        moveTo(165f, 485f); lineTo(178f, 485f); lineTo(168f, 660f); lineTo(155f, 630f)
        moveTo(335f, 485f); lineTo(322f, 485f); lineTo(332f, 660f); lineTo(345f, 630f)
    }

    val tibialisPath: Path = buildPath {
        moveTo(175f, 725f); lineTo(215f, 725f); lineTo(205f, 910f); lineTo(180f, 910f)
        moveTo(325f, 725f); lineTo(285f, 725f); lineTo(295f, 910f); lineTo(320f, 910f)
    }

    // --- NEW MODULAR REAR BODY REGIONS ---
    val postDeltPath: Path = buildPath {
        moveTo(100f, 170f); lineTo(145f, 175f); lineTo(135f, 240f); lineTo(90f, 230f)
        moveTo(400f, 170f); lineTo(355f, 175f); lineTo(365f, 240f); lineTo(410f, 230f)
    }

    val tricepsPath: Path = buildPath {
        moveTo(80f, 245f); lineTo(115f, 245f); lineTo(110f, 350f); lineTo(75f, 345f)
        moveTo(420f, 245f); lineTo(385f, 245f); lineTo(390f, 350f); lineTo(425f, 345f)
    }

    val upperTrapsPath: Path = buildPath {
        moveTo(215f, 115f); lineTo(250f, 115f); lineTo(285f, 115f); lineTo(345f, 160f); lineTo(155f, 160f)
    }

    val midTrapsPath: Path = buildPath {
        moveTo(195f, 160f); lineTo(305f, 160f); lineTo(285f, 225f); lineTo(215f, 225f)
    }

    val lowerTrapsPath: Path = buildPath {
        moveTo(215f, 225f); lineTo(285f, 225f); lineTo(250f, 310f)
    }

    val latsPath: Path = buildPath {
        moveTo(155f, 215f); lineTo(205f, 225f); lineTo(195f, 345f); lineTo(155f, 320f)
        moveTo(345f, 215f); lineTo(295f, 225f); lineTo(305f, 345f); lineTo(345f, 320f)
    }

    val rhomboidsPath: Path = buildPath {
        moveTo(205f, 190f); lineTo(295f, 190f); lineTo(285f, 255f); lineTo(215f, 255f)
    }

    val spinalErectorsPath: Path = buildPath {
        moveTo(225f, 325f); lineTo(275f, 325f); lineTo(270f, 435f); lineTo(230f, 435f)
    }

    val gluteMaxPath: Path = buildPath {
        moveTo(175f, 440f); lineTo(325f, 440f); lineTo(310f, 545f); lineTo(190f, 545f)
    }

    val gluteMedPath: Path = buildPath {
        moveTo(165f, 420f); lineTo(335f, 420f); lineTo(325f, 460f); lineTo(175f, 460f)
    }

    val hamstringsPath: Path = buildPath {
        moveTo(170f, 550f); lineTo(240f, 550f); lineTo(230f, 710f); lineTo(165f, 710f)
        moveTo(330f, 550f); lineTo(260f, 550f); lineTo(270f, 710f); lineTo(335f, 710f)
    }

    val calvesPath: Path = buildPath {
        moveTo(170f, 715f); lineTo(230f, 715f); lineTo(210f, 915f); lineTo(175f, 915f)
        moveTo(330f, 715f); lineTo(270f, 715f); lineTo(290f, 915f); lineTo(325f, 915f)
    }

    // --- REPOSITORY ACCESS POINT ---
    fun loadRegionsForSide(isFront: Boolean): List<BodyRegion> {
        val list = mutableListOf<BodyRegion>()
        val regions = if (isFront) MuscleRegion.FRONT_REGIONS else MuscleRegion.BACK_REGIONS
        for (r in regions) {
            val path = when (r) {
                MuscleRegion.CHEST -> chestPath
                MuscleRegion.UPPER_CHEST -> upperChestPath
                MuscleRegion.ANTERIOR_DELTOID -> antDeltPath
                MuscleRegion.LATERAL_DELTOID -> latDeltPath
                MuscleRegion.BICEPS -> bicepsPath
                MuscleRegion.FOREARMS_ANTERIOR -> forearmsAntPath
                MuscleRegion.RECTUS_ABDOMINIS -> rectusAbsPath
                MuscleRegion.OBLIQUES -> obliquesPath
                MuscleRegion.HIP_FLEXORS -> hipFlexorsPath
                MuscleRegion.QUADRICEPS -> quadsPath
                MuscleRegion.ADDUCTORS -> adductorsPath
                MuscleRegion.ABDUCTORS -> abductorsPath
                MuscleRegion.TIBIALIS_ANTERIOR -> tibialisPath

                MuscleRegion.POSTERIOR_DELTOID -> postDeltPath
                MuscleRegion.TRICEPS -> tricepsPath
                MuscleRegion.UPPER_TRAPEZIUS -> upperTrapsPath
                MuscleRegion.MIDDLE_TRAPEZIUS -> midTrapsPath
                MuscleRegion.LOWER_TRAPEZIUS -> lowerTrapsPath
                MuscleRegion.LATISSIMUS_DORSI -> latsPath
                MuscleRegion.RHOMBOIDS -> rhomboidsPath
                MuscleRegion.SPINAL_ERECTORS -> spinalErectorsPath
                MuscleRegion.GLUTE_MAXIMUS -> gluteMaxPath
                MuscleRegion.GLUTE_MEDIUS -> gluteMedPath
                MuscleRegion.HAMSTRINGS -> hamstringsPath
                MuscleRegion.CALVES -> calvesPath
                else -> chestPath
            }
            list.add(BodyRegion(r, path, isFront))
        }
        return list
    }
}
