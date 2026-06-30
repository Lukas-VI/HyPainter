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
