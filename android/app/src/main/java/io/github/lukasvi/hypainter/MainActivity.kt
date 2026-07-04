package io.github.lukasvi.hypainter

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.FileProvider
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.unit.dp
import io.github.lukasvi.hypainter.engine.EngineSample
import io.github.lukasvi.hypainter.engine.EngineStroke
import io.github.lukasvi.hypainter.engine.PaintingEngine
import io.github.lukasvi.hypainter.engine.createPaintingEngine
import java.io.File
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot

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
    val context = LocalContext.current
    val view = LocalView.current
    val engine = remember { createPaintingEngine() }
    val canvasVersion = remember { mutableStateOf(0) }
    val modelVersion = remember { mutableStateOf(0) }
    val viewport = remember { mutableStateOf(ViewportState()) }
    val latestPressure = remember { mutableStateOf(0f) }
    val exportStatus = remember { mutableStateOf<String?>(null) }
    val projectStatus = remember { mutableStateOf<String?>(null) }
    val debugOverlayVisible = remember { mutableStateOf(false) }
    val debugState = remember { mutableStateOf(CanvasDebugState()) }
    val inputRouter = remember { CanvasInputRouter() }
    val frameInvalidator = remember(view) {
        FrameInvalidator(view) {
            canvasVersion.value++
        }
    }
    val snapshot = remember(canvasVersion.value) { engine.snapshot() }
    val toolbarSnapshot = remember(modelVersion.value) { engine.snapshot() }
    val refreshCanvas = {
        canvasVersion.value++
        Unit
    }
    val refreshModel = {
        modelVersion.value++
        canvasVersion.value++
        Unit
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF15171A)),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInteropFilter { event ->
                    inputRouter.onMotionEvent(
                        event = event,
                        viewport = viewport.value,
                        engine = engine,
                        onViewportChanged = { viewport.value = it },
                        onEngineChanged = refreshCanvas,
                        onEngineChangedNextFrame = { frameInvalidator.request() },
                        onPressure = { latestPressure.value = it },
                        debugEnabled = debugOverlayVisible.value,
                        onDebugChanged = { debugState.value = it },
                    )
                },
        ) {
            val state = viewport.value
            withTransformCompat(state) {
                drawCanvasBackground(snapshot.canvasWidth, snapshot.canvasHeight)
                snapshot.renderedImage?.let { image ->
                    drawImage(image)
                }
                snapshot.committedStrokes.forEach { stroke ->
                    drawStroke(stroke)
                }
                snapshot.activeStroke?.let { drawStroke(it) }
            }
        }

        CanvasToolbar(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .horizontalScroll(rememberScrollState()),
            context = context,
            engine = engine,
            snapshot = toolbarSnapshot,
            latestPressure = latestPressure.value,
            exportStatus = exportStatus.value,
            projectStatus = projectStatus.value,
            debugOverlayVisible = debugOverlayVisible.value,
            onExportStatusChanged = { exportStatus.value = it },
            onProjectStatusChanged = { projectStatus.value = it },
            onDebugOverlayChanged = { debugOverlayVisible.value = it },
            onModelChanged = refreshModel,
        )

        if (BuildConfig.DEBUG && debugOverlayVisible.value) {
            CanvasDebugOverlay(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                state = debugState.value,
                viewport = viewport.value,
                snapshot = snapshot,
            )
        }
    }
}

@Composable
private fun BrushChip(label: String, onClick: () -> Unit) {
    AssistChip(onClick = onClick, label = { Text(label) })
}

@Composable
private fun CanvasToolbar(
    modifier: Modifier,
    context: android.content.Context,
    engine: PaintingEngine,
    snapshot: io.github.lukasvi.hypainter.engine.EngineSnapshot,
    latestPressure: Float,
    exportStatus: String?,
    projectStatus: String?,
    debugOverlayVisible: Boolean,
    onExportStatusChanged: (String) -> Unit,
    onProjectStatusChanged: (String) -> Unit,
    onDebugOverlayChanged: (Boolean) -> Unit,
    onModelChanged: () -> Unit,
) {
    Row(modifier = modifier) {
        AssistChip(
            onClick = {
                engine.clear()
                onModelChanged()
            },
            label = { Text("Clear") },
        )
        Box(modifier = Modifier.size(8.dp))
        AssistChip(
            onClick = {
                engine.undo()
                onModelChanged()
            },
            label = { Text("Undo") },
        )
        Box(modifier = Modifier.size(8.dp))
        AssistChip(
            onClick = {},
            label = {
                Text(
                    "${if (engine.nativeBacked) "Native" else "Kotlin"} · Pressure ${
                        "%.2f".format(latestPressure)
                    }",
                )
            },
        )
        Box(modifier = Modifier.size(8.dp))
        BrushChip("Black") {
            engine.setBrush(snapshot.brush.copy(colorArgb = 0xff000000.toInt()))
            onModelChanged()
        }
        Box(modifier = Modifier.size(8.dp))
        BrushChip("Red") {
            engine.setBrush(snapshot.brush.copy(colorArgb = 0xffd72638.toInt()))
            onModelChanged()
        }
        Box(modifier = Modifier.size(8.dp))
        BrushChip("Blue") {
            engine.setBrush(snapshot.brush.copy(colorArgb = 0xff2563eb.toInt()))
            onModelChanged()
        }
        Box(modifier = Modifier.size(8.dp))
        AssistChip(
            onClick = {
                engine.setBrush(snapshot.brush.copy(radiusPx = (snapshot.brush.radiusPx - 2f).coerceAtLeast(2f)))
                onModelChanged()
            },
            label = { Text("-") },
        )
        Box(modifier = Modifier.size(8.dp))
        AssistChip(
            onClick = {
                engine.setBrush(snapshot.brush.copy(radiusPx = (snapshot.brush.radiusPx + 2f).coerceAtMost(48f)))
                onModelChanged()
            },
            label = { Text("Size ${snapshot.brush.radiusPx.toInt()}") },
        )
        Box(modifier = Modifier.size(8.dp))
        AssistChip(
            onClick = {
                val output = File(context.filesDir, "hypainter-export.png")
                onExportStatusChanged(if (engine.exportPng(output.absolutePath)) "Exported" else "Export failed")
            },
            label = { Text(exportStatus ?: "Export") },
        )
        Box(modifier = Modifier.size(8.dp))
        AssistChip(
            onClick = {
                val output = File(context.filesDir, "hypainter-export.png")
                if (engine.exportPng(output.absolutePath)) {
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        output,
                    )
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(share, "Share HyPainter export"))
                    onExportStatusChanged("Shared")
                } else {
                    onExportStatusChanged("Share failed")
                }
            },
            label = { Text("Share") },
        )
        Box(modifier = Modifier.size(8.dp))
        AssistChip(
            onClick = {
                val output = File(context.filesDir, "hypainter-project.hyp")
                onProjectStatusChanged(if (engine.saveProject(output.absolutePath)) "Saved" else "Save failed")
            },
            label = { Text(projectStatus ?: "Save") },
        )
        Box(modifier = Modifier.size(8.dp))
        AssistChip(
            onClick = {
                val input = File(context.filesDir, "hypainter-project.hyp")
                onProjectStatusChanged(
                    if (engine.loadProject(input.absolutePath)) {
                        onModelChanged()
                        "Loaded"
                    } else {
                        "Load failed"
                    },
                )
            },
            label = { Text("Load") },
        )
        Box(modifier = Modifier.size(8.dp))
        AssistChip(
            onClick = {
                engine.addLayer()
                onModelChanged()
            },
            label = { Text("+ Layer") },
        )
        snapshot.layers.forEach { layer ->
            Box(modifier = Modifier.size(8.dp))
            AssistChip(
                onClick = {
                    engine.selectLayer(layer.id)
                    onModelChanged()
                },
                label = {
                    Text(
                        "${if (layer.id == snapshot.activeLayerId) "*" else ""}${layer.name}",
                    )
                },
            )
            Box(modifier = Modifier.size(8.dp))
            AssistChip(
                onClick = {
                    engine.toggleLayerVisibility(layer.id)
                    onModelChanged()
                },
                label = { Text(if (layer.visible) "Hide" else "Show") },
            )
        }
        if (BuildConfig.DEBUG) {
            Box(modifier = Modifier.size(8.dp))
            AssistChip(
                onClick = { onDebugOverlayChanged(!debugOverlayVisible) },
                label = { Text(if (debugOverlayVisible) "Debug On" else "Debug") },
            )
        }
    }
}

private class FrameInvalidator(
    private val view: View,
    private val onFrame: () -> Unit,
) {
    private var scheduled = false

    fun request() {
        if (scheduled) {
            return
        }
        scheduled = true
        view.postOnAnimation {
            scheduled = false
            onFrame()
        }
    }
}

@Composable
private fun CanvasDebugOverlay(
    modifier: Modifier,
    state: CanvasDebugState,
    viewport: ViewportState,
    snapshot: io.github.lukasvi.hypainter.engine.EngineSnapshot,
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
        Text("Pressure: ${"%.2f".format(state.pressure)}", color = Color.White)
        Text("Screen: ${state.screenPosition.format()}", color = Color.White)
        Text("Canvas: ${state.canvasPosition.format()}", color = Color.White)
        Text(
            "View: pan=${viewport.pan.format()} scale=${"%.2f".format(viewport.scale)} rot=${"%.1f".format(viewport.rotation)}",
            color = Color.White,
        )
        Text("Active stroke: ${snapshot.activeStroke?.points?.size ?: 0}", color = Color.White)
    }
}

private class CanvasInputRouter {
    private var stylusPointerId: Int? = null
    private var lastTouchGesture: TouchGestureFrame? = null
    private var lastDebugState = CanvasDebugState()
    private var fingerStreamActive = false
    private var strokeSamples = 0
    private var historySamples = 0
    private var lastLogTime = 0L
    private var lastPressureReportTime = 0L

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
        val consumed = if (stylusPointerId != null || event.actionPointerIsStylus()) {
            handleStylusEvent(event, viewport, engine, onEngineChanged, onEngineChangedNextFrame, onPressure)
        } else {
            handleTouchEvent(event, viewport, onViewportChanged)
        }
        publishDebug(event, viewport, consumed, debugEnabled, onDebugChanged)
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
                    return stylusPointerId != null
                }
                stylusPointerId = event.getPointerId(pointerIndex)
                fingerStreamActive = false
                lastTouchGesture = null
                strokeSamples = 1
                historySamples = 0
                lastPressureReportTime = event.eventTime
                engine.beginStroke(event.toSample(pointerIndex, viewport, historicalIndex = null))
                onPressure(event.getPressure(pointerIndex).coerceIn(0f, 1f))
                onEngineChanged()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val pointerIndex = event.findActiveStylusPointerIndex()
                if (pointerIndex == null) {
                    engine.endStroke()
                    stylusPointerId = null
                    lastTouchGesture = null
                    fingerStreamActive = false
                    onEngineChanged()
                    return true
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
                val activePointerId = stylusPointerId
                if (activePointerId != null && event.getPointerId(event.actionIndex) == activePointerId) {
                    engine.endStroke()
                    stylusPointerId = null
                    lastTouchGesture = null
                    fingerStreamActive = false
                    onEngineChanged()
                }
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                if (stylusPointerId != null) {
                    engine.endStroke()
                    stylusPointerId = null
                    lastTouchGesture = null
                    fingerStreamActive = false
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
            lastTouchGesture = null
            return false
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                fingerStreamActive = true
                lastTouchGesture = null
                return true
            }

            MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount < 2) {
                    lastTouchGesture = null
                    return fingerStreamActive
                }

                val next = event.touchGestureFrame()
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

            MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                lastTouchGesture = null
                if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
                    fingerStreamActive = false
                }
                return fingerStreamActive
            }
        }

        return fingerStreamActive
    }

    private fun MotionEvent.findActiveStylusPointerIndex(): Int? {
        val activePointerId = stylusPointerId ?: return null
        val pointerIndex = findPointerIndex(activePointerId)
        if (pointerIndex < 0) {
            stylusPointerId = null
            return null
        }
        return pointerIndex
    }

    private fun publishDebug(
        event: MotionEvent,
        viewport: ViewportState,
        consumed: Boolean,
        debugEnabled: Boolean,
        onDebugChanged: (CanvasDebugState) -> Unit,
    ) {
        if (!BuildConfig.DEBUG || !debugEnabled) {
            return
        }

        val pointerIndex = event.actionIndex.coerceIn(0, event.pointerCount - 1)
        val screen = Offset(event.getX(pointerIndex), event.getY(pointerIndex))
        val next = CanvasDebugState(
            route = when {
                stylusPointerId != null -> "stylus"
                event.pointerCount >= 2 && event.allPointersAreFingers() -> "two-finger"
                event.allPointersAreFingers() -> "single-finger"
                else -> "ignored"
            },
            action = event.actionMasked.actionName(),
            toolType = event.getToolType(pointerIndex).toolTypeName(),
            pointerCount = event.pointerCount,
            consumed = consumed,
            pressure = event.getPressure(pointerIndex).coerceIn(0f, 1f),
            screenPosition = screen,
            canvasPosition = viewport.toCanvas(screen),
            strokeSamples = strokeSamples,
            historySamples = historySamples,
        )
        lastDebugState = next
        onDebugChanged(next)

        if (event.eventTime - lastLogTime >= DEBUG_LOG_INTERVAL_MS || event.actionMasked != MotionEvent.ACTION_MOVE) {
            lastLogTime = event.eventTime
            Log.d(DEBUG_LOG_TAG, next.toLogLine(viewport))
        }
    }
}

private fun DrawScope.withTransformCompat(
    viewport: ViewportState,
    block: DrawScope.() -> Unit,
) {
    withTransform({
        translate(viewport.pan.x, viewport.pan.y)
        rotate(viewport.rotation, pivot = Offset.Zero)
        scale(viewport.scale, viewport.scale, pivot = Offset.Zero)
    }) {
        block()
    }
}

private fun DrawScope.drawCanvasBackground(width: Int, height: Int) {
    drawRect(
        color = Color.White,
        size = androidx.compose.ui.geometry.Size(width.toFloat(), height.toFloat()),
    )
}

private fun DrawScope.drawStroke(stroke: EngineStroke) {
    for (index in 1 until stroke.points.size) {
        val from = stroke.points[index - 1]
        val to = stroke.points[index]
        drawLine(
            color = Color(stroke.brush.colorArgb).copy(alpha = to.pressure.coerceIn(0.1f, 1f)),
            start = from.position,
            end = to.position,
            strokeWidth = stroke.brush.radiusPx * 2f * to.pressure.coerceIn(0.1f, 1f),
            cap = StrokeCap.Round,
        )
    }

    stroke.points.singleOrNull()?.let { point ->
        drawCircle(
            color = Color(stroke.brush.colorArgb).copy(alpha = point.pressure.coerceIn(0.1f, 1f)),
            radius = stroke.brush.radiusPx * point.pressure.coerceIn(0.1f, 1f),
            center = point.position,
            style = Stroke(width = 1f),
        )
    }
}

private data class CanvasDebugState(
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
) {
    fun toLogLine(viewport: ViewportState): String {
        return "route=$route action=$action tool=$toolType pointers=$pointerCount consumed=$consumed " +
            "pressure=${"%.2f".format(pressure)} screen=${screenPosition.format()} canvas=${canvasPosition.format()} " +
            "samples=$strokeSamples history=$historySamples pan=${viewport.pan.format()} " +
            "scale=${"%.2f".format(viewport.scale)} rotation=${"%.1f".format(viewport.rotation)}"
    }
}

private fun MotionEvent.actionPointerIsStylus(): Boolean {
    return actionIndex in 0 until pointerCount && isStylusPointer(actionIndex)
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

private fun Offset.format(): String {
    if (x.isNaN() || y.isNaN()) {
        return "n/a"
    }
    return "${"%.1f".format(x)},${"%.1f".format(y)}"
}

private fun Int.toolTypeName(): String {
    return when (this) {
        MotionEvent.TOOL_TYPE_FINGER -> "finger"
        MotionEvent.TOOL_TYPE_STYLUS -> "stylus"
        MotionEvent.TOOL_TYPE_ERASER -> "eraser"
        MotionEvent.TOOL_TYPE_MOUSE -> "mouse"
        MotionEvent.TOOL_TYPE_UNKNOWN -> "unknown"
        else -> "tool-$this"
    }
}

private fun Int.actionName(): String {
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

private const val DEBUG_LOG_TAG = "HyPainterInput"
private const val DEBUG_LOG_INTERVAL_MS = 250L
private const val PRESSURE_REPORT_INTERVAL_MS = 80L
