package com.replog.domain.visual.anatomy

import androidx.compose.ui.graphics.Path

/**
 * Immutable scalable posterior (back) vector geometry defined over a 500x1000 coordinate grid.
 * Pre-compiled paths guarantee zero object allocations during Canvas draw operations.
 */
object BackBody {

    val silhouettePath: Path = AnatomyGeometry.buildPath {
        // Head & Posterior Neck
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
        // Right Back Torso & Waist
        lineTo(335f, 320f)
        lineTo(325f, 440f)
        // Right Glute & Leg
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
        // Left Glute & Leg
        lineTo(235f, 715f)
        lineTo(230f, 925f)
        lineTo(220f, 965f)
        lineTo(150f, 965f)
        lineTo(170f, 925f)
        lineTo(170f, 715f)
        cubicTo(160f, 690f, 155f, 620f, 160f, 550f)
        lineTo(175f, 440f)
        // Left Back Torso & Waist
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
        MuscleRegion.UPPER_TRAPEZIUS to AnatomyGeometry.buildPath {
            // Upper Traps Neck to Shoulder Slope
            moveTo(215f, 125f)
            lineTo(285f, 125f)
            lineTo(345f, 175f)
            lineTo(250f, 195f)
            lineTo(155f, 175f)
        },
        MuscleRegion.MIDDLE_TRAPEZIUS to AnatomyGeometry.buildPath {
            // Mid Traps Diamond Across Scapulae
            moveTo(250f, 195f)
            lineTo(320f, 230f)
            lineTo(250f, 275f)
            lineTo(180f, 230f)
        },
        MuscleRegion.LOWER_TRAPEZIUS to AnatomyGeometry.buildPath {
            // Lower Traps Tapering Down Mid Spine
            moveTo(250f, 275f)
            lineTo(290f, 260f)
            lineTo(250f, 350f)
            lineTo(210f, 260f)
        },
        MuscleRegion.POSTERIOR_DELTOID to AnatomyGeometry.buildPath {
            // Left Rear Delt
            moveTo(110f, 170f)
            lineTo(155f, 175f)
            lineTo(145f, 240f)
            lineTo(100f, 225f)
            // Right Rear Delt
            moveTo(390f, 170f)
            lineTo(345f, 175f)
            lineTo(355f, 240f)
            lineTo(400f, 225f)
        },
        MuscleRegion.TRICEPS to AnatomyGeometry.buildPath {
            // Left Tricep
            moveTo(95f, 245f)
            lineTo(135f, 250f)
            lineTo(125f, 355f)
            lineTo(90f, 350f)
            // Right Tricep
            moveTo(405f, 245f)
            lineTo(365f, 250f)
            lineTo(375f, 355f)
            lineTo(410f, 350f)
        },
        MuscleRegion.FOREARMS_POSTERIOR to AnatomyGeometry.buildPath {
            // Left Posterior Forearm
            moveTo(85f, 365f)
            lineTo(120f, 365f)
            lineTo(105f, 500f)
            lineTo(75f, 495f)
            // Right Posterior Forearm
            moveTo(415f, 365f)
            lineTo(380f, 365f)
            lineTo(395f, 500f)
            lineTo(425f, 495f)
        },
        MuscleRegion.RHOMBOIDS to AnatomyGeometry.buildPath {
            // Beneath Mid Traps Scapular Border
            moveTo(180f, 215f)
            lineTo(240f, 225f)
            lineTo(240f, 265f)
            lineTo(175f, 250f)
            moveTo(320f, 215f)
            lineTo(260f, 225f)
            lineTo(260f, 265f)
            lineTo(325f, 250f)
        },
        MuscleRegion.TERES_MAJOR to AnatomyGeometry.buildPath {
            // Upper Lateral Scapular Border
            moveTo(145f, 245f)
            lineTo(175f, 255f)
            lineTo(170f, 290f)
            lineTo(135f, 280f)
            moveTo(355f, 245f)
            lineTo(325f, 255f)
            lineTo(330f, 290f)
            lineTo(365f, 280f)
        },
        MuscleRegion.LATISSIMUS_DORSI to AnatomyGeometry.buildPath {
            // Left Lat Sweep
            moveTo(165f, 265f)
            lineTo(235f, 285f)
            lineTo(225f, 410f)
            lineTo(170f, 390f)
            // Right Lat Sweep
            moveTo(335f, 265f)
            lineTo(265f, 285f)
            lineTo(275f, 410f)
            lineTo(330f, 390f)
        },
        MuscleRegion.SPINAL_ERECTORS to AnatomyGeometry.buildPath {
            // Lower Back Erectors Column
            moveTo(225f, 355f)
            lineTo(275f, 355f)
            lineTo(275f, 445f)
            lineTo(225f, 445f)
        },
        MuscleRegion.GLUTE_MAXIMUS to AnatomyGeometry.buildPath {
            // Left Glute Max
            moveTo(175f, 455f)
            lineTo(245f, 455f)
            lineTo(240f, 560f)
            lineTo(170f, 545f)
            // Right Glute Max
            moveTo(325f, 455f)
            lineTo(255f, 455f)
            lineTo(260f, 560f)
            lineTo(330f, 545f)
        },
        MuscleRegion.GLUTE_MEDIUS to AnatomyGeometry.buildPath {
            // Upper Hip Outer Shelf
            moveTo(165f, 435f)
            lineTo(195f, 440f)
            lineTo(185f, 480f)
            lineTo(155f, 470f)
            moveTo(335f, 435f)
            lineTo(305f, 440f)
            lineTo(315f, 480f)
            lineTo(345f, 470f)
        },
        MuscleRegion.HAMSTRINGS to AnatomyGeometry.buildPath {
            // Left Hamstring
            moveTo(175f, 565f)
            lineTo(235f, 565f)
            lineTo(225f, 705f)
            lineTo(170f, 705f)
            // Right Hamstring
            moveTo(325f, 565f)
            lineTo(265f, 565f)
            lineTo(275f, 705f)
            lineTo(330f, 705f)
        },
        MuscleRegion.CALVES to AnatomyGeometry.buildPath {
            // Left Calf Gastrocnemius & Soleus
            moveTo(175f, 725f)
            lineTo(225f, 725f)
            lineTo(215f, 880f)
            lineTo(180f, 880f)
            // Right Calf Gastrocnemius & Soleus
            moveTo(325f, 725f)
            lineTo(275f, 725f)
            lineTo(285f, 880f)
            lineTo(320f, 880f)
        }
    )
}
