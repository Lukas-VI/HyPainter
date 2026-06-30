use std::{ptr, slice};

use hyp_brush::Stroke;
use hyp_compositor::RasterLayer;
use hyp_core::{BrushSettings, CanvasSize, Rgba8, StylusSample};
use hyp_export::export_rgba_bytes;

type JBoolean = u8;
type JInt = i32;
type JLong = i64;

#[repr(C)]
#[derive(Clone, Copy, Debug)]
pub struct HyPaintSample {
    pub x: f32,
    pub y: f32,
    pub pressure: f32,
    pub tilt_x: f32,
    pub tilt_y: f32,
    pub timestamp_ms: u64,
}

#[repr(C)]
#[derive(Clone, Copy, Debug)]
pub struct HyPaintBuffer {
    pub ptr: *mut u8,
    pub len: usize,
}

#[derive(Debug)]
pub struct HyPaintDocument {
    canvas_size: CanvasSize,
    layers: Vec<RasterLayer>,
    brush: BrushSettings,
}

impl HyPaintDocument {
    pub fn new(width: u32, height: u32) -> Self {
        let canvas_size = CanvasSize::new(width, height);
        Self {
            canvas_size,
            layers: vec![RasterLayer::new(1, "Ink", canvas_size)],
            brush: BrushSettings::ink(8.0, Rgba8::BLACK),
        }
    }

    pub fn append_stroke(&mut self, samples: &[HyPaintSample]) {
        if samples.is_empty() {
            return;
        }

        let mut stroke = Stroke::new();
        stroke.append(samples.iter().map(|sample| StylusSample {
            position: hyp_core::Point::new(sample.x, sample.y),
            pressure: sample.pressure,
            tilt_x: sample.tilt_x,
            tilt_y: sample.tilt_y,
            timestamp_ms: sample.timestamp_ms,
        }));

        if let Some(layer) = self.layers.last_mut() {
            stroke.rasterize(&mut layer.tiles, self.brush);
        }
    }

    pub fn clear(&mut self) {
        self.layers = vec![RasterLayer::new(1, "Ink", self.canvas_size)];
    }

    pub fn render_rgba(&self) -> Vec<u8> {
        export_rgba_bytes(self.canvas_size, &self.layers)
    }
}

#[no_mangle]
pub extern "C" fn hyp_document_create(width: u32, height: u32) -> *mut HyPaintDocument {
    if width == 0 || height == 0 {
        return ptr::null_mut();
    }

    Box::into_raw(Box::new(HyPaintDocument::new(width, height)))
}

#[no_mangle]
pub unsafe extern "C" fn hyp_document_free(document: *mut HyPaintDocument) {
    if !document.is_null() {
        drop(Box::from_raw(document));
    }
}

#[no_mangle]
pub unsafe extern "C" fn hyp_document_append_stroke(
    document: *mut HyPaintDocument,
    samples: *const HyPaintSample,
    sample_count: usize,
) -> bool {
    let Some(document) = document.as_mut() else {
        return false;
    };

    if samples.is_null() || sample_count == 0 {
        return false;
    }

    let samples = slice::from_raw_parts(samples, sample_count);
    document.append_stroke(samples);
    true
}

#[no_mangle]
pub unsafe extern "C" fn hyp_document_clear(document: *mut HyPaintDocument) -> bool {
    let Some(document) = document.as_mut() else {
        return false;
    };

    document.clear();
    true
}

#[no_mangle]
pub unsafe extern "C" fn hyp_document_render_rgba(
    document: *const HyPaintDocument,
) -> HyPaintBuffer {
    let Some(document) = document.as_ref() else {
        return HyPaintBuffer {
            ptr: ptr::null_mut(),
            len: 0,
        };
    };

    let mut bytes = document.render_rgba();
    let buffer = HyPaintBuffer {
        ptr: bytes.as_mut_ptr(),
        len: bytes.len(),
    };
    std::mem::forget(bytes);
    buffer
}

#[no_mangle]
pub unsafe extern "C" fn hyp_buffer_free(buffer: HyPaintBuffer) {
    if !buffer.ptr.is_null() && buffer.len > 0 {
        drop(Vec::from_raw_parts(buffer.ptr, buffer.len, buffer.len));
    }
}

#[no_mangle]
pub extern "system" fn Java_io_github_lukasvi_hypainter_engine_NativePaintingEngine_nativeCreate(
    _env: *mut std::ffi::c_void,
    _class: *mut std::ffi::c_void,
    width: JInt,
    height: JInt,
) -> JLong {
    if width <= 0 || height <= 0 {
        return 0;
    }

    hyp_document_create(width as u32, height as u32) as JLong
}

#[no_mangle]
pub unsafe extern "system" fn Java_io_github_lukasvi_hypainter_engine_NativePaintingEngine_nativeDestroy(
    _env: *mut std::ffi::c_void,
    _class: *mut std::ffi::c_void,
    handle: JLong,
) {
    hyp_document_free(handle as *mut HyPaintDocument);
}

#[no_mangle]
pub unsafe extern "system" fn Java_io_github_lukasvi_hypainter_engine_NativePaintingEngine_nativeClear(
    _env: *mut std::ffi::c_void,
    _class: *mut std::ffi::c_void,
    handle: JLong,
) -> JBoolean {
    hyp_document_clear(handle as *mut HyPaintDocument) as JBoolean
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn ffi_document_renders_stroke_pixels() {
        let mut document = HyPaintDocument::new(32, 32);

        document.append_stroke(&[
            HyPaintSample {
                x: 4.0,
                y: 8.0,
                pressure: 1.0,
                tilt_x: 0.0,
                tilt_y: 0.0,
                timestamp_ms: 0,
            },
            HyPaintSample {
                x: 20.0,
                y: 8.0,
                pressure: 1.0,
                tilt_x: 0.0,
                tilt_y: 0.0,
                timestamp_ms: 16,
            },
        ]);

        let pixels = document.render_rgba();

        assert_eq!(pixels.len(), 32 * 32 * 4);
        assert!(pixels.iter().any(|value| *value != 0));
    }

    #[test]
    fn ffi_create_rejects_empty_canvas() {
        let document = hyp_document_create(0, 32);

        assert!(document.is_null());
    }
}
