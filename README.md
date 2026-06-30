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

The repository is at the foundation stage. The first implementation milestone is M0: stylus input on Android, batched FFI into Rust, a basic circular brush, and viewport/export verification.
