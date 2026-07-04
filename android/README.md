# Android Workspace

This directory contains the Android-facing application code.

- `app`: Compose application shell and canvas host.
- `core-ffi`: Kotlin boundary around the Rust core.
- `design-system`: Shared visual language and reusable UI controls.

Current app status:

- Builds as a debug Android application.
- Shows a full-screen Compose painting surface.
- Captures stylus and eraser `MotionEvent` input with pressure and historical samples.
- Uses a layered canvas input router: stylus input has priority, single-finger touch is reserved for UI/future selection, and two-finger touch transforms the canvas.
- Routes canvas actions through a `PaintingEngine` abstraction.
- Supports a Kotlin fallback engine for stroke preview, clear, undo, pan, zoom, and rotation.
- Defines a `NativePaintingEngine` bridge for future `hyp_ffi` shared-library packaging.
- Packages `libhyp_ffi.so` for `arm64-v8a` through the Gradle Rust build hook when the Rust Android target is installed.
- Displays the native RGBA render result as a Compose canvas image when `NativePaintingEngine` loads.
- Provides MVP controls for brush color, brush size, clear, undo, pressure readout, and PNG export.
- Saves and loads an app-private draft project file for committed strokes and brush settings.
- Provides MVP layer controls: add layer, select active layer, hide/show layer, and persist layer metadata.
- Maps screen coordinates through viewport pan, zoom, and rotation before samples enter the painting engine.

Debug input on a connected device:

```powershell
adb logcat -s HyPainterInput
```

In debug builds, use the toolbar `Debug` chip to show the canvas input overlay. It reports the current route, action, tool type, pointer count, pressure, sample counts, screen/canvas coordinates, and viewport transform.

The next step is validating the input router on a real stylus tablet and improving the visual layout beyond the current single-row MVP toolbar.
