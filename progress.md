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
