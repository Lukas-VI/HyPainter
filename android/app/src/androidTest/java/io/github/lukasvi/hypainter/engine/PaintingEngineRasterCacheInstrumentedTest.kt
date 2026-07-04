package io.github.lukasvi.hypainter.engine

import androidx.compose.ui.geometry.Offset
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PaintingEngineRasterCacheInstrumentedTest {
    @Test
    fun endingStrokeMovesActivePreviewIntoCommittedCache() {
        val engine = KotlinPaintingEngine()

        engine.beginStroke(sampleAt(10f, 10f, pressure = 0.6f, timestamp = 1L))
        engine.appendSample(sampleAt(30f, 30f, pressure = 0.8f, timestamp = 2L))
        assertNotNull(engine.canvasSnapshot().activeImage)

        engine.endStroke()
        val canvasSnapshot = engine.canvasSnapshot()

        assertNotNull(canvasSnapshot.renderedImage)
        assertNull(canvasSnapshot.activeImage)
        assertEquals(1, engine.snapshot().committedStrokes.size)
    }

    private fun sampleAt(
        x: Float,
        y: Float,
        pressure: Float,
        timestamp: Long,
    ): EngineSample {
        return EngineSample(
            position = Offset(x, y),
            pressure = pressure,
            tiltX = 0f,
            tiltY = 0f,
            timestamp = timestamp,
        )
    }
}
