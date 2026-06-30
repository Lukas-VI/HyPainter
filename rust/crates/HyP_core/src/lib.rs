pub const TILE_SIZE: u32 = 256;

#[derive(Clone, Copy, Debug, PartialEq, Eq, Hash)]
pub struct TileCoord {
    pub x: i32,
    pub y: i32,
}

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub struct CanvasSize {
    pub width: u32,
    pub height: u32,
}

impl CanvasSize {
    pub fn new(width: u32, height: u32) -> Self {
        Self { width, height }
    }

    pub fn contains(self, x: i32, y: i32) -> bool {
        x >= 0 && y >= 0 && x < self.width as i32 && y < self.height as i32
    }
}

#[derive(Clone, Copy, Debug, PartialEq)]
pub struct Point {
    pub x: f32,
    pub y: f32,
}

impl Point {
    pub fn new(x: f32, y: f32) -> Self {
        Self { x, y }
    }
}

#[derive(Clone, Copy, Debug, PartialEq)]
pub struct StylusSample {
    pub position: Point,
    pub pressure: f32,
    pub tilt_x: f32,
    pub tilt_y: f32,
    pub timestamp_ms: u64,
}

impl StylusSample {
    pub fn new(x: f32, y: f32, pressure: f32, timestamp_ms: u64) -> Self {
        Self {
            position: Point::new(x, y),
            pressure: pressure.clamp(0.0, 1.0),
            tilt_x: 0.0,
            tilt_y: 0.0,
            timestamp_ms,
        }
    }
}

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub struct Rgba8 {
    pub r: u8,
    pub g: u8,
    pub b: u8,
    pub a: u8,
}

impl Rgba8 {
    pub const TRANSPARENT: Self = Self::new(0, 0, 0, 0);
    pub const BLACK: Self = Self::new(0, 0, 0, 255);
    pub const WHITE: Self = Self::new(255, 255, 255, 255);

    pub const fn new(r: u8, g: u8, b: u8, a: u8) -> Self {
        Self { r, g, b, a }
    }

    pub fn with_alpha(self, alpha: f32) -> Self {
        Self {
            a: ((self.a as f32) * alpha.clamp(0.0, 1.0)).round() as u8,
            ..self
        }
    }
}

#[derive(Clone, Copy, Debug, PartialEq)]
pub struct BrushSettings {
    pub radius_px: f32,
    pub opacity: f32,
    pub spacing: f32,
    pub color: Rgba8,
}

impl BrushSettings {
    pub fn ink(radius_px: f32, color: Rgba8) -> Self {
        Self {
            radius_px: radius_px.max(0.5),
            opacity: 1.0,
            spacing: 0.25,
            color,
        }
    }
}

#[derive(Clone, Debug, PartialEq, Eq)]
pub struct LayerInfo {
    pub id: u64,
    pub name: String,
    pub visible: bool,
    pub opacity: u8,
}

impl LayerInfo {
    pub fn new(id: u64, name: impl Into<String>) -> Self {
        Self {
            id,
            name: name.into(),
            visible: true,
            opacity: 255,
        }
    }
}
