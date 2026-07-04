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
    private val layers = mutableListOf(EngineLayer(id = 1L, name = "Layer 1", visible = true))
    private var activeLayerId = 1L
    private var nextLayerId = 2L

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
            committedStrokes.add(EngineStroke(activeStroke.toList(), brush, activeLayerId))
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
        layers.clear()
        layers.add(EngineLayer(id = 1L, name = "Layer 1", visible = true))
        activeLayerId = 1L
        nextLayerId = 2L
    }

    override fun addLayer() {
        val id = nextLayerId++
        layers.add(EngineLayer(id = id, name = "Layer ${layers.size + 1}", visible = true))
        activeLayerId = id
    }

    override fun selectLayer(layerId: Long) {
        if (layers.any { it.id == layerId }) {
            activeLayerId = layerId
        }
    }

    override fun toggleLayerVisibility(layerId: Long) {
        val index = layers.indexOfFirst { it.id == layerId }
        if (index >= 0) {
            val layer = layers[index]
            layers[index] = layer.copy(visible = !layer.visible)
        }
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

    override fun saveProject(path: String): Boolean {
        return ProjectCodec.save(path, snapshot())
    }

    override fun loadProject(path: String): Boolean {
        val project = ProjectCodec.load(path) ?: return false
        clear()
        layers.clear()
        layers.addAll(project.layers)
        activeLayerId = project.activeLayerId
        nextLayerId = (layers.maxOfOrNull { it.id } ?: 0L) + 1L
        committedStrokes.addAll(project.strokes)
        return true
    }

    override fun snapshot(): EngineSnapshot {
        return EngineSnapshot(
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            brush = brush,
            layers = layers.toList(),
            activeLayerId = activeLayerId,
            committedStrokes = committedStrokes.toList(),
            activeStroke = activeStroke.takeIf { it.isNotEmpty() }?.let {
                EngineStroke(it, brush, activeLayerId)
            },
        )
    }

    private fun renderBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(AndroidColor.WHITE)
        val visibleLayerIds = layers.filter { it.visible }.map { it.id }.toSet()
        committedStrokes.filter { it.layerId in visibleLayerIds }.forEach { stroke ->
            drawStroke(canvas, stroke)
        }
        activeStroke.takeIf {
            it.isNotEmpty() && activeLayerId in visibleLayerIds
        }?.let { points ->
            drawStroke(canvas, EngineStroke(points.toList(), brush, activeLayerId))
        }
        return bitmap
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
