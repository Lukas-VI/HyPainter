package io.github.lukasvi.hypainter.debug

import androidx.compose.ui.geometry.Offset
import io.github.lukasvi.hypainter.ViewportState

internal data class CanvasDebugState(
    val route: String = "idle",
    val action: String = "none",
    val toolType: String = "none",
    val pointerCount: Int = 0,
    val consumed: Boolean = false,
    val pressure: Float = 0f,
    val screenPosition: Offset = Offset.Unspecified,
    val canvasPosition: Offset = Offset.Unspecified,
    val strokeSamples: Int = 0,
    val historySamples: Int = 0,
    val eventAgeMs: Long = 0L,
    val handleDurationMs: Float = 0f,
    val heapUsedKb: Long = 0L,
    val heapFreeKb: Long = 0L,
    val heapMaxKb: Long = 0L,
) {
    fun toLogLine(viewport: ViewportState): String {
        return "route=$route action=$action tool=$toolType pointers=$pointerCount consumed=$consumed " +
            "pressure=${"%.2f".format(pressure)} screen=${screenPosition.debugFormat()} canvas=${canvasPosition.debugFormat()} " +
            "samples=$strokeSamples history=$historySamples pan=${viewport.pan.debugFormat()} " +
            "scale=${"%.2f".format(viewport.scale)} rotation=${"%.1f".format(viewport.rotation)} " +
            "eventAgeMs=$eventAgeMs handleMs=${"%.2f".format(handleDurationMs)} " +
            "heapKb=$heapUsedKb/$heapMaxKb freeKb=$heapFreeKb"
    }
}
