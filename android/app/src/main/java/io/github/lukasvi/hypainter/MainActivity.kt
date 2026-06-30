package io.github.lukasvi.hypainter

import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HyPainterApp()
        }
    }
}

@Composable
private fun HyPainterApp() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF15171A)) {
            CanvasScreen()
        }
    }
}

@Composable
private fun CanvasScreen() {
    val strokes = remember { mutableStateListOf<StrokeModel>() }
    val activeStroke = remember { mutableStateOf<StrokeModel?>(null) }
    val viewport = remember { mutableStateOf(ViewportState()) }
    val latestPressure = remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF15171A))
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, rotation ->
                    if (activeStroke.value == null) {
                        viewport.value = viewport.value.transform(pan, zoom, rotation)
                    }
                }
            }
            .pointerInteropFilter { event ->
                handleStylusEvent(
                    event = event,
                    viewport = viewport.value,
                    activeStroke = activeStroke.value,
                    onActiveStrokeChange = { activeStroke.value = it },
                    onStrokeCommitted = { strokes.add(it) },
                    onPressure = { latestPressure.value = it },
                )
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val state = viewport.value
            withTransformCompat(state) {
                strokes.forEach { stroke ->
                    drawStroke(stroke)
                }
                activeStroke.value?.let { drawStroke(it) }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
        ) {
            AssistChip(
                onClick = { strokes.clear() },
                label = { Text("Clear") },
            )
            Box(modifier = Modifier.size(8.dp))
            AssistChip(
                onClick = {
                    if (strokes.isNotEmpty()) {
                        strokes.removeAt(strokes.lastIndex)
                    }
                },
                label = { Text("Undo") },
            )
            Box(modifier = Modifier.size(8.dp))
            AssistChip(
                onClick = {},
                label = { Text("Pressure ${"%.2f".format(latestPressure.value)}") },
            )
        }
    }
}

private fun handleStylusEvent(
    event: MotionEvent,
    viewport: ViewportState,
    activeStroke: StrokeModel?,
    onActiveStrokeChange: (StrokeModel?) -> Unit,
    onStrokeCommitted: (StrokeModel) -> Unit,
    onPressure: (Float) -> Unit,
): Boolean {
    val toolType = event.getToolType(event.actionIndex)
    val isStylus = toolType == MotionEvent.TOOL_TYPE_STYLUS ||
        toolType == MotionEvent.TOOL_TYPE_ERASER ||
        toolType == MotionEvent.TOOL_TYPE_UNKNOWN

    if (!isStylus) {
        return false
    }

    val sample = StylusSample(
        position = viewport.toCanvas(Offset(event.x, event.y)),
        pressure = event.pressure.coerceIn(0f, 1f),
        timestamp = event.eventTime,
    )
    onPressure(sample.pressure)

    when (event.actionMasked) {
        MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
            onActiveStrokeChange(StrokeModel(points = mutableListOf(sample)))
            return true
        }

        MotionEvent.ACTION_MOVE -> {
            activeStroke?.points?.add(sample)
            return true
        }

        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_UP -> {
            activeStroke?.let(onStrokeCommitted)
            onActiveStrokeChange(null)
            return true
        }
    }

    return true
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.withTransformCompat(
    viewport: ViewportState,
    block: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit,
) {
    withTransform({
        translate(viewport.pan.x, viewport.pan.y)
        rotate(viewport.rotation)
        scale(viewport.scale, viewport.scale)
    }) {
        block()
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStroke(stroke: StrokeModel) {
    stroke.points.zipWithNext().forEach { (from, to) ->
        drawLine(
            color = Color.Black.copy(alpha = to.pressure.coerceIn(0.1f, 1f)),
            start = from.position,
            end = to.position,
            strokeWidth = 4f + to.pressure * 16f,
            cap = StrokeCap.Round,
        )
    }

    stroke.points.singleOrNull()?.let { point ->
        drawCircle(
            color = Color.Black.copy(alpha = point.pressure.coerceIn(0.1f, 1f)),
            radius = 4f + point.pressure * 16f,
            center = point.position,
            style = Stroke(width = 1f),
        )
    }
}

private data class ViewportState(
    val pan: Offset = Offset.Zero,
    val scale: Float = 1f,
    val rotation: Float = 0f,
) {
    fun transform(panDelta: Offset, zoomDelta: Float, rotationDelta: Float): ViewportState {
        return copy(
            pan = pan + panDelta,
            scale = (scale * zoomDelta).coerceIn(0.25f, 8f),
            rotation = rotation + rotationDelta,
        )
    }

    fun toCanvas(screen: Offset): Offset {
        return (screen - pan) / scale
    }
}

private data class StrokeModel(
    val points: MutableList<StylusSample>,
)

private data class StylusSample(
    val position: Offset,
    val pressure: Float,
    val timestamp: Long,
)
