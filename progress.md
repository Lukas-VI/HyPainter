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
