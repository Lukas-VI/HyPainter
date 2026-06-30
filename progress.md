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
