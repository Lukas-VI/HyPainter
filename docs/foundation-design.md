# 现代安卓板绘应用奠基设计方案

## 业务定位

本项目目标不是做一个“能画线的画板”，而是构建一款面向安卓平板和压感笔用户的现代板绘应用。产品体验对标 Procreate 这类闭源成熟软件，技术底座借鉴 Krita、MyPaint、Blender Grease Pencil 等开源软件中已经被验证过的稳定思路，最终形成“低延迟、稳定笔刷、高质量图层、适合触控笔创作”的移动端创作工具。

首版必须优先证明三件事：

1. 笔迹真实可控：压感、倾斜、速度、笔刷纹理、混色和稳定器不能只是参数存在，而要能支撑真实绘画。
2. 画布性能可靠：大画布、多图层、连续绘制、撤销重做不能明显卡顿或丢笔。
3. 架构可长期演进：UI、输入、渲染、笔刷、文档格式和导出能力需要分层清楚，避免后期被移动端限制锁死。

## 产品原则

### 以创作流为中心

主界面应默认进入可绘制画布，而不是文件管理或功能介绍（类sketchbook）。用户打开应用后的第一感受应是“已经可以开始画”，高级功能以工具、面板和手势逐步展开。

### 安卓平板优先

目标设备是支持手写笔的安卓平板。设计应优先考虑横屏、双手操作、笔尖绘制、非惯用手触控手势、悬停预览、掌托误触过滤和高刷新率屏幕。手机适配可以保留空间，但优先级可以放的很低。

### 闭源体验，开源工程

可以学习闭源 SOTA 的交互成熟度，例如 Procreate、sketchbook 的手势、快速菜单、画笔库、图层操作和导出流程；但核心工程实现应尽量选择可解释、可测试、可替换的方案，避免依赖黑盒逻辑。

## 首版范围

### MVP 必须包含

- 画布：新建画布、缩放、旋转、旋转锁定、平移、适配屏幕。
- 笔输入：压感、倾斜、速度、时间戳、橡皮擦、掌托过滤。
- 笔刷：铅笔、墨线、软笔、喷枪、橡皮擦五类基础笔刷。
- 图层：新增、删除、隐藏、透明度、普通混合模式、图层重排。
- 撤销重做：笔画级撤销，图层操作撤销。
- 颜色：色轮、HSV/RGB、最近颜色、吸管。
- 文件：文件管理页面，项目保存、缩略图、PNG 导出、基础崩溃恢复。
- 菜单：基础的设置

### 展望

- 动画时间轴。
- 复杂透视辅助。
- 文本排版。
- 3D 材质绘制（甚至与Blender联动）。
- 支持自托管的WebADV云同步与多人协作。
- 插件系统。

这些能力不是不重要，而是会显著扩大架构面。首版应先打穿“笔输入 -> 笔刷引擎 -> tile 画布 -> 图层合成 -> 保存导出”的主链路。

## 总体架构

推荐采用 Kotlin/Jetpack Compose 负责 UI 与 Android 平台能力，Rust 负责画布文档、笔刷计算、tile 管理、图层合成、撤销栈和文件编解码。渲染层根据阶段选择两步走：早期用 Android Canvas/Bitmap 快速验证交互，稳定后切入 GPU 渲染。

```text
Android App
├─ Compose UI
│  ├─ 工具栏 / 图层面板 / 画笔面板 / 颜色面板
│  ├─ 手势状态与 UI 状态
│  └─ Android 输入事件接入
├─ Kotlin Platform Layer
│  ├─ MotionEvent / stylus 采样
│  ├─ 文件选择、权限、分享、系统导出
│  ├─ JNI / UniFFI / JNA 边界
│  └─ 生命周期、崩溃恢复、后台保存
└─ Rust Core
   ├─ Document Model
   ├─ Brush Engine
   ├─ Tile Store
   ├─ Layer Compositor
   ├─ Undo/Redo Command Log
   ├─ File Format
   └─ Export Pipeline
```

## 模块设计

### 1. Compose UI 层

UI 层只持有交互状态，不直接维护真实画布数据。Compose 的职责是展示工具状态、响应面板操作、接收输入事件、调度 Rust Core 命令。

建议主界面分为：

- `CanvasScreen`：画布和所有浮动工具的宿主。
- `CanvasViewport`：显示当前视口，处理缩放、旋转、平移。
- `ToolRail`：画笔、橡皮、涂抹、吸管、选择等主工具。
- `BrushPanel`：笔刷列表、大小、不透明度、稳定器、纹理参数。
- `LayerPanel`：图层列表、可见性、透明度、混合模式。
- `ColorPanel`：色轮、数值输入、调色板和历史颜色。
- `QuickMenu`：长按、双指或笔按钮触发的快速动作。

Compose 不应在每次笔迹采样时触发复杂重组。绘制中的实时轨迹应通过专用渲染视图或低频状态快照驱动，避免把高频输入直接变成 Compose state。

### 2. 输入系统

安卓笔输入来自 `MotionEvent`。输入层需要保留原始信息，然后转换成统一的 `StylusSample`。

核心字段：

```text
StylusSample
├─ x, y: 画布坐标
├─ pressure: 压感
├─ tiltX, tiltY 或 tiltAngle/azimuth: 倾斜与方向
├─ velocity: 速度
├─ timestamp: 采样时间
├─ toolType: pen / eraser / finger
├─ buttons: 笔身按键
└─ pointerId: 多指区分
```

输入策略：

- 笔尖绘制优先级最高。
- 笔绘制期间忽略非笔触摸，降低掌托误触。
- 双指缩放、旋转和平移只在没有活动笔画时生效。
- 支持 Android hover 事件时，用悬停位置显示画笔轮廓。
- 支持笔输入方向与角度检测
- 所有采样先进入短队列，按时间戳排序后交给笔刷引擎，减少事件乱序。

### 3. Rust Core

Rust Core 是长期资产，应尽量保持平台无关。Android 只是第一个壳，未来桌面端或其他平台不应重写核心绘画逻辑。

推荐 crate 拆分：

```text
core/
├─ HyP_core         文档模型、命令、公共类型
├─ HyP_brush        笔刷引擎、采样、曲线、纹理
├─ HyP_tiles        tile 存储、脏区追踪、缓存
├─ HyP_compositor   图层合成、混合模式
├─ HyP_format       项目文件保存与加载
├─ HyP_export       PNG/WebP/PSD 方向的导出管线
└─ HyP_ffi          面向 Kotlin 的稳定边界
```

Rust 侧对 Kotlin 暴露的是粗粒度命令，而不是细碎内部结构。比如：

- `create_document(width, height, dpi, color_space)`
- `begin_stroke(tool_id, brush_id, layer_id)`
- `append_stylus_samples(stroke_id, samples)`
- `end_stroke(stroke_id)`
- `set_layer_visible(layer_id, visible)`
- `render_viewport(viewport, scale, rotation)`
- `save_document(path)`
- `export_png(path, options)`

### 4. 笔刷引擎

笔刷引擎是产品生命线。采用“可解释的参数化笔刷 + 可替换的渲染后端”。初期，在此基础上应该尽可能使用已有的成熟方案

基础模型：

- Stroke Stabilizer：对用户输入做稳定与预测。
- Dab Generator：把连续轨迹转换成笔刷 dab。
- Brush Shape：圆形、纹理形状、方向性笔尖。
- Dynamics：压感、速度、倾斜映射到大小、不透明度、流量、散布、旋转。
- Paint Model：普通覆盖、累积流量、湿混、涂抹。
- Rasterizer：把 dab 写入 tile。

优先实现的笔刷能力：

- Size by pressure。
- Opacity/flow by pressure。
- Spacing。
- Stabilization。
- Tilt direction。
- Texture mask。
- Smudge sampling。

后期应该可以兼容Karita等常见笔刷格式导入导出

### 5. Tile 画布

移动端大画布不能用单张巨型 bitmap 长期承载。推荐 tile-based raster document。

默认 tile 大小可从 `256x256` 开始评估。每个图层由 tile 网格组成，只加载和合成视口附近 tile。

关键机制：

- Dirty Rect：每次笔画只标记受影响 tile。
- Tile Cache：热区 tile 常驻内存，远区 tile 可压缩或延迟加载。
- Mipmap/Preview：缩放时使用预览层，停止手势后补高质量渲染。
- Stroke Buffer：绘制中的当前笔画可先落在临时缓冲，结束后提交到图层 tile。
- Background Save：保存时写入命令日志或脏 tile，不阻塞绘制。

### 6. 图层与合成

首版图层模型保持克制：

```text
Document
├─ canvas_size
├─ color_profile
├─ layers[]
└─ metadata

Layer
├─ id
├─ name
├─ visible
├─ opacity
├─ blend_mode
├─ lock_alpha
└─ tile_grid
```

首版混合模式：

- Normal。
- Multiply。
- Screen。
- Overlay。
- Erase/Alpha mask 内部模式。

图层合成应支持局部重算。图层修改后只重算受影响区域，不全画布刷新。

### 7. 撤销重做

撤销栈建议使用命令日志 + tile delta。

命令类型：

- StrokeCommand。
- AddLayerCommand。
- DeleteLayerCommand。
- MoveLayerCommand。
- SetLayerPropertyCommand。
- FillCommand。
- TransformCommand。

笔画撤销不要只保存完整图层快照。更合理的是保存笔画影响 tile 的前后差异，或者保存命令与必要输入，并在局部重放时恢复。MVP 可先使用 tile before/after delta，后续再优化为混合策略。

### 8. 文件格式

项目文件建议使用自定义容器格式，后缀暂定 `.pdraw` 或 `.pdoc`。

后期应该可以兼容Karita、psd等常见格式导入导出

内部结构：

```text
project.pdraw
├─ manifest.json
├─ preview.png
├─ document.bin
├─ layers/
│  ├─ layer-{id}/tiles/{x}_{y}.bin
│  └─ layer-{id}/metadata.json
└─ history/
   └─ command-log.bin
```

实现方式可优先使用 zip 容器，manifest 用 JSON，tile 数据用压缩二进制。这样方便调试、恢复和后续兼容。

必须从第一版就考虑：

- 文件版本号。
- 最低兼容版本。
- 崩溃恢复草稿。
- 缩略图。
- 未识别字段保留或忽略策略。

### 9. 渲染路线

建议两阶段：

第一阶段：CPU tile raster + Android Bitmap 显示。验证后立刻舍弃

第二阶段：GPU 合成与显示。

- 渲染：Vulkan、多线程支持。
- 目标测试平台：骁龙860-6GB-120hz-小米平板5.
- Rust 侧继续负责笔刷与 tile 数据，GPU 侧负责合成、缩放、旋转和预览。

不要在项目最早期同时攻克复杂 GPU 渲染和复杂笔刷。先让笔迹质量和数据模型成立，再替换显示后端。

## Android 工程建议

推荐初始工程结构：

```text
android/
├─ app/                  Compose Android 应用
├─ core-ffi/             Kotlin FFI 封装
└─ design-system/        图标、主题、通用控件

rust/
├─ crates/
│  ├─ HyP_core/
│  ├─ HyP_brush/
│  ├─ HyP_tiles/
│  ├─ HyP_compositor/
│  ├─ HyP_format/
│  └─ HyP_ffi/
└─ Cargo.toml

docs/
├─ foundation-design.md
├─ architecture.md
├─ brush-engine.md
├─ file-format.md
└─ android-input.md
```

FFI 可选方案：

- UniFFI：适合稳定 API 和跨语言绑定，开发体验较好。

建议先用 UniFFI 或手写 JNI 的薄封装证明链路。关键原则是减少跨边界调用频率，高频采样应批量传递。

## 性能目标

首版建议建立明确指标：

- 笔输入到屏幕反馈：目标低于 30ms，理想接近 16ms。
- 绘制采样：支持 120Hz 输入采样。
- 画布：至少支持 4096x4096、8 层、常见笔刷连续绘制。
- 撤销：最近 50 次笔画可稳定撤销。
- 保存：中等项目后台保存不阻塞绘制。
- 崩溃恢复：异常退出后能恢复到最近自动保存点。

性能验证不能只靠主观手感。需要在 debug 面板显示帧率、输入延迟、tile 缓存、脏区数量、内存占用和 FFI 调用耗时。

## 交互设计重点

### 画布手势

- 单笔：绘制。
- 双指：缩放、旋转、平移。
- 双指点按：撤销。
- 三指点按：重做。
- 长按：吸管或快速菜单。
- 笔悬停：画笔轮廓预览。
- 笔身按键：临时切换橡皮或吸管。

### 工具组织

主操作应靠近屏幕边缘，减少遮挡画布。面板可以浮动，但不能频繁覆盖绘制区域。图层和画笔是最高频面板，应支持快速打开、快速调整、快速收起。

### 专业感来源

专业感不是按钮多，而是反馈稳定：

- 笔刷大小调整时画布上立即显示轮廓。
- 图层可见性变化即时反映。
- 缩放旋转时画面不闪烁。
- 大笔刷绘制时优先显示近似预览，再补最终质量。
- 撤销重做必须可预期，不能跳步。

## 风险与边界

### 最大技术风险

1. 笔刷手感不足：参数存在但笔迹不好用，会直接影响产品成立。
2. 大画布性能不足：如果 tile、缓存和合成设计太晚介入，后期重构成本高。
3. FFI 边界过细：高频事件跨语言调用会带来延迟和维护问题。
4. 文件格式缺版本：早期项目文件无法兼容会影响测试和用户信任。
5. Compose 承载高频绘制：如果把绘制采样直接绑定 UI 重组，会导致不可控卡顿。

### 应避免的路线

- 直接把所有画布数据放在 Compose state。
- 首版就做完整 Photoshop/Krita 级别参数面板。
- 没有 tile 架构就支持超大画布。
- 没有命令日志就开始复杂图层编辑。
- 先做云同步、社区、素材市场，再做核心绘画链路。

## 里程碑

### M0：技术验证

目标：证明 Android 笔输入可以稳定进入 Rust，并返回可显示的绘制结果。

交付：

- Compose 画布页面。
- MotionEvent 到 StylusSample 的转换。
- Rust 创建文档和单图层。
- 基础圆形笔刷绘制。
- PNG 导出。

验收：

- 真机压感可用。
- 连续绘制不卡死。
- 导出图片与画布一致。

### M1：真实绘画闭环

目标：形成可用于草图和线稿的最小产品。

交付：

- 5 类基础笔刷。
- 撤销重做。
- 图层新增、隐藏、透明度。
- 色轮与吸管。
- 项目保存和加载。

验收：

- 用户可以完成一张简单插画草稿。
- 退出应用后可恢复项目。
- 4096x4096 画布 4 层可连续绘制。

### M2：专业化基础

目标：提升手感、性能和项目可靠性。

交付：

- tile 缓存优化。
- 图层局部合成。
- 笔刷纹理和稳定器。
- 自动保存与崩溃恢复。
- debug 性能面板。

验收：

- 8 层中等画布可稳定使用。
- 撤销 64 次以内稳定。但希望可以到512次
- 异常退出后可恢复最近草稿。

### M3：可展示 Alpha

目标：形成可公开演示的安卓平板绘画应用。

交付：

- 完整画笔库雏形。
- 图层重排、复制、合并。
- 更多混合模式。
- 快速菜单。
- 导出 PNG/WebP，预研 PSD。
- 首轮 UX 打磨。

验收：

- 可录屏展示完整绘画流程。
- 非开发者用户能独立完成新建、绘制、保存、导出。

## 决策建议

当前最小可执行动作是先建立 M0 技术验证工程，而不是继续扩大产品功能清单。具体顺序建议：

1. 建 Android Compose 空壳，只做一个全屏画布。
2. 建 Rust workspace，只实现文档、单图层、圆形笔刷和 PNG 导出。
3. 打通 Kotlin 到 Rust 的批量采样调用。
4. 用真机压感笔验证输入延迟和笔迹质量。
5. 验证通过后，再扩展 tile、图层、撤销和文件格式。

这条路线能最快暴露项目最关键的不确定性：安卓笔输入、Rust FFI、笔刷手感和画布性能。只要这条主链路成立，后续功能扩展才有可靠地基。
