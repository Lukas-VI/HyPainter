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

The next step is rendering the native RGBA result back into the Compose canvas instead of keeping the Kotlin preview as the visible source of truth.
