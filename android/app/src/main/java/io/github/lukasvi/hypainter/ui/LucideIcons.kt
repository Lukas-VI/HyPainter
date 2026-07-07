package io.github.lukasvi.hypainter.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

// Local subset keeps HUD iteration independent from third-party icon library availability.
internal object LucideIcons {
    val Menu = lucide("Menu") {
        moveTo(4f, 6f)
        lineTo(20f, 6f)
        moveTo(4f, 12f)
        lineTo(20f, 12f)
        moveTo(4f, 18f)
        lineTo(20f, 18f)
    }


    val Palette = lucide("Palette") {
        moveTo(12f, 3f)
        curveTo(7f, 3f, 3f, 6.8f, 3f, 11.5f)
        curveTo(3f, 16.3f, 6.9f, 20f, 11.5f, 20f)
        lineTo(13.3f, 20f)
        curveTo(14.7f, 20f, 15.4f, 18.3f, 14.4f, 17.3f)
        curveTo(13.5f, 16.4f, 14.1f, 15f, 15.4f, 15f)
        lineTo(17f, 15f)
        curveTo(19.2f, 15f, 21f, 13.2f, 21f, 11f)
        curveTo(21f, 6.6f, 17f, 3f, 12f, 3f)
        moveTo(7.5f, 10f)
        lineTo(7.5f, 10.1f)
        moveTo(10f, 7f)
        lineTo(10f, 7.1f)
        moveTo(14f, 7f)
        lineTo(14f, 7.1f)
        moveTo(16.5f, 10f)
        lineTo(16.5f, 10.1f)
    }


    val BrushLibrary = lucide("BrushLibrary") {
        moveTo(4f, 5f)
        curveTo(6f, 4f, 8f, 4f, 10f, 5f)
        lineTo(10f, 19f)
        curveTo(8f, 18f, 6f, 18f, 4f, 19f)
        close()
        moveTo(14f, 5f)
        curveTo(16f, 4f, 18f, 4f, 20f, 5f)
        lineTo(20f, 19f)
        curveTo(18f, 18f, 16f, 18f, 14f, 19f)
        close()
        moveTo(10f, 5f)
        lineTo(14f, 5f)
        moveTo(10f, 19f)
        lineTo(14f, 19f)
    }

    val Brush = lucide("Brush") {
        moveTo(15f, 4f)
        lineTo(20f, 9f)
        lineTo(10f, 19f)
        lineTo(5f, 19f)
        lineTo(5f, 14f)
        close()
        moveTo(13f, 6f)
        lineTo(18f, 11f)
    }

    val Pen = lucide("Pen") {
        moveTo(4f, 20f)
        lineTo(8f, 19f)
        lineTo(19f, 8f)
        lineTo(16f, 5f)
        lineTo(5f, 16f)
        close()
        moveTo(13f, 8f)
        lineTo(16f, 11f)
    }

    val Droplet = lucide("Droplet") {
        moveTo(12f, 3f)
        curveTo(8f, 8f, 6f, 11f, 6f, 15f)
        curveTo(6f, 18.3f, 8.7f, 21f, 12f, 21f)
        curveTo(15.3f, 21f, 18f, 18.3f, 18f, 15f)
        curveTo(18f, 11f, 16f, 8f, 12f, 3f)
    }

    val Size = lucide("Size") {
        moveTo(5f, 19f)
        lineTo(19f, 5f)
        moveTo(6f, 9f)
        curveTo(6f, 7.3f, 7.3f, 6f, 9f, 6f)
        curveTo(10.7f, 6f, 12f, 7.3f, 12f, 9f)
        curveTo(12f, 10.7f, 10.7f, 12f, 9f, 12f)
        curveTo(7.3f, 12f, 6f, 10.7f, 6f, 9f)
        moveTo(14f, 16f)
        curveTo(14f, 14.9f, 14.9f, 14f, 16f, 14f)
        curveTo(17.1f, 14f, 18f, 14.9f, 18f, 16f)
        curveTo(18f, 17.1f, 17.1f, 18f, 16f, 18f)
        curveTo(14.9f, 18f, 14f, 17.1f, 14f, 16f)
    }

    val Layers2 = lucide("Layers2") {
        moveTo(13f, 13.74f)
        arcToRelative(2f, 2f, 0f, false, true, -2f, 0f)
        lineTo(2.5f, 8.87f)
        arcToRelative(1f, 1f, 0f, false, true, 0f, -1.74f)
        lineTo(11f, 2.26f)
        arcToRelative(2f, 2f, 0f, false, true, 2f, 0f)
        lineToRelative(8.5f, 4.87f)
        arcToRelative(1f, 1f, 0f, false, true, 0f, 1.74f)
        close()
        moveTo(20f, 14.285f)
        lineToRelative(1.5f, .845f)
        arcToRelative(1f, 1f, 0f, false, true, 0f, 1.74f)
        lineTo(13f, 21.74f)
        arcToRelative(2f, 2f, 0f, false, true, -2f, 0f)
        lineToRelative(-8.5f, -4.87f)
        arcToRelative(1f, 1f, 0f, false, true, 0f, -1.74f)
        lineToRelative(1.5f, -.845f)
    }

    val SquareDashedMousePointer = lucide("SquareDashedMousePointer") {
        moveTo(12.034f, 12.681f)
        arcToRelative(.498f, .498f, 0f, false, true, .647f, -.647f)
        lineToRelative(9f, 3.5f)
        arcToRelative(.5f, .5f, 0f, false, true, -.033f, .943f)
        lineToRelative(-3.444f, 1.068f)
        arcToRelative(1f, 1f, 0f, false, false, -.66f, .66f)
        lineToRelative(-1.067f, 3.443f)
        arcToRelative(.5f, .5f, 0f, false, true, -.943f, .033f)
        close()
        moveTo(5f, 3f)
        arcToRelative(2f, 2f, 0f, false, false, -2f, 2f)
        moveTo(19f, 3f)
        arcToRelative(2f, 2f, 0f, false, true, 2f, 2f)
        moveTo(5f, 21f)
        arcToRelative(2f, 2f, 0f, false, true, -2f, -2f)
        moveTo(9f, 3f)
        lineTo(10f, 3f)
        moveTo(9f, 21f)
        lineTo(11f, 21f)
        moveTo(14f, 3f)
        lineTo(15f, 3f)
        moveTo(3f, 9f)
        lineTo(3f, 10f)
        moveTo(21f, 9f)
        lineTo(21f, 11f)
        moveTo(3f, 14f)
        lineTo(3f, 15f)
    }

    val VectorSquare = lucide("VectorSquare") {
        moveTo(19.5f, 7f)
        arcToRelative(24f, 24f, 0f, false, true, 0f, 10f)
        moveTo(4.5f, 7f)
        arcToRelative(24f, 24f, 0f, false, false, 0f, 10f)
        moveTo(7f, 19.5f)
        arcToRelative(24f, 24f, 0f, false, false, 10f, 0f)
        moveTo(7f, 4.5f)
        arcToRelative(24f, 24f, 0f, false, true, 10f, 0f)
        moveTo(18f, 17f)
        lineTo(21f, 17f)
        arcToRelative(1f, 0f, 0f, false, true, 1f, 0f)
        lineTo(22f, 22f)
        arcToRelative(1f, 0f, 0f, false, true, -1f, 0f)
        lineTo(18f, 22f)
        arcToRelative(1f, 0f, 0f, false, true, -1f, 0f)
        lineTo(17f, 17f)
        arcToRelative(1f, 0f, 0f, false, true, 1f, 0f)
        close()
        moveTo(18f, 2f)
        lineTo(21f, 2f)
        arcToRelative(1f, 0f, 0f, false, true, 1f, 0f)
        lineTo(22f, 7f)
        arcToRelative(1f, 0f, 0f, false, true, -1f, 0f)
        lineTo(18f, 7f)
        arcToRelative(1f, 0f, 0f, false, true, -1f, 0f)
        lineTo(17f, 2f)
        arcToRelative(1f, 0f, 0f, false, true, 1f, 0f)
        close()
        moveTo(3f, 17f)
        lineTo(6f, 17f)
        arcToRelative(1f, 0f, 0f, false, true, 1f, 0f)
        lineTo(7f, 22f)
        arcToRelative(1f, 0f, 0f, false, true, -1f, 0f)
        lineTo(3f, 22f)
        arcToRelative(1f, 0f, 0f, false, true, -1f, 0f)
        lineTo(2f, 17f)
        arcToRelative(1f, 0f, 0f, false, true, 1f, 0f)
        close()
        moveTo(3f, 2f)
        lineTo(6f, 2f)
        arcToRelative(1f, 0f, 0f, false, true, 1f, 0f)
        lineTo(7f, 7f)
        arcToRelative(1f, 0f, 0f, false, true, -1f, 0f)
        lineTo(3f, 7f)
        arcToRelative(1f, 0f, 0f, false, true, -1f, 0f)
        lineTo(2f, 7f)
        arcToRelative(1f, 0f, 0f, false, true, 1f, 0f)
        close()
    }

    val Expand = lucide("Expand") {
        moveTo(15f, 15f)
        lineToRelative(6f, 6f)
        moveTo(15f, 9f)
        lineToRelative(6f, -6f)
        moveTo(21f, 16f)
        lineTo(21f, 21f)
        lineTo(16f, 21f)
        moveTo(21f, 8f)
        lineTo(21f, 3f)
        lineTo(16f, 3f)
        moveTo(3f, 16f)
        lineTo(3f, 21f)
        lineTo(8f, 21f)
        moveTo(3f, 21f)
        lineToRelative(6f, -6f)
        moveTo(3f, 8f)
        lineTo(3f, 3f)
        lineTo(8f, 3f)
        moveTo(9f, 9f)
        lineTo(3f, 3f)
    }

    val PencilRuler = lucide("PencilRuler") {
        moveTo(13f, 7f)
        lineTo(8.7f, 2.7f)
        arcToRelative(2.41f, 2.41f, 0f, false, false, -3.4f, 0f)
        lineTo(2.7f, 5.3f)
        arcToRelative(2.41f, 2.41f, 0f, false, false, 0f, 3.4f)
        lineTo(7f, 13f)
        moveTo(8f, 6f)
        lineToRelative(2f, -2f)
        moveTo(18f, 16f)
        lineToRelative(2f, -2f)
        moveTo(17f, 11f)
        lineToRelative(4.3f, 4.3f)
        curveTo(.94f, .94f, .94f, 2.46f, 0f, 3.4f)
        lineToRelative(-2.6f, 2.6f)
        curveTo(-.94f, .94f, -2.46f, .94f, -3.4f, 0f)
        lineTo(11f, 17f)
        moveTo(21.174f, 6.812f)
        arcToRelative(1f, 1f, 0f, false, false, -3.986f, -3.987f)
        lineTo(3.842f, 16.174f)
        arcToRelative(2f, 2f, 0f, false, false, -.5f, .83f)
        lineToRelative(-1.321f, 4.352f)
        arcToRelative(.5f, .5f, 0f, false, false, .623f, .622f)
        lineToRelative(4.353f, -1.32f)
        arcToRelative(2f, 2f, 0f, false, false, .83f, -.497f)
        close()
        moveTo(15f, 5f)
        lineToRelative(4f, 4f)
    }

}

private fun lucide(name: String, block: PathBuilder.() -> Unit): ImageVector {
    return ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            fill = SolidColor(Color.Transparent),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = block,
        )
    }.build()
}
