package com.replog.domain.visual.animation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.replog.domain.visual.body.HumanBodyRenderer
import com.replog.domain.visual.camera.CameraSystem
import com.replog.domain.visual.layered.LayeredRenderingPipeline
import com.replog.domain.visual.orientation.BodyOrientationEngine
import com.replog.domain.visual.spec.BodyOrientation
import com.replog.domain.visual.spec.EquipmentSpec
import com.replog.domain.visual.spec.EquipmentType
import com.replog.domain.visual.spec.ExerciseVisualSpec
import com.replog.domain.visual.spec.SupportType

private object SkeletalRenderStyles {
    val boneStrokeCap = StrokeCap.Round
}

object SkeletalRenderer {

    fun drawSkeleton(
        drawScope: DrawScope,
        skeleton: SolvedSkeleton,
        boneColor: Color,
        jointColor: Color,
        implementColor: Color,
        equipmentType: EquipmentType
    ) {
        val spec = ExerciseVisualSpec(
            equipment = EquipmentSpec(type = equipmentType, supportType = SupportType.STANDING),
            bodyOrientation = BodyOrientation.STANDING,
            supportType = SupportType.STANDING
        )
        drawCommercial(drawScope, skeleton, spec, boneColor, jointColor, implementColor, progress = 0.5f, cameraView = CameraSystem.CameraView.FRONT)
    }

    fun drawCommercial(
        drawScope: DrawScope,
        skeleton: SolvedSkeleton,
        spec: ExerciseVisualSpec,
        boneColor: Color,
        jointColor: Color,
        implementColor: Color,
        progress: Float = 0.5f,
        cameraView: CameraSystem.CameraView = CameraSystem.CameraView.AUTO
    ) {
        with(drawScope) {
            val width = size.width
            val height = size.height
            fun toScreen(pt: Offset) = Offset(width * pt.x, height * pt.y)
            val referenceSize = HumanBodyRenderer.computeReferenceSize(width, height)
            val context = LayeredRenderingPipeline.RenderContext(
                skeleton = skeleton,
                spec = spec,
                primaryColor = boneColor,
                tertiaryColor = implementColor,
                onSurface = jointColor,
                toScreen = { world -> toScreen(world) },
                referenceSize = referenceSize,
                progress = progress,
                cameraView = cameraView
            )
            LayeredRenderingPipeline.run {
                this@with.drawPipeline(context)
            }
        }
    }
}

fun DrawScope.drawSkeletonCommercial(
    skeleton: SolvedSkeleton,
    spec: ExerciseVisualSpec,
    boneColor: Color,
    jointColor: Color,
    implementColor: Color,
    progress: Float = 0.5f,
    cameraView: CameraSystem.CameraView = CameraSystem.CameraView.AUTO
) {
    SkeletalRenderer.drawCommercial(this, skeleton, spec, boneColor, jointColor, implementColor, progress, cameraView)
}
