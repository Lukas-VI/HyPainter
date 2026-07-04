package io.github.lukasvi.hypainter

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Test

class CanvasViewportTest {
    @Test
    fun screenToCanvasRoundTripsThroughPanScaleAndRotation() {
        val viewport = ViewportState(
            pan = Offset(240f, -80f),
            scale = 2.5f,
            rotation = 37f,
        )
        val screen = Offset(812f, 433f)

        val canvas = viewport.toCanvas(screen)
        val roundTrip = viewport.toScreen(canvas)

        assertOffsetEquals(screen, roundTrip)
    }

    @Test
    fun transformAroundKeepsPreviousCentroidCanvasPointUnderNextCentroid() {
        val viewport = ViewportState(
            pan = Offset(130f, 95f),
            scale = 1.7f,
            rotation = -24f,
        )
        val previous = TouchGestureFrame(
            centroid = Offset(500f, 300f),
            distance = 220f,
            angleDegrees = 20f,
        )
        val next = TouchGestureFrame(
            centroid = Offset(640f, 380f),
            distance = 310f,
            angleDegrees = 73f,
        )

        val anchoredCanvasPoint = viewport.toCanvas(previous.centroid)
        val transformed = viewport.transformAround(previous, next)

        assertOffsetEquals(next.centroid, transformed.toScreen(anchoredCanvasPoint))
    }

    @Test
    fun transformAroundClampsScaleButStillAnchorsCentroid() {
        val viewport = ViewportState(
            pan = Offset(-90f, 60f),
            scale = 7.5f,
            rotation = 12f,
        )
        val previous = TouchGestureFrame(
            centroid = Offset(250f, 250f),
            distance = 100f,
            angleDegrees = 0f,
        )
        val next = TouchGestureFrame(
            centroid = Offset(280f, 260f),
            distance = 1000f,
            angleDegrees = 15f,
        )

        val anchoredCanvasPoint = viewport.toCanvas(previous.centroid)
        val transformed = viewport.transformAround(previous, next)

        assertEquals(8f, transformed.scale, EPSILON)
        assertOffsetEquals(next.centroid, transformed.toScreen(anchoredCanvasPoint))
    }

    private fun assertOffsetEquals(expected: Offset, actual: Offset) {
        assertEquals(expected.x, actual.x, EPSILON)
        assertEquals(expected.y, actual.y, EPSILON)
    }

    private companion object {
        const val EPSILON = 0.01f
    }
}
