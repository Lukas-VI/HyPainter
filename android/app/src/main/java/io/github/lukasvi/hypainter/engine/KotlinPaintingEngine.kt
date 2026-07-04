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
    private val displayCache = StrokeRasterCache(canvasWidth, canvasHeight)
    private val activeCache = StrokeRasterCache(canvasWidth, canvasHeight)
    private var activeLayerId = 1L
    private var nextLayerId = 2L

    override fun beginStroke(sample: EngineSample) {
        activeCache.clear()
        activeStroke = mutableListOf(sample)
        activeCache.appendPoint(sample, brush, activeLayerId, layers)
    }

    override fun appendSample(sample: EngineSample) {
        if (activeStroke.isEmpty()) {
            beginStroke(sample)
            return
        }

        val previous = activeStroke[activeStroke.lastIndex]
        activeStroke.add(sample)
        activeCache.appendSegment(previous, sample, brush, activeLayerId, layers)
    }

    override fun endStroke() {
        if (activeStroke.isNotEmpty()) {
            val stroke = EngineStroke(activeStroke.toList(), brush, activeLayerId)
            committedStrokes.add(stroke)
            displayCache.append(stroke, layers)
            activeStroke = mutableListOf()
            activeCache.clear()
        }
    }

    override fun undo() {
        if (committedStrokes.isNotEmpty()) {
            committedStrokes.removeAt(committedStrokes.lastIndex)
            displayCache.rebuild(committedStrokes, layers)
        }
    }

    override fun clear() {
        committedStrokes.clear()
        activeStroke.clear()
        displayCache.clear()
        activeCache.clear()
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
            displayCache.rebuild(committedStrokes, layers)
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
        activeCache.clear()
        activeLayerId = project.activeLayerId
        nextLayerId = (layers.maxOfOrNull { it.id } ?: 0L) + 1L
        committedStrokes.addAll(project.strokes)
        displayCache.rebuild(committedStrokes, layers)
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

    override fun canvasSnapshot(): EngineSnapshot {
        return EngineSnapshot(
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            brush = brush,
            layers = layers,
            activeLayerId = activeLayerId,
            committedStrokes = emptyList(),
            activeStroke = activeStroke.takeIf { it.isNotEmpty() }?.let {
                EngineStroke(it, brush, activeLayerId)
            },
            renderedImage = displayCache.renderedImage,
            activeImage = activeCache.renderedImage,
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
