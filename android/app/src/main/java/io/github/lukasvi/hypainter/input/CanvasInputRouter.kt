package io.github.lukasvi.hypainter.input

import android.os.SystemClock
import android.util.Log
import android.view.MotionEvent
import androidx.compose.ui.geometry.Offset
import io.github.lukasvi.hypainter.BuildConfig
import io.github.lukasvi.hypainter.TouchGestureFrame
import io.github.lukasvi.hypainter.ViewportState
import io.github.lukasvi.hypainter.debug.DEBUG_LOG_INTERVAL_MS
import io.github.lukasvi.hypainter.debug.DEBUG_LOG_TAG
import io.github.lukasvi.hypainter.debug.CanvasDebugState
import io.github.lukasvi.hypainter.debug.actionName
import io.github.lukasvi.hypainter.debug.toolTypeName
import io.github.lukasvi.hypainter.engine.EngineSample
import io.github.lukasvi.hypainter.engine.PaintingEngine
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot

internal class CanvasInputRouter {
    private val session = CanvasInputSession()
    private var lastDebugState = CanvasDebugState()
    private var strokeSamples = 0
    private var historySamples = 0
    private var lastLogTime = 0L
    private var lastPressureReportTime = 0L
    private var recoveredStylusPointerOnLastEvent = false

    fun onMotionEvent(
        event: MotionEvent,
        viewport: ViewportState,
        engine: PaintingEngine,
        onViewportChanged: (ViewportState) -> Unit,
        onEngineChanged: () -> Unit,
        onEngineChangedNextFrame: () -> Unit,
        onPressure: (Float) -> Unit,
        debugEnabled: Boolean,
        onDebugChanged: (CanvasDebugState) -> Unit,
    ): Boolean {
        val debugStartNs = if (BuildConfig.DEBUG && debugEnabled) System.nanoTime() else 0L
        recoveredStylusPointerOnLastEvent = false
        val consumed = if (session.shouldRouteToStylus(event.hasStylusPointer())) {
            handleStylusEvent(event, viewport, engine, onEngineChanged, onEngineChangedNextFrame, onPressure)
        } else {
            handleTouchEvent(event, viewport, onViewportChanged)
        }
        publishDebug(
            event = event,
            viewport = viewport,
            consumed = consumed,
            debugEnabled = debugEnabled,
            debugStartNs = debugStartNs,
            onDebugChanged = onDebugChanged,
        )
        return consumed
    }

    private fun handleStylusEvent(
        event: MotionEvent,
        viewport: ViewportState,
        engine: PaintingEngine,
        onEngineChanged: () -> Unit,
        onEngineChangedNextFrame: () -> Unit,
        onPressure: (Float) -> Unit,
    ): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val pointerIndex = event.actionIndex
                if (!event.isStylusPointer(pointerIndex)) {
                    return session.stylusPointerId != null
                }
                session.beginStylus(event.getPointerId(pointerIndex))
                strokeSamples = 1
                historySamples = 0
                lastPressureReportTime = event.eventTime
                engine.beginStroke(event.toSample(pointerIndex, viewport, historicalIndex = null))
                onPressure(event.getPressure(pointerIndex).coerceIn(0f, 1f))
                onEngineChanged()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val pointerIndex = event.findActiveStylusPointerIndex() ?: event.findAnyStylusPointerIndex()
                if (pointerIndex == null) {
                    engine.endStroke()
                    session.loseStylusPointer()
                    onEngineChanged()
                    return true
                }
                if (session.stylusPointerId == null) {
                    session.beginStylus(event.getPointerId(pointerIndex))
                    strokeSamples = 0
                    historySamples = 0
                    recoveredStylusPointerOnLastEvent = true
                }
                for (historyIndex in 0 until event.historySize) {
                    engine.appendSample(event.toSample(pointerIndex, viewport, historyIndex))
                }
                engine.appendSample(event.toSample(pointerIndex, viewport, historicalIndex = null))
                historySamples += event.historySize
                strokeSamples += event.historySize + 1
                if (event.eventTime - lastPressureReportTime >= PRESSURE_REPORT_INTERVAL_MS) {
                    lastPressureReportTime = event.eventTime
                    onPressure(event.getPressure(pointerIndex).coerceIn(0f, 1f))
                }
                onEngineChangedNextFrame()
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                if (session.endStylusIfActive(event.getPointerId(event.actionIndex))) {
                    engine.endStroke()
                    onEngineChanged()
                }
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                if (session.cancelStylus()) {
                    engine.endStroke()
                    onEngineChanged()
                }
                return true
            }
        }

        return true
    }

    private fun handleTouchEvent(
        event: MotionEvent,
        viewport: ViewportState,
        onViewportChanged: (ViewportState) -> Unit,
    ): Boolean {
        if (!event.allPointersAreFingers()) {
            return session.rejectNonFingerTouch()
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                return session.beginFingerStream()
            }

            MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount < 2) {
                    return session.updateSingleFingerTouch()
                }

                return session.updateTwoFingerTouch(
                    viewport = viewport,
                    next = event.touchGestureFrame(),
                    onViewportChanged = onViewportChanged,
                )
            }

            MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                return session.endTouchStream(
                    terminal = event.actionMasked == MotionEvent.ACTION_UP ||
                        event.actionMasked == MotionEvent.ACTION_CANCEL,
                )
            }
        }

        return session.updateSingleFingerTouch()
    }

    private fun MotionEvent.findActiveStylusPointerIndex(): Int? {
        val activePointerId = session.stylusPointerId ?: return null
        val pointerIndex = findPointerIndex(activePointerId)
        if (pointerIndex < 0) {
            session.loseStylusPointer()
            return null
        }
        return pointerIndex
    }

    private fun publishDebug(
        event: MotionEvent,
        viewport: ViewportState,
        consumed: Boolean,
        debugEnabled: Boolean,
        debugStartNs: Long,
        onDebugChanged: (CanvasDebugState) -> Unit,
    ) {
        if (!BuildConfig.DEBUG || !debugEnabled) {
            return
        }

        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        val pointerIndex = event.findActiveStylusPointerIndex()
            ?: event.findAnyStylusPointerIndex()
            ?: event.actionIndex.coerceIn(0, event.pointerCount - 1)
        val screen = Offset(event.getX(pointerIndex), event.getY(pointerIndex))
        val next = CanvasDebugState(
            route = when {
                session.stylusPointerId != null -> "stylus"
                event.pointerCount >= 2 && event.allPointersAreFingers() -> "two-finger"
                event.allPointersAreFingers() -> "single-finger"
                else -> "ignored"
            },
            action = event.actionMasked.actionName(),
            toolType = event.getToolType(pointerIndex).toolTypeName(),
            pointerCount = event.pointerCount,
            pointerId = event.getPointerId(pointerIndex),
            activeStylusPointerId = session.stylusPointerId,
            consumed = consumed,
            pressure = event.getPressure(pointerIndex).coerceIn(0f, 1f),
            screenPosition = screen,
            canvasPosition = viewport.toCanvas(screen),
            strokeSamples = strokeSamples,
            historySamples = historySamples,
            historySize = event.historySize,
            eventTime = event.eventTime,
            downTime = event.downTime,
            recoveredStylusPointer = recoveredStylusPointerOnLastEvent,
            eventAgeMs = (SystemClock.uptimeMillis() - event.eventTime).coerceAtLeast(0L),
            handleDurationMs = ((System.nanoTime() - debugStartNs).coerceAtLeast(0L) / 1_000_000f),
            heapUsedKb = (totalMemory - freeMemory) / 1024L,
            heapFreeKb = freeMemory / 1024L,
            heapMaxKb = maxMemory / 1024L,
        )
        lastDebugState = next
        onDebugChanged(next)

        if (event.eventTime - lastLogTime >= DEBUG_LOG_INTERVAL_MS || event.actionMasked != MotionEvent.ACTION_MOVE) {
            lastLogTime = event.eventTime
            Log.d(DEBUG_LOG_TAG, next.toLogLine(viewport))
        }
    }
}

private fun MotionEvent.hasStylusPointer(): Boolean {
    return findAnyStylusPointerIndex() != null
}

private fun MotionEvent.isStylusPointer(pointerIndex: Int): Boolean {
    val toolType = getToolType(pointerIndex)
    return toolType == MotionEvent.TOOL_TYPE_STYLUS ||
        toolType == MotionEvent.TOOL_TYPE_ERASER
}

private fun MotionEvent.allPointersAreFingers(): Boolean {
    for (index in 0 until pointerCount) {
        if (getToolType(index) != MotionEvent.TOOL_TYPE_FINGER) {
            return false
        }
    }
    return true
}

private fun MotionEvent.findAnyStylusPointerIndex(): Int? {
    for (index in 0 until pointerCount) {
        if (isStylusPointer(index)) {
            return index
        }
    }
    return null
}

private fun MotionEvent.touchGestureFrame(): TouchGestureFrame {
    val first = Offset(getX(0), getY(0))
    val second = Offset(getX(1), getY(1))
    val delta = second - first
    return TouchGestureFrame(
        centroid = (first + second) / 2f,
        distance = hypot(delta.x, delta.y).coerceAtLeast(1f),
        angleDegrees = (atan2(delta.y, delta.x) * 180f / PI.toFloat()),
    )
}

private fun MotionEvent.toSample(
    pointerIndex: Int,
    viewport: ViewportState,
    historicalIndex: Int?,
): EngineSample {
    val screenPosition = if (historicalIndex == null) {
        Offset(getX(pointerIndex), getY(pointerIndex))
    } else {
        Offset(getHistoricalX(pointerIndex, historicalIndex), getHistoricalY(pointerIndex, historicalIndex))
    }
    return EngineSample(
        position = viewport.toCanvas(screenPosition),
        pressure = if (historicalIndex == null) {
            getPressure(pointerIndex)
        } else {
            getHistoricalPressure(pointerIndex, historicalIndex)
        }.coerceIn(0f, 1f),
        tiltX = if (historicalIndex == null) {
            getAxisValue(MotionEvent.AXIS_TILT, pointerIndex)
        } else {
            getHistoricalAxisValue(MotionEvent.AXIS_TILT, pointerIndex, historicalIndex)
        },
        tiltY = if (historicalIndex == null) {
            getAxisValue(MotionEvent.AXIS_ORIENTATION, pointerIndex)
        } else {
            getHistoricalAxisValue(MotionEvent.AXIS_ORIENTATION, pointerIndex, historicalIndex)
        },
        timestamp = if (historicalIndex == null) eventTime else getHistoricalEventTime(historicalIndex),
    )
}

private const val PRESSURE_REPORT_INTERVAL_MS = 80L
