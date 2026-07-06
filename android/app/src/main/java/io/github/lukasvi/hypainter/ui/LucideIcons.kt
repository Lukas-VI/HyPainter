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

    val Selection = lucide("Selection") {
        moveTo(4f, 6f)
        lineTo(4f, 4f)
        lineTo(6f, 4f)
        moveTo(18f, 4f)
        lineTo(20f, 4f)
        lineTo(20f, 6f)
        moveTo(20f, 18f)
        lineTo(20f, 20f)
        lineTo(18f, 20f)
        moveTo(6f, 20f)
        lineTo(4f, 20f)
        lineTo(4f, 18f)
        moveTo(9f, 8f)
        lineTo(15f, 14f)
        moveTo(15f, 14f)
        lineTo(11f, 14f)
        moveTo(15f, 14f)
        lineTo(15f, 10f)
    }

    val Transform = lucide("Transform") {
        moveTo(12f, 3f)
        lineTo(12f, 21f)
        moveTo(7f, 8f)
        lineTo(12f, 3f)
        lineTo(17f, 8f)
        moveTo(7f, 16f)
        lineTo(12f, 21f)
        lineTo(17f, 16f)
        moveTo(3f, 12f)
        lineTo(21f, 12f)
        moveTo(8f, 7f)
        lineTo(3f, 12f)
        lineTo(8f, 17f)
        moveTo(16f, 7f)
        lineTo(21f, 12f)
        lineTo(16f, 17f)
    }

    val Tool = lucide("Tool") {
        moveTo(14f, 5f)
        lineTo(19f, 10f)
        moveTo(4f, 20f)
        lineTo(14f, 10f)
        moveTo(12f, 8f)
        lineTo(16f, 4f)
        lineTo(20f, 8f)
        lineTo(16f, 12f)
        moveTo(6f, 18f)
        lineTo(8f, 20f)
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

    val Layers = lucide("Layers") {
        moveTo(12f, 3f)
        lineTo(21f, 8f)
        lineTo(12f, 13f)
        lineTo(3f, 8f)
        close()
        moveTo(3f, 13f)
        lineTo(12f, 18f)
        lineTo(21f, 13f)
        moveTo(3f, 17f)
        lineTo(12f, 22f)
        lineTo(21f, 17f)
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
