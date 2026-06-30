package io.github.lukasvi.hypainter.engine

import androidx.compose.ui.geometry.Offset

interface PaintingEngine {
    val nativeBacked: Boolean

    fun beginStroke(sample: EngineSample)

    fun appendSample(sample: EngineSample)

    fun endStroke()

    fun undo()

    fun clear()

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
)

data class EngineSnapshot(
    val committedStrokes: List<EngineStroke>,
    val activeStroke: EngineStroke?,
)

fun createPaintingEngine(width: Int = 4096, height: Int = 4096): PaintingEngine {
    return NativePaintingEngine.createOrNull(width, height) ?: KotlinPaintingEngine()
}
