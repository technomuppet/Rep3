package com.replog.domain.visual.anatomy

import androidx.compose.ui.graphics.Path

/**
 * Immutable scalable front-body vector geometry defined over a 500x1000 coordinate grid.
 * All paths are pre-compiled and cached to guarantee zero allocations during rendering.
 */
object FrontBody {

    val silhouettePath: Path = AnatomyGeometry.buildPath {
        // Head & Neck
        moveTo(250f, 30f)
        cubicTo(290f, 30f, 300f, 75f, 290f, 115f)
        lineTo(280f, 150f)
        // Right Shoulder & Arm
        lineTo(390f, 165f)
        cubicTo(415f, 185f, 420f, 240f, 415f, 350f)
        lineTo(435f, 510f)
        lineTo(440f, 565f)
        lineTo(410f, 565f)
        lineTo(395f, 510f)
        lineTo(375f, 355f)
        lineTo(345f, 215f)
        // Right Torso & Waist
        lineTo(335f, 320f)
        lineTo(325f, 440f)
        // Right Leg
        lineTo(340f, 550f)
        cubicTo(345f, 620f, 340f, 690f, 330f, 715f)
        lineTo(330f, 925f)
        lineTo(350f, 965f)
        lineTo(280f, 965f)
        lineTo(270f, 925f)
        lineTo(265f, 715f)
        lineTo(255f, 480f)
        // Crotch Center
        lineTo(245f, 480f)
        // Left Leg
        lineTo(235f, 715f)
        lineTo(230f, 925f)
        lineTo(220f, 965f)
        lineTo(150f, 965f)
        lineTo(170f, 925f)
        lineTo(170f, 715f)
        cubicTo(160f, 690f, 155f, 620f, 160f, 550f)
        lineTo(175f, 440f)
        // Left Torso & Waist
        lineTo(165f, 320f)
        lineTo(155f, 215f)
        // Left Arm & Shoulder
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

    val regionPaths: Map<MuscleRegion, Path> = mapOf(
        MuscleRegion.CHEST to AnatomyGeometry.buildPath {
            // Left Pec
            moveTo(165f, 175f)
            lineTo(245f, 175f)
            lineTo(245f, 260f)
            cubicTo(215f, 270f, 175f, 255f, 160f, 230f)
            // Right Pec
            moveTo(255f, 175f)
            lineTo(335f, 175f)
            lineTo(340f, 230f)
            cubicTo(325f, 255f, 285f, 270f, 255f, 260f)
        },
        MuscleRegion.UPPER_CHEST to AnatomyGeometry.buildPath {
            // Left Upper Pec
            moveTo(170f, 155f)
            lineTo(245f, 155f)
            lineTo(245f, 185f)
            lineTo(165f, 185f)
            // Right Upper Pec
            moveTo(255f, 155f)
            lineTo(330f, 155f)
            lineTo(335f, 185f)
            lineTo(255f, 185f)
        },
        MuscleRegion.ANTERIOR_DELTOID to AnatomyGeometry.buildPath {
            // Left Front Delt
            moveTo(110f, 165f)
            lineTo(155f, 170f)
            lineTo(145f, 235f)
            lineTo(100f, 220f)
            // Right Front Delt
            moveTo(390f, 165f)
            lineTo(345f, 170f)
            lineTo(355f, 235f)
            lineTo(400f, 220f)
        },
        MuscleRegion.LATERAL_DELTOID to AnatomyGeometry.buildPath {
            // Left Side Delt
            moveTo(85f, 175f)
            lineTo(105f, 170f)
            lineTo(95f, 245f)
            lineTo(80f, 235f)
            // Right Side Delt
            moveTo(415f, 175f)
            lineTo(395f, 170f)
            lineTo(405f, 245f)
            lineTo(420f, 235f)
        },
        MuscleRegion.BICEPS to AnatomyGeometry.buildPath {
            // Left Bicep
            moveTo(95f, 245f)
            lineTo(135f, 250f)
            lineTo(125f, 350f)
            lineTo(90f, 345f)
            // Right Bicep
            moveTo(405f, 245f)
            lineTo(365f, 250f)
            lineTo(375f, 350f)
            lineTo(410f, 345f)
        },
        MuscleRegion.FOREARMS_ANTERIOR to AnatomyGeometry.buildPath {
            // Left Forearm
            moveTo(85f, 365f)
            lineTo(120f, 365f)
            lineTo(105f, 500f)
            lineTo(75f, 495f)
            // Right Forearm
            moveTo(415f, 365f)
            lineTo(380f, 365f)
            lineTo(395f, 500f)
            lineTo(425f, 495f)
        },
        MuscleRegion.RECTUS_ABDOMINIS to AnatomyGeometry.buildPath {
            // Abs Center Tiered
            moveTo(215f, 275f)
            lineTo(285f, 275f)
            lineTo(280f, 430f)
            lineTo(220f, 430f)
        },
        MuscleRegion.OBLIQUES to AnatomyGeometry.buildPath {
            // Left Oblique
            moveTo(165f, 275f)
            lineTo(210f, 275f)
            lineTo(215f, 430f)
            lineTo(175f, 430f)
            // Right Oblique
            moveTo(335f, 275f)
            lineTo(290f, 275f)
            lineTo(285f, 430f)
            lineTo(325f, 430f)
        },
        MuscleRegion.HIP_FLEXORS to AnatomyGeometry.buildPath {
            // Left Hip Flexor
            moveTo(180f, 440f)
            lineTo(240f, 440f)
            lineTo(235f, 485f)
            lineTo(175f, 480f)
            // Right Hip Flexor
            moveTo(320f, 440f)
            lineTo(260f, 440f)
            lineTo(265f, 485f)
            lineTo(325f, 480f)
        },
        MuscleRegion.QUADRICEPS to AnatomyGeometry.buildPath {
            // Left Quad
            moveTo(175f, 495f)
            lineTo(235f, 495f)
            lineTo(225f, 700f)
            lineTo(165f, 700f)
            // Right Quad
            moveTo(325f, 495f)
            lineTo(265f, 495f)
            lineTo(275f, 700f)
            lineTo(335f, 700f)
        },
        MuscleRegion.ADDUCTORS to AnatomyGeometry.buildPath {
            // Left Adductor
            moveTo(235f, 490f)
            lineTo(250f, 490f)
            lineTo(240f, 640f)
            lineTo(228f, 640f)
            // Right Adductor
            moveTo(265f, 490f)
            lineTo(250f, 490f)
            lineTo(260f, 640f)
            lineTo(272f, 640f)
        },
        MuscleRegion.ABDUCTORS to AnatomyGeometry.buildPath {
            // Left Outer Thigh
            moveTo(165f, 485f)
            lineTo(178f, 485f)
            lineTo(168f, 660f)
            lineTo(155f, 630f)
            // Right Outer Thigh
            moveTo(335f, 485f)
            lineTo(322f, 485f)
            lineTo(332f, 660f)
            lineTo(345f, 630f)
        },
        MuscleRegion.TIBIALIS_ANTERIOR to AnatomyGeometry.buildPath {
            // Left Shin
            moveTo(175f, 725f)
            lineTo(215f, 725f)
            lineTo(205f, 910f)
            lineTo(180f, 910f)
            // Right Shin
            moveTo(325f, 725f)
            lineTo(285f, 725f)
            lineTo(295f, 910f)
            lineTo(320f, 910f)
        }
    )
}
