# Rust Workspace

Placeholder for the Rust painting core workspace.

The Rust side owns the platform-independent drawing engine: document model, brush engine, tile storage, compositing, file format, export, and FFI boundary.

Current implemented core:

- `hyp_core`: shared canvas, color, layer, brush, and stylus sample types.
- `hyp_tiles`: tile-backed raster storage and source-over pixel blending.
- `hyp_brush`: pressure-sensitive stroke rasterization into tiles.
- `hyp_compositor`: visible layer compositing into RGBA pixels.
- `hyp_export`: dependency-free PNG export and raw RGBA export helpers.

Verify with:

```powershell
cargo fmt --all -- --check
cargo test
```
