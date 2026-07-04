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
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeCap = Paint.Cap.ROUND
    }

    val renderedImage: ImageBitmap?
        get() = image

    fun append(stroke: EngineStroke, layers: List<EngineLayer>) {
        if (!isLayerVisible(stroke.layerId, layers)) {
            ensureBitmap()
            return
        }
        drawStroke(ensureCanvas(), stroke)
    }

    fun appendSegment(from: EngineSample, to: EngineSample, brush: EngineBrush, layerId: Long, layers: List<EngineLayer>) {
        if (!isLayerVisible(layerId, layers)) {
            ensureBitmap()
            return
        }
        drawSegment(ensureCanvas(), from, to, brush)
    }

    fun appendPoint(sample: EngineSample, brush: EngineBrush, layerId: Long, layers: List<EngineLayer>) {
        if (!isLayerVisible(layerId, layers)) {
            ensureBitmap()
            return
        }
        drawPoint(ensureCanvas(), sample, brush)
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
            image = it.asImageBitmap()
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
        for (index in 1 until stroke.points.size) {
            drawSegment(canvas, stroke.points[index - 1], stroke.points[index], stroke.brush)
        }

        stroke.points.singleOrNull()?.let { point ->
            drawPoint(canvas, point, stroke.brush)
        }
    }

    private fun drawSegment(canvas: Canvas, from: EngineSample, to: EngineSample, brush: EngineBrush) {
        val pressure = to.pressure.coerceIn(0.1f, 1f)
        paint.color = brush.colorArgb
        paint.alpha = (pressure * 255).toInt()
        paint.strokeWidth = brush.radiusPx * 2f * pressure
        canvas.drawLine(
            from.position.x,
            from.position.y,
            to.position.x,
            to.position.y,
            paint,
        )
    }

    private fun drawPoint(canvas: Canvas, sample: EngineSample, brush: EngineBrush) {
        val pressure = sample.pressure.coerceIn(0.1f, 1f)
        paint.color = brush.colorArgb
        paint.alpha = (pressure * 255).toInt()
        canvas.drawCircle(
            sample.position.x,
            sample.position.y,
            brush.radiusPx * pressure,
            paint,
        )
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
