package io.github.lukasvi.hypainter.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color as AndroidColor
import java.io.FileOutputStream

class KotlinPaintingEngine(
    private val canvasWidth: Int = 1024,
    private val canvasHeight: Int = 1024,
) : PaintingEngine {
    override val nativeBacked: Boolean = false

    private val committedStrokes = mutableListOf<EngineStroke>()
    private var activeStroke = mutableListOf<EngineSample>()
    private var brush = EngineBrush(colorArgb = AndroidColor.BLACK, radiusPx = 8f)

    override fun beginStroke(sample: EngineSample) {
        activeStroke = mutableListOf(sample)
    }

    override fun appendSample(sample: EngineSample) {
        if (activeStroke.isEmpty()) {
            beginStroke(sample)
            return
        }

        activeStroke.add(sample)
    }

    override fun endStroke() {
        if (activeStroke.isNotEmpty()) {
            committedStrokes.add(EngineStroke(activeStroke.toList(), brush))
            activeStroke = mutableListOf()
        }
    }

    override fun undo() {
        if (committedStrokes.isNotEmpty()) {
            committedStrokes.removeAt(committedStrokes.lastIndex)
        }
    }

    override fun clear() {
        committedStrokes.clear()
        activeStroke.clear()
    }

    override fun setBrush(brush: EngineBrush) {
        this.brush = brush
    }

    override fun exportPng(path: String): Boolean {
        return runCatching {
            FileOutputStream(path).use { output ->
                renderBitmap().compress(Bitmap.CompressFormat.PNG, 100, output)
            }
        }.getOrDefault(false)
    }

    override fun snapshot(): EngineSnapshot {
        return EngineSnapshot(
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            brush = brush,
            committedStrokes = committedStrokes.toList(),
            activeStroke = activeStroke.takeIf { it.isNotEmpty() }?.let { EngineStroke(it.toList(), brush) },
        )
    }

    private fun renderBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(AndroidColor.WHITE)
        committedStrokes.forEach { stroke ->
            drawStroke(canvas, stroke)
        }
        activeStroke.takeIf { it.isNotEmpty() }?.let { points ->
            drawStroke(canvas, EngineStroke(points.toList(), brush))
        }
        return bitmap
    }

    private fun drawStroke(canvas: Canvas, stroke: EngineStroke) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = stroke.brush.colorArgb
            strokeWidth = stroke.brush.radiusPx * 2f
            strokeCap = Paint.Cap.ROUND
        }

        stroke.points.zipWithNext().forEach { (from, to) ->
            paint.alpha = ((to.pressure.coerceIn(0.1f, 1f)) * 255).toInt()
            canvas.drawLine(
                from.position.x,
                from.position.y,
                to.position.x,
                to.position.y,
                paint,
            )
        }
    }
}
