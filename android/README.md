# Android Workspace

This directory contains the Android-facing application code.

- `app`: Compose application shell and canvas host.
- `core-ffi`: Kotlin boundary around the Rust core.
- `design-system`: Shared visual language and reusable UI controls.

Current app status:

- Builds as a debug Android application.
- Shows a full-screen Compose painting surface.
- Captures stylus-like `MotionEvent` input with pressure.
- Routes canvas actions through a `PaintingEngine` abstraction.
- Supports a Kotlin fallback engine for stroke preview, clear, undo, pan, zoom, and rotation.
- Defines a `NativePaintingEngine` bridge for future `hyp_ffi` shared-library packaging.
- Packages `libhyp_ffi.so` for `arm64-v8a` through the Gradle Rust build hook when the Rust Android target is installed.
- Displays the native RGBA render result as a Compose canvas image when `NativePaintingEngine` loads.
- Provides MVP controls for brush color, brush size, clear, undo, pressure readout, and PNG export.
- Saves and loads an app-private draft project file for committed strokes and brush settings.

The next step is adding the remaining MVP product controls: visible layer UI and a user-facing export/share destination.
