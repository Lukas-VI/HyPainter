package io.github.lukasvi.hypainter.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

internal class StrokeRasterCache(
    private val width: Int,
    private val height: Int,
) {
    private var bitmap: Bitmap? = null
    private var canvas: Canvas? = null
    private var image: ImageBitmap? = null

    val renderedImage: ImageBitmap?
        get() = image

    fun append(stroke: EngineStroke, layers: List<EngineLayer>) {
        if (!isLayerVisible(stroke.layerId, layers)) {
            ensureBitmap()
            return
        }
        drawStroke(ensureCanvas(), stroke)
        image = ensureBitmap().asImageBitmap()
    }

    fun rebuild(strokes: List<EngineStroke>, layers: List<EngineLayer>) {
        clearPixels()
        val target = ensureCanvas()
        for (index in strokes.indices) {
            val stroke = strokes[index]
            if (isLayerVisible(stroke.layerId, layers)) {
                drawStroke(target, stroke)
            }
        }
        image = ensureBitmap().asImageBitmap()
    }

    fun clear() {
        bitmap = null
        canvas = null
        image = null
    }

    private fun ensureBitmap(): Bitmap {
        return bitmap ?: Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also {
            bitmap = it
            canvas = Canvas(it)
        }
    }

    private fun ensureCanvas(): Canvas {
        ensureBitmap()
        return checkNotNull(canvas)
    }

    private fun clearPixels() {
        ensureBitmap().eraseColor(Color.TRANSPARENT)
    }

    private fun drawStroke(canvas: Canvas, stroke: EngineStroke) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = stroke.brush.colorArgb
            strokeWidth = stroke.brush.radiusPx * 2f
            strokeCap = Paint.Cap.ROUND
        }

        for (index in 1 until stroke.points.size) {
            val from = stroke.points[index - 1]
            val to = stroke.points[index]
            val pressure = to.pressure.coerceIn(0.1f, 1f)
            paint.alpha = (pressure * 255).toInt()
            paint.strokeWidth = stroke.brush.radiusPx * 2f * pressure
            canvas.drawLine(
                from.position.x,
                from.position.y,
                to.position.x,
                to.position.y,
                paint,
            )
        }

        stroke.points.singleOrNull()?.let { point ->
            val pressure = point.pressure.coerceIn(0.1f, 1f)
            paint.alpha = (pressure * 255).toInt()
            canvas.drawCircle(
                point.position.x,
                point.position.y,
                stroke.brush.radiusPx * pressure,
                paint,
            )
        }
    }
}

private fun isLayerVisible(layerId: Long, layers: List<EngineLayer>): Boolean {
    for (index in layers.indices) {
        val layer = layers[index]
        if (layer.id == layerId) {
            return layer.visible
        }
    }
    return false
}
