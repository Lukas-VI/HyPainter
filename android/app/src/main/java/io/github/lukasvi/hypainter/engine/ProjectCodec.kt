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
                snapshot.committedStrokes.forEach { stroke ->
                    writer.println("STROKE ${stroke.brush.colorArgb} ${stroke.brush.radiusPx} ${stroke.points.size}")
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

                    "STROKE" -> {
                        val color = parts.getOrNull(1)?.toIntOrNull() ?: 0xff000000.toInt()
                        val radius = parts.getOrNull(2)?.toFloatOrNull() ?: 8f
                        val count = parts.getOrNull(3)?.toIntOrNull() ?: 0
                        val samples = mutableListOf<EngineSample>()
                        repeat(count) {
                            index++
                            val sample = parseSample(lines.getOrNull(index).orEmpty())
                            if (sample != null) {
                                samples.add(sample)
                            }
                        }
                        if (samples.isNotEmpty()) {
                            strokes.add(EngineStroke(samples, EngineBrush(color, radius)))
                        }
                        index++
                    }

                    else -> index++
                }
            }

            ProjectData(canvasWidth, canvasHeight, strokes)
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
    val strokes: List<EngineStroke>,
)
