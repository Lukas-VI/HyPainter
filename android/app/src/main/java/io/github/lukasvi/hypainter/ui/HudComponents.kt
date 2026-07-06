package io.github.lukasvi.hypainter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import io.github.lukasvi.hypainter.engine.EngineBrush
import io.github.lukasvi.hypainter.engine.EngineSnapshot
import io.github.lukasvi.hypainter.engine.PaintingEngine
import io.github.lukasvi.hypainter.render.BitmapSampling
import io.github.lukasvi.hypainter.render.CanvasRenderOptions

/**
 * HUD 组件：悬浮工具栏、面板和状态显示。
 *
 * 该文件包含 HyPainter 的多个 Compose 组件，用于显示画笔、图层、
 * 颜色选择以及菜单和状态信息的悬浮 HUD。注释以中文说明每个组件的用途。
 */

/** 悬浮检查面板的类型枚举，用于在菜单、画笔、图层和颜色面板间切换。 */
internal enum class FloatingPanel {
    Menu,
    Brush,
    Layers,
    Color,
}

/**
 * HUD 菜单动作集合：封装菜单中需要的状态与回调。
 *
 * - `renderOptions`: 当前画布渲染选项（例如采样方式）
 * - `debugEnabled`/`debugOverlayVisible`: 调试用开关和覆盖层可见性
 * - `wallpaperColorsAvailable`/`useWallpaperColors`: Android 壁纸色彩相关选项
 * - 其余为用户在菜单中触发的动作回调
 */
internal data class HudMenuActions(
    val renderOptions: CanvasRenderOptions,
    val debugEnabled: Boolean,
    val debugOverlayVisible: Boolean,
    val wallpaperColorsAvailable: Boolean,
    val useWallpaperColors: Boolean,
    val onNewCanvas: () -> Unit,
    val onSaveDraft: () -> Unit,
    val onLoadDraft: () -> Unit,
    val onExportPng: () -> Unit,
    val onSharePng: () -> Unit,
    val onCanvasSettings: () -> Unit,
    val onResetView: () -> Unit,
    val onRenderOptionsChanged: (CanvasRenderOptions) -> Unit,
    val onDebugOverlayChanged: (Boolean) -> Unit,
    val onUseWallpaperColorsChanged: (Boolean) -> Unit,
)

@Composable
internal fun FloatingLeftToolHud(
    modifier: Modifier,
    snapshot: EngineSnapshot,
    toolbarBusy: Boolean,
    brushOpacity: Float,
    onBrushLibrary: () -> Unit,
    onOpacityChanged: (Float) -> Unit,
    onBrushChanged: (EngineBrush) -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HudIconButton(
                icon = LucideIcons.BrushLibrary,
                contentDescription = "Brush library",
                enabled = !toolbarBusy,
                onClick = onBrushLibrary,
            )
            QuickBrushGroup(
                enabled = !toolbarBusy,
                snapshot = snapshot,
                onBrushChanged = onBrushChanged,
            )
            VerticalHudSlider(
                icon = LucideIcons.Droplet,
                value = brushOpacity,
                valueRange = 0.05f..1f,
                enabled = !toolbarBusy,
                // EngineBrush does not expose opacity yet; keep this UI state local until the engine protocol grows.
                onValueChange = onOpacityChanged,
            )
            VerticalHudSlider(
                icon = LucideIcons.Size,
                value = snapshot.brush.radiusPx,
                valueRange = 2f..96f,
                enabled = !toolbarBusy,
                onValueChange = { onBrushChanged(snapshot.brush.copy(radiusPx = it)) },
            )
        }
    }
}

/**
 * 左侧悬浮工具 HUD：包含画笔库按钮、快速画笔组以及垂直滑块（例如不透明度、大小）。
 * 该组件会使用一个半透明的 Surface 以保持与画布的视觉分离。
 */

@Composable
internal fun FloatingToolBar(
    modifier: Modifier,
    activePanel: io.github.lukasvi.hypainter.ui.FloatingPanel?,
    onMenuPanel: () -> Unit,
    onSelectionTool: () -> Unit,
    onTransformTool: () -> Unit,
    onToolPanel: () -> Unit,
    onColorPanel: () -> Unit,
    onLayersPanel: () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(34.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        tonalElevation = 8.dp,
        shadowElevation = 10.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ToolbarIcon(LucideIcons.Menu, "Menus", active = activePanel == FloatingPanel.Menu, onClick = onMenuPanel)
            ToolbarIcon(LucideIcons.Selection, "Selection placeholder", active = false, onClick = onSelectionTool)
            ToolbarIcon(LucideIcons.Transform, "Transform placeholder", active = false, onClick = onTransformTool)
            ToolbarIcon(LucideIcons.Tool, "Tool placeholder", active = activePanel == FloatingPanel.Brush, onClick = onToolPanel)
            ToolbarIcon(LucideIcons.Palette, "Color", active = activePanel == FloatingPanel.Color, onClick = onColorPanel)
            ToolbarIcon(LucideIcons.Layers, "Layers", active = activePanel == FloatingPanel.Layers, onClick = onLayersPanel)
        }
    }
}

/**
 * 上下居中位于右侧的浮动工具栏：提供常用工具图标（菜单、选择、变换、调色板、图层）。
 */

@Composable
internal fun FloatingInspectorPanel(
    modifier: Modifier,
    panel: FloatingPanel,
    engine: PaintingEngine,
    snapshot: EngineSnapshot,
    toolbarBusy: Boolean,
    menuActions: HudMenuActions,
    onClose: () -> Unit,
    onModelChanged: () -> Unit,
) {
    Surface(
        modifier = modifier.width(336.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = when (panel) {
                        FloatingPanel.Menu -> "Menus"
                        FloatingPanel.Brush -> "Brush"
                        FloatingPanel.Layers -> "Layers"
                        FloatingPanel.Color -> "Color"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
            }
            when (panel) {
                FloatingPanel.Menu -> MenuFloatingPanel(menuActions)
                FloatingPanel.Brush -> BrushFloatingPanel(
                    snapshot = snapshot,
                    toolbarBusy = toolbarBusy,
                    onBrushChanged = {
                        engine.setBrush(it)
                        onModelChanged()
                    },
                )
                FloatingPanel.Layers -> LayersFloatingPanel(
                    engine = engine,
                    snapshot = snapshot,
                    toolbarBusy = toolbarBusy,
                    onModelChanged = onModelChanged,
                )
                FloatingPanel.Color -> ColorFloatingPanel(
                    snapshot = snapshot,
                    toolbarBusy = toolbarBusy,
                    onBrushChanged = {
                        engine.setBrush(it)
                        onModelChanged()
                    },
                )
            }
        }
    }
}

/**
 * 检查面板（Inspector）：根据 `panel` 显示不同内容（菜单 / 画笔 / 图层 / 颜色）。
 * 通过 `engine` 与 `snapshot` 操作数据，并在需要时调用 `onModelChanged` 更新模型。
 */

@Composable
internal fun CanvasHudStatus(
    modifier: Modifier,
    documentTitle: String,
    snapshot: EngineSnapshot,
    latestPressure: Float,
    exportStatus: String?,
    projectStatus: String?,
    nativeBacked: Boolean,
    renderOptions: CanvasRenderOptions,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
        tonalElevation = 4.dp,
    ) {
        Text(
            text = listOfNotNull(
                "$documentTitle ${snapshot.canvasWidth}x${snapshot.canvasHeight}",
                if (nativeBacked) "Native" else "Kotlin",
                renderOptions.bitmapSampling.hudLabel(),
                "Pressure ${"%.2f".format(latestPressure)}",
                projectStatus,
                exportStatus,
            ).joinToString(" / "),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

/**
 * 画布状态栏：在一行内显示文档标题、分辨率、渲染后端、采样方式、压力值等信息。
 */

@Composable
private fun QuickBrushGroup(
    enabled: Boolean,
    snapshot: EngineSnapshot,
    onBrushChanged: (EngineBrush) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
    ) {
        Column(modifier = Modifier.padding(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            QuickBrushButton(
                icon = LucideIcons.Brush,
                color = Color(0xFF111318),
                enabled = enabled,
                onClick = { onBrushChanged(snapshot.brush.copy(colorArgb = 0xff111318.toInt())) },
            )
            QuickBrushButton(
                icon = LucideIcons.Pen,
                color = Color(0xFFD72638),
                enabled = enabled,
                onClick = { onBrushChanged(snapshot.brush.copy(colorArgb = 0xffd72638.toInt())) },
            )
            QuickBrushButton(
                icon = LucideIcons.Brush,
                color = Color(0xFF2563EB),
                enabled = enabled,
                onClick = { onBrushChanged(snapshot.brush.copy(colorArgb = 0xff2563eb.toInt())) },
            )
        }
    }
}

/** 快速画笔组：展示几个预设色与画笔，便于快速切换。 */

@Composable
private fun VerticalHudSlider(
    icon: ImageVector,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Box(modifier = Modifier.size(width = 48.dp, height = 142.dp), contentAlignment = Alignment.Center) {
            Slider(
                modifier = Modifier
                    .width(132.dp)
                    .height(36.dp)
                    .graphicsLayer(rotationZ = -90f),
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                valueRange = valueRange,
            )
        }
    }
}

/**
 * 垂直 HUD 滑块：通过旋转水平 Slider 实现竖直显示，带有图标和滑轨。
 * 常用于展示画笔大小或不透明度控制。
 */

@Composable
private fun HudIconButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(52.dp),
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp),
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(24.dp))
    }
}

/** 圆形图标按钮，固定大小并用于主工具入口（如画笔库）。 */

@Composable
private fun QuickBrushButton(
    icon: ImageVector,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(44.dp)) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(color.copy(alpha = 0.18f), CircleShape)
                .border(1.dp, color.copy(alpha = 0.72f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        }
    }
}

/**
 * 快速画笔按钮：在圆形背景上展示颜色与图标，用于设置画笔颜色或快速样式切换。
 */

@Composable
private fun ToolbarIcon(
    icon: ImageVector,
    contentDescription: String,
    active: Boolean = false,
    onClick: () -> Unit,
) {
    val backgroundColor = if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val iconTint = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

    IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(backgroundColor, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = contentDescription, tint = iconTint, modifier = Modifier.size(24.dp))
        }
    }
}

/**
 * 工具栏图标（标准大小），用于放在主浮动工具栏中。支持 `active` 状态以改变背景与图标颜色。
 */

@Composable
private fun MenuFloatingPanel(actions: HudMenuActions) {
    // 限制面板最大高度并改为可滚动，避免内容过多时超出屏幕
    val scrollState = rememberScrollState()
    Box(modifier = Modifier.heightIn(max = 360.dp)) {
        Column(
            modifier = Modifier.verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("File", style = MaterialTheme.typography.labelLarge)
            MenuAction("New Canvas", onClick = actions.onNewCanvas)
            MenuAction("Save Draft", onClick = actions.onSaveDraft)
            MenuAction("Load Draft", onClick = actions.onLoadDraft)
            MenuAction("Export PNG", onClick = actions.onExportPng)
            MenuAction("Share PNG", onClick = actions.onSharePng)
            Text("Canvas", style = MaterialTheme.typography.labelLarge)
            MenuAction("Canvas Settings", onClick = actions.onCanvasSettings)
            MenuAction("Reset View", onClick = actions.onResetView)
            Text("View", style = MaterialTheme.typography.labelLarge)
            BitmapSampling.entries.forEach { sampling ->
                MenuAction(sampling.hudLabel()) {
                    actions.onRenderOptionsChanged(actions.renderOptions.copy(bitmapSampling = sampling))
                }
            }
            MenuAction(
                label = when {
                    !actions.wallpaperColorsAvailable -> "Wallpaper Colors (Android 12+)"
                    actions.useWallpaperColors -> "Disable Wallpaper Colors"
                    else -> "Use Wallpaper Colors"
                },
                enabled = actions.wallpaperColorsAvailable,
            ) {
                actions.onUseWallpaperColorsChanged(!actions.useWallpaperColors)
            }
            if (actions.debugEnabled) {
                MenuAction(if (actions.debugOverlayVisible) "Hide Debug Overlay" else "Show Debug Overlay") {
                    actions.onDebugOverlayChanged(!actions.debugOverlayVisible)
                }
            }
        }
    }
}

/**
 * 菜单面板：展示与文件/画布/视图相关的一系列操作项。
 * 使用 `HudMenuActions` 传入回调以完成具体行为。
 */

@Composable
private fun MenuAction(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    TextButton(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f))
    }
}

/** 简单的菜单动作行：使用 `TextButton` 显示并响应点击。 */

@Composable
private fun BrushFloatingPanel(
    snapshot: EngineSnapshot,
    toolbarBusy: Boolean,
    onBrushChanged: (EngineBrush) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Current size ${snapshot.brush.radiusPx.toInt()} px")
        Slider(
            value = snapshot.brush.radiusPx,
            onValueChange = { onBrushChanged(snapshot.brush.copy(radiusPx = it)) },
            enabled = !toolbarBusy,
            valueRange = 2f..96f,
        )
        Text("Brush library and stabilizer controls are placeholders for the next UI pass.")
    }
}

/**
 * 画笔面板：显示当前画笔大小控制与占位符说明（画笔库 / 稳定器为后续 UI）。
 */

@Composable
private fun LayersFloatingPanel(
    engine: PaintingEngine,
    snapshot: EngineSnapshot,
    toolbarBusy: Boolean,
    onModelChanged: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            enabled = !toolbarBusy,
            onClick = {
                engine.addLayer()
                onModelChanged()
            },
        ) {
            Text("Add Layer")
        }
        snapshot.layers.forEach { layer ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (layer.id == snapshot.activeLayerId) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = layer.name,
                        modifier = Modifier.weight(1f),
                        color = if (layer.id == snapshot.activeLayerId) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                    TextButton(
                        enabled = !toolbarBusy,
                        onClick = {
                            engine.selectLayer(layer.id)
                            onModelChanged()
                        },
                    ) {
                        Text("Select")
                    }
                    TextButton(
                        enabled = !toolbarBusy,
                        onClick = {
                            engine.toggleLayerVisibility(layer.id)
                            onModelChanged()
                        },
                    ) {
                        Text(if (layer.visible) "Hide" else "Show")
                    }
                }
            }
        }
    }
}

/**
 * 图层面板：列出当前快照中的所有图层，允许添加、选择和切换可见性。
 */

@Composable
private fun ColorFloatingPanel(
    snapshot: EngineSnapshot,
    toolbarBusy: Boolean,
    onBrushChanged: (EngineBrush) -> Unit,
) {
    val swatches = listOf(
        0xff111318.toInt(),
        0xffffffff.toInt(),
        0xffd72638.toInt(),
        0xff2563eb.toInt(),
        0xff35c2a1.toInt(),
        0xfff2b84b.toInt(),
        0xff8a5cf6.toInt(),
        0xffff7a59.toInt(),
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(
                modifier = Modifier
                    .size(58.dp)
                    .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape),
                shape = CircleShape,
                color = Color(snapshot.brush.colorArgb),
            ) {}
            Text("Current color")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            swatches.take(4).forEach { color ->
                ColorSwatch(color, enabled = !toolbarBusy) {
                    onBrushChanged(snapshot.brush.copy(colorArgb = color))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            swatches.drop(4).forEach { color ->
                ColorSwatch(color, enabled = !toolbarBusy) {
                    onBrushChanged(snapshot.brush.copy(colorArgb = color))
                }
            }
        }
        Text("A custom color wheel should replace this placeholder.")
    }
}

/**
 * 颜色面板：展示当前颜色、若干色块样式的色板占位符。
 * 实际产品中可替换为自定义颜色轮。
 */

@Composable
private fun ColorSwatch(colorArgb: Int, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(42.dp),
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp),
    ) {
        Surface(modifier = Modifier.size(28.dp), shape = CircleShape, color = Color(colorArgb)) {}
    }
}

/** 颜色色板按钮：以圆形显示某个颜色并响应选择。 */

private fun BitmapSampling.hudLabel(): String {
    return when (this) {
        BitmapSampling.PixelPerfect -> "Pixel Perfect"
        BitmapSampling.Nearest -> "Nearest"
        BitmapSampling.Linear -> "Linear"
        BitmapSampling.Bilinear -> "Bilinear"
        BitmapSampling.Bicubic -> "Bicubic"
    }
}

/**
 * 将 `BitmapSampling` 转换为用于 HUD 显示的文本标签。
 */

// preview
@Preview(showBackground = true, widthDp = 360)
@Composable
private fun MenuFloatingPanelPreview() {
    HyPainterTheme(useWallpaperColors = false) {
        MenuFloatingPanel(
            actions = HudMenuActions(
                renderOptions = CanvasRenderOptions(),
                debugEnabled = true,
                debugOverlayVisible = false,
                wallpaperColorsAvailable = true,
                useWallpaperColors = false,
                onNewCanvas = {},
                onSaveDraft = {},
                onLoadDraft = {},
                onExportPng = {},
                onSharePng = {},
                onCanvasSettings = {},
                onResetView = {},
                onRenderOptionsChanged = {},
                onDebugOverlayChanged = {},
                onUseWallpaperColorsChanged = {},
            )
        )
    }
}