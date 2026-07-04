package io.github.lukasvi.hypainter

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.FileProvider
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.lukasvi.hypainter.debug.CanvasDebugOverlay
import io.github.lukasvi.hypainter.debug.CanvasDebugState
import io.github.lukasvi.hypainter.engine.EngineStroke
import io.github.lukasvi.hypainter.engine.PaintingEngine
import io.github.lukasvi.hypainter.engine.ProjectCodec
import io.github.lukasvi.hypainter.engine.createPaintingEngine
import io.github.lukasvi.hypainter.render.BitmapSampling
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
    var engine by remember { mutableStateOf<PaintingEngine>(createPaintingEngine()) }
    var documentTitle by remember { mutableStateOf("Untitled") }
    val canvasVersion = remember { mutableStateOf(0) }
    val modelVersion = remember { mutableStateOf(0) }
    val viewport = remember { mutableStateOf(ViewportState()) }
    val latestPressure = remember { mutableStateOf(0f) }
    val exportStatus = remember { mutableStateOf<String?>(null) }
    val projectStatus = remember { mutableStateOf<String?>(null) }
    val toolbarBusy = remember { mutableStateOf(false) }
    val debugOverlayVisible = remember { mutableStateOf(false) }
    val debugState = remember { mutableStateOf(CanvasDebugState()) }
    var newCanvasDialogVisible by remember { mutableStateOf(false) }
    var canvasSettingsVisible by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var inputRouter by remember { mutableStateOf(CanvasInputRouter()) }
    var renderOptions by remember { mutableStateOf(CanvasRenderOptions()) }
    val frameInvalidator = remember(view) {
        FrameInvalidator(view) {
            canvasVersion.value++
        }
    }
    val snapshot = remember(canvasVersion.value, engine) { engine.canvasSnapshot() }
    val toolbarSnapshot = remember(modelVersion.value, engine) { engine.snapshot() }
    val refreshCanvas = {
        canvasVersion.value++
        Unit
    }
    val refreshModel = {
        modelVersion.value++
        canvasVersion.value++
        Unit
    }
    val resetInputAndViewport = {
        inputRouter = CanvasInputRouter()
        viewport.value = ViewportState()
        Unit
    }
    val replaceDocument = { width: Int, height: Int, title: String ->
        engine = createPaintingEngine(width, height)
        documentTitle = title.ifBlank { "Untitled" }
        exportStatus.value = null
        projectStatus.value = null
        latestPressure.value = 0f
        resetInputAndViewport()
        refreshModel()
        Unit
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF15171A)),
    ) {
        AppCommandBar(
            modifier = Modifier.fillMaxWidth(),
            context = context,
            engine = engine,
            snapshot = toolbarSnapshot,
            documentTitle = documentTitle,
            latestPressure = latestPressure.value,
            exportStatus = exportStatus.value,
            projectStatus = projectStatus.value,
            toolbarBusy = toolbarBusy.value,
            renderOptions = renderOptions,
            onRenderOptionsChanged = {
                renderOptions = it
                refreshCanvas()
            },
            onNewCanvas = { newCanvasDialogVisible = true },
            onCanvasSettings = { canvasSettingsVisible = true },
            onResetView = { resetInputAndViewport() },
            onExportStatusChanged = { exportStatus.value = it },
            onProjectStatusChanged = { projectStatus.value = it },
            onToolbarBusyChanged = { toolbarBusy.value = it },
            onModelChanged = refreshModel,
            onDocumentLoaded = { loadedEngine, loadedTitle ->
                engine = loadedEngine
                documentTitle = loadedTitle
                latestPressure.value = 0f
                resetInputAndViewport()
                refreshModel()
            },
            launchBackground = { block -> coroutineScope.launch { block() } },
            debugEnabled = BuildConfig.DEBUG,
            debugOverlayVisible = debugOverlayVisible.value,
            onDebugOverlayChanged = { debugOverlayVisible.value = it },
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInteropFilter { event ->
                        if (toolbarBusy.value) {
                            return@pointerInteropFilter true
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

        if (newCanvasDialogVisible) {
            NewCanvasDialog(
                onDismiss = { newCanvasDialogVisible = false },
                onCreate = { width, height, title ->
                    newCanvasDialogVisible = false
                    replaceDocument(width, height, title)
                },
            )
        }

        if (canvasSettingsVisible) {
            CanvasSettingsDialog(
                snapshot = toolbarSnapshot,
                renderOptions = renderOptions,
                onRenderOptionsChanged = {
                    renderOptions = it
                    refreshCanvas()
                },
                onResetView = {
                    resetInputAndViewport()
                    canvasSettingsVisible = false
                },
                onDismiss = { canvasSettingsVisible = false },
            )
        }
    }
}

@Composable
private fun AppCommandBar(
    modifier: Modifier,
    context: android.content.Context,
    engine: PaintingEngine,
    snapshot: io.github.lukasvi.hypainter.engine.EngineSnapshot,
    documentTitle: String,
    latestPressure: Float,
    exportStatus: String?,
    projectStatus: String?,
    toolbarBusy: Boolean,
    renderOptions: CanvasRenderOptions,
    onRenderOptionsChanged: (CanvasRenderOptions) -> Unit,
    onNewCanvas: () -> Unit,
    onCanvasSettings: () -> Unit,
    onResetView: () -> Unit,
    onExportStatusChanged: (String) -> Unit,
    onProjectStatusChanged: (String) -> Unit,
    onToolbarBusyChanged: (Boolean) -> Unit,
    onModelChanged: () -> Unit,
    onDocumentLoaded: (PaintingEngine, String) -> Unit,
    launchBackground: (suspend () -> Unit) -> Unit,
    debugEnabled: Boolean,
    debugOverlayVisible: Boolean,
    onDebugOverlayChanged: (Boolean) -> Unit,
) {
    Surface(modifier = modifier, color = Color(0xFF202329), tonalElevation = 4.dp) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FileMenuButton(
                context = context,
                engine = engine,
                toolbarBusy = toolbarBusy,
                onNewCanvas = onNewCanvas,
                onExportStatusChanged = onExportStatusChanged,
                onProjectStatusChanged = onProjectStatusChanged,
                onToolbarBusyChanged = onToolbarBusyChanged,
                onModelChanged = onModelChanged,
                onDocumentLoaded = onDocumentLoaded,
                launchBackground = launchBackground,
            )
            Spacer(modifier = Modifier.width(8.dp))
            CanvasMenuButton(
                onCanvasSettings = onCanvasSettings,
                onResetView = onResetView,
            )
            Spacer(modifier = Modifier.width(8.dp))
            ViewMenuButton(
                renderOptions = renderOptions,
                onRenderOptionsChanged = onRenderOptionsChanged,
                debugEnabled = debugEnabled,
                debugOverlayVisible = debugOverlayVisible,
                onDebugOverlayChanged = onDebugOverlayChanged,
            )
            Spacer(modifier = Modifier.width(12.dp))
            AssistChip(
                onClick = {},
                label = {
                    Text(
                        "$documentTitle · ${snapshot.canvasWidth}x${snapshot.canvasHeight} · ${
                            if (engine.nativeBacked) "Native" else "Kotlin"
                        }",
                    )
                },
            )
            Spacer(modifier = Modifier.width(8.dp))
            CanvasToolbar(
                modifier = Modifier,
                engine = engine,
                snapshot = snapshot,
                latestPressure = latestPressure,
                toolbarBusy = toolbarBusy,
                onModelChanged = onModelChanged,
            )
        }
    }
}

@Composable
private fun FileMenuButton(
    context: android.content.Context,
    engine: PaintingEngine,
    toolbarBusy: Boolean,
    onNewCanvas: () -> Unit,
    onExportStatusChanged: (String) -> Unit,
    onProjectStatusChanged: (String) -> Unit,
    onToolbarBusyChanged: (Boolean) -> Unit,
    onModelChanged: () -> Unit,
    onDocumentLoaded: (PaintingEngine, String) -> Unit,
    launchBackground: (suspend () -> Unit) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Button(onClick = { expanded = true }, enabled = !toolbarBusy) {
            Text("File")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("New Canvas") },
                onClick = {
                    expanded = false
                    onNewCanvas()
                },
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Save Draft") },
                onClick = {
                    expanded = false
                    val output = File(context.filesDir, PROJECT_FILE_NAME)
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
            )
            DropdownMenuItem(
                text = { Text("Load Draft") },
                onClick = {
                    expanded = false
                    val input = File(context.filesDir, PROJECT_FILE_NAME)
                    runToolbarIo(
                        busy = toolbarBusy,
                        setBusy = onToolbarBusyChanged,
                        launchBackground = launchBackground,
                        onStart = { onProjectStatusChanged("Loading") },
                        block = {
                            val project = ProjectCodec.load(input.absolutePath)
                            if (project == null) {
                                null
                            } else {
                                createPaintingEngine(project.canvasWidth, project.canvasHeight).takeIf {
                                    it.loadProject(input.absolutePath)
                                }
                            }
                        },
                        onResult = { loadedEngine ->
                            if (loadedEngine != null) {
                                onDocumentLoaded(loadedEngine, PROJECT_FILE_NAME)
                                onProjectStatusChanged("Loaded")
                            } else {
                                onProjectStatusChanged("Load failed")
                            }
                        },
                        onError = { onProjectStatusChanged("Load failed") },
                    )
                },
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Export PNG") },
                onClick = {
                    expanded = false
                    val output = File(context.filesDir, EXPORT_FILE_NAME)
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
            )
            DropdownMenuItem(
                text = { Text("Share PNG") },
                onClick = {
                    expanded = false
                    shareExportedPng(
                        context = context,
                        engine = engine,
                        toolbarBusy = toolbarBusy,
                        launchBackground = launchBackground,
                        onToolbarBusyChanged = onToolbarBusyChanged,
                        onExportStatusChanged = onExportStatusChanged,
                    )
                },
            )
        }
    }
}

@Composable
private fun CanvasMenuButton(
    onCanvasSettings: () -> Unit,
    onResetView: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Button(onClick = { expanded = true }) {
            Text("Canvas")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Canvas Settings") },
                onClick = {
                    expanded = false
                    onCanvasSettings()
                },
            )
            DropdownMenuItem(
                text = { Text("Reset View") },
                onClick = {
                    expanded = false
                    onResetView()
                },
            )
        }
    }
}

@Composable
private fun ViewMenuButton(
    renderOptions: CanvasRenderOptions,
    onRenderOptionsChanged: (CanvasRenderOptions) -> Unit,
    debugEnabled: Boolean,
    debugOverlayVisible: Boolean,
    onDebugOverlayChanged: (Boolean) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Button(onClick = { expanded = true }) {
            Text("View")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            BitmapSampling.entries.forEach { sampling ->
                DropdownMenuItem(
                    text = { Text(sampling.label()) },
                    onClick = {
                        expanded = false
                        onRenderOptionsChanged(renderOptions.copy(bitmapSampling = sampling))
                    },
                )
            }
            if (debugEnabled) {
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text(if (debugOverlayVisible) "Hide Debug Overlay" else "Show Debug Overlay") },
                    onClick = {
                        expanded = false
                        onDebugOverlayChanged(!debugOverlayVisible)
                    },
                )
            }
        }
    }
}

@Composable
private fun NewCanvasDialog(
    onDismiss: () -> Unit,
    onCreate: (Int, Int, String) -> Unit,
) {
    var title by remember { mutableStateOf("Untitled") }
    var widthText by remember { mutableStateOf("2048") }
    var heightText by remember { mutableStateOf("2048") }
    val width = widthText.toIntOrNull()
    val height = heightText.toIntOrNull()
    val valid = width != null && height != null && width in CANVAS_MIN_SIZE..CANVAS_MAX_SIZE &&
        height in CANVAS_MIN_SIZE..CANVAS_MAX_SIZE

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Canvas") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Name") },
                    singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        modifier = Modifier.weight(1f),
                        value = widthText,
                        onValueChange = { widthText = it.filter(Char::isDigit).take(5) },
                        label = { Text("Width") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    OutlinedTextField(
                        modifier = Modifier.weight(1f),
                        value = heightText,
                        onValueChange = { heightText = it.filter(Char::isDigit).take(5) },
                        label = { Text("Height") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PresetButton("1024", 1024, 1024) { presetWidth, presetHeight ->
                        widthText = presetWidth.toString()
                        heightText = presetHeight.toString()
                    }
                    PresetButton("2048", 2048, 2048) { presetWidth, presetHeight ->
                        widthText = presetWidth.toString()
                        heightText = presetHeight.toString()
                    }
                    PresetButton("4K", 4096, 4096) { presetWidth, presetHeight ->
                        widthText = presetWidth.toString()
                        heightText = presetHeight.toString()
                    }
                }
                Text("Allowed size: $CANVAS_MIN_SIZE-$CANVAS_MAX_SIZE px")
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = { onCreate(checkNotNull(width), checkNotNull(height), title) },
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun PresetButton(
    label: String,
    width: Int,
    height: Int,
    onSelect: (Int, Int) -> Unit,
) {
    TextButton(onClick = { onSelect(width, height) }) {
        Text(label)
    }
}

@Composable
private fun CanvasSettingsDialog(
    snapshot: io.github.lukasvi.hypainter.engine.EngineSnapshot,
    renderOptions: CanvasRenderOptions,
    onRenderOptionsChanged: (CanvasRenderOptions) -> Unit,
    onResetView: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Canvas Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Size ${snapshot.canvasWidth} x ${snapshot.canvasHeight}px")
                Text("Bitmap sampling")
                BitmapSampling.entries.forEach { sampling ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = renderOptions.bitmapSampling == sampling,
                            onClick = { onRenderOptionsChanged(renderOptions.copy(bitmapSampling = sampling)) },
                        )
                        Text(sampling.label())
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        },
        dismissButton = {
            TextButton(onClick = onResetView) {
                Text("Reset View")
            }
        },
    )
}

@Composable
private fun BrushChip(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    AssistChip(onClick = onClick, enabled = enabled, label = { Text(label) })
}

@Composable
private fun CanvasToolbar(
    modifier: Modifier,
    engine: PaintingEngine,
    snapshot: io.github.lukasvi.hypainter.engine.EngineSnapshot,
    latestPressure: Float,
    toolbarBusy: Boolean,
    onModelChanged: () -> Unit,
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

private fun shareExportedPng(
    context: android.content.Context,
    engine: PaintingEngine,
    toolbarBusy: Boolean,
    launchBackground: (suspend () -> Unit) -> Unit,
    onToolbarBusyChanged: (Boolean) -> Unit,
    onExportStatusChanged: (String) -> Unit,
) {
    val output = File(context.filesDir, EXPORT_FILE_NAME)
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
}

private fun BitmapSampling.label(): String {
    return when (this) {
        BitmapSampling.PixelPerfect -> "Pixel Perfect"
        BitmapSampling.Nearest -> "Nearest"
        BitmapSampling.Linear -> "Linear"
        BitmapSampling.Bilinear -> "Bilinear"
        BitmapSampling.Bicubic -> "Bicubic"
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

private const val PROJECT_FILE_NAME = "hypainter-project.hyp"
private const val EXPORT_FILE_NAME = "hypainter-export.png"
private const val CANVAS_MIN_SIZE = 64
private const val CANVAS_MAX_SIZE = 8192
