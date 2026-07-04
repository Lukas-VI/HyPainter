package io.github.lukasvi.hypainter.engine

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Test

class StrokeSnapshotsTest {
    @Test
    fun stableCopyForLayerDoesNotAliasMutablePointList() {
        val mutablePoints = mutableListOf(
            EngineSample(
                position = Offset(1f, 2f),
                pressure = 0.5f,
                tiltX = 0f,
                tiltY = 0f,
                timestamp = 10L,
            ),
            EngineSample(
                position = Offset(3f, 4f),
                pressure = 0.7f,
                tiltX = 0f,
                tiltY = 0f,
                timestamp = 20L,
            ),
        )
        val stroke = EngineStroke(
            points = mutablePoints,
            brush = EngineBrush(colorArgb = 0xff000000.toInt(), radiusPx = 8f),
            layerId = 1L,
        )

        val stable = stroke.stableCopyForLayer(layerId = 4L)
        mutablePoints.clear()

        assertEquals(4L, stable.layerId)
        assertEquals(2, stable.points.size)
        assertEquals(Offset(1f, 2f), stable.points.first().position)
    }
}
