package io.github.lukasvi.hypainter.input

import androidx.compose.ui.geometry.Offset
import io.github.lukasvi.hypainter.TouchGestureFrame
import io.github.lukasvi.hypainter.ViewportState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CanvasInputSessionTest {
    @Test
    fun stylusTakesPriorityAndClearsFingerGestureState() {
        val session = CanvasInputSession()
        var viewportChanges = 0

        assertTrue(session.beginFingerStream())
        assertTrue(
            session.updateTwoFingerTouch(
                viewport = ViewportState(),
                next = TouchGestureFrame(centroid = Offset(100f, 100f), distance = 120f, angleDegrees = 0f),
                onViewportChanged = { viewportChanges++ },
            ),
        )

        session.beginStylus(pointerId = 9)

        assertTrue(session.shouldRouteToStylus(actionPointerIsStylus = false))
        assertEquals(9, session.stylusPointerId)
        assertTrue(session.updateSingleFingerTouch().not())
        assertEquals(0, viewportChanges)
    }

    @Test
    fun nonActiveStylusPointerUpDoesNotEndStroke() {
        val session = CanvasInputSession()
        session.beginStylus(pointerId = 4)

        assertFalse(session.endStylusIfActive(pointerId = 7))

        assertEquals(4, session.stylusPointerId)
        assertTrue(session.shouldRouteToStylus(actionPointerIsStylus = false))
    }

    @Test
    fun activeStylusPointerUpEndsStrokeAndReleasesPriority() {
        val session = CanvasInputSession()
        session.beginStylus(pointerId = 4)

        assertTrue(session.endStylusIfActive(pointerId = 4))

        assertNull(session.stylusPointerId)
        assertFalse(session.shouldRouteToStylus(actionPointerIsStylus = false))
    }

    @Test
    fun singleFingerStreamConsumesButDoesNotTransformViewport() {
        val session = CanvasInputSession()
        var viewportChanges = 0

        assertTrue(session.beginFingerStream())
        assertTrue(session.updateSingleFingerTouch())

        assertEquals(0, viewportChanges)
    }

    @Test
    fun twoFingerGestureTransformsAroundPreviousCentroidCanvasPoint() {
        val session = CanvasInputSession()
        val viewport = ViewportState(
            pan = Offset(40f, 60f),
            scale = 1.5f,
            rotation = 20f,
        )
        val previous = TouchGestureFrame(
            centroid = Offset(300f, 220f),
            distance = 180f,
            angleDegrees = 15f,
        )
        val next = TouchGestureFrame(
            centroid = Offset(460f, 260f),
            distance = 240f,
            angleDegrees = 55f,
        )
        val anchoredCanvasPoint = viewport.toCanvas(previous.centroid)
        var transformed: ViewportState? = null

        assertTrue(session.beginFingerStream())
        assertTrue(session.updateTwoFingerTouch(viewport, previous) { transformed = it })
        assertNull(transformed)
        assertTrue(session.updateTwoFingerTouch(viewport, next) { transformed = it })

        val nextViewport = checkNotNull(transformed)
        assertOffsetEquals(next.centroid, nextViewport.toScreen(anchoredCanvasPoint))
    }

    @Test
    fun twoFingerTouchIsIgnoredBeforeFingerStreamBegins() {
        val session = CanvasInputSession()
        var viewportChanges = 0

        assertFalse(
            session.updateTwoFingerTouch(
                viewport = ViewportState(),
                next = TouchGestureFrame(centroid = Offset(100f, 100f), distance = 80f, angleDegrees = 0f),
                onViewportChanged = { viewportChanges++ },
            ),
        )

        assertEquals(0, viewportChanges)
    }

    @Test
    fun stylusPriorityBlocksResidualTwoFingerTouchUntilFreshFingerDown() {
        val session = CanvasInputSession()
        var viewportChanges = 0

        assertTrue(session.beginFingerStream())
        assertTrue(
            session.updateTwoFingerTouch(
                viewport = ViewportState(),
                next = TouchGestureFrame(centroid = Offset(100f, 100f), distance = 80f, angleDegrees = 0f),
                onViewportChanged = { viewportChanges++ },
            ),
        )
        session.beginStylus(pointerId = 3)
        assertTrue(session.endStylusIfActive(pointerId = 3))

        assertFalse(
            session.updateTwoFingerTouch(
                viewport = ViewportState(),
                next = TouchGestureFrame(centroid = Offset(160f, 120f), distance = 100f, angleDegrees = 30f),
                onViewportChanged = { viewportChanges++ },
            ),
        )

        assertEquals(0, viewportChanges)
    }

    private fun assertOffsetEquals(expected: Offset, actual: Offset) {
        assertEquals(expected.x, actual.x, EPSILON)
        assertEquals(expected.y, actual.y, EPSILON)
    }

    private companion object {
        const val EPSILON = 0.01f
    }
}
