package io.github.lukasvi.hypainter.input

import io.github.lukasvi.hypainter.TouchGestureFrame
import io.github.lukasvi.hypainter.ViewportState

internal class CanvasInputSession {
    var stylusPointerId: Int? = null
        private set

    private var fingerStreamActive = false
    var firstFingerPointerId: Int? = null
        private set
    var secondFingerPointerId: Int? = null
        private set
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

    fun beginFingerStream(pointerId: Int): Boolean {
        fingerStreamActive = true
        firstFingerPointerId = pointerId
        secondFingerPointerId = null
        lastTouchGesture = null
        return true
    }

    fun beginAdditionalFinger(pointerId: Int): Boolean {
        if (!fingerStreamActive || firstFingerPointerId == null) {
            return false
        }
        if (pointerId != firstFingerPointerId && secondFingerPointerId == null) {
            secondFingerPointerId = pointerId
            lastTouchGesture = null
        }
        return fingerStreamActive
    }

    fun rejectNonFingerTouch(): Boolean {
        lastTouchGesture = null
        return false
    }

    fun updateSingleFingerTouch(): Boolean {
        secondFingerPointerId = null
        lastTouchGesture = null
        return fingerStreamActive
    }

    fun hasActiveTwoFingerPointers(): Boolean {
        return fingerStreamActive &&
            firstFingerPointerId != null &&
            secondFingerPointerId != null
    }

    fun updateTwoFingerTouch(
        viewport: ViewportState,
        next: TouchGestureFrame,
        onViewportChanged: (ViewportState) -> Unit,
    ): Boolean {
        if (!hasActiveTwoFingerPointers()) {
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

    fun endTouchPointer(pointerId: Int?, terminal: Boolean): Boolean {
        lastTouchGesture = null
        if (terminal) {
            clearTouch()
            return false
        }
        if (pointerId == firstFingerPointerId) {
            firstFingerPointerId = secondFingerPointerId
            secondFingerPointerId = null
        } else if (pointerId == secondFingerPointerId) {
            secondFingerPointerId = null
        }
        return fingerStreamActive
    }

    fun cancelTouchStream(): Boolean {
        clearTouch()
        return false
    }

    private fun clearAll() {
        stylusPointerId = null
        clearTouch()
    }

    private fun clearTouch() {
        fingerStreamActive = false
        firstFingerPointerId = null
        secondFingerPointerId = null
        lastTouchGesture = null
    }
}
