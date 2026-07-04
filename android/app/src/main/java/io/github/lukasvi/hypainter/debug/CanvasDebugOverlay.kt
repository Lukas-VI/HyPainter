package io.github.lukasvi.hypainter.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.lukasvi.hypainter.ViewportState
import io.github.lukasvi.hypainter.engine.EngineSnapshot

@Composable
internal fun CanvasDebugOverlay(
    modifier: Modifier,
    state: CanvasDebugState,
    viewport: ViewportState,
    snapshot: EngineSnapshot,
) {
    Column(
        modifier = modifier
            .background(Color(0xCC101418))
            .padding(12.dp),
    ) {
        Text("Input: ${state.route}", color = Color.White)
        Text("Action: ${state.action}", color = Color.White)
        Text("Tool: ${state.toolType} pointers=${state.pointerCount}", color = Color.White)
        Text("Consumed: ${state.consumed}", color = Color.White)
        Text("Samples: ${state.strokeSamples} history=${state.historySamples}", color = Color.White)
        Text("Latency: age=${state.eventAgeMs}ms handle=${"%.2f".format(state.handleDurationMs)}ms", color = Color.White)
        Text("Heap: ${state.heapUsedKb}/${state.heapMaxKb} KB free=${state.heapFreeKb} KB", color = Color.White)
        Text("Pressure: ${"%.2f".format(state.pressure)}", color = Color.White)
        Text("Screen: ${state.screenPosition.debugFormat()}", color = Color.White)
        Text("Canvas: ${state.canvasPosition.debugFormat()}", color = Color.White)
        Text(
            "View: pan=${viewport.pan.debugFormat()} scale=${"%.2f".format(viewport.scale)} rot=${"%.1f".format(viewport.rotation)}",
            color = Color.White,
        )
        Text("Active stroke: ${snapshot.activeStroke?.points?.size ?: 0}", color = Color.White)
    }
}
