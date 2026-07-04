# MVP Status

## Status

HyPainter has a runnable MVP build for Android debug. It is not a polished alpha, but it now covers the core drawing loop:

- stylus-priority input into a Compose canvas;
- two-finger pan, zoom, and rotation with centroid anchoring;
- screen-to-canvas coordinate mapping that respects viewport pan, zoom, and rotation;
- pixel-perfect bitmap sampling by default;
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

Canvas input is handled through a single MotionEvent router on the painting surface. Stylus and eraser pointers are consumed as drawing input and do not participate in viewport movement; two-finger finger touch drives pan, zoom, and rotation around the two-finger centroid; single-finger touch is left unconsumed for UI and future selection tools.

Debug builds include a `Debug` toolbar chip that opens a low-friction input overlay. When that chip is enabled, the same router writes throttled Logcat lines under `HyPainterInput`, which can be watched with `adb logcat -s HyPainterInput` while testing long stylus strokes and two-finger transforms. With the chip disabled, overlay state updates and input logs stay off to avoid diagnostic overhead during normal drawing.

Real-device stylus and touch verification is tracked in `docs/device-input-test-plan.md`.
Android Studio debugging notes are tracked in `docs/android-studio-debugging.md`.

## Remaining Non-MVP Work

- Replace the single-row toolbar with a proper tablet drawing UI.
- Add real file picker/save location instead of fixed app-private project path.
- Promote the draft format to a versioned `.pdraw` container.
- Move layer compositing deeper into Rust core.
- Add tile/dirty-rect incremental refresh for performance.
- Add richer brush library, texture dynamics, eraser mode, opacity, and blend modes.
- Add UI for bitmap sampling modes: nearest, linear, bilinear, and bicubic.
- Add real device testing on a stylus Android tablet.
