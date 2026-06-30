# MVP Status

## Status

HyPainter has a runnable MVP build for Android debug. It is not a polished alpha, but it now covers the core drawing loop:

- stylus/touch input into a Compose canvas;
- pressure-sensitive strokes;
- Rust native engine packaging and rendering;
- visible committed strokes from Rust RGBA output;
- active stroke preview in Kotlin for immediate feedback;
- undo and clear;
- brush color and size controls;
- basic layer add/select/hide/show;
- app-private project save/load;
- PNG export and Android share sheet.

## Verified Commands

```powershell
cd rust
cargo fmt --all -- --check
cargo test
```

```powershell
.\gradlew.bat :android:app:assembleDebug --stacktrace
```

Additional checks:

- APK contains `lib/arm64-v8a/libhyp_ffi.so`.
- APK contains `res/xml/file_paths.xml`.
- merged debug manifest contains `androidx.core.content.FileProvider`.

## Current Implementation Shape

Android owns the MVP product model through `PaintingEngine`. When native loading succeeds, committed strokes are submitted to `hyp_ffi`, rendered as RGBA by Rust, converted to an Android bitmap, and shown in Compose. Kotlin keeps the active-stroke preview responsive.

Layers are implemented as MVP semantic layers in Android. Rust still renders a single raster document; Android rebuilds that document by replaying visible layer strokes when layer visibility changes or a project is loaded.

The draft project format is app-private text. It saves canvas size, layers, active layer, brush settings, and stylus samples. It is intentionally separate from the future formal `.pdraw` container.

## Remaining Non-MVP Work

- Replace the single-row toolbar with a proper tablet drawing UI.
- Add real file picker/save location instead of fixed app-private project path.
- Promote the draft format to a versioned `.pdraw` container.
- Move layer compositing deeper into Rust core.
- Add tile/dirty-rect incremental refresh for performance.
- Add richer brush library, texture dynamics, eraser mode, opacity, and blend modes.
- Add real device testing on a stylus Android tablet.
