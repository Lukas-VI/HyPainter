## 2026-06-30 - Task: 初始化 HyPainter 项目结构

### What was done
- 拉取 `Lukas-VI/HyPainter` 仓库到本地工作区。
- 将批改后的现代安卓板绘应用奠基设计文档迁入仓库 `docs/`。
- 创建 Android、Rust core、文档目录的占位结构，为 Jetpack Compose UI 与 Rust 绘画引擎后续实现预留落点。
- 扩展 `.gitignore`，覆盖 Android、Rust、FFI、构建产物、本地环境文件和 HyPainter 项目运行文件。

### Testing
- 通过 `git status` 检查新增文件与忽略规则变更。
- 通过文件列表检查确认文档与占位目录已落盘。

### Notes
- `.gitignore`：补充 Kotlin/Java、Rust、FFI、构建产物、本地环境和项目文件忽略规则。
- `README.md`：更新项目定位、目录结构和当前阶段说明。
- `docs/foundation-design.md`：迁入用户批改后的奠基设计方案。
- `docs/README.md`：说明文档目录用途。
- `android/**/README.md`：创建 Android 应用、FFI、设计系统占位说明。
- `rust/**/README.md`：创建 Rust workspace 与各计划 crate 的占位说明。
- `progress.md`：记录本轮初始化交付、验证说明和回滚信息。
- 回滚方式：执行 `git revert <本轮提交哈希>`；若尚未共享提交，也可删除新增目录文件并恢复 `.gitignore` 与 `README.md` 到上一提交。

## 2026-06-30 - Task: 推进 MVP 可运行骨架

### What was done
- 建立可构建的 Android Gradle 工程，配置与本机已验证的 `D:\Java\NzHelper` Gradle 版本体系对齐。
- 实现首个 Compose 全屏画布界面，支持笔/触控事件采样、压感显示、临时笔迹绘制、清空、撤销、缩放、平移和旋转。
- 建立 Rust workspace，并实现最小绘画核心链路：共享类型、tile 像素存储、压感笔刷采样、图层合成、RGBA/PNG 导出。
- 去除 Rust 外部 PNG 依赖，使用项目内最小 PNG 编码器，避免核心测试被网络镜像可用性阻塞。

### Testing
- `cd rust; cargo fmt --all -- --check; cargo test`：通过。
- `.\gradlew.bat :android:app:assembleDebug --stacktrace`：通过，生成 debug APK。

### Notes
- `settings.gradle.kts`、`build.gradle.kts`、`gradle.properties`、`gradle/**`、`gradlew*`：新增 Android Gradle 工程入口和 wrapper。
- `gradle/libs.versions.toml`：新增与本机可用 Android 工程对齐的插件和依赖版本目录。
- `android/app/build.gradle.kts`、`android/app/src/**`：新增 Compose Android 应用骨架和首屏画布。
- `rust/Cargo.toml`、`rust/Cargo.lock`、`rust/crates/**`：新增 Rust workspace 与核心绘画模块实现。
- `README.md`、`android/README.md`、`rust/README.md`：补充当前可构建状态、验证命令和后续 MVP 方向。
- 回滚方式：执行 `git revert <本轮提交哈希>`；如未提交，删除本轮新增的 Gradle、Android 源码和 Rust 源码文件，并还原 README/progress 修改。

## 2026-06-30 - Task: 建立 Android 到 Rust 的引擎边界

### What was done
- 新增 `hyp_ffi` Rust crate，纳入 workspace，提供文档创建、释放、清空、笔画提交、RGBA 渲染和 buffer 释放的 C ABI 起点。
- 为 `hyp_ffi` 补充基础 JNI 符号，使 Android native bridge 与 Rust 文档生命周期、清空入口开始对齐。
- 将 Android 画布状态从 `MainActivity` 拆到 `PaintingEngine` 抽象，新增 Kotlin fallback engine 与 native engine 桥接类。
- 让 Compose 画布通过 engine snapshot 绘制，保留当前可用的压感笔迹、清空、撤销、平移、缩放和旋转体验。

### Testing
- `cd rust; cargo fmt --all -- --check; cargo test`：通过。
- `.\gradlew.bat :android:app:assembleDebug --stacktrace`：通过。

### Notes
- `rust/Cargo.toml`、`rust/crates/HyP_ffi/**`：新增 FFI crate、C ABI、部分 JNI 生命周期入口和测试。
- `android/app/src/main/java/io/github/lukasvi/hypainter/engine/**`：新增绘画引擎抽象、Kotlin fallback 和 native bridge。
- `android/app/src/main/java/io/github/lukasvi/hypainter/MainActivity.kt`：改为通过 `PaintingEngine` 管理笔画状态。
- `README.md`、`android/README.md`、`rust/README.md`：更新当前桥接状态与后续 MVP 方向。
- 当前限制：Rust `.so` 尚未自动构建并打包进 Android APK，native bridge 默认会回落到 Kotlin engine。
- 回滚方式：执行 `git revert <本轮提交哈希>`；如未提交，删除 `HyP_ffi` crate 与 Android `engine/` 包，并还原 `MainActivity.kt` 和 README/progress 修改。

## 2026-06-30 - Task: 打通 Rust native 库打包进 APK

### What was done
- 新增 `scripts/build-rust-android.ps1`，使用 Android NDK clang 将 `hyp_ffi` 构建为 `arm64-v8a` 的 `libhyp_ffi.so`。
- 将 Android Gradle debug 构建接入 Rust native 构建任务，并把生成的 `.so` 作为 generated `jniLibs` 打包进 APK。
- 补全 `NativePaintingEngine` 对应的 Rust JNI 入口，使样本数组提交和 RGBA byte array 返回具备 native 实现。
- 安装并验证 `aarch64-linux-android` Rust target。

### Testing
- `cd rust; cargo fmt --all -- --check; cargo test`：通过。
- `.\gradlew.bat :android:app:assembleDebug --stacktrace`：通过，执行 `:android:app:buildRustArm64Debug` 并生成 native 库。
- 检查 APK 内容确认存在 `lib/arm64-v8a/libhyp_ffi.so`，大小为 `4909976` bytes。

### Notes
- `scripts/build-rust-android.ps1`：新增 Android Rust native 构建脚本。
- `android/app/build.gradle.kts`：新增 generated `jniLibs` 路径和 debug 构建前的 Rust native 构建任务。
- `rust/crates/HyP_ffi/src/lib.rs`：补全 JNI float array 输入和 byte array 输出入口。
- `README.md`、`android/README.md`、`rust/README.md`：更新 native 打包状态与构建说明。
- 当前限制：APK 已包含 native 库，但画布可视结果仍使用 Kotlin preview；下一步需要把 native RGBA 渲染结果显示到 Compose 画布。
- 回滚方式：执行 `git revert <本轮提交哈希>`；如未提交，删除脚本、还原 Gradle native hook 和 `HyP_ffi` JNI 补充。

## 2026-06-30 - Task: 显示 Rust 引擎渲染结果

### What was done
- 扩展 `PaintingEngine` snapshot，加入画布尺寸与 native render image。
- `NativePaintingEngine` 在提交笔画后调用 Rust `nativeRenderRgba()`，将 RGBA byte array 转换为 Compose `ImageBitmap` 并显示到画布。
- Compose 画布新增白色画布底色，native image 作为已提交笔画层，活动笔画继续用 Kotlin preview 保持实时反馈。
- Rust `HyPaintDocument` 记录已提交笔画，新增 undo 重放逻辑，支持 MVP 阶段的笔画级撤销。
- 补充 `nativeUndo` JNI 入口，并让 Android undo 在 native engine 下调用 Rust undo 后刷新 render image。

### Testing
- `cd rust; cargo fmt --all -- --check; cargo test`：通过。
- `.\gradlew.bat :android:app:assembleDebug --stacktrace`：通过。
- 检查 APK 内容确认存在 `lib/arm64-v8a/libhyp_ffi.so`，大小为 `4989288` bytes。
- 使用 NDK `llvm-nm` 确认 `.so` 导出 `nativeCreate`、`nativeAppendStroke`、`nativeUndo`、`nativeRenderRgba` JNI 符号。

### Notes
- `android/app/src/main/java/io/github/lukasvi/hypainter/engine/**`：新增 native image snapshot、RGBA 转 ImageBitmap、native undo 刷新逻辑。
- `android/app/src/main/java/io/github/lukasvi/hypainter/MainActivity.kt`：画布显示白底、native render image 和活动笔画 preview。
- `rust/crates/HyP_ffi/src/lib.rs`：新增 stroke replay 存储、undo 重建图层和 JNI undo。
- `README.md`、`android/README.md`、`rust/README.md`：更新 native render 已进入 Compose 画布的状态。
- 当前限制：native render 仍是全画布 RGBA 刷新，后续需要 tile/dirty rect 局部刷新；MVP 还缺图层 UI、颜色/笔刷控制、保存加载和导出入口。
- 回滚方式：执行 `git revert <本轮提交哈希>`；如未提交，还原 Android engine/MainActivity 和 `HyP_ffi` 本轮修改。

## 2026-06-30 - Task: 增加 MVP 笔刷控制与 PNG 导出

### What was done
- Android `PaintingEngine` 新增笔刷设置和 PNG 导出接口，native 与 Kotlin fallback 均实现。
- Compose 画布新增基础色彩按钮、笔刷尺寸调整、压力读数和 Export 按钮。
- Rust `hyp_ffi` 新增 `nativeSetBrush`，支持将颜色和半径传入 Rust 绘画核心。
- Rust 文档模型改为每条笔画保存提交时的笔刷参数，确保改色或改尺寸后 undo/replay 不会改变旧笔画外观。
- PNG 导出会把当前画布写入应用私有目录 `hypainter-export.png`，作为 MVP 导出闭环的第一步。

### Testing
- `cd rust; cargo fmt --all -- --check; cargo test`：通过。
- `.\gradlew.bat :android:app:assembleDebug --stacktrace`：通过。
- 检查 APK 内容确认存在 `lib/arm64-v8a/libhyp_ffi.so`，大小为 `4993024` bytes。
- 使用 NDK `llvm-nm` 确认 `.so` 导出 `nativeCreate`、`nativeAppendStroke`、`nativeUndo`、`nativeSetBrush`、`nativeRenderRgba` JNI 符号。

### Notes
- `android/app/src/main/java/io/github/lukasvi/hypainter/MainActivity.kt`：新增颜色、尺寸和导出控制，并按笔刷样式绘制 preview。
- `android/app/src/main/java/io/github/lukasvi/hypainter/engine/**`：新增 `EngineBrush`、笔刷设置、PNG 导出实现。
- `rust/crates/HyP_ffi/src/lib.rs`：新增每笔画笔刷存储、brush setter 和 JNI `nativeSetBrush`。
- `README.md`、`android/README.md`、`rust/README.md`：更新 MVP 控制和导出状态。
- 当前限制：导出目前写入 app-private 文件，尚未接 Android 分享面板或系统文件选择器；仍缺图层 UI 和项目保存加载。
- 回滚方式：执行 `git revert <本轮提交哈希>`；如未提交，还原 Android UI/engine 和 `HyP_ffi` 本轮修改。

## 2026-06-30 - Task: 增加项目保存与加载闭环

### What was done
- `PaintingEngine` 新增项目保存和加载接口，native engine 与 Kotlin fallback 均实现。
- 新增 `ProjectCodec`，使用简单文本草稿格式保存画布尺寸、已提交笔画、每笔画刷颜色和半径、采样点数据。
- Compose 工具条新增 Save/Load 按钮，保存到 app-private `hypainter-project.hyp`，加载后恢复笔画并刷新画布。
- Native engine 保留 committed stroke history，用于项目保存；加载项目时会清空 Rust 文档、逐笔设置 brush 并重新提交到 native 渲染核心。

### Testing
- `cd rust; cargo fmt --all -- --check; cargo test`：通过。
- `.\gradlew.bat :android:app:assembleDebug --stacktrace`：通过。
- 检查 APK 内容确认存在 `lib/arm64-v8a/libhyp_ffi.so`，大小为 `4993024` bytes。

### Notes
- `android/app/src/main/java/io/github/lukasvi/hypainter/engine/ProjectCodec.kt`：新增草稿项目保存加载编解码。
- `android/app/src/main/java/io/github/lukasvi/hypainter/engine/**`：新增 save/load project 接口与 native/fallback 实现。
- `android/app/src/main/java/io/github/lukasvi/hypainter/MainActivity.kt`：新增 Save/Load UI 控制。
- `README.md`、`android/README.md`：更新 app-private 项目保存加载状态。
- 当前限制：保存格式是 MVP 草稿格式，还不是正式 `.pdraw` 容器；Load 固定读取 app-private 默认路径，尚未接文件管理页。
- 回滚方式：执行 `git revert <本轮提交哈希>`；如未提交，删除 `ProjectCodec.kt` 并还原 engine/MainActivity/README/progress 本轮修改。

## 2026-06-30 - Task: 增加 MVP 图层控制

### What was done
- `PaintingEngine` 新增图层模型和操作接口：新增图层、选择当前图层、切换图层可见性。
- 每条笔画新增 `layerId`，新笔画会提交到当前图层。
- Kotlin fallback 渲染、native engine 重放、PNG 导出和项目保存加载均按图层可见性过滤。
- Compose 工具条新增横向滚动，加入 `+ Layer`、图层选择和 Hide/Show 控制。
- 草稿项目格式新增 active layer、layer metadata 和 stroke layer id，加载后可恢复图层状态。

### Testing
- `cd rust; cargo fmt --all -- --check; cargo test`：通过。
- `.\gradlew.bat :android:app:assembleDebug --stacktrace`：通过。

### Notes
- `android/app/src/main/java/io/github/lukasvi/hypainter/engine/PaintingEngine.kt`：新增 `EngineLayer` 与图层操作接口。
- `android/app/src/main/java/io/github/lukasvi/hypainter/engine/KotlinPaintingEngine.kt`：实现图层状态、可见性过滤和图层持久化加载。
- `android/app/src/main/java/io/github/lukasvi/hypainter/engine/NativePaintingEngine.kt`：通过清空并重放可见图层笔画刷新 Rust render。
- `android/app/src/main/java/io/github/lukasvi/hypainter/engine/ProjectCodec.kt`：保存/加载图层元数据和 stroke layer id。
- `android/app/src/main/java/io/github/lukasvi/hypainter/MainActivity.kt`：新增横向滚动工具条和图层控制。
- 当前限制：图层实现是 MVP 语义层，Rust core 仍是单 raster document，通过 Android 侧可见图层重放实现显隐；还没有图层重排、透明度和合并。
- 回滚方式：执行 `git revert <本轮提交哈希>`；如未提交，还原 engine、ProjectCodec、MainActivity 和 README/progress 本轮修改。

## 2026-06-30 - Task: 增加分享导出与 MVP 验收记录

### What was done
- 新增 Android `FileProvider` 配置和 `file_paths.xml`，允许分享 app-private PNG 导出文件。
- Compose 工具条新增 Share 按钮，导出 PNG 后通过 Android chooser 分享。
- 增加 `docs/mvp-status.md`，记录当前 MVP 已覆盖能力、验证命令、实现边界和后续非 MVP 工作。
- README 增加 MVP 状态文档入口。

### Testing
- `cd rust; cargo fmt --all -- --check; cargo test`：通过。
- `.\gradlew.bat :android:app:assembleDebug --stacktrace`：通过。
- 检查 APK 内容确认存在 `lib/arm64-v8a/libhyp_ffi.so` 和 `res/xml/file_paths.xml`。
- 检查 merged debug manifest 确认包含 `androidx.core.content.FileProvider`、`io.github.lukasvi.hypainter.fileprovider` 和 `@xml/file_paths`。

### Notes
- `android/app/src/main/AndroidManifest.xml`：新增 FileProvider。
- `android/app/src/main/res/xml/file_paths.xml`：新增 app-private 文件分享路径。
- `android/app/src/main/java/io/github/lukasvi/hypainter/MainActivity.kt`：新增 Share 按钮与 Android chooser 调用。
- `gradle/libs.versions.toml`、`android/app/build.gradle.kts`：新增 AndroidX core 依赖用于 FileProvider。
- `docs/mvp-status.md`：新增 MVP 状态与验收说明。
- `README.md`、`progress.md`：更新分享导出与 MVP 状态记录。
- 当前限制：分享导出已可用，但尚未做真机安装后的手写笔交互验收；UI 仍是 MVP 单行工具条，不是最终平板绘画布局。
- 回滚方式：执行 `git revert <本轮提交哈希>`；如未提交，删除 FileProvider 配置、分享按钮、MVP 状态文档并还原 Gradle/README/progress 修改。

## 2026-06-30 - Task: 巩固画布输入分层与坐标映射

### What was done
- 将画布输入从页面级 `detectTransformGestures` 与 `pointerInteropFilter` 混用，改为画布级单一 `MotionEvent` 路由。
- 新增笔输入优先级：stylus/eraser 指针开始绘制后会消费后续笔事件，杜绝笔事件参与画布平移/旋转。
- 双指 finger touch 独立负责画布 pan/zoom/rotation，旋转与缩放锚点固定为双指连线中心。
- 修正 screen-to-canvas 映射，采样坐标现在会反向应用画布位移、缩放和旋转。
- 修正 Compose 绘制变换 pivot，画布缩放/旋转围绕文档原点应用，再由输入路由按双指中心计算新的 pan。
- MOVE 事件补录 `MotionEvent` historical samples，减少高频笔输入时的采样丢失。
- 更新 README、Android README 与 MVP 状态文档，记录当前输入语义和验证边界。

### Testing
- `.\gradlew.bat :android:app:assembleDebug`：通过。

### Notes
- 参考 Android 官方 `MotionEvent` 文档的 pointer/tool type 与 historical sample API，按 `TOOL_TYPE_STYLUS`、`TOOL_TYPE_ERASER`、`TOOL_TYPE_FINGER` 分层。
- 参考 Jetpack Compose pointer input 文档后，当前选择不用通用 `detectTransformGestures`，而是在画布 surface 上统一处理事件优先级。
- 当前仍缺真实 Android 平板手写笔长时间绘制验收；下一步应通过 Android Studio/ADB 在真机上观察 Logcat、帧率和输入中断情况。
- 回滚方式：执行 `git revert <本轮提交哈希>`；如未提交，还原 `MainActivity.kt`、README、android/README、docs/mvp-status.md 和 progress 本轮修改。

## 2026-07-05 - Task: 增加输入调试叠层并修复触摸流回归

### What was done
- 为 debug build 增加工具栏 `Debug` 开关，显示画布输入叠层：route、action、tool type、pointer count、pressure、sample counts、screen/canvas 坐标和 viewport transform。
- 增加 `HyPainterInput` Logcat 输出，MOVE 事件按 250ms 节流，非 MOVE 事件即时记录。
- 修复上一轮路由回归：`pointerInteropFilter` 在第一根 finger `ACTION_DOWN` 返回 `false` 会导致后续双指事件收不到；现在画布 finger stream 会先接管事件流，但只有双指才会改变画布视口。
- 将 `.kotlin/` 加入 `.gitignore`，避免 Kotlin 本地 session 缓存进入工作区。

### Testing
- `.\gradlew.bat :android:app:assembleDebug`：通过。

### Notes
- 命令行调试入口：`adb logcat -s HyPainterInput`。
- 屏幕叠层只在 `BuildConfig.DEBUG` 下显示，属于低粘性的调试组件，不进入 release 产品路径。
- 当前仍需真机确认：单指在画布上不触发平移，第二根手指落下后双指 pan/zoom/rotation 恢复，stylus 绘制中 finger 事件不抢占。
- 回滚方式：执行 `git revert <本轮提交哈希>`；如未提交，还原 `.gitignore`、`MainActivity.kt`、android/README、docs/mvp-status.md 和 progress 本轮修改。

## 2026-07-05 - Task: 为画布视口坐标变换增加单测护栏

### What was done
- 将 `ViewportState`、`TouchGestureFrame` 和旋转数学从 `MainActivity.kt` 抽到 `CanvasViewport.kt`。
- 为视口增加 `toScreen()`，用于调试与单测验证 screen/canvas 映射的互逆关系。
- 新增 `CanvasViewportTest`，覆盖 pan/scale/rotation 下的坐标往返、双指中心锚定和 scale clamp 后的锚点稳定性。

### Testing
- `.\gradlew.bat :android:app:testDebugUnitTest`：通过。
- `.\gradlew.bat :android:app:assembleDebug`：通过。

### Notes
- 这组测试直接约束本轮最容易回归的底层漏洞：旋转后的 screen-to-canvas 映射和双指变换中心。
- 仍需要真机输入流验证，因为 JVM 单测不能证明厂商手写笔事件序列、掌触事件序列和 Compose/Android 分发细节。
- 回滚方式：执行 `git revert <本轮提交哈希>`；如未提交，删除 `CanvasViewport.kt`、`CanvasViewportTest.kt`，并将视口数据结构恢复到 `MainActivity.kt`。

## 2026-07-05 - Task: 降低输入调试与压感读数的高频开销

### What was done
- `Debug` chip 关闭时不再更新 overlay Compose state，也不再写 `HyPainterInput` Logcat，避免调试组件默认参与每个输入事件。
- 压感读数更新节流到 80ms，减少长笔画 MOVE 事件对工具栏文本的重组压力。
- 当 MOVE 事件中找不到当前 stylus pointer 时主动结束当前 stroke 并刷新状态，避免 active stroke 悬挂。
- 更新 Android README 与 MVP 状态文档，说明 Logcat 输入调试需要先打开 `Debug` chip。

### Testing
- `.\gradlew.bat :android:app:testDebugUnitTest`：通过。

### Notes
- 这次只降低诊断和读数 UI 的高频开销；实时笔画预览仍保留每个 MOVE 事件刷新，后续若仍卡顿，应继续做帧率节流或局部绘制刷新。
- 回滚方式：执行 `git revert <本轮提交哈希>`；如未提交，还原 `MainActivity.kt`、android/README、docs/mvp-status.md 和 progress 本轮修改。

## 2026-07-05 - Task: 将笔画预览刷新合并到屏幕帧节奏

### What was done
- 新增 `FrameInvalidator`，使用 `View.postOnAnimation` 合并高频 stylus MOVE 的 Compose 刷新请求。
- 笔输入采样仍逐个追加到 engine，historical samples 不丢；只将 active stroke preview 的 `version++` 限制到下一帧。
- `ACTION_DOWN`、`ACTION_UP`、`ACTION_CANCEL` 和找不到 stylus pointer 的异常收尾仍立即刷新，保证开始/结束状态及时一致。

### Testing
- `.\gradlew.bat :android:app:testDebugUnitTest`：通过。
- `.\gradlew.bat :android:app:assembleDebug`：通过。

### Notes
- 这是针对“笔操作比触摸低得多、长笔画几秒后中断/卡顿”的核心高频路径优化，减少输入采样频率高于屏幕刷新率时的无效重组。
- 后续真机若仍出现卡顿，应继续拆分 active stroke preview 绘制与整页 toolbar 重组，或引入更细粒度的 canvas invalidation。
- 回滚方式：执行 `git revert <本轮提交哈希>`；如未提交，移除 `FrameInvalidator` 并将 stylus MOVE 恢复为直接 `onEngineChanged()`。

## 2026-07-05 - Task: 加固笔触与触摸交错时的输入状态机

### What was done
- stylus/eraser 一旦接管输入，立即清理 finger stream 与双指 gesture 状态，避免先手指后笔、掌触混入时残留旧状态。
- stylus 正常抬起、取消、或 MOVE 中找不到 active pointer 的异常收尾都会清理 stylus、finger 与双指 gesture 状态。
- 保持笔输入最高优先级，触摸事件不会在笔画期间切回画布平移/旋转。

### Testing
- `.\gradlew.bat :android:app:testDebugUnitTest`：通过。
- `.\gradlew.bat :android:app:assembleDebug`：通过。

### Notes
- 这次修复针对真机常见的混合事件序列：掌触、先 finger 再 stylus、pointer index 变化和厂商事件取消。
- 仍需用平板验证 overlay 中 route 在笔画期间保持 stylus，不会跳到 two-finger 或 single-finger。
- 回滚方式：执行 `git revert <本轮提交哈希>`；如未提交，还原 `MainActivity.kt` 与 progress 本轮修改。

## 2026-07-05 - Task: 修正 Native active stroke 预览图层一致性

### What was done
- 修正 `NativePaintingEngine.snapshot()`，active stroke preview 会按 native engine 当前 `activeLayerId` 重标记。
- 避免 Kotlin fallback preview 在 `clear()` 后回到 Layer 1，导致 native 模式下绘制中预览 stroke 的 layer id 与最终提交 layer id 不一致。

### Testing
- `.\gradlew.bat :android:app:testDebugUnitTest`：通过。
- `.\gradlew.bat :android:app:assembleDebug`：通过。

### Notes
- 这是图层/预览一致性修复，避免后续图层显隐、选择与预览行为继续分叉。
- 当前仍需要更完整的 engine 层测试，尤其是 native bridge 在真实设备上的图层与预览一致性。
- 回滚方式：执行 `git revert <本轮提交哈希>`；如未提交，还原 `NativePaintingEngine.kt` 与 progress 本轮修改。

## 2026-07-05 - Task: 降低长笔画预览的分配压力

### What was done
- `KotlinPaintingEngine.snapshot()` 的 active stroke preview 不再每帧 `toList()` 复制点列表，减少长笔画期间随点数增长的重复分配。
- Compose `drawStroke()` 与 Kotlin bitmap `drawStroke()` 从 `zipWithNext()` 改为 index loop，减少每帧绘制 active stroke 时的 Pair/Iterable 临时对象。

### Testing
- `.\gradlew.bat :android:app:testDebugUnitTest`：通过。
- `.\gradlew.bat :android:app:assembleDebug`：通过。

### Notes
- 这次继续针对长时间 stylus 绘制卡顿路径，减少 frame-throttled preview 内仍会发生的 O(n) 列表复制和绘制分配。
- active stroke 仍在主线程输入/绘制路径使用，当前没有并发访问；若未来引入后台渲染，需要重新评估 snapshot 的不可变边界。
- 回滚方式：执行 `git revert <本轮提交哈希>`；如未提交，恢复 active stroke `toList()` 和 `zipWithNext()` 绘制。

## 2026-07-05 - Task: 拆分画布预览刷新与工具栏模型刷新

### What was done
- 将单一 `version` 拆为 `canvasVersion` 与 `modelVersion`，stylus MOVE 只刷新画布预览 snapshot。
- 新增 `CanvasToolbar` composable，工具栏使用独立的 `toolbarSnapshot`，只有笔刷、图层、加载、清空、撤销等模型操作才刷新。
- 保持按钮行为、导出/分享、项目保存加载和 Debug overlay 开关不变。

### Testing
- `.\gradlew.bat :android:app:testDebugUnitTest`：通过。
- `.\gradlew.bat :android:app:assembleDebug`：通过。

### Notes
- 这次继续降低长笔画期间工具栏参与高频重组的概率，让 active stroke preview 与按钮区模型状态分离。
- 后续如果仍有按钮卡顿，应进一步把工具栏变为常驻控制面板状态模型，避免直接读取完整 engine snapshot。
- 回滚方式：执行 `git revert <本轮提交哈希>`；如未提交，恢复 `CanvasScreen` 内联工具栏和单一 `version` 计数。

## 2026-07-05 - Task: 修复 active stroke 预览列表别名进入提交历史

### What was done
- 新增 `stableCopyForLayer()`，在 stroke 进入 committed/native 提交边界前复制点列表并重标记图层。
- `NativePaintingEngine.endStroke()` 使用稳定副本生成 native samples 和 committed history，避免 fallback preview 清空时影响已提交 stroke。
- 新增 `StrokeSnapshotsTest`，验证稳定副本不会被原 mutable point list 清空影响。

### Testing
- `.\gradlew.bat :android:app:testDebugUnitTest`：通过。
- `.\gradlew.bat :android:app:assembleDebug`：通过。

### Notes
- 这是上一轮降低预览复制开销后发现的数据正确性边界：预览可以共享 live list，但提交历史必须稳定复制。
- 回滚方式：执行 `git revert <本轮提交哈希>`；如未提交，删除 `StrokeSnapshots.kt`、`StrokeSnapshotsTest.kt` 并还原 `NativePaintingEngine.kt`。

## 2026-07-05 - Task: 审计 Compose 绘制变换与 viewport 数学一致性

### What was done
- 复核画布绘制变换顺序，当前 `translate -> rotate -> scale` 与 `ViewportState.toScreen()` 的 `pan + rotate(canvas * scale)` 模型一致。
- 在 `withTransformCompat()` 添加注释，防止后续调整 draw transform 顺序时破坏 screen/canvas 映射不变量。

### Testing
- 本轮是注释与审计记录更新，依赖上一轮 `CanvasViewportTest` 约束 viewport 数学；未改变运行逻辑。

### Notes
- 若后续改用 `graphicsLayer` 或矩阵绘制，需要重新验证绘制矩阵与 `toCanvas()` 逆变换仍然一致。
- 回滚方式：执行 `git revert <本轮提交哈希>`；如未提交，还原 `MainActivity.kt` 与 progress 本轮修改。

## 2026-07-05 - Task: 抽离输入调试叠加层到 debug 包

### What was done
- 新增 `io.github.lukasvi.hypainter.debug` 包，集中放置 `CanvasDebugOverlay`、`CanvasDebugState` 和 debug 格式化/Logcat 常量。
- `MainActivity.kt` 移除调试叠加层 UI 和调试状态格式化实现，只保留输入路由生产逻辑与 debug state 发布入口。
- 保持 `Debug` chip、overlay 显示内容和 `HyPainterInput` Logcat 行为不变。

### Testing
- `.\gradlew.bat :android:app:testDebugUnitTest`：通过。
- `.\gradlew.bat :android:app:assembleDebug`：通过。

### Notes
- 这次是低粘性调试组件整理，为后续继续增加 frame time、event stream、pointer trace 等调试视图留出独立目录。
- 回滚方式：执行 `git revert <本轮提交哈希>`；如未提交，删除 `debug/` 包并把 overlay/debug state 定义恢复到 `MainActivity.kt`。

## 2026-07-05 - Task: 抽离画布输入路由到 input 包

### What was done
- 新增 `io.github.lukasvi.hypainter.input.CanvasInputRouter`，集中放置 stylus/finger 分层、pointer state、historical sample、debug publish 和 MotionEvent helper。
- `MainActivity.kt` 删除输入状态机实现，只保留 Compose 组合、画布绘制和工具栏控制。
- 保持现有输入行为不变：笔优先、单指保留事件流、双指变换、Debug overlay/Logcat 仍按开关启用。

### Testing
- `.\gradlew.bat :android:app:testDebugUnitTest`：通过。
- `.\gradlew.bat :android:app:assembleDebug`：通过。

### Notes
- 这次是结构整理，为后续对输入状态机做单元测试或替换为更正式的事件管线留出边界。
- 回滚方式：执行 `git revert <本轮提交哈希>`；如未提交，删除 `input/CanvasInputRouter.kt` 并把路由类与 helper 恢复到 `MainActivity.kt`。

## 2026-07-05 - Task: 增加真机输入验收清单

### What was done
- 新增 `docs/device-input-test-plan.md`，覆盖 stylus priority、screen-to-canvas mapping、touch layering、two-finger centroid rotation 和 long-stroke performance。
- 清单写明 Debug overlay、`adb logcat -s HyPainterInput`、预期 route/action/tool 和失败时应记录的证据。
- Android README 与 MVP 状态文档增加真机输入验收入口。

### Testing
- 文档变更，无运行逻辑修改。

### Notes
- 这份清单用于补齐当前自动化测试无法证明的部分：厂商手写笔事件、掌触序列、真实触控采样率和实际 UI 卡顿。
- 回滚方式：执行 `git revert <本轮提交哈希>`；如未提交，删除 `docs/device-input-test-plan.md` 并还原 android/README、docs/mvp-status.md 和 progress 本轮修改。

## 2026-07-05 - Task: 将 native 全量 bitmap 渲染移出输入路径

### What was done
- `NativePaintingEngine.endStroke()` 不再在抬笔时执行 `nativeRenderRgba()`、RGBA 转 Bitmap 和 ImageBitmap 创建。
- undo、图层显隐和 native document rebuild 只标记 rendered bitmap dirty，不立刻生成大图。
- `exportPng()` 按需生成 native bitmap，GC-heavy 的全量渲染移到导出/分享路径。
- Compose 画布按 visible layer 过滤 committed/active vector strokes，保证 rendered image 延迟生成后图层显隐仍能正确预览。
- 新增 `docs/android-studio-debugging.md`，说明 Android Studio Logcat、`HyPainterInput`、Debug chip 和 GC/input latency 搜索方式。

### Testing
- `.\gradlew.bat :android:app:testDebugUnitTest`：通过。
- `.\gradlew.bat :android:app:assembleDebug`：通过。

### Notes
- 该修复针对真机日志中的 `InputEventAction=1` 后 `Skipped 102 frames`、`Davey duration=1855ms` 和 sticky GC。抬笔事件不应触发全量 bitmap 分配。
- 导出/分享仍可能卡顿，因为它们仍需生成 PNG；后续应把导出迁到后台任务并显示状态。
- 回滚方式：执行 `git revert <本轮提交哈希>`；如未提交，还原 `NativePaintingEngine.kt`、`MainActivity.kt` 和新增/修改文档。

## 2026-07-05 - Task: 增加输入延迟与堆内存调试指标

### What was done
- Debug overlay 和 `HyPainterInput` Logcat 增加 `eventAgeMs`，用于判断事件到达 HyPainter 前是否已经积压。
- 增加 `handleDurationMs`，用于判断 HyPainter 输入路由本身是否耗时过长。
- 增加 Java heap used/free/max KB，辅助关联 sticky GC 与输入事件。
- 更新 Android Studio 调试文档，说明 `age`、`handle` 与 `Heap` 字段的判读方式。

### Testing
- `.\gradlew.bat :android:app:testDebugUnitTest`：通过。
- `.\gradlew.bat :android:app:assembleDebug`：通过。

### Notes
- 指标只在 debug build 且 `Debug` chip 打开时计算，避免影响正常绘制性能基线。
- 真机排查时先关 Debug 测 GC/Choreographer，再短暂打开 Debug 区分事件 backlog 与 router 自身耗时。
- 回滚方式：执行 `git revert <本轮提交哈希>`；如未提交，还原 `debug/*`、`input/CanvasInputRouter.kt`、`docs/android-studio-debugging.md` 和 progress 本轮修改。

## 2026-07-05 - Task: 默认 bitmap 渲染使用完美像素采样

### What was done
- 新增 `render/BitmapSampling.kt`，定义 `PixelPerfect`、`Nearest`、`Linear`、`Bilinear` 和 `Bicubic` 采样选项。
- 新增 `CanvasRenderOptions`，默认 `bitmapSampling = PixelPerfect`。
- Compose `drawImage()` 显式使用 `FilterQuality.None`，避免 bitmap 缩放/旋转时默认插值导致模糊。
- README 与 MVP 状态文档记录默认 pixel-perfect sampling 和后续 UI 选择计划。

### Testing
- `.\gradlew.bat :android:app:testDebugUnitTest`：通过。
- `.\gradlew.bat :android:app:assembleDebug`：通过。

### Notes
- 目前只接入默认策略，UI 选择器后续可直接绑定 `BitmapSampling`。
- Compose `FilterQuality` 不直接暴露“线性/双线性”的严格命名，本轮先将 Linear/Bilinear 映射到中等过滤质量，Bicubic 映射到高质量过滤。
- 回滚方式：执行 `git revert <本轮提交哈希>`；如未提交，删除 `render/BitmapSampling.kt` 并恢复 `drawImage(image)`。

## 2026-07-05 - Task: 将工具栏重 I/O 操作移出主线程

### What was done
- Export、Share、Save 和 Load 通过 `rememberCoroutineScope` 启动后台任务，并在 `Dispatchers.IO` 中执行 engine/file 工作。
- 工具栏新增 busy 状态，重 I/O 运行时禁用 Export/Share/Save/Load，避免连续点击堆积任务。
- 操作开始、成功、失败状态仍回主线程更新；Share 的 Android chooser 仍在主线程启动。
- 后台任务异常时会恢复 busy 状态并显示失败，避免按钮永久卡住。

### Testing
- `.\gradlew.bat :android:app:testDebugUnitTest`：通过。
- `.\gradlew.bat :android:app:assembleDebug`：通过。

### Notes
- 这次针对频繁点击按钮造成的主线程卡顿，将 PNG 压缩、项目文件 I/O、项目解析等移出点击回调主线程。
- 当前仍是 MVP 级异步处理；后续应给 engine 增加正式任务队列或读写锁，避免后台导出/加载和绘制输入并发访问同一 engine 状态。
- 回滚方式：执行 `git revert <本轮提交哈希>`；如未提交，还原 `MainActivity.kt` 本轮修改。

## 2026-07-05 - Task: 为后台工具栏任务增加 engine 并发访问保护

### What was done
- 工具栏重 I/O 任务 busy 期间，画布 pointer 事件直接消费并忽略，避免手写输入同时修改 engine。
- Export、Share、Save、Load 运行中禁用 Clear、Undo、Brush、Size、Layer Select、Layer Visibility 等所有 engine 变更按钮。
- Debug chip 保持可用，便于观察 busy 时输入是否被屏蔽。

### Testing
- `.\gradlew.bat :android:app:testDebugUnitTest`：通过。
- `.\gradlew.bat :android:app:assembleDebug`：通过。

### Notes
- 这是异步化后的保守 MVP 并发策略：后台任务运行时暂停绘制和模型变更，避免主线程等待锁或后台任务读写半更新状态。
- 后续更完整方案应使用 engine 单线程任务队列、snapshot copy 或读写锁，使导出可与绘制更细粒度地并行。
- 回滚方式：执行 `git revert <本轮提交哈希>`；如未提交，还原 `MainActivity.kt` 与 progress 本轮修改。

## 2026-07-05 - Task: 减少画布每帧图层过滤分配

### What was done
- 移除画布绘制路径中的 `filter -> map -> toSet -> filter` 可见图层集合构建。
- 改用 indexed loop 绘制 committed strokes，并用 `layerIsVisible()` 直接检查图层可见性。
- active stroke 也走同一可见性检查，避免每帧创建临时集合。

### Testing
- `.\gradlew.bat :android:app:testDebugUnitTest`：通过。
- `.\gradlew.bat :android:app:assembleDebug`：通过。

### Notes
- 这是针对长笔画/高帧率预览的 GC 压力削减，减少每帧小对象和集合分配。
- 后续图层数量增多后可维护 layer visibility cache，但当前 indexed scan 对 MVP 图层数量更简单。
- 回滚方式：执行 `git revert <本轮提交哈希>`；如未提交，恢复 draw path 中的 visible layer set/filter 逻辑。

## 2026-07-05 - Task: 将已提交笔画实时落到显示 bitmap 缓存

### What was done
- 新增 engine 内部 `StrokeRasterCache`，在笔画提交、撤销、图层显隐和项目加载时维护透明 bitmap 缓存。
- Kotlin fallback 与 Native engine 的画布 snapshot 现在返回缓存 bitmap + active stroke preview，画布绘制路径不再每帧重放全部 committed strokes。
- Native engine 继续把 Rust 文档作为导出和长期状态来源，但 UI 显示缓存由 Android 侧实时增量更新，避免抬笔后触发全量 RGBA 大图刷新。
- `MainActivity` 对有 `renderedImage` 的 snapshot 跳过 committed vector replay，防止缓存与历史笔画双重绘制。
- 修正 native 不可用回落 Kotlin engine 时未传递自定义画布尺寸的小边界。
- 复跑用户附件中的 `:android:app:packageDebug` 路径，当前 `assembleDebug` 已通过，未复现 AGP IncrementalSplitter NPE。

### Testing
- `.\gradlew.bat :android:app:testDebugUnitTest`：通过。
- `.\gradlew.bat :android:app:assembleDebug --stacktrace`：通过，包含 `:android:app:packageDebug`。

### Notes
- 这次解决的是“历史笔画越多，画布每帧 replay 越慢”的主路径；active stroke 仍按当前 preview 方式绘制，长单笔极端采样量后续还应进入 tile/dirty-rect 预览。
- 如果 `packageDebug` NPE 之后再次出现，优先清理 `android/app/build/intermediates` 与 APK outputs，或执行 `.\gradlew.bat clean :android:app:assembleDebug --stacktrace`；本轮代码状态下未复现。
- 当前仍需真实安卓平板验证：多笔画压力测试下 GC、`Choreographer` skipped frames 和 MIUI input latency 是否明显下降。
- 回滚方式：执行 `git revert <本轮提交哈希>`；如未提交，删除 `StrokeRasterCache.kt` 并还原 `MainActivity.kt` 与 engine 本轮修改。

## 2026-07-05 - Task: 将活动笔画预览改为增量 bitmap 缓存

### What was done
- 扩展 `StrokeRasterCache`，支持按单点和相邻样本段增量追加，且同一张 mutable bitmap 只创建一次 `ImageBitmap` 包装。
- `KotlinPaintingEngine` 新增 active stroke 预览缓存：`beginStroke()` 清空并画起点，`appendSample()` 只追加最新线段，`endStroke()` 提交到 committed cache 后清空 active cache。
- `EngineSnapshot` 新增 `activeImage`，Compose 画布优先绘制活动 bitmap 预览，只有没有缓存时才回退到 vector active stroke 绘制。
- Native engine 继续复用 Kotlin fallback preview，因此 native 模式下活动笔画也进入同一增量预览路径。

### Testing
- `.\gradlew.bat :android:app:testDebugUnitTest`：通过。
- `.\gradlew.bat :android:app:assembleDebug --stacktrace`：通过，包含 Rust native 构建和 `:android:app:packageDebug`。

### Notes
- 这次针对“单条长笔画越画越卡”的底层路径，避免每帧遍历整条 active stroke；点列表仍保留用于提交、保存和 Debug overlay 的采样计数。
- 预览缓存仍是全画布透明 bitmap，后续更完整方案应升级为 tile/dirty-rect active preview，以减少大画布内存和局部刷新成本。
- 当前仍需真机验证：长按笔连续绘制 10-30 秒，观察 `HyPainterInput handle`、Java heap、`Skipped frames` 和 MIUI input latency。
- 回滚方式：执行 `git revert <本轮提交哈希>`；如未提交，还原 `StrokeRasterCache.kt`、`PaintingEngine.kt`、engine 和 `MainActivity.kt` 本轮修改。

## 2026-07-05 - Task: 为输入优先级状态机增加单测护栏

### What was done
- 新增 `CanvasInputSession`，把 stylus priority、finger stream、two-finger previous frame 等输入状态从 `CanvasInputRouter` 中抽成纯 Kotlin 核心。
- `CanvasInputRouter` 继续负责 `MotionEvent` 读取、historical samples 和 engine 调用，但输入流接管/释放改由 `CanvasInputSession` 统一管理。
- 新增 `CanvasInputSessionTest`，覆盖 stylus 接管后清理 finger gesture、非当前 stylus pointer up 不结束笔画、当前 stylus pointer up 释放优先级、单指不触发 viewport transform、双指从上一帧到下一帧围绕 centroid 锚定。
- 对照 Android `MotionEvent` 的 action index、pointer id、tool type 与 historical sample 语义，继续保持生产路由按 active pointer id 查找 stylus pointer。

### Testing
- `.\gradlew.bat :android:app:testDebugUnitTest`：通过。
- `.\gradlew.bat :android:app:assembleDebug --stacktrace`：通过，包含 Rust native 构建和 `:android:app:packageDebug`。

### Notes
- 这组测试不能替代真机厂商输入序列验证，但能防止最关键的输入分层规则被后续 UI/手势改动无声破坏。
- 后续可在引入 Robolectric 或 instrumentation test 后，直接构造/回放真实 `MotionEvent` 序列验证 router 到 engine 的端到端行为。
- 回滚方式：执行 `git revert <本轮提交哈希>`；如未提交，删除 `CanvasInputSession.kt` 与 `CanvasInputSessionTest.kt`，并将状态字段恢复到 `CanvasInputRouter.kt`。

## 2026-07-05 - Task: 改善首段笔画丢失诊断与 stylus 指针恢复

### What was done
- `CanvasInputRouter` 不再只用 `actionIndex` 判断是否进入 stylus 路由，改为扫描当前事件中的任意 stylus/eraser pointer，降低厂商多指/掌触事件中 stylus pointer 不是 action pointer 时漏判的概率。
- 当 `ACTION_MOVE` 中找不到已记录的 active stylus pointer，但事件里仍存在 stylus pointer 时，会重新接管该 pointer 并继续追加样本，避免首个 down/pointer id 状态异常后整段笔画被放弃。
- Debug overlay/Logcat 的 MOVE 坐标优先显示 active stylus pointer，其次显示任意 stylus pointer，避免用无语义的 MOVE `actionIndex` 误导排查。
- 更新 Android Studio 调试文档，说明 `-D --suspend`/`Connected to the target VM` 是断点调试启动方式，不适合作为 stylus 延迟和 GC 的基线测量方式。

### Testing
- `.\gradlew.bat :android:app:testDebugUnitTest`：通过。
- `.\gradlew.bat :android:app:assembleDebug --stacktrace`：通过。

### Notes
- 用户真机反馈实时渲染后笔画、撤销已非常流畅且跟手，但偶发“吃首部笔画”；本轮先修复最可疑的 stylus pointer 选择和诊断误差。
- 如果仍出现首段缺失，下一步应短暂打开 in-app `Debug` chip，用 `adb logcat -s HyPainterInput` 对比缺失笔画开头的 action/tool/pointers/samples/history。
- 回滚方式：执行 `git revert <本轮提交哈希>`；如未提交，还原 `CanvasInputRouter.kt`、`docs/android-studio-debugging.md` 与 progress 本轮修改。

## 2026-07-05 - Task: 增强首段笔画缺失调试字段

### What was done
- `CanvasDebugState` 和 `HyPainterInput` Logcat 增加 selected `pointerId`、active stylus pointer id、`historySize`、`eventTime`、`downTime` 和 `recoveredStylusPointer`。
- Debug overlay 同步显示 pointer、history、event/down time 和 recovery 状态，便于在真机上短时间打开 overlay 观察首段缺失。
- `CanvasInputRouter` 在 MOVE 中恢复 stylus pointer 时标记 `recoveredStylusPointer=true`，用于区分正常 DOWN 起笔和 MOVE 恢复起笔。

### Testing
- `.\gradlew.bat :android:app:testDebugUnitTest`：通过。
- `.\gradlew.bat :android:app:assembleDebug --stacktrace`：通过。

### Notes
- 用户提供的 MIUIInput 日志只能证明系统窗口收到 DOWN/UP/MOVE 摘要，不能证明 tool type、HyPainter route、历史样本和 canvas 映射；后续首段缺失需抓 `HyPainterInput`。
- 回滚方式：执行 `git revert <本轮提交哈希>`；如未提交，还原 `debug/*`、`CanvasInputRouter.kt` 和 progress 本轮修改。

## 2026-07-05 - Task: 降低抬笔提交路径阻塞

### What was done
- 分析真机 `HyPainterInput` 日志，确认典型笔画的 MOVE 处理约 0.6-0.7ms，但 `ACTION_UP` 达到 57ms，主要风险集中在抬笔提交。
- `StrokeRasterCache` 增加 active cache 合并能力，提交时可直接把活动笔画 bitmap 合并到 committed display cache，避免在 UI 线程重新遍历整条 stroke 绘制。
- Kotlin fallback 和 Native engine 抬笔时优先合并 active cache，只有缺少 active cache 时才回退 vector stroke replay。
- Native engine 不再在 `endStroke()` 同步调用 Rust `nativeAppendStroke()` rasterize；Rust 文档改为导出/分享需要 bitmap 时按 committed history 重建，避免输入路径被 native rasterization 阻塞。
- Debug action 名称补充 `hover-enter`、`hover-move`、`hover-exit`，避免真机日志出现 `action-9/7/10` 难以判读。

### Testing
- `.\gradlew.bat :android:app:testDebugUnitTest`：通过。
- `.\gradlew.bat :android:app:assembleDebug --stacktrace`：通过。

### Notes
- 这次针对用户日志中的 `up handleMs=57.25`，目标是让抬笔不再阻塞后续 hover/下一笔输入。
- 导出/分享仍可能触发 Rust 文档重建和 PNG 压缩，但这些操作已经在 toolbar IO 路径中，不应参与 stylus MOVE/UP 热路径。
- 回滚方式：执行 `git revert <本轮提交哈希>`；如未提交，还原 `StrokeRasterCache.kt`、engine、debug format 和 progress 本轮修改。

## 2026-07-05 - Task: 笔接近工具栏时临时隐藏 UI

### What was done
- 顶部工具栏记录自身 root bounds，Canvas 收到 stylus/eraser pointer 且命中该区域时会临时隐藏工具栏约 1.2 秒，并消费这次 UI 命中，避免误画或按钮抢事件。
- 顶部工具栏自身也会在收到 stylus/eraser hover/down 时立即隐藏；finger touch 仍可正常点击和横向滚动工具栏。
- Debug chip 从顶部工具栏移到左下角，降低它对顶部绘制路径的干扰，同时保留调试入口。

### Testing
- `.\gradlew.bat :android:app:testDebugUnitTest`：通过。
- `.\gradlew.bat :android:app:assembleDebug --stacktrace`：通过。

### Notes
- 这次是针对“笔碰到 UI 时让它暂时隐藏”的交互修复；当前是 MVP 级自动让路，后续正式 UI 应改成工具面板、浮动 palette 与 palm/stylus-aware hit test 体系。
- 回滚方式：执行 `git revert <本轮提交哈希>`；如未提交，还原 `MainActivity.kt` 和 progress 本轮修改。

## 2026-07-05 - Task: Debug chip 遵守 stylus UI 避让

### What was done
- 将 stylus UI 避让状态从 toolbar 专用语义调整为 controls 语义，顶部工具栏和左下角 Debug chip 共用隐藏状态。
- Debug chip 收到 stylus/eraser pointer 时会消费事件并临时隐藏控制 UI，不再被笔误触切换调试开关。
- Finger touch 仍可正常点击 Debug chip 打开或关闭调试叠层。

### Testing
- `.\gradlew.bat :android:app:testDebugUnitTest`：通过。
- `.\gradlew.bat :android:app:assembleDebug --stacktrace`：通过。

### Notes
- 这次补齐了“笔碰 UI 自动让路”的底部调试入口边界，避免把 Debug chip 从顶部移到底部后形成新的 stylus 误触点。
- 回滚方式：执行 `git revert <本轮提交哈希>`；如未提交，还原 `MainActivity.kt` 和 progress 本轮修改。

## 2026-07-05 - Task: 调整 stylus UI 避让为 hover 恢复

### What was done
- 将 controls hiding 状态抽到 `StylusControlsHider`，规则改为“stylus/eraser 按压进入 UI 区隐藏，hover 或离开 UI 区立即显示”。
- 顶部工具栏继续在按压命中时隐藏，Canvas 在检测到 stylus hover 或 pointer 离开工具栏 bounds 时立刻恢复 UI，避免笔无法点 UI。
- Debug chip 保持 finger 可点击；stylus 按压命中会隐藏 controls，stylus hover 会恢复 controls。
- Debug 黑色 overlay 从右下角移动到左下角；Debug chip 移到右上角，避免与 overlay 重叠。
- 新增 `StylusControlsHiderTest`，覆盖隐藏直到 hover、hover 立即显示、离开 UI 使用同一即时显示路径。

### Testing
- `.\gradlew.bat :android:app:testDebugUnitTest`：通过。
- `.\gradlew.bat :android:app:assembleDebug --stacktrace`：通过。

### Notes
- 这次修正了上一轮定时恢复模型：UI 不再靠固定 1.2 秒恢复，而是由 stylus hover/离开 UI 这两个输入语义驱动，更符合板绘应用里“笔靠近工具才参与 UI”的预期。
- 回滚方式：执行 `git revert <本轮提交哈希>`；如未提交，删除 `StylusControlsHider.kt`、`StylusControlsHiderTest.kt`，并还原 `MainActivity.kt` 和 progress 本轮修改。

## 2026-07-05 - Task: 允许 hover 后用笔点击 UI

### What was done
- `StylusControlsHider` 增加 hover-armed 状态：stylus hover 到 controls 上会显示 UI 并允许随后的 stylus press 交给 UI 控件处理。
- 未经过 hover 的 stylus press 进入 controls 仍会隐藏 UI，用于区分“从画布按压扫进 UI”和“悬停后主动点 UI”。
- Stylus 离开 controls 区域会立即显示 UI 并清除 hover armed 状态。
- Canvas、顶部工具栏和右上 Debug chip 都接入同一规则；Debug overlay 保持左下角。
- 扩展 `StylusControlsHiderTest`，覆盖 hover 后按压允许、未 hover 按压隐藏、离开 UI 后重新要求 hover。

### Testing
- `.\gradlew.bat :android:app:testDebugUnitTest`：通过。
- `.\gradlew.bat :android:app:assembleDebug --stacktrace`：通过。

### Notes
- 这次修复上一轮的关键边界：如果笔已经 hover 在 UI 上，再按下应该是明确的 UI 操作，而不是继续隐藏控件。
- 回滚方式：执行 `git revert <本轮提交哈希>`；如未提交，还原 `StylusControlsHider.kt`、`StylusControlsHiderTest.kt`、`MainActivity.kt` 和 progress 本轮修改。

## 2026-07-05 - Task: 修复 stylus MOVE 恢复时未显式开笔

### What was done
- 修复 `CanvasInputRouter` 的 stylus MOVE 恢复路径：当 `ACTION_DOWN` 被 UI 避让或系统边界吞掉、后续 MOVE 才恢复 stylus pointer 时，现在会先用最早历史采样显式 `beginStroke`，再追加剩余历史点和当前点。
- 无历史采样的恢复 MOVE 会以当前点开 stroke 并立即刷新画布，避免只有输入 session、没有 active stroke 的半恢复状态。
- Debug overlay 的 active pointer 查询改为无副作用路径，避免仅打开 debug 时因为查询缺失 pointer 而清掉输入 session。
- 修正恢复路径中的 debug sample 计数，避免首个历史点既作为 begin 又被重复计入。

### Testing
- `.\gradlew.bat :android:app:testDebugUnitTest`：通过。

### Notes
- 这个漏洞会在“按压扫进/扫出 UI、DOWN 被消费，但 MOVE 继续到画布”的场景里放大，表现可能是首段笔画丢失或恢复后输入状态与 engine stroke 状态不一致。
- 当前项目没有 Robolectric，未新增 `MotionEvent` 级路由单测；后续如果要继续压实输入链路，建议加一个轻量 Android/Robolectric 测试层专门覆盖 ACTION_DOWN/MOVE/UP/CANCEL 序列。
- 回滚方式：执行 `git revert <本轮提交哈希>`；如未提交，还原 `CanvasInputRouter.kt` 和 progress 本轮修改。

## 2026-07-05 - Task: 阻止 stylus 后残留双指触摸接管画布

### What was done
- `CanvasInputSession.updateTwoFingerTouch` 现在要求已有有效 finger stream；没有 fresh `ACTION_DOWN -> beginFingerStream` 的双指 MOVE/POINTER_DOWN 不再启动平移/缩放/旋转。
- 修正双指旋转单测，让它显式模拟真实输入顺序：先开始 finger stream，再建立双指 gesture frame。
- 新增测试覆盖 stray two-finger touch 被忽略，以及 stylus 抢占并释放后，残留手指 MOVE 不会直接接管画布。

### Testing
- `.\gradlew.bat :android:app:testDebugUnitTest`：通过。

### Notes
- 这个修复面向“笔最高优先级”的底层边界：stylus drawing 期间被清掉的手指流，不能在 stylus 抬起后不经新的 down 事件自动恢复成画布变换。
- 回滚方式：执行 `git revert <本轮提交哈希>`；如未提交，还原 `CanvasInputSession.kt`、`CanvasInputSessionTest.kt` 和 progress 本轮修改。

## 2026-07-05 - Task: 总结当前架构职责与 MVP 进展

### What was done
- 新增 `docs/current-architecture.md`，总结当前 Android、engine、input、debug、render 和 Rust crate 的职责边界。
- 文档加入结构图与运行时数据流图，明确 `PaintingEngine` 应作为高内聚 facade，UI/input/debug 不应泄漏或改变 engine 内部一致性。
- 区分当前 MVP 临时实现与长期目标：Android 仍承担部分文档语义，Rust 已有 brush/tile/ffi 雏形但还需要继续下沉文档、图层、撤销和文件格式。
- 更新 `docs/README.md`，把当前架构文档、MVP 状态、设备输入测试和 Android Studio 调试文档纳入索引。

### Testing
- 文档改动，无编译测试。

### Notes
- 该文档用于后续重构对照：新增功能时优先确认它应该进入 UI、input、viewport、engine、debug 还是 Rust core。
- 回滚方式：执行 `git revert <本轮提交哈希>`；如未提交，删除 `docs/current-architecture.md` 并还原 `docs/README.md`、`progress.md`。

## 2026-07-05 - Task: UI 避让改回 hover 后再显示

### What was done
- `StylusControlsHider` 移除离开 controls 即显示的路径，隐藏后的 controls 只通过 stylus hover 恢复显示。
- Canvas 层不再在 stylus press/move 离开 toolbar bounds 时自动显示 UI，避免按压绘制过程中 UI 闪回。
- Debug chip 移回顶部工具栏 Row 内，跟其他 UI 一起隐藏/显示；debug overlay 仍保持左下角不变。
- 更新 `StylusControlsHiderTest`，覆盖 hide 后保持隐藏直到 hover，以及 hover 后允许 stylus press 交给 UI。

### Testing
- `.\gradlew.bat :android:app:testDebugUnitTest`：通过。

### Notes
- 当前语义：按压进入 UI 区域会隐藏 controls；隐藏后必须出现 stylus hover 才显示 controls；显示后 hover armed，笔可点击 UI。
- 回滚方式：执行 `git revert <本轮提交哈希>`；如未提交，还原 `MainActivity.kt`、`StylusControlsHider.kt`、`StylusControlsHiderTest.kt` 和 progress 本轮修改。

## 2026-07-05 - Task: 降低实时绘制与 debug 高频分配

### What was done
- `KotlinPaintingEngine.canvasSnapshot()` 在已有 active bitmap preview 的实时绘制路径中不再额外包装 `activeStroke`，避免每帧为 fallback 绘制创建无用 `EngineStroke` 对象。
- `NativePaintingEngine.canvasSnapshot()` 通过 fallback preview 同步受益，实时 native 预览也不再携带这份无用 active stroke 包装。
- `CanvasInputRouter` 对 debug MOVE 发布做节流：非 MOVE 仍即时发布，MOVE 按 `DEBUG_LOG_INTERVAL_MS` 发布 overlay 状态和 Logcat，避免 debug 打开时每个输入 MOVE 都触发 heap 统计、debug state 分配和 Compose overlay 重组。
- 移除未使用的 `lastDebugState` 与独立 log 节流字段，debug overlay 和 Logcat 使用同一低频发布节奏。

### Testing
- `.\gradlew.bat :android:app:testDebugUnitTest`：通过。

### Notes
- 正常绘制仍由 committed bitmap + active bitmap preview 渲染，功能输出不变；这次只减少实时路径里不必要的对象流。
- 回滚方式：执行 `git revert <本轮提交哈希>`；如未提交，还原 `KotlinPaintingEngine.kt`、`CanvasInputRouter.kt` 和 progress 本轮修改。

## 2026-07-05 - Task: 移除 native 实时预览 snapshot 桥接

### What was done
- `KotlinPaintingEngine` 暴露内部轻量 `activePreviewImage`，直接返回 active raster cache 的 `ImageBitmap` 引用。
- `NativePaintingEngine.canvasSnapshot()` 不再调用 `fallbackPreview.canvasSnapshot()`，避免 native 实时绘制路径每帧为取 active preview 构造一份 fallback `EngineSnapshot`。
- Native 实时 canvas snapshot 继续返回 committed display cache 与 active preview bitmap，功能输出保持一致。

### Testing
- `.\gradlew.bat :android:app:testDebugUnitTest`：通过。

### Notes
- 完整 `snapshot()` 仍保留 fallback preview 的 active stroke 信息，用于低频工具栏/保存等模型路径；本轮只收紧高频 canvas 绘制路径。
- 回滚方式：执行 `git revert <本轮提交哈希>`；如未提交，还原 `KotlinPaintingEngine.kt`、`NativePaintingEngine.kt` 和 progress 本轮修改。

## 2026-07-05 - Task: 恢复手指点击 UI 并稳定双指 pointer 跟踪

### What was done
- Canvas input 在 finger 事件落入 toolbar bounds 时直接放行，不再把 UI 区域内的手指事件送入 canvas input router。
- 移除 toolbar Row 上包住所有 chip 的 `pointerInteropFilter`，避免 stylus 避让监听干扰 Material chip 的 finger 点击链路。
- `CanvasInputSession` 为 finger stream 记录 first/second pointer id；双指 MOVE 根据 pointer id 用 `findPointerIndex` 取当前 index，避免 Android pointer index 重排导致旋转/缩放跳变。
- `CanvasInputRouter` 拆分 `ACTION_POINTER_DOWN`、`ACTION_MOVE`、`ACTION_POINTER_UP` 处理，双指 transform 只用 session 追踪的两根手指。
- 扩展 `CanvasInputSessionTest`，覆盖 tracked pointer 抬起后双指 transform 停止，直到新的 second finger down 建立新基准。

### Testing
- `.\gradlew.bat :android:app:testDebugUnitTest`：通过。
- `.\gradlew.bat :android:app:assembleDebug --stacktrace`：通过。

### Notes
- Android 官方 MotionEvent 文档说明 pointer index 的顺序可能随事件变化，pointer id 才在同一手势内保持稳定；因此双指手势也应像 stylus 一样基于 id 追踪。
- 回滚方式：执行 `git revert <本轮提交哈希>`；如未提交，还原 `MainActivity.kt`、`CanvasInputRouter.kt`、`CanvasInputSession.kt`、`CanvasInputSessionTest.kt` 和 progress 本轮修改。

## 2026-07-05 - Task: 恢复 stylus UI 避让且不阻断 finger 点击

### What was done
- Toolbar 容器改用 Compose `pointerInput` 做 stylus-only 避让监听：只有 stylus/eraser 新按下且未 hover armed 时才 consume 并隐藏 controls。
- Finger touch 在 toolbar 内不被 stylus 避让监听消费，继续交给 Material `AssistChip` 处理点击。
- 保留 canvas 层对 finger-in-toolbar 的放行逻辑，避免 UI 区域手指事件进入 canvas input router。
- `KotlinPaintingEngine` 增加 `drainActiveStroke`，`NativePaintingEngine.endStroke()` 不再通过完整 `fallbackPreview.snapshot()` 取 active stroke，减少频繁短笔画抬笔时的对象构造。

### Testing
- `.\gradlew.bat :android:app:testDebugUnitTest`：通过。
- `.\gradlew.bat :android:app:assembleDebug --stacktrace`：通过。

### Notes
- 当前预期：finger 可点 UI；未 hover 的 stylus press 进入 UI 会隐藏 controls；hover 后 stylus press 可交给 UI；debug overlay 位置不变。
- 回滚方式：执行 `git revert <本轮提交哈希>`；如未提交，还原 `MainActivity.kt`、`KotlinPaintingEngine.kt`、`NativePaintingEngine.kt` 和 progress 本轮修改。

## 2026-07-05 - Task: 修复抬笔后笔画未提交到画布

### What was done
- 修复 `KotlinPaintingEngine.drainActiveStroke` 清理顺序：现在只取出 active stroke，不提前清空 active raster cache。
- `KotlinPaintingEngine.endStroke` 在 committed cache merge 完成后再清理 active preview，避免“预览正常、抬笔后消失/画不上去”。
- Toolbar stylus-only pointerInput 增加 hover arming：stylus/eraser hover 到 toolbar 区域时立即 `showForHover`，hover 后按 UI 不再触发隐藏。

### Testing
- `.\gradlew.bat :android:app:testDebugUnitTest`：通过。
- `.\gradlew.bat :android:app:assembleDebug --stacktrace`：通过。

### Notes
- 根因是上一轮轻量 `drainActiveStroke` 优化把 active cache 也清掉了，而 committed bitmap cache 的合并正依赖这份 active cache。
- 当前预期：笔画 preview 抬笔后应留在 committed canvas；未 hover stylus press UI 隐藏；hover 后 stylus press UI 可点击；finger UI 点击仍不被消费。
- 回滚方式：执行 `git revert <本轮提交哈希>`；如未提交，还原 `MainActivity.kt`、`KotlinPaintingEngine.kt` 和 progress 本轮修改。

## 2026-07-05 - Task: 恢复 hover 退出后的 stylus UI 隐藏判定

### What was done
- `StylusControlsHider` 增加 `showAfterHoverExit()`：hover 离开时保持 controls 可见，但解除 hover armed 状态。
- Canvas MotionEvent 路径区分 stylus hover enter/move 与 hover exit；exit 后下一次未 hover 的 stylus 直按 UI 会重新触发隐藏。
- Toolbar Compose `pointerInput` 同步使用 `PointerEventType.Exit` 解除 hover armed，保留 hover 后按 UI 可点击的能力。
- 新增 `StylusControlsHiderTest.pressInControlsAfterHoverExitHidesAgain`，锁住 hover 退出后的状态机语义。
- 新增真机 instrumented test `PaintingEngineRasterCacheInstrumentedTest`，验证 `KotlinPaintingEngine.endStroke()` 会把 active preview 提交到 committed cache，并清空 active preview。

### Testing
- `.\gradlew.bat :android:app:testDebugUnitTest`：通过。
- `.\gradlew.bat :android:app:assembleDebug --stacktrace`：通过。
- `.\gradlew.bat :android:app:assembleDebugAndroidTest --stacktrace`：通过。
- `.\gradlew.bat :android:app:connectedDebugAndroidTest`：在设备 `21051182C - 16` 上通过 1 条 instrumented test。
- `.\gradlew.bat :android:app:installDebug`：已安装到在线设备。

### Notes
- 当前预期：hover 到 UI 后可以用笔点击 UI；hover 离开或 hover exit 后 UI 仍显示，但下一次无 hover 直接用笔碰 UI 会隐藏；finger 点击 UI 不受影响。
- 回滚方式：执行 `git revert <本轮提交哈希>`；如未提交，还原 `MainActivity.kt`、`StylusControlsHider.kt`、`StylusControlsHiderTest.kt`、`PaintingEngineRasterCacheInstrumentedTest.kt` 和 progress 本轮修改。

## 2026-07-05 - Task: 建立应用命令栏与画布创建设置节点

### What was done
- 将命令 UI 从画布上方悬浮层改为固定应用命令栏，画布区域只负责绘制和输入，暂时停止继续投入 stylus-hide UI feature。
- 新增 `File` 菜单：New Canvas、Save Draft、Load Draft、Export PNG、Share PNG。
- 新增 `Canvas` 菜单：Canvas Settings、Reset View。
- 新增 `View` 菜单：Pixel Perfect/Nearest/Linear/Bilinear/Bicubic 采样选择，以及 debug overlay 开关。
- 新增 `NewCanvasDialog`，支持命名、宽高输入、1024/2048/4K 快速预设，并在创建后重建 `PaintingEngine`、重置 viewport/input router。
- 新增 `CanvasSettingsDialog`，展示当前画布大小并允许切换 bitmap sampling。
- 快速工具区收敛为绘画命令：Clear、Undo、压力/引擎状态、颜色、笔刷大小、图层添加/选择/显隐。
- `docs/current-architecture.md` 同步记录 app shell 当前职责和后续拆分方向。

### Testing
- `.\gradlew.bat :android:app:testDebugUnitTest`：通过。
- `.\gradlew.bat :android:app:assembleDebug --stacktrace`：通过。
- `.\gradlew.bat :android:app:assembleDebugAndroidTest --stacktrace`：通过。
- `.\gradlew.bat :android:app:installDebug`：已安装到设备 `21051182C - 16`。
- `adb shell monkey -p io.github.lukasvi.hypainter -c android.intent.category.LAUNCHER 1`：可启动；`ps` 看到进程 `io.github.lukasvi.hypainter`。

### Notes
- 这是完整系统骨架的第一个节点，不追求最终 UI 视觉，只先建立文件/画布/视图命令入口和职责分离。
- Draft load 通过 `ProjectCodec` 先读取画布尺寸，再创建匹配尺寸的 engine 后加载，避免旧固定尺寸 engine 承载不同尺寸项目。
- 回滚方式：执行 `git revert <本轮提交哈希>`；如未提交，还原 `MainActivity.kt`、`docs/current-architecture.md` 和 progress 本轮修改。

## 2026-07-05 - Task: 接入系统壁纸动态 UI 主题色

### What was done
- 新增 Compose 主题入口，Android 12+ 默认使用 Material You 动态色，从系统壁纸生成 UI 色板。
- 在 `View` 菜单加入 `Wallpaper Colors` 开关；旧系统显示为不可用项，关闭后回落到固定 HyPainter 明暗主题。
- 将应用壳层背景和顶部命令栏接到 `MaterialTheme.colorScheme`，但画布像素、笔刷颜色、导出内容不受 UI 主题影响。
- `docs/current-architecture.md` 同步记录主题模块职责与边界。

### Testing
- `.\gradlew.bat :android:app:assembleDebug --stacktrace`：通过。
- `.\gradlew.bat :android:app:testDebugUnitTest`：通过。

### Notes
- 改动文件：`android/app/src/main/java/io/github/lukasvi/hypainter/ui/HyPainterTheme.kt` 新增动态色主题入口；`android/app/src/main/java/io/github/lukasvi/hypainter/MainActivity.kt` 接入主题开关和主题色；`docs/current-architecture.md` 记录主题边界；`progress.md` 追加本轮记录。
- 回滚方式：执行 `git revert <本轮提交哈希>`；如未提交，还原上述四个文件或删除新增 `ui/HyPainterTheme.kt`。
- 设备安装补充：`.\gradlew.bat :android:app:installDebug` 因 `No connected devices!` 未完成，debug APK 已构建但未安装到平板。

## 2026-07-05 - Task: 按草图改为画布浮动 HUD UI

### What was done
- 移除固定顶部命令栏，画布改为全屏铺底，常用命令通过浮动 HUD 覆盖在画布之上。
- 新增左侧快捷 HUD：Undo、Clear、Brush、常用颜色、颜色入口和笔刷大小滑条。
- 新增右上浮动工具胶囊：保留 File/Canvas/View 菜单，并提供 Brush、Color、Layers 面板入口。
- 新增右侧浮动 inspector 面板：Brush 调整、Layers 添加/选择/显隐、Color 色板选择。
- 新增底部半透明状态 chip，显示文档、引擎、采样、压力和保存/导出状态。
- 清理被新 HUD 取代的旧固定命令栏和横向快速工具条孤儿代码。
- `docs/current-architecture.md` 同步更新当前 UI 壳层职责。

### Testing
- `.\gradlew.bat :android:app:assembleDebug --stacktrace`：通过。
- `.\gradlew.bat :android:app:testDebugUnitTest`：通过。

### Notes
- 当前没有实现 Redo，因为 `PaintingEngine` 目前只暴露 `undo()`；不做假按钮，后续应先补引擎 redo 协议再接 UI。
- 改动文件：`android/app/src/main/java/io/github/lukasvi/hypainter/MainActivity.kt` 改为浮动 HUD 与 inspector；`docs/current-architecture.md` 更新 HUD 架构说明；`progress.md` 追加本轮记录。
- 回滚方式：执行 `git revert <本轮提交哈希>`；如未提交，还原上述三个文件。

## 2026-07-05 - Task: 拆分 HUD 组件并按草图改为图标工具栏

### What was done
- 将 HUD 与 inspector 组件从 `MainActivity.kt` 拆到 `ui/HudComponents.kt`，`MainActivity.kt` 从 1247 行降到 667 行。
- 新增 `ui/LucideIcons.kt`，提供本地 Lucide 风格图标子集，避免第三方图标库或设计工具继续阻塞 HUD 迭代。
- 左 HUD 按草图顺序调整为：笔刷库、快捷笔刷 1/2/3 button group、透明度条、大小条。
- 右上浮动 toolbar 按草图顺序调整为：菜单、选区、变换、工具、颜色、图层。
- 未完成的选区、变换、工具和透明度协议均保留可见占位；选区/变换通过状态 chip 给反馈，透明度暂存 UI 状态并加注释说明尚未写入引擎。
- 菜单内容迁移到浮动 inspector 的 `Menu` 面板，保留新建画布、保存/加载、导出/分享、画布设置、视图采样、壁纸色和 debug overlay 入口。
- `docs/current-architecture.md` 同步记录 HUD 组件拆分和图标边界。

### Testing
- `.\gradlew.bat :android:app:testDebugUnitTest`：通过。
- `.\gradlew.bat :android:app:assembleDebug --stacktrace`：通过。
- `cargo test -p hyp_compositor`：通过。

### Notes
- 改动文件：`android/app/src/main/java/io/github/lukasvi/hypainter/MainActivity.kt` 精简为状态/引擎/文件 IO 接线；`android/app/src/main/java/io/github/lukasvi/hypainter/ui/HudComponents.kt` 新增 HUD、toolbar、inspector 和菜单组件；`android/app/src/main/java/io/github/lukasvi/hypainter/ui/LucideIcons.kt` 新增本地图标子集；`rust/crates/HyP_compositor/src/lib.rs` 补充公开合成语义注释；`docs/current-architecture.md` 更新架构说明；`progress.md` 追加本轮记录。
- 回滚方式：执行 `git revert <本轮提交哈希>`；如未提交，还原上述六个文件并删除新增 `HudComponents.kt`、`LucideIcons.kt`。
