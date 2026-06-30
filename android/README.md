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

The next step is adding the remaining MVP product controls: visible layer UI, brush size/color controls, project save/load, and export from the Android app.
