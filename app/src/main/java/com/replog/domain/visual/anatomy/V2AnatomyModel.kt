package com.replog.domain.visual.anatomy

import androidx.compose.ui.graphics.Path

/**
 * Version 2 Visual Engine — Anatomically detailed vector geometry model.
 * Overhauled to a medical diagram style with organic muscle curves
 * and precise proportional contours on a standard 500x1000 grid.
 * All paths are pre-compiled with proper sub-path closures to guarantee
 * zero visual artifacts and zero heap allocations during the rendering loop.
 */
object V2AnatomyModel {

    // Helper to build and close Compose Paths cleanly
    private fun buildPath(block: Path.() -> Unit): Path = Path().apply {
        block()
        close()
    }

    // --- FRONT SILHOUETTE ---
    val frontSilhouette: Path = buildPath {
        // Head
        moveTo(250f, 30f)
        cubicTo(285f, 30f, 295f, 70f, 285f, 110f)
        // Neck
        cubicTo(275f, 130f, 275f, 150f, 280f, 150f)
        // Shoulder slope & arm
        cubicTo(320f, 155f, 360f, 160f, 390f, 165f)
        cubicTo(415f, 185f, 420f, 240f, 415f, 350f)
        lineTo(435f, 510f)
        lineTo(440f, 565f)
        lineTo(410f, 565f)
        lineTo(395f, 510f)
        lineTo(375f, 355f)
        // Chest/Armpit down to waist/hips
        cubicTo(365f, 255f, 345f, 215f, 345f, 215f)
        cubicTo(335f, 300f, 325f, 380f, 325f, 440f)
        cubicTo(335f, 470f, 345f, 510f, 340f, 550f)
        // Outer thigh down to knee
        cubicTo(345f, 620f, 340f, 690f, 330f, 715f)
        // Calf down to ankle
        cubicTo(345f, 780f, 345f, 870f, 330f, 925f)
        // Foot
        lineTo(350f, 965f)
        lineTo(280f, 965f)
        // Inner ankle/leg up to crotch
        lineTo(270f, 925f)
        lineTo(265f, 715f)
        lineTo(255f, 480f)
        lineTo(245f, 480f)
        lineTo(235f, 715f)
        lineTo(230f, 925f)
        // Right side (symmetrical left side going up)
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

    // --- REAR SILHOUETTE ---
    val rearSilhouette: Path = frontSilhouette // Symmetric projection for silhouette

    // --- NEW MODULAR FRONT BODY REGIONS ---
    val chestPath: Path = buildPath {
        // Right pectoral
        moveTo(252f, 175f)
        lineTo(335f, 175f)
        lineTo(340f, 230f)
        cubicTo(325f, 255f, 285f, 270f, 252f, 260f)
        close()
        // Left pectoral
        moveTo(248f, 175f)
        lineTo(165f, 175f)
        lineTo(160f, 230f)
        cubicTo(175f, 255f, 215f, 270f, 248f, 260f)
    }

    val upperChestPath: Path = buildPath {
        // Right upper chest
        moveTo(252f, 155f)
        lineTo(330f, 155f)
        lineTo(335f, 185f)
        lineTo(252f, 185f)
        close()
        // Left upper chest
        moveTo(248f, 155f)
        lineTo(170f, 155f)
        lineTo(165f, 185f)
        lineTo(248f, 185f)
    }

    val antDeltPath: Path = buildPath {
        // Right anterior deltoid
        moveTo(340f, 170f)
        cubicTo(370f, 175f, 385f, 200f, 375f, 235f)
        cubicTo(365f, 235f, 345f, 210f, 340f, 195f)
        close()
        // Left anterior deltoid
        moveTo(160f, 170f)
        cubicTo(130f, 175f, 115f, 200f, 125f, 235f)
        cubicTo(135f, 235f, 155f, 210f, 160f, 195f)
    }

    val latDeltPath: Path = buildPath {
        // Right lateral deltoid
        moveTo(375f, 170f)
        cubicTo(405f, 175f, 415f, 210f, 400f, 245f)
        cubicTo(390f, 235f, 380f, 200f, 375f, 185f)
        close()
        // Left lateral deltoid
        moveTo(125f, 170f)
        cubicTo(95f, 175f, 85f, 210f, 100f, 245f)
        cubicTo(110f, 235f, 120f, 200f, 125f, 185f)
    }

    val bicepsPath: Path = buildPath {
        // Right biceps
        moveTo(365f, 245f)
        cubicTo(390f, 260f, 385f, 310f, 370f, 345f)
        cubicTo(360f, 335f, 355f, 310f, 355f, 275f)
        close()
        // Left biceps
        moveTo(135f, 245f)
        cubicTo(110f, 260f, 115f, 310f, 130f, 345f)
        cubicTo(140f, 335f, 145f, 310f, 145f, 275f)
    }

    val forearmsAntPath: Path = buildPath {
        // Right forearm
        moveTo(380f, 360f)
        cubicTo(410f, 380f, 405f, 450f, 395f, 500f)
        cubicTo(385f, 495f, 375f, 430f, 370f, 375f)
        close()
        // Left forearm
        moveTo(120f, 360f)
        cubicTo(90f, 380f, 95f, 450f, 105f, 500f)
        cubicTo(115f, 495f, 125f, 430f, 130f, 375f)
    }

    val rectusAbsPath: Path = buildPath {
        moveTo(215f, 270f)
        lineTo(285f, 270f)
        cubicTo(290f, 320f, 285f, 380f, 280f, 430f)
        lineTo(220f, 430f)
        cubicTo(215f, 380f, 210f, 320f, 215f, 270f)
    }

    val obliquesPath: Path = buildPath {
        // Right obliques
        moveTo(290f, 270f)
        cubicTo(320f, 275f, 330f, 350f, 325f, 430f)
        lineTo(285f, 430f)
        cubicTo(285f, 350f, 290f, 310f, 290f, 270f)
        close()
        // Left obliques
        moveTo(210f, 270f)
        cubicTo(180f, 275f, 170f, 350f, 175f, 430f)
        lineTo(215f, 430f)
        cubicTo(215f, 350f, 210f, 310f, 210f, 270f)
    }

    val hipFlexorsPath: Path = buildPath {
        // Right hip flexor
        moveTo(260f, 440f)
        cubicTo(295f, 440f, 305f, 465f, 295f, 485f)
        lineTo(265f, 485f)
        close()
        // Left hip flexor
        moveTo(240f, 440f)
        cubicTo(205f, 440f, 195f, 465f, 205f, 485f)
        lineTo(235f, 485f)
    }

    val quadsPath: Path = buildPath {
        // Right quads
        moveTo(262f, 495f)
        cubicTo(280f, 495f, 330f, 530f, 325f, 620f)
        cubicTo(320f, 670f, 285f, 700f, 275f, 700f)
        cubicTo(268f, 680f, 260f, 570f, 262f, 495f)
        close()
        // Left quads
        moveTo(238f, 495f)
        cubicTo(220f, 495f, 170f, 530f, 175f, 620f)
        cubicTo(180f, 670f, 215f, 700f, 225f, 700f)
        cubicTo(232f, 680f, 240f, 570f, 238f, 495f)
    }

    val adductorsPath: Path = buildPath {
        // Right adductor
        moveTo(258f, 495f)
        lineTo(250f, 495f)
        lineTo(258f, 640f)
        cubicTo(265f, 640f, 262f, 580f, 258f, 495f)
        close()
        // Left adductor
        moveTo(242f, 495f)
        lineTo(250f, 495f)
        lineTo(242f, 640f)
        cubicTo(235f, 640f, 238f, 580f, 242f, 495f)
    }

    val abductorsPath: Path = buildPath {
        // Right abductor
        moveTo(321f, 485f)
        cubicTo(323f, 540f, 325f, 610f, 332f, 660f)
        cubicTo(345f, 630f, 340f, 520f, 326f, 485f)
        close()
        // Left abductor
        moveTo(179f, 485f)
        cubicTo(177f, 540f, 175f, 610f, 168f, 660f)
        cubicTo(155f, 630f, 160f, 520f, 174f, 485f)
    }

    val tibialisPath: Path = buildPath {
        // Right tibialis
        moveTo(285f, 725f)
        cubicTo(310f, 725f, 315f, 820f, 295f, 910f)
        lineTo(285f, 910f)
        close()
        // Left tibialis
        moveTo(215f, 725f)
        cubicTo(190f, 725f, 185f, 820f, 205f, 910f)
        lineTo(215f, 910f)
    }

    // --- NEW MODULAR REAR BODY REGIONS ---
    val postDeltPath: Path = buildPath {
        // Right posterior deltoid
        moveTo(355f, 175f)
        cubicTo(385f, 180f, 395f, 210f, 385f, 240f)
        cubicTo(375f, 240f, 360f, 215f, 355f, 195f)
        close()
        // Left posterior deltoid
        moveTo(145f, 175f)
        cubicTo(115f, 180f, 105f, 210f, 115f, 240f)
        cubicTo(125f, 240f, 140f, 215f, 145f, 195f)
    }

    val tricepsPath: Path = buildPath {
        // Right triceps
        moveTo(385f, 245f)
        cubicTo(410f, 255f, 415f, 310f, 395f, 350f)
        cubicTo(385f, 340f, 380f, 310f, 380f, 275f)
        close()
        // Left triceps
        moveTo(115f, 245f)
        cubicTo(90f, 255f, 85f, 310f, 105f, 350f)
        cubicTo(115f, 340f, 120f, 310f, 120f, 275f)
    }

    val upperTrapsPath: Path = buildPath {
        moveTo(215f, 115f)
        cubicTo(230f, 130f, 270f, 130f, 285f, 115f)
        lineTo(340f, 160f)
        cubicTo(290f, 160f, 210f, 160f, 160f, 160f)
    }

    val midTrapsPath: Path = buildPath {
        moveTo(195f, 160f)
        lineTo(305f, 160f)
        cubicTo(295f, 190f, 280f, 210f, 250f, 225f)
        cubicTo(220f, 210f, 205f, 190f, 195f, 160f)
    }

    val lowerTrapsPath: Path = buildPath {
        moveTo(215f, 225f)
        lineTo(285f, 225f)
        cubicTo(275f, 255f, 260f, 285f, 250f, 310f)
        cubicTo(240f, 285f, 225f, 255f, 215f, 225f)
    }

    val latsPath: Path = buildPath {
        // Right lat
        moveTo(295f, 225f)
        cubicTo(335f, 230f, 340f, 300f, 305f, 345f)
        cubicTo(295f, 335f, 285f, 290f, 295f, 225f)
        close()
        // Left lat
        moveTo(205f, 225f)
        cubicTo(165f, 230f, 160f, 300f, 195f, 345f)
        cubicTo(205f, 335f, 215f, 290f, 205f, 225f)
    }

    val rhomboidsPath: Path = buildPath {
        moveTo(205f, 190f)
        lineTo(295f, 190f)
        cubicTo(285f, 220f, 275f, 240f, 250f, 255f)
        cubicTo(225f, 240f, 215f, 220f, 205f, 190f)
    }

    val spinalErectorsPath: Path = buildPath {
        moveTo(230f, 325f)
        lineTo(270f, 325f)
        cubicTo(275f, 360f, 270f, 400f, 265f, 435f)
        lineTo(235f, 435f)
        cubicTo(230f, 400f, 225f, 360f, 230f, 325f)
    }

    val gluteMaxPath: Path = buildPath {
        // Right glute
        moveTo(252f, 440f)
        lineTo(325f, 440f)
        cubicTo(315f, 490f, 280f, 545f, 252f, 540f)
        close()
        // Left glute
        moveTo(248f, 440f)
        lineTo(175f, 440f)
        cubicTo(185f, 490f, 220f, 545f, 248f, 540f)
    }

    val gluteMedPath: Path = buildPath {
        // Right glute medius
        moveTo(252f, 420f)
        lineTo(335f, 420f)
        cubicTo(330f, 440f, 310f, 460f, 252f, 455f)
        close()
        // Left glute medius
        moveTo(248f, 420f)
        lineTo(175f, 420f)
        cubicTo(180f, 440f, 190f, 460f, 248f, 455f)
    }

    val hamstringsPath: Path = buildPath {
        // Right hamstring
        moveTo(260f, 550f)
        cubicTo(285f, 550f, 325f, 570f, 320f, 710f)
        cubicTo(305f, 710f, 280f, 660f, 260f, 550f)
        close()
        // Left hamstring
        moveTo(240f, 550f)
        cubicTo(215f, 550f, 175f, 570f, 180f, 710f)
        cubicTo(195f, 710f, 220f, 660f, 240f, 550f)
    }

    val calvesPath: Path = buildPath {
        // Right calf
        moveTo(272f, 715f)
        cubicTo(310f, 715f, 315f, 800f, 290f, 915f)
        lineTo(272f, 915f)
        close()
        // Left calf
        moveTo(228f, 715f)
        cubicTo(190f, 715f, 185f, 800f, 210f, 915f)
        lineTo(228f, 915f)
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
