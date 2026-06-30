package io.github.lukasvi.hypainter.engine

import androidx.compose.ui.geometry.Offset
import java.io.File
import java.util.Locale

object ProjectCodec {
    private const val HEADER = "HYPDRAFT 1"

    fun save(path: String, snapshot: EngineSnapshot): Boolean {
        return runCatching {
            File(path).printWriter().use { writer ->
                writer.println(HEADER)
                writer.println("CANVAS ${snapshot.canvasWidth} ${snapshot.canvasHeight}")
                writer.println("ACTIVE_LAYER ${snapshot.activeLayerId}")
                snapshot.layers.forEach { layer ->
                    writer.println("LAYER ${layer.id} ${if (layer.visible) 1 else 0} ${layer.name}")
                }
                snapshot.committedStrokes.forEach { stroke ->
                    writer.println(
                        "STROKE ${stroke.layerId} ${stroke.brush.colorArgb} ${stroke.brush.radiusPx} ${stroke.points.size}",
                    )
                    stroke.points.forEach { sample ->
                        writer.println(
                            listOf(
                                sample.position.x,
                                sample.position.y,
                                sample.pressure,
                                sample.tiltX,
                                sample.tiltY,
                                sample.timestamp,
                            ).joinToString(" "),
                        )
                    }
                }
            }
        }.isSuccess
    }

    fun load(path: String): ProjectData? {
        return runCatching {
            val lines = File(path).readLines()
            if (lines.firstOrNull() != HEADER) {
                return@runCatching null
            }

            var canvasWidth = 1024
            var canvasHeight = 1024
            var activeLayerId = 1L
            val layers = mutableListOf<EngineLayer>()
            val strokes = mutableListOf<EngineStroke>()
            var index = 1

            while (index < lines.size) {
                val parts = lines[index].trim().split(" ").filter { it.isNotBlank() }
                when (parts.firstOrNull()) {
                    "CANVAS" -> {
                        canvasWidth = parts.getOrNull(1)?.toIntOrNull() ?: canvasWidth
                        canvasHeight = parts.getOrNull(2)?.toIntOrNull() ?: canvasHeight
                        index++
                    }

                    "ACTIVE_LAYER" -> {
                        activeLayerId = parts.getOrNull(1)?.toLongOrNull() ?: activeLayerId
                        index++
                    }

                    "LAYER" -> {
                        val id = parts.getOrNull(1)?.toLongOrNull() ?: 1L
                        val visible = parts.getOrNull(2) != "0"
                        val name = parts.drop(3).joinToString(" ").ifBlank { "Layer $id" }
                        layers.add(EngineLayer(id, name, visible))
                        index++
                    }

                    "STROKE" -> {
                        val layerId = parts.getOrNull(1)?.toLongOrNull() ?: 1L
                        val color = parts.getOrNull(2)?.toIntOrNull() ?: 0xff000000.toInt()
                        val radius = parts.getOrNull(3)?.toFloatOrNull() ?: 8f
                        val count = parts.getOrNull(4)?.toIntOrNull() ?: 0
                        val samples = mutableListOf<EngineSample>()
                        repeat(count) {
                            index++
                            val sample = parseSample(lines.getOrNull(index).orEmpty())
                            if (sample != null) {
                                samples.add(sample)
                            }
                        }
                        if (samples.isNotEmpty()) {
                            strokes.add(EngineStroke(samples, EngineBrush(color, radius), layerId))
                        }
                        index++
                    }

                    else -> index++
                }
            }

            if (layers.isEmpty()) {
                layers.add(EngineLayer(1L, "Layer 1", true))
            }
            if (layers.none { it.id == activeLayerId }) {
                activeLayerId = layers.last().id
            }

            ProjectData(canvasWidth, canvasHeight, layers, activeLayerId, strokes)
        }.getOrNull()
    }

    private fun parseSample(line: String): EngineSample? {
        val parts = line.trim().split(" ").filter { it.isNotBlank() }
        if (parts.size < 6) {
            return null
        }

        return EngineSample(
            position = Offset(
                x = parts[0].toFloatStrict(),
                y = parts[1].toFloatStrict(),
            ),
            pressure = parts[2].toFloatStrict(),
            tiltX = parts[3].toFloatStrict(),
            tiltY = parts[4].toFloatStrict(),
            timestamp = parts[5].toLongOrNull() ?: 0L,
        )
    }

    private fun String.toFloatStrict(): Float {
        return lowercase(Locale.US).toFloatOrNull() ?: 0f
    }
}

data class ProjectData(
    val canvasWidth: Int,
    val canvasHeight: Int,
    val layers: List<EngineLayer>,
    val activeLayerId: Long,
    val strokes: List<EngineStroke>,
)
