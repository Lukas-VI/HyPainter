package io.github.lukasvi.hypainter

import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.FileProvider
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import io.github.lukasvi.hypainter.debug.CanvasDebugOverlay
import io.github.lukasvi.hypainter.debug.CanvasDebugState
import io.github.lukasvi.hypainter.engine.EngineStroke
import io.github.lukasvi.hypainter.engine.PaintingEngine
import io.github.lukasvi.hypainter.engine.createPaintingEngine
import io.github.lukasvi.hypainter.input.CanvasInputRouter
import io.github.lukasvi.hypainter.render.CanvasRenderOptions
import io.github.lukasvi.hypainter.render.toFilterQuality
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val toolbarBusy = remember { mutableStateOf(false) }
    val toolbarBounds = remember { mutableStateOf<Rect?>(null) }
    val debugChipBounds = remember { mutableStateOf<Rect?>(null) }
    var controlsHiddenForStylus by remember { mutableStateOf(false) }
    val debugOverlayVisible = remember { mutableStateOf(false) }
    val debugState = remember { mutableStateOf(CanvasDebugState()) }
    val coroutineScope = rememberCoroutineScope()
    val inputRouter = remember { CanvasInputRouter() }
    val renderOptions = remember { CanvasRenderOptions() }
    val frameInvalidator = remember(view) {
        FrameInvalidator(view) {
            canvasVersion.value++
        }
    }
    val controlsHider = remember { StylusControlsHider() }
    val snapshot = remember(canvasVersion.value) { engine.canvasSnapshot() }
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
    val hideControlsForStylus = {
        controlsHider.hideUntilHover()
        controlsHiddenForStylus = controlsHider.hidden
        Unit
    }
    val showControlsForStylusHover = {
        controlsHider.showForHoverInControls()
        controlsHiddenForStylus = controlsHider.hidden
        Unit
    }
    val showControlsForStylusLeave = {
        controlsHider.showForLeaveControls()
        controlsHiddenForStylus = controlsHider.hidden
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
                    if (toolbarBusy.value) {
                        return@pointerInteropFilter true
                    }
                    if (event.hasStylusOrEraserPointer()) {
                        val insideControls = event.isInsideAny(toolbarBounds.value, debugChipBounds.value)
                        if (event.isStylusHoverEvent()) {
                            if (insideControls) {
                                showControlsForStylusHover()
                            } else {
                                showControlsForStylusLeave()
                            }
                        } else if (!insideControls) {
                            showControlsForStylusLeave()
                        } else if (
                            !controlsHiddenForStylus &&
                            event.isStylusPressEvent() &&
                            controlsHider.shouldHidePressInControls()
                        ) {
                            hideControlsForStylus()
                            return@pointerInteropFilter true
                        }
                    }
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
                    drawImage(
                        image = image,
                        filterQuality = renderOptions.bitmapSampling.toFilterQuality(),
                    )
                }
                if (snapshot.renderedImage == null) {
                    for (index in snapshot.committedStrokes.indices) {
                        val stroke = snapshot.committedStrokes[index]
                        if (snapshot.layerIsVisible(stroke.layerId)) {
                            drawStroke(stroke)
                        }
                    }
                }
                snapshot.activeImage?.let { image ->
                    drawImage(
                        image = image,
                        filterQuality = renderOptions.bitmapSampling.toFilterQuality(),
                    )
                }
                if (snapshot.activeImage == null) {
                    snapshot.activeStroke?.let { stroke ->
                        if (snapshot.layerIsVisible(stroke.layerId)) {
                            drawStroke(stroke)
                        }
                    }
                }
            }
        }

        if (!controlsHiddenForStylus) {
            CanvasToolbar(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .onGloballyPositioned { coordinates ->
                        toolbarBounds.value = coordinates.boundsInRoot()
                    }
                    .pointerInteropFilter { event ->
                        if (event.isStylusHoverEvent()) {
                            showControlsForStylusHover()
                            false
                        } else if (event.isStylusPressEvent() && controlsHider.shouldHidePressInControls()) {
                            hideControlsForStylus()
                            true
                        } else {
                            false
                        }
                    }
                    .horizontalScroll(rememberScrollState()),
                context = context,
                engine = engine,
                snapshot = toolbarSnapshot,
                latestPressure = latestPressure.value,
                exportStatus = exportStatus.value,
                projectStatus = projectStatus.value,
                toolbarBusy = toolbarBusy.value,
                onExportStatusChanged = { exportStatus.value = it },
                onProjectStatusChanged = { projectStatus.value = it },
                onToolbarBusyChanged = { toolbarBusy.value = it },
                onModelChanged = refreshModel,
                launchBackground = { block -> coroutineScope.launch { block() } },
            )
        }

        if (BuildConfig.DEBUG && !controlsHiddenForStylus) {
            AssistChip(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .onGloballyPositioned { coordinates ->
                        debugChipBounds.value = coordinates.boundsInRoot()
                    }
                    .pointerInteropFilter { event ->
                        if (event.isStylusHoverEvent()) {
                            showControlsForStylusHover()
                            false
                        } else if (event.isStylusPressEvent() && controlsHider.shouldHidePressInControls()) {
                            hideControlsForStylus()
                            true
                        } else {
                            false
                        }
                    },
                onClick = { debugOverlayVisible.value = !debugOverlayVisible.value },
                label = { Text(if (debugOverlayVisible.value) "Debug On" else "Debug") },
            )
        }

        if (BuildConfig.DEBUG && debugOverlayVisible.value) {
            CanvasDebugOverlay(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
                state = debugState.value,
                viewport = viewport.value,
                snapshot = snapshot,
            )
        }
    }
}

@Composable
private fun BrushChip(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    AssistChip(onClick = onClick, enabled = enabled, label = { Text(label) })
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
    toolbarBusy: Boolean,
    onExportStatusChanged: (String) -> Unit,
    onProjectStatusChanged: (String) -> Unit,
    onToolbarBusyChanged: (Boolean) -> Unit,
    onModelChanged: () -> Unit,
    launchBackground: (suspend () -> Unit) -> Unit,
) {
    Row(modifier = modifier) {
        AssistChip(
            onClick = {
                engine.clear()
                onModelChanged()
            },
            enabled = !toolbarBusy,
            label = { Text("Clear") },
        )
        Box(modifier = Modifier.size(8.dp))
        AssistChip(
            onClick = {
                engine.undo()
                onModelChanged()
            },
            enabled = !toolbarBusy,
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
        BrushChip("Black", enabled = !toolbarBusy) {
            engine.setBrush(snapshot.brush.copy(colorArgb = 0xff000000.toInt()))
            onModelChanged()
        }
        Box(modifier = Modifier.size(8.dp))
        BrushChip("Red", enabled = !toolbarBusy) {
            engine.setBrush(snapshot.brush.copy(colorArgb = 0xffd72638.toInt()))
            onModelChanged()
        }
        Box(modifier = Modifier.size(8.dp))
        BrushChip("Blue", enabled = !toolbarBusy) {
            engine.setBrush(snapshot.brush.copy(colorArgb = 0xff2563eb.toInt()))
            onModelChanged()
        }
        Box(modifier = Modifier.size(8.dp))
        AssistChip(
            onClick = {
                engine.setBrush(snapshot.brush.copy(radiusPx = (snapshot.brush.radiusPx - 2f).coerceAtLeast(2f)))
                onModelChanged()
            },
            enabled = !toolbarBusy,
            label = { Text("-") },
        )
        Box(modifier = Modifier.size(8.dp))
        AssistChip(
            onClick = {
                engine.setBrush(snapshot.brush.copy(radiusPx = (snapshot.brush.radiusPx + 2f).coerceAtMost(48f)))
                onModelChanged()
            },
            enabled = !toolbarBusy,
            label = { Text("Size ${snapshot.brush.radiusPx.toInt()}") },
        )
        Box(modifier = Modifier.size(8.dp))
        AssistChip(
            onClick = {
                val output = File(context.filesDir, "hypainter-export.png")
                runToolbarIo(
                    busy = toolbarBusy,
                    setBusy = onToolbarBusyChanged,
                    launchBackground = launchBackground,
                    onStart = { onExportStatusChanged("Exporting") },
                    block = { engine.exportPng(output.absolutePath) },
                    onResult = { ok -> onExportStatusChanged(if (ok) "Exported" else "Export failed") },
                    onError = { onExportStatusChanged("Export failed") },
                )
            },
            enabled = !toolbarBusy,
            label = { Text(exportStatus ?: "Export") },
        )
        Box(modifier = Modifier.size(8.dp))
        AssistChip(
            onClick = {
                val output = File(context.filesDir, "hypainter-export.png")
                runToolbarIo(
                    busy = toolbarBusy,
                    setBusy = onToolbarBusyChanged,
                    launchBackground = launchBackground,
                    onStart = { onExportStatusChanged("Sharing") },
                    block = { engine.exportPng(output.absolutePath) },
                    onResult = { ok ->
                        if (ok) {
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
                    onError = { onExportStatusChanged("Share failed") },
                )
            },
            enabled = !toolbarBusy,
            label = { Text("Share") },
        )
        Box(modifier = Modifier.size(8.dp))
        AssistChip(
            onClick = {
                val output = File(context.filesDir, "hypainter-project.hyp")
                runToolbarIo(
                    busy = toolbarBusy,
                    setBusy = onToolbarBusyChanged,
                    launchBackground = launchBackground,
                    onStart = { onProjectStatusChanged("Saving") },
                    block = { engine.saveProject(output.absolutePath) },
                    onResult = { ok -> onProjectStatusChanged(if (ok) "Saved" else "Save failed") },
                    onError = { onProjectStatusChanged("Save failed") },
                )
            },
            enabled = !toolbarBusy,
            label = { Text(projectStatus ?: "Save") },
        )
        Box(modifier = Modifier.size(8.dp))
        AssistChip(
            onClick = {
                val input = File(context.filesDir, "hypainter-project.hyp")
                runToolbarIo(
                    busy = toolbarBusy,
                    setBusy = onToolbarBusyChanged,
                    launchBackground = launchBackground,
                    onStart = { onProjectStatusChanged("Loading") },
                    block = { engine.loadProject(input.absolutePath) },
                    onResult = { ok ->
                        if (ok) {
                            onModelChanged()
                            onProjectStatusChanged("Loaded")
                        } else {
                            onProjectStatusChanged("Load failed")
                        }
                    },
                    onError = { onProjectStatusChanged("Load failed") },
                )
            },
            enabled = !toolbarBusy,
            label = { Text("Load") },
        )
        Box(modifier = Modifier.size(8.dp))
        AssistChip(
            onClick = {
                engine.addLayer()
                onModelChanged()
            },
            enabled = !toolbarBusy,
            label = { Text("+ Layer") },
        )
        snapshot.layers.forEach { layer ->
            Box(modifier = Modifier.size(8.dp))
            AssistChip(
                onClick = {
                    engine.selectLayer(layer.id)
                    onModelChanged()
                },
                enabled = !toolbarBusy,
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
                enabled = !toolbarBusy,
                label = { Text(if (layer.visible) "Hide" else "Show") },
            )
        }
    }
}

private fun <T> runToolbarIo(
    busy: Boolean,
    setBusy: (Boolean) -> Unit,
    launchBackground: (suspend () -> Unit) -> Unit,
    onStart: () -> Unit,
    block: () -> T,
    onResult: (T) -> Unit,
    onError: (Throwable) -> Unit,
) {
    if (busy) {
        return
    }
    setBusy(true)
    onStart()
    launchBackground {
        val result = runCatching {
            withContext(Dispatchers.IO) {
                block()
            }
        }
        setBusy(false)
        result.fold(
            onSuccess = onResult,
            onFailure = onError,
        )
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

private fun MotionEvent.hasStylusOrEraserPointer(): Boolean {
    for (index in 0 until pointerCount) {
        val toolType = getToolType(index)
        if (toolType == MotionEvent.TOOL_TYPE_STYLUS || toolType == MotionEvent.TOOL_TYPE_ERASER) {
            return true
        }
    }
    return false
}

private fun MotionEvent.isStylusHoverEvent(): Boolean {
    return hasStylusOrEraserPointer() &&
        (actionMasked == MotionEvent.ACTION_HOVER_ENTER ||
            actionMasked == MotionEvent.ACTION_HOVER_MOVE ||
            actionMasked == MotionEvent.ACTION_HOVER_EXIT)
}

private fun MotionEvent.isStylusPressEvent(): Boolean {
    return hasStylusOrEraserPointer() &&
        (actionMasked == MotionEvent.ACTION_DOWN ||
            actionMasked == MotionEvent.ACTION_POINTER_DOWN ||
            actionMasked == MotionEvent.ACTION_MOVE)
}

private fun MotionEvent.isInside(bounds: Rect): Boolean {
    for (index in 0 until pointerCount) {
        val toolType = getToolType(index)
        if (toolType == MotionEvent.TOOL_TYPE_STYLUS || toolType == MotionEvent.TOOL_TYPE_ERASER) {
            val x = getX(index)
            val y = getY(index)
            if (x >= bounds.left && x <= bounds.right && y >= bounds.top && y <= bounds.bottom) {
                return true
            }
        }
    }
    return false
}

private fun MotionEvent.isInsideAny(vararg bounds: Rect?): Boolean {
    for (bound in bounds) {
        if (bound != null && isInside(bound)) {
            return true
        }
    }
    return false
}

private fun DrawScope.withTransformCompat(
    viewport: ViewportState,
    block: DrawScope.() -> Unit,
) {
    withTransform({
        // Keep this order aligned with ViewportState.toScreen(): pan + rotate(canvas * scale).
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

private fun io.github.lukasvi.hypainter.engine.EngineSnapshot.layerIsVisible(layerId: Long): Boolean {
    for (index in layers.indices) {
        val layer = layers[index]
        if (layer.id == layerId) {
            return layer.visible
        }
    }
    return false
}
