package io.github.lukasvi.hypainter.input

import io.github.lukasvi.hypainter.TouchGestureFrame
import io.github.lukasvi.hypainter.ViewportState

internal class CanvasInputSession {
    var stylusPointerId: Int? = null
        private set

    private var fingerStreamActive = false
    private var lastTouchGesture: TouchGestureFrame? = null

    fun shouldRouteToStylus(actionPointerIsStylus: Boolean): Boolean {
        return stylusPointerId != null || actionPointerIsStylus
    }

    fun beginStylus(pointerId: Int) {
        stylusPointerId = pointerId
        fingerStreamActive = false
        lastTouchGesture = null
    }

    fun endStylusIfActive(pointerId: Int): Boolean {
        if (stylusPointerId != pointerId) {
            return false
        }
        clearAll()
        return true
    }

    fun cancelStylus(): Boolean {
        if (stylusPointerId == null) {
            return false
        }
        clearAll()
        return true
    }

    fun loseStylusPointer() {
        clearAll()
    }

    fun beginFingerStream(): Boolean {
        fingerStreamActive = true
        lastTouchGesture = null
        return true
    }

    fun rejectNonFingerTouch(): Boolean {
        lastTouchGesture = null
        return false
    }

    fun updateSingleFingerTouch(): Boolean {
        lastTouchGesture = null
        return fingerStreamActive
    }

    fun updateTwoFingerTouch(
        viewport: ViewportState,
        next: TouchGestureFrame,
        onViewportChanged: (ViewportState) -> Unit,
    ): Boolean {
        if (!fingerStreamActive) {
            lastTouchGesture = null
            return false
        }
        val previous = lastTouchGesture
        lastTouchGesture = next
        if (previous != null) {
            onViewportChanged(
                viewport.transformAround(
                    previous = previous,
                    next = next,
                ),
            )
        }
        return true
    }

    fun endTouchStream(terminal: Boolean): Boolean {
        lastTouchGesture = null
        if (terminal) {
            fingerStreamActive = false
        }
        return fingerStreamActive
    }

    private fun clearAll() {
        stylusPointerId = null
        fingerStreamActive = false
        lastTouchGesture = null
    }
}
