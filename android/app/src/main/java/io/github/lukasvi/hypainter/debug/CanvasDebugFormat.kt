package io.github.lukasvi.hypainter.debug

import android.view.MotionEvent
import androidx.compose.ui.geometry.Offset

internal const val DEBUG_LOG_TAG = "HyPainterInput"
internal const val DEBUG_LOG_INTERVAL_MS = 250L

internal fun Offset.debugFormat(): String {
    if (x.isNaN() || y.isNaN()) {
        return "n/a"
    }
    return "${"%.1f".format(x)},${"%.1f".format(y)}"
}

internal fun Int.toolTypeName(): String {
    return when (this) {
        MotionEvent.TOOL_TYPE_FINGER -> "finger"
        MotionEvent.TOOL_TYPE_STYLUS -> "stylus"
        MotionEvent.TOOL_TYPE_ERASER -> "eraser"
        MotionEvent.TOOL_TYPE_MOUSE -> "mouse"
        MotionEvent.TOOL_TYPE_UNKNOWN -> "unknown"
        else -> "tool-$this"
    }
}

internal fun Int.actionName(): String {
    return when (this) {
        MotionEvent.ACTION_DOWN -> "down"
        MotionEvent.ACTION_UP -> "up"
        MotionEvent.ACTION_MOVE -> "move"
        MotionEvent.ACTION_CANCEL -> "cancel"
        MotionEvent.ACTION_POINTER_DOWN -> "pointer-down"
        MotionEvent.ACTION_POINTER_UP -> "pointer-up"
        else -> "action-$this"
    }
}
