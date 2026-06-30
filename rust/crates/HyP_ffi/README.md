# HyP_ffi

Stable FFI boundary placeholder for Android.

This crate should hide internal engine details and expose coarse-grained document, stroke, render, save, and export commands.

Current C ABI surface:

- `hyp_document_create`
- `hyp_document_free`
- `hyp_document_append_stroke`
- `hyp_document_clear`
- `hyp_document_render_rgba`
- `hyp_buffer_free`

The Android side should batch stylus samples before calling into this crate.
