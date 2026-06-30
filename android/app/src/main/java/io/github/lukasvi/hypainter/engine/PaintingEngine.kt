package io.github.lukasvi.hypainter.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap

interface PaintingEngine {
    val nativeBacked: Boolean

    fun beginStroke(sample: EngineSample)

    fun appendSample(sample: EngineSample)

    fun endStroke()

    fun undo()

    fun clear()

    fun addLayer()

    fun selectLayer(layerId: Long)

    fun toggleLayerVisibility(layerId: Long)

    fun setBrush(brush: EngineBrush)

    fun exportPng(path: String): Boolean

    fun saveProject(path: String): Boolean

    fun loadProject(path: String): Boolean

    fun snapshot(): EngineSnapshot
}

data class EngineSample(
    val position: Offset,
    val pressure: Float,
    val tiltX: Float,
    val tiltY: Float,
    val timestamp: Long,
)

data class EngineStroke(
    val points: List<EngineSample>,
    val brush: EngineBrush,
    val layerId: Long,
)

data class EngineLayer(
    val id: Long,
    val name: String,
    val visible: Boolean,
)

data class EngineBrush(
    val colorArgb: Int,
    val radiusPx: Float,
)

data class EngineSnapshot(
    val canvasWidth: Int,
    val canvasHeight: Int,
    val brush: EngineBrush,
    val layers: List<EngineLayer>,
    val activeLayerId: Long,
    val committedStrokes: List<EngineStroke>,
    val activeStroke: EngineStroke?,
    val renderedImage: ImageBitmap? = null,
)

fun createPaintingEngine(width: Int = 1024, height: Int = 1024): PaintingEngine {
    return NativePaintingEngine.createOrNull(width, height) ?: KotlinPaintingEngine()
}
