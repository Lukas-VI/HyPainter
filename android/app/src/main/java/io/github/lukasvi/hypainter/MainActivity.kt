package io.github.lukasvi.hypainter

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.MotionEvent
import android.os.Build
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.FileProvider
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntSize
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
import io.github.lukasvi.hypainter.ui.FloatingInspectorPanel
import io.github.lukasvi.hypainter.ui.FloatingLeftToolHud
import io.github.lukasvi.hypainter.ui.FloatingPanel
import io.github.lukasvi.hypainter.ui.FloatingToolBar
import io.github.lukasvi.hypainter.ui.HudMenuActions
import io.github.lukasvi.hypainter.ui.HyPainterTheme
import io.github.lukasvi.hypainter.ui.supportsWallpaperColors
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 应用入口：封装主题并显示主应用界面 `HyPainterApp`。
 */

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 移除默认背景
        window.setBackgroundDrawable(null)
        
        setContent {
            HyPainterApp()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideNavigationBar()
        }
    }

    private fun hideNavigationBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.navigationBars())
            window.insetsController?.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        }
    }
}



/**
 * 应用根 Composable：负责主题开关（是否使用壁纸色彩）和承载 `CanvasScreen`。
 */

@Composable
private fun HyPainterApp() {
    val wallpaperColorsAvailable = supportsWallpaperColors()
    var useWallpaperColors by rememberSaveable { mutableStateOf(wallpaperColorsAvailable) }

    HyPainterTheme(useWallpaperColors = useWallpaperColors) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            CanvasScreen(
                wallpaperColorsAvailable = wallpaperColorsAvailable,
                useWallpaperColors = useWallpaperColors,
                onUseWallpaperColorsChanged = { useWallpaperColors = it },
            )
        }
    }
}

/**
 * 画布屏幕：包含画布渲染、输入路由、工具栏、面板、导出/保存等应用级状态。
 *
 * 该函数集中管理：
 * - 引擎实例与快照 (`engine`, `snapshot`, `toolbarSnapshot`)；
 * - 视口与输入路由 (`viewport`, `inputRouter`)；
 * - UI 面板状态和工具栏忙碌状态 (`activePanel`, `toolbarBusy`)；
 * - 导出/保存/加载等异步操作（使用 `runToolbarIo` 封装）。
 */

@Composable
private fun CanvasScreen(
    wallpaperColorsAvailable: Boolean,
    useWallpaperColors: Boolean,
    onUseWallpaperColorsChanged: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    var engine by remember { mutableStateOf<PaintingEngine>(createPaintingEngine()) }
    var documentTitle by remember { mutableStateOf("Untitled") }
    val canvasVersion = remember { mutableStateOf(0) }
    val modelVersion = remember { mutableStateOf(0) }
    val viewport = remember { mutableStateOf(ViewportState()) }
    val containerSize = remember { mutableStateOf(IntSize(0, 0)) }
    val hasCentered = remember { mutableStateOf(false) }

    val latestPressure = remember { mutableStateOf(0f) }
    val exportStatus = remember { mutableStateOf<String?>(null) }
    val projectStatus = remember { mutableStateOf<String?>(null) }
    val toolbarBusy = remember { mutableStateOf(false) }
    val debugOverlayVisible = remember { mutableStateOf(false) }
    val debugState = remember { mutableStateOf(CanvasDebugState()) }
    var newCanvasDialogVisible by remember { mutableStateOf(false) }
    var canvasSettingsVisible by remember { mutableStateOf(false) }
    var activePanel by remember { mutableStateOf<FloatingPanel?>(null) }
    var brushOpacity by remember { mutableStateOf(1f) }
    val coroutineScope = rememberCoroutineScope()
    var inputRouter by remember { mutableStateOf(CanvasInputRouter()) }
    var renderOptions by remember { mutableStateOf(CanvasRenderOptions()) }
    val frameInvalidator = remember(view) {
        FrameInvalidator(view) {
            canvasVersion.value++
        }
    }
    val gestureToastVisible = remember { mutableStateOf(false) }
    val gestureToastScale = remember { mutableStateOf("100%") }
    val gestureToastRotation = remember { mutableStateOf("0°") }
    val gestureHideJob = remember { mutableStateOf<Job?>(null) }
    val menuToastVisible = remember { mutableStateOf(false) }
    val menuToastMessage = remember { mutableStateOf("") }
    val menuToastJob = remember { mutableStateOf<Job?>(null) }
    val showMenuToast: (String) -> Unit = { message ->
        menuToastJob.value?.cancel()
        menuToastMessage.value = message
        menuToastVisible.value = true
        menuToastJob.value = coroutineScope.launch {
            delay(1500)
            menuToastVisible.value = false
        }
    }
    val snapshot = remember(canvasVersion.value, engine) { engine.canvasSnapshot() }
    val toolbarSnapshot = remember(modelVersion.value, engine) { engine.snapshot() }

    val centerCanvas = {
        val size = containerSize.value
        if (size.width > 0 && size.height > 0) {
            val s = engine.canvasSnapshot()
            val scaleX = size.width.toFloat() / s.canvasWidth
            val scaleY = size.height.toFloat() / s.canvasHeight
            val scale = minOf(scaleX, scaleY).coerceIn(0.25f, 8f)
            viewport.value = ViewportState(
                pan = Offset(
                    (size.width - s.canvasWidth * scale) / 2f,
                    (size.height - s.canvasHeight * scale) / 2f
                ),
                scale = scale,
            )
        }
    }
    if (containerSize.value.width > 0 && !hasCentered.value) {
        LaunchedEffect(Unit) {
            centerCanvas()
            hasCentered.value = true
        }
    }
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
        centerCanvas()
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
    val saveDraft = {
        val output = File(context.filesDir, PROJECT_FILE_NAME)
        runToolbarIo(
            busy = toolbarBusy.value,
            setBusy = { toolbarBusy.value = it },
            launchBackground = { block -> coroutineScope.launch { block() } },
            onStart = {
                projectStatus.value = "Saving"
                showMenuToast("Saving")
            },
            block = { engine.saveProject(output.absolutePath) },
            onResult = { ok ->
                val message = if (ok) "Saved" else "Save failed"
                projectStatus.value = message
                showMenuToast(message)
            },
            onError = {
                projectStatus.value = "Save failed"
                showMenuToast("Save failed")
            },
        )
    }
    val loadDraft = {
        val input = File(context.filesDir, PROJECT_FILE_NAME)
        runToolbarIo(
            busy = toolbarBusy.value,
            setBusy = { toolbarBusy.value = it },
            launchBackground = { block -> coroutineScope.launch { block() } },
            onStart = {
                projectStatus.value = "Loading"
                showMenuToast("Loading")
            },
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
                    engine = loadedEngine
                    documentTitle = PROJECT_FILE_NAME
                    latestPressure.value = 0f
                    resetInputAndViewport()
                    refreshModel()
                    projectStatus.value = "Loaded"
                    showMenuToast("Loaded")
                } else {
                    projectStatus.value = "Load failed"
                    showMenuToast("Load failed")
                }
            },
            onError = {
                projectStatus.value = "Load failed"
                showMenuToast("Load failed")
            },
        )
    }
    val exportPng = {
        val output = File(context.filesDir, EXPORT_FILE_NAME)
        runToolbarIo(
            busy = toolbarBusy.value,
            setBusy = { toolbarBusy.value = it },
            launchBackground = { block -> coroutineScope.launch { block() } },
            onStart = {
                exportStatus.value = "Exporting"
                showMenuToast("Exporting")
            },
            block = { engine.exportPng(output.absolutePath) },
            onResult = { ok ->
                val message = if (ok) "Exported" else "Export failed"
                exportStatus.value = message
                showMenuToast(message)
            },
            onError = {
                exportStatus.value = "Export failed"
                showMenuToast("Export failed")
            },
        )
    }
    val menuActions = HudMenuActions(
        renderOptions = renderOptions,
        debugEnabled = BuildConfig.DEBUG,
        debugOverlayVisible = debugOverlayVisible.value,
        wallpaperColorsAvailable = wallpaperColorsAvailable,
        useWallpaperColors = useWallpaperColors,
        onNewCanvas = { newCanvasDialogVisible = true },
        onSaveDraft = saveDraft,
        onLoadDraft = loadDraft,
        onExportPng = exportPng,
        onSharePng = {
            shareExportedPng(
                context = context,
                engine = engine,
                toolbarBusy = toolbarBusy.value,
                launchBackground = { block -> coroutineScope.launch { block() } },
                onToolbarBusyChanged = { toolbarBusy.value = it },
                onExportStatusChanged = {
                    exportStatus.value = it
                    showMenuToast(it)
                },
            )
        },
        onCanvasSettings = { canvasSettingsVisible = true },
        onResetView = { resetInputAndViewport() },
        onRenderOptionsChanged = {
            renderOptions = it
            refreshCanvas()
        },
        onDebugOverlayChanged = { debugOverlayVisible.value = it },
        onUseWallpaperColorsChanged = onUseWallpaperColorsChanged,
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .onSizeChanged { containerSize.value = it },
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInteropFilter { event ->
                    if (toolbarBusy.value) {
                        return@pointerInteropFilter true
                    }
                    // 当存在活动面板时，单击画布空白处关闭面板（简单点击判断）。
                    if (activePanel != null && event.action == MotionEvent.ACTION_UP) {
                        activePanel = null
                        return@pointerInteropFilter true
                    }
                    if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                        gestureHideJob.value = coroutineScope.launch {
                            delay(1500)
                            gestureToastVisible.value = false
                        }
                    }
                    inputRouter.onMotionEvent(
                        event = event,
                        viewport = viewport.value,
                        engine = engine,
                        onViewportChanged = {
                        viewport.value = it
                        gestureHideJob.value?.cancel()
                        gestureToastVisible.value = true
                        val scalePct = (it.scale * 100).roundToInt().coerceIn(25, 800)
                        gestureToastScale.value = "${scalePct}%"
                        gestureToastRotation.value = "${it.rotation.toInt()}°"
                    },
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

        FloatingLeftToolHud(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 18.dp),
            snapshot = toolbarSnapshot,
            toolbarBusy = toolbarBusy.value,
            brushOpacity = brushOpacity,
            onBrushLibrary = { activePanel = FloatingPanel.Brush },
            onOpacityChanged = {
                brushOpacity = it
                projectStatus.value = "Opacity placeholder ${((it * 100).toInt())}%"
            },
            onBrushChanged = {
                engine.setBrush(it)
                refreshModel()
            },
        )

        FloatingToolBar(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 18.dp, end =18.dp),
            activePanel = activePanel,
            onMenuPanel = { activePanel = if (activePanel == FloatingPanel.Menu) null else FloatingPanel.Menu },
            onSelectionTool = { projectStatus.value = "Selection placeholder" },
            onTransformTool = { projectStatus.value = "Transform placeholder" },
            onToolPanel = { activePanel = if (activePanel == FloatingPanel.Brush) null else FloatingPanel.Brush },
            onColorPanel = { activePanel = if (activePanel == FloatingPanel.Color) null else FloatingPanel.Color },
            onLayersPanel = { activePanel = if (activePanel == FloatingPanel.Layers) null else FloatingPanel.Layers },
        )

        // ---- Animation design ----
        // Menu fades in/out from top-right. Brush/Layers/Color fade in/out
        // centered vertically; AnimatedContent handles cross-panel switches.
        // ----
        AnimatedVisibility(
            visible = activePanel == FloatingPanel.Menu,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(Modifier.fillMaxSize()) {
                FloatingInspectorPanel(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 92.dp, end = 18.dp),
                    panel = FloatingPanel.Menu,
                    engine = engine,
                    snapshot = toolbarSnapshot,
                    toolbarBusy = toolbarBusy.value,
                    menuActions = menuActions,
                    onClose = { activePanel = null },
                    onModelChanged = refreshModel,
                )
            }
        }

        AnimatedContent(
            targetState = if (activePanel == null || activePanel == FloatingPanel.Menu) null else activePanel,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "panelSwitch",
            modifier = Modifier.fillMaxSize(),
        ) { panel ->
            Box(Modifier.fillMaxSize()) {
                panel?.let { p ->
                    FloatingInspectorPanel(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 22.dp),
                        panel = panel,
                        engine = engine,
                        snapshot = toolbarSnapshot,
                        toolbarBusy = toolbarBusy.value,
                        menuActions = menuActions,
                        onClose = { activePanel = null },
                        onModelChanged = refreshModel,
                    )
                }
            }
        }



        if (BuildConfig.DEBUG && debugOverlayVisible.value) {
            CanvasDebugOverlay(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 96.dp, bottom = 18.dp),
                state = debugState.value,
                viewport = viewport.value,
                snapshot = snapshot,
            )
        }

        // Gesture toast scale + rotation badge, visible during pinch-zoom/rotate
        StatusToast(
            visible = gestureToastVisible.value,
            message = "${gestureToastScale.value}  ${gestureToastRotation.value}",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 92.dp),
        )

        // Menu action feedback toast, visible when save/load/export/share commands finish.
        StatusToast(
            visible = menuToastVisible.value,
            message = menuToastMessage.value,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 18.dp),
        )

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

/**
 * 通用状态吐司：用于显示手势和菜单操作反馈，保持一致的外观与动画。
 */
@Composable
private fun StatusToast(
    visible: Boolean,
    message: String,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = modifier,
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
        ) {
            Text(
                text = message,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}


/**
 * 新建画布对话框：允许输入名称和像素尺寸，提供若干预设按钮。
 * 验证宽高在允许范围内后回调 `onCreate` 创建新文档。
 */

@Composable
private fun NewCanvasDialog(
    onDismiss: () -> Unit,
    onCreate: (Int, Int, String) -> Unit,
) {
    val screenMetrics = LocalContext.current.resources.displayMetrics
    val screenWidth = screenMetrics.widthPixels
    val screenHeight = screenMetrics.heightPixels
    var title by remember { mutableStateOf("Untitled") }
    var widthText by remember { mutableStateOf(screenWidth.toString()) }
    var heightText by remember { mutableStateOf(screenHeight.toString()) }
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
                    PresetButton("Screen", screenWidth, screenHeight) { presetWidth, presetHeight ->
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

/** 预设尺寸按钮，点击后将宽高通过 `onSelect` 回传。 */

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

/**
 * 画布设置对话框：显示当前画布尺寸并允许选择位图采样（BitmapSampling）。
 */

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

/**
 * 导出并分享 PNG：先通过 `runToolbarIo` 导出文件，成功后使用 `FileProvider` 分享。
 * UI 状态通过 `onToolbarBusyChanged` 与 `onExportStatusChanged` 回传。
 */

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

/** 将 `BitmapSampling` 转换为用于设置对话框的可读标签。 */

private fun BitmapSampling.label(): String {
    return when (this) {
        BitmapSampling.PixelPerfect -> "Pixel Perfect"
        BitmapSampling.Nearest -> "Nearest"
        BitmapSampling.Linear -> "Linear"
        BitmapSampling.Bilinear -> "Bilinear"
        BitmapSampling.Bicubic -> "Bicubic"
    }
}

/**
 * 通用的工具栏异步 IO 封装：
 * - 防止并发执行（`busy` 标志）
 * - 在后台线程运行 `block` 并在完成后在回调中返回结果或错误
 */

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

/**
 * 帧无效器：在需要在下一帧重新绘制时（例如引擎变更）调用 `request()`。
 * 它使用 `View.postOnAnimation` 来安排下一次动画帧回调。
 */

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

/**
 * 兼容性变换包装：根据 `ViewportState` 应用平移、旋转与缩放，
 * 保证在绘制画布元素前正确变换坐标系。
 */

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

/**
 * 绘制画布背景（白色）。用于清空并作为画布底色。
 */

private fun DrawScope.drawCanvasBackground(width: Int, height: Int) {
    drawRect(
        color = Color.White,
        size = androidx.compose.ui.geometry.Size(width.toFloat(), height.toFloat()),
    )
}

/**
 * 绘制单条笔触：遍历点序列并以压力值调节色彩透明度与线宽。
 */

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

/**
 * 检查指定图层 ID 是否在快照中可见。
 */

private fun io.github.lukasvi.hypainter.engine.EngineSnapshot.layerIsVisible(layerId: Long): Boolean {
    for (index in layers.indices) {
        val layer = layers[index]
        if (layer.id == layerId) {
            return layer.visible
        }
    }
    return false
}

/** 常量：项目文件名、导出文件名以及允许的画布尺寸范围。 */

private const val PROJECT_FILE_NAME = "hypainter-project.hyp"
private const val EXPORT_FILE_NAME = "hypainter-export.png"
private const val CANVAS_MIN_SIZE = 8
private const val CANVAS_MAX_SIZE = 8192

