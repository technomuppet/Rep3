package com.replog.domain.visual

import androidx.compose.ui.geometry.Offset
import com.replog.domain.visual.animation.BoneCatalog
import com.replog.domain.visual.animation.ForwardKinematicsSolver
import com.replog.domain.visual.animation.JointId
import com.replog.domain.visual.biomechanics.*
import com.replog.domain.visual.camera.CameraSystem
import com.replog.domain.visual.equipment.EquipmentEngine
import com.replog.domain.visual.spec.EquipmentSpec
import com.replog.domain.visual.spec.EquipmentType
import com.replog.domain.visual.spec.SupportType
import org.junit.Assert.*
import org.junit.Test

class CommercialBiomechanicsTest {

    @Test
    fun testBiomechanicalJointModelClamping() {
        // Pelvis tilt realistic -20..20 not -180
        assertEquals(20f, BiomechanicalJointModel.clamp(JointId.PELVIS, 100f), 0.01f)
        assertEquals(-20f, BiomechanicalJointModel.clamp(JointId.PELVIS, -100f), 0.01f)
        // Shoulder flexion -60..180
        assertTrue(BiomechanicalJointModel.isValid(JointId.LEFT_SHOULDER, 0f))
        assertFalse(BiomechanicalJointModel.isValid(JointId.LEFT_SHOULDER, 200f))
        // Elbow -5..145
        assertEquals(145f, BiomechanicalJointModel.clamp(JointId.LEFT_ELBOW, 200f), 0.01f)
    }

    @Test
    fun testCentreOfMassCalculation() {
        val solved = ForwardKinematicsSolver.solve(emptyMap())
        val result = CentreOfMassCalculator.calculate(solved)
        assertNotNull(result.comWorld)
        assertNotNull(result.midFootWorld)
        // COM should be above feet Y < foot Y? Actually COM Y should be above feet (smaller Y because Y down? World Y 0 top)
        // For standing, COM Y should be around 0.5-0.6, feet Y 0.9, so COM Y < feet Y
        println("DEBUG COM Y: ${result.comWorld.y}, LEFT FOOT Y: ${result.leftFoot.y}")
        assertTrue(result.comWorld.y < result.leftFoot.y)
    }

    @Test
    fun testIKSolverFABRIKReachable() {
        val shoulder = Offset(0.5f, 0.3f)
        val elbow = Offset(0.5f, 0.5f)
        val wrist = Offset(0.5f, 0.7f)
        val target = Offset(0.6f, 0.4f)
        val upperLen = IKSolver.boneLength(shoulder, elbow)
        val foreLen = IKSolver.boneLength(elbow, wrist)
        val (newElbow, newWrist) = IKSolver.solveTwoBoneArm(shoulder, elbow, wrist, target, upperLen, foreLen)
        assertNotNull(newElbow)
        assertNotNull(newWrist)
        // New wrist should be near target
        val dist = kotlin.math.hypot((newWrist.x - target.x).toDouble(), (newWrist.y - target.y).toDouble()).toFloat()
        assertTrue(dist < 0.05f)
    }

    @Test
    fun testBarPathEngineVertical() {
        val start = Offset(0.5f, 0.2f)
        val end = Offset(0.5f, 0.6f)
        val mid = BarPathEngine.calculateBarPosition(BarPathEngine.BarPathType.VERTICAL, start, end, 0.5f)
        assertTrue(mid.y > start.y && mid.y < end.y)
        val positions = listOf(start, mid, end)
        val validation = BarPathEngine.validateBarPath(BarPathEngine.BarPathType.VERTICAL, positions)
        assertTrue(validation.isValid)
    }

    @Test
    fun testStabilisationEngine() {
        val solved = ForwardKinematicsSolver.solve(emptyMap())
        val cues = StabilisationEngine.evaluate(solved, "SQUAT")
        assertNotNull(cues)
        assertTrue(cues.messages.isNotEmpty() || cues.balanced)
    }

    @Test
    fun testCommercialMotionLibrary() {
        val bench = CommercialMotionLibrary.benchPressTimeline()
        assertTrue(bench.durationSeconds > 0f)
        val pose = bench.evaluate(0f, 1f)
        assertNotNull(pose)
        // Check joint limits within realistic via BiomechanicalJointModel
        for ((id, rot) in pose.jointRotations) {
            assertTrue("Joint $id rot $rot out of realistic", BiomechanicalJointModel.isValid(id, rot))
        }
    }

    @Test
    fun testEquipmentEngine() {
        val renderers = EquipmentEngine.getAllRenderers()
        assertTrue(renderers.size >= 22)
        val spec = EquipmentSpec(type = EquipmentType.BARBELL, supportType = SupportType.STANDING)
        val renderer = EquipmentEngine.resolvePrimary(spec)
        assertNotNull(renderer)
    }

    @Test
    fun testCameraSystem() {
        val spec = com.replog.domain.visual.resolver.ExerciseVisualResolver.resolve(
            com.replog.data.model.Exercise(name = "Barbell Back Squat", category = "Legs", equipment = "Barbell", movementPattern = "Legs • Squat")
        )
        val view = CameraSystem.selectBestView(spec)
        assertEquals(CameraSystem.CameraView.RIGHT_SIDE, view) // squat -> side
        assertTrue(CameraSystem.shouldCullLeftSide(view))
    }

    @Test
    fun testMuscleActivationEngine() {
        val anatomySpec = com.replog.domain.visual.spec.AnatomySpec(primaryMuscles = setOf("Chest"), secondaryMuscles = setOf("Triceps"))
        val activations = com.replog.domain.visual.anatomy.MuscleActivationEngine.calculateActivations(anatomySpec, 0.5f, "HORIZONTAL_PUSH")
        assertTrue(activations.isNotEmpty())
        for (act in activations) {
            assertTrue(act.factor in 0.2f..1.0f)
        }
    }

    @Test
    fun testExerciseMotionValidator() {
        val exercise = com.replog.data.model.Exercise(name = "Barbell Bench Press", category = "Chest", equipment = "Barbell", primaryMuscles = "Chest", secondaryMuscles = "Triceps", movementPattern = "Push • Horizontal Press")
        val spec = com.replog.domain.visual.resolver.ExerciseVisualResolver.resolve(exercise)
        val skeleton = ForwardKinematicsSolver.solve(emptyMap())
        val report = ExerciseMotionValidator.validate(listOf(spec), listOf(skeleton)) { it }
        assertTrue(report.total > 0)
        assertTrue(report.passRate > 0f)
    }
}
