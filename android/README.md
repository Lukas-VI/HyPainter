# Android Workspace

This directory contains the Android-facing application code.

- `app`: Compose application shell and canvas host.
- `core-ffi`: Kotlin boundary around the Rust core.
- `design-system`: Shared visual language and reusable UI controls.

Current app status:

- Builds as a debug Android application.
- Shows a full-screen Compose painting surface.
- Captures stylus-like `MotionEvent` input with pressure.
- Supports a temporary in-Compose stroke preview, clear, undo, pan, zoom, and rotation.

The next step is replacing the temporary Compose stroke store with batched calls into `HyP_ffi`.
