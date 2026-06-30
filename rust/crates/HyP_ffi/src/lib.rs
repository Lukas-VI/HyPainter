use std::{ptr, slice};

use hyp_brush::Stroke;
use hyp_compositor::RasterLayer;
use hyp_core::{BrushSettings, CanvasSize, Rgba8, StylusSample};
use hyp_export::export_rgba_bytes;

type JBoolean = u8;
type JInt = i32;
type JLong = i64;
type JSize = i32;

#[repr(C)]
struct JNINativeInterface {
    reserved0: *mut std::ffi::c_void,
    reserved1: *mut std::ffi::c_void,
    reserved2: *mut std::ffi::c_void,
    reserved3: *mut std::ffi::c_void,
    get_version: unsafe extern "system" fn(*mut JNIEnv) -> JInt,
    define_class: *mut std::ffi::c_void,
    find_class: *mut std::ffi::c_void,
    from_reflected_method: *mut std::ffi::c_void,
    from_reflected_field: *mut std::ffi::c_void,
    to_reflected_method: *mut std::ffi::c_void,
    get_superclass: *mut std::ffi::c_void,
    is_assignable_from: *mut std::ffi::c_void,
    to_reflected_field: *mut std::ffi::c_void,
    throw: *mut std::ffi::c_void,
    throw_new: *mut std::ffi::c_void,
    exception_occurred: *mut std::ffi::c_void,
    exception_describe: *mut std::ffi::c_void,
    exception_clear: *mut std::ffi::c_void,
    fatal_error: *mut std::ffi::c_void,
    push_local_frame: *mut std::ffi::c_void,
    pop_local_frame: *mut std::ffi::c_void,
    new_global_ref: *mut std::ffi::c_void,
    delete_global_ref: *mut std::ffi::c_void,
    delete_local_ref: *mut std::ffi::c_void,
    is_same_object: *mut std::ffi::c_void,
    new_local_ref: *mut std::ffi::c_void,
    ensure_local_capacity: *mut std::ffi::c_void,
    alloc_object: *mut std::ffi::c_void,
    new_object: *mut std::ffi::c_void,
    new_object_v: *mut std::ffi::c_void,
    new_object_a: *mut std::ffi::c_void,
    get_object_class: *mut std::ffi::c_void,
    is_instance_of: *mut std::ffi::c_void,
    get_method_id: *mut std::ffi::c_void,
    call_object_method: *mut std::ffi::c_void,
    call_object_method_v: *mut std::ffi::c_void,
    call_object_method_a: *mut std::ffi::c_void,
    call_boolean_method: *mut std::ffi::c_void,
    call_boolean_method_v: *mut std::ffi::c_void,
    call_boolean_method_a: *mut std::ffi::c_void,
    call_byte_method: *mut std::ffi::c_void,
    call_byte_method_v: *mut std::ffi::c_void,
    call_byte_method_a: *mut std::ffi::c_void,
    call_char_method: *mut std::ffi::c_void,
    call_char_method_v: *mut std::ffi::c_void,
    call_char_method_a: *mut std::ffi::c_void,
    call_short_method: *mut std::ffi::c_void,
    call_short_method_v: *mut std::ffi::c_void,
    call_short_method_a: *mut std::ffi::c_void,
    call_int_method: *mut std::ffi::c_void,
    call_int_method_v: *mut std::ffi::c_void,
    call_int_method_a: *mut std::ffi::c_void,
    call_long_method: *mut std::ffi::c_void,
    call_long_method_v: *mut std::ffi::c_void,
    call_long_method_a: *mut std::ffi::c_void,
    call_float_method: *mut std::ffi::c_void,
    call_float_method_v: *mut std::ffi::c_void,
    call_float_method_a: *mut std::ffi::c_void,
    call_double_method: *mut std::ffi::c_void,
    call_double_method_v: *mut std::ffi::c_void,
    call_double_method_a: *mut std::ffi::c_void,
    call_void_method: *mut std::ffi::c_void,
    call_void_method_v: *mut std::ffi::c_void,
    call_void_method_a: *mut std::ffi::c_void,
    call_nonvirtual_object_method: *mut std::ffi::c_void,
    call_nonvirtual_object_method_v: *mut std::ffi::c_void,
    call_nonvirtual_object_method_a: *mut std::ffi::c_void,
    call_nonvirtual_boolean_method: *mut std::ffi::c_void,
    call_nonvirtual_boolean_method_v: *mut std::ffi::c_void,
    call_nonvirtual_boolean_method_a: *mut std::ffi::c_void,
    call_nonvirtual_byte_method: *mut std::ffi::c_void,
    call_nonvirtual_byte_method_v: *mut std::ffi::c_void,
    call_nonvirtual_byte_method_a: *mut std::ffi::c_void,
    call_nonvirtual_char_method: *mut std::ffi::c_void,
    call_nonvirtual_char_method_v: *mut std::ffi::c_void,
    call_nonvirtual_char_method_a: *mut std::ffi::c_void,
    call_nonvirtual_short_method: *mut std::ffi::c_void,
    call_nonvirtual_short_method_v: *mut std::ffi::c_void,
    call_nonvirtual_short_method_a: *mut std::ffi::c_void,
    call_nonvirtual_int_method: *mut std::ffi::c_void,
    call_nonvirtual_int_method_v: *mut std::ffi::c_void,
    call_nonvirtual_int_method_a: *mut std::ffi::c_void,
    call_nonvirtual_long_method: *mut std::ffi::c_void,
    call_nonvirtual_long_method_v: *mut std::ffi::c_void,
    call_nonvirtual_long_method_a: *mut std::ffi::c_void,
    call_nonvirtual_float_method: *mut std::ffi::c_void,
    call_nonvirtual_float_method_v: *mut std::ffi::c_void,
    call_nonvirtual_float_method_a: *mut std::ffi::c_void,
    call_nonvirtual_double_method: *mut std::ffi::c_void,
    call_nonvirtual_double_method_v: *mut std::ffi::c_void,
    call_nonvirtual_double_method_a: *mut std::ffi::c_void,
    call_nonvirtual_void_method: *mut std::ffi::c_void,
    call_nonvirtual_void_method_v: *mut std::ffi::c_void,
    call_nonvirtual_void_method_a: *mut std::ffi::c_void,
    get_field_id: *mut std::ffi::c_void,
    get_object_field: *mut std::ffi::c_void,
    get_boolean_field: *mut std::ffi::c_void,
    get_byte_field: *mut std::ffi::c_void,
    get_char_field: *mut std::ffi::c_void,
    get_short_field: *mut std::ffi::c_void,
    get_int_field: *mut std::ffi::c_void,
    get_long_field: *mut std::ffi::c_void,
    get_float_field: *mut std::ffi::c_void,
    get_double_field: *mut std::ffi::c_void,
    set_object_field: *mut std::ffi::c_void,
    set_boolean_field: *mut std::ffi::c_void,
    set_byte_field: *mut std::ffi::c_void,
    set_char_field: *mut std::ffi::c_void,
    set_short_field: *mut std::ffi::c_void,
    set_int_field: *mut std::ffi::c_void,
    set_long_field: *mut std::ffi::c_void,
    set_float_field: *mut std::ffi::c_void,
    set_double_field: *mut std::ffi::c_void,
    get_static_method_id: *mut std::ffi::c_void,
    call_static_object_method: *mut std::ffi::c_void,
    call_static_object_method_v: *mut std::ffi::c_void,
    call_static_object_method_a: *mut std::ffi::c_void,
    call_static_boolean_method: *mut std::ffi::c_void,
    call_static_boolean_method_v: *mut std::ffi::c_void,
    call_static_boolean_method_a: *mut std::ffi::c_void,
    call_static_byte_method: *mut std::ffi::c_void,
    call_static_byte_method_v: *mut std::ffi::c_void,
    call_static_byte_method_a: *mut std::ffi::c_void,
    call_static_char_method: *mut std::ffi::c_void,
    call_static_char_method_v: *mut std::ffi::c_void,
    call_static_char_method_a: *mut std::ffi::c_void,
    call_static_short_method: *mut std::ffi::c_void,
    call_static_short_method_v: *mut std::ffi::c_void,
    call_static_short_method_a: *mut std::ffi::c_void,
    call_static_int_method: *mut std::ffi::c_void,
    call_static_int_method_v: *mut std::ffi::c_void,
    call_static_int_method_a: *mut std::ffi::c_void,
    call_static_long_method: *mut std::ffi::c_void,
    call_static_long_method_v: *mut std::ffi::c_void,
    call_static_long_method_a: *mut std::ffi::c_void,
    call_static_float_method: *mut std::ffi::c_void,
    call_static_float_method_v: *mut std::ffi::c_void,
    call_static_float_method_a: *mut std::ffi::c_void,
    call_static_double_method: *mut std::ffi::c_void,
    call_static_double_method_v: *mut std::ffi::c_void,
    call_static_double_method_a: *mut std::ffi::c_void,
    call_static_void_method: *mut std::ffi::c_void,
    call_static_void_method_v: *mut std::ffi::c_void,
    call_static_void_method_a: *mut std::ffi::c_void,
    get_static_field_id: *mut std::ffi::c_void,
    get_static_object_field: *mut std::ffi::c_void,
    get_static_boolean_field: *mut std::ffi::c_void,
    get_static_byte_field: *mut std::ffi::c_void,
    get_static_char_field: *mut std::ffi::c_void,
    get_static_short_field: *mut std::ffi::c_void,
    get_static_int_field: *mut std::ffi::c_void,
    get_static_long_field: *mut std::ffi::c_void,
    get_static_float_field: *mut std::ffi::c_void,
    get_static_double_field: *mut std::ffi::c_void,
    set_static_object_field: *mut std::ffi::c_void,
    set_static_boolean_field: *mut std::ffi::c_void,
    set_static_byte_field: *mut std::ffi::c_void,
    set_static_char_field: *mut std::ffi::c_void,
    set_static_short_field: *mut std::ffi::c_void,
    set_static_int_field: *mut std::ffi::c_void,
    set_static_long_field: *mut std::ffi::c_void,
    set_static_float_field: *mut std::ffi::c_void,
    set_static_double_field: *mut std::ffi::c_void,
    new_string: *mut std::ffi::c_void,
    get_string_length: *mut std::ffi::c_void,
    get_string_chars: *mut std::ffi::c_void,
    release_string_chars: *mut std::ffi::c_void,
    new_string_utf: *mut std::ffi::c_void,
    get_string_utf_length: *mut std::ffi::c_void,
    get_string_utf_chars: *mut std::ffi::c_void,
    release_string_utf_chars: *mut std::ffi::c_void,
    get_array_length: unsafe extern "system" fn(*mut JNIEnv, *mut std::ffi::c_void) -> JSize,
    new_object_array: *mut std::ffi::c_void,
    get_object_array_element: *mut std::ffi::c_void,
    set_object_array_element: *mut std::ffi::c_void,
    new_boolean_array: *mut std::ffi::c_void,
    new_byte_array: unsafe extern "system" fn(*mut JNIEnv, JSize) -> *mut std::ffi::c_void,
    new_char_array: *mut std::ffi::c_void,
    new_short_array: *mut std::ffi::c_void,
    new_int_array: *mut std::ffi::c_void,
    new_long_array: *mut std::ffi::c_void,
    new_float_array: *mut std::ffi::c_void,
    new_double_array: *mut std::ffi::c_void,
    get_boolean_array_elements: *mut std::ffi::c_void,
    get_byte_array_elements: *mut std::ffi::c_void,
    get_char_array_elements: *mut std::ffi::c_void,
    get_short_array_elements: *mut std::ffi::c_void,
    get_int_array_elements: *mut std::ffi::c_void,
    get_long_array_elements: *mut std::ffi::c_void,
    get_float_array_elements:
        unsafe extern "system" fn(*mut JNIEnv, *mut std::ffi::c_void, *mut JBoolean) -> *mut f32,
    get_double_array_elements: *mut std::ffi::c_void,
    release_boolean_array_elements: *mut std::ffi::c_void,
    release_byte_array_elements: *mut std::ffi::c_void,
    release_char_array_elements: *mut std::ffi::c_void,
    release_short_array_elements: *mut std::ffi::c_void,
    release_int_array_elements: *mut std::ffi::c_void,
    release_long_array_elements: *mut std::ffi::c_void,
    release_float_array_elements:
        unsafe extern "system" fn(*mut JNIEnv, *mut std::ffi::c_void, *mut f32, JInt),
    release_double_array_elements: *mut std::ffi::c_void,
    get_boolean_array_region: *mut std::ffi::c_void,
    get_byte_array_region: *mut std::ffi::c_void,
    get_char_array_region: *mut std::ffi::c_void,
    get_short_array_region: *mut std::ffi::c_void,
    get_int_array_region: *mut std::ffi::c_void,
    get_long_array_region: *mut std::ffi::c_void,
    get_float_array_region: *mut std::ffi::c_void,
    get_double_array_region: *mut std::ffi::c_void,
    set_boolean_array_region: *mut std::ffi::c_void,
    set_byte_array_region:
        unsafe extern "system" fn(*mut JNIEnv, *mut std::ffi::c_void, JSize, JSize, *const i8),
}

#[repr(C)]
pub struct JNIEnv {
    functions: *const JNINativeInterface,
}

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
    strokes: Vec<Vec<HyPaintSample>>,
    brush: BrushSettings,
}

impl HyPaintDocument {
    pub fn new(width: u32, height: u32) -> Self {
        let canvas_size = CanvasSize::new(width, height);
        Self {
            canvas_size,
            layers: vec![RasterLayer::new(1, "Ink", canvas_size)],
            strokes: Vec::new(),
            brush: BrushSettings::ink(8.0, Rgba8::BLACK),
        }
    }

    pub fn append_stroke(&mut self, samples: &[HyPaintSample]) {
        if samples.is_empty() {
            return;
        }

        self.strokes.push(samples.to_vec());
        self.rasterize_stroke(samples);
    }

    pub fn clear(&mut self) {
        self.strokes.clear();
        self.reset_layers();
    }

    pub fn undo(&mut self) -> bool {
        if self.strokes.pop().is_none() {
            return false;
        }

        self.reset_layers();
        for stroke in self.strokes.clone() {
            self.rasterize_stroke(&stroke);
        }
        true
    }

    pub fn render_rgba(&self) -> Vec<u8> {
        export_rgba_bytes(self.canvas_size, &self.layers)
    }

    fn reset_layers(&mut self) {
        self.layers = vec![RasterLayer::new(1, "Ink", self.canvas_size)];
    }

    fn rasterize_stroke(&mut self, samples: &[HyPaintSample]) {
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
pub unsafe extern "C" fn hyp_document_undo(document: *mut HyPaintDocument) -> bool {
    let Some(document) = document.as_mut() else {
        return false;
    };

    document.undo()
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

#[no_mangle]
pub unsafe extern "system" fn Java_io_github_lukasvi_hypainter_engine_NativePaintingEngine_nativeUndo(
    _env: *mut std::ffi::c_void,
    _class: *mut std::ffi::c_void,
    handle: JLong,
) -> JBoolean {
    hyp_document_undo(handle as *mut HyPaintDocument) as JBoolean
}

#[no_mangle]
pub unsafe extern "system" fn Java_io_github_lukasvi_hypainter_engine_NativePaintingEngine_nativeAppendStroke(
    env: *mut JNIEnv,
    _class: *mut std::ffi::c_void,
    handle: JLong,
    samples: *mut std::ffi::c_void,
) -> JBoolean {
    let Some(document) = (handle as *mut HyPaintDocument).as_mut() else {
        return 0;
    };

    if env.is_null() || samples.is_null() {
        return 0;
    }

    let functions = (*env).functions;
    let len = ((*functions).get_array_length)(env, samples);
    if len <= 0 || len as usize % JNI_SAMPLE_STRIDE != 0 {
        return 0;
    }

    let raw = ((*functions).get_float_array_elements)(env, samples, ptr::null_mut());
    if raw.is_null() {
        return 0;
    }

    let floats = slice::from_raw_parts(raw, len as usize);
    let mut decoded = Vec::with_capacity(floats.len() / JNI_SAMPLE_STRIDE);
    for chunk in floats.chunks_exact(JNI_SAMPLE_STRIDE) {
        decoded.push(HyPaintSample {
            x: chunk[0],
            y: chunk[1],
            pressure: chunk[2],
            tilt_x: chunk[3],
            tilt_y: chunk[4],
            timestamp_ms: chunk[5].max(0.0) as u64,
        });
    }

    ((*functions).release_float_array_elements)(env, samples, raw, JNI_ABORT);
    document.append_stroke(&decoded);
    1
}

#[no_mangle]
pub unsafe extern "system" fn Java_io_github_lukasvi_hypainter_engine_NativePaintingEngine_nativeRenderRgba(
    env: *mut JNIEnv,
    _class: *mut std::ffi::c_void,
    handle: JLong,
) -> *mut std::ffi::c_void {
    let Some(document) = (handle as *const HyPaintDocument).as_ref() else {
        return ptr::null_mut();
    };

    if env.is_null() {
        return ptr::null_mut();
    }

    let bytes = document.render_rgba();
    if bytes.len() > JSize::MAX as usize {
        return ptr::null_mut();
    }

    let functions = (*env).functions;
    let array = ((*functions).new_byte_array)(env, bytes.len() as JSize);
    if array.is_null() {
        return ptr::null_mut();
    }

    ((*functions).set_byte_array_region)(
        env,
        array,
        0,
        bytes.len() as JSize,
        bytes.as_ptr() as *const i8,
    );
    array
}

const JNI_ABORT: JInt = 2;
const JNI_SAMPLE_STRIDE: usize = 6;

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

    #[test]
    fn ffi_undo_rebuilds_without_last_stroke() {
        let mut document = HyPaintDocument::new(32, 32);
        document.append_stroke(&[HyPaintSample {
            x: 8.0,
            y: 8.0,
            pressure: 1.0,
            tilt_x: 0.0,
            tilt_y: 0.0,
            timestamp_ms: 0,
        }]);
        assert!(document.render_rgba().iter().any(|value| *value != 0));

        assert!(document.undo());

        assert!(document.render_rgba().iter().all(|value| *value == 0));
    }
}
