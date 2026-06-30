# HyPainter

HyPainter is an open-source Android tablet painting application foundation.

The project aims to build a modern stylus-first drawing experience with Jetpack Compose on Android and a Rust-powered painting core.

## Project Layout

- `android/app`: Android application shell and Compose canvas UI.
- `android/core-ffi`: Kotlin-facing bindings for the Rust core.
- `android/design-system`: Shared Compose theme, icons, and reusable controls.
- `rust/crates/HyP_core`: Document model, commands, and shared types.
- `rust/crates/HyP_brush`: Brush engine, stroke sampling, and brush dynamics.
- `rust/crates/HyP_tiles`: Tile storage, dirty-region tracking, and cache planning.
- `rust/crates/HyP_compositor`: Layer compositing and blend-mode planning.
- `rust/crates/HyP_format`: Native project file format planning.
- `rust/crates/HyP_export`: Export pipeline planning.
- `rust/crates/HyP_ffi`: Stable FFI boundary for Android.
- `docs`: Product and architecture design documents.

## Current Stage

The repository is moving from foundation into M0. It now has a runnable Android Compose canvas shell and a tested Rust raster core for stylus samples, brush dabs, tile storage, layer compositing, and PNG export.

## Build

Android debug build:

```powershell
.\gradlew.bat :android:app:assembleDebug
```

If the Rust Android target is missing, install it first:

```powershell
rustup target add aarch64-linux-android
```

Rust core verification:

```powershell
cd rust
cargo fmt --all -- --check
cargo test
```

## MVP Direction

The next MVP work is to connect the Android stylus sample stream to the Rust core through the FFI boundary, then add project save/load, layer UI, brush controls, color controls, and export from the Android app.

Current bridge status:

- Android UI now talks to a `PaintingEngine` abstraction instead of owning strokes directly.
- `KotlinPaintingEngine` keeps the debug app usable while native packaging is not wired.
- `NativePaintingEngine` defines the Android native method boundary for `hyp_ffi`.
- `hyp_ffi` exposes the first C ABI/JNI entry points for document lifecycle, clear, stroke submission, and RGBA rendering.
- Gradle builds `hyp_ffi` for `arm64-v8a` and packages `libhyp_ffi.so` into the debug APK when the Rust Android target is installed.
