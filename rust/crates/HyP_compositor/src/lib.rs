use hyp_core::{CanvasSize, LayerInfo, Rgba8};
use hyp_tiles::{source_over, TileGrid};

#[derive(Clone, Debug)]
pub struct RasterLayer {
    pub info: LayerInfo,
    pub tiles: TileGrid,
}

impl RasterLayer {
    /// Creates a visible raster layer with an empty tile grid sized to the document canvas.
    pub fn new(id: u64, name: impl Into<String>, canvas_size: CanvasSize) -> Self {
        Self {
            info: LayerInfo::new(id, name),
            tiles: TileGrid::new(canvas_size),
        }
    }
}

/// Composites one canvas pixel by applying visible layers in order with source-over blending.
pub fn composite_pixel(layers: &[RasterLayer], x: i32, y: i32) -> Rgba8 {
    layers
        .iter()
        .filter(|layer| layer.info.visible)
        .fold(Rgba8::TRANSPARENT, |acc, layer| {
            let opacity = layer.info.opacity as f32 / 255.0;
            let source = layer.tiles.get_pixel(x, y).with_alpha(opacity);
            source_over(acc, source)
        })
}

/// Flattens the layer stack into an RGBA byte buffer suitable for Android bitmap/export handoff.
pub fn composite_rgba(layers: &[RasterLayer], canvas_size: CanvasSize) -> Vec<u8> {
    let mut rgba = Vec::with_capacity((canvas_size.width * canvas_size.height * 4) as usize);

    for y in 0..canvas_size.height as i32 {
        for x in 0..canvas_size.width as i32 {
            let pixel = composite_pixel(layers, x, y);
            rgba.extend_from_slice(&[pixel.r, pixel.g, pixel.b, pixel.a]);
        }
    }

    rgba
}

#[cfg(test)]
mod tests {
    use hyp_brush::Stroke;
    use hyp_core::{BrushSettings, Rgba8, StylusSample};

    use super::*;

    #[test]
    fn visible_layers_are_composited() {
        let canvas_size = CanvasSize::new(64, 64);
        let mut layer = RasterLayer::new(1, "Ink", canvas_size);
        let mut stroke = Stroke::new();
        stroke.append([StylusSample::new(20.0, 20.0, 1.0, 0)]);
        stroke.rasterize(&mut layer.tiles, BrushSettings::ink(5.0, Rgba8::BLACK));

        let pixel = composite_pixel(&[layer], 20, 20);

        assert_ne!(pixel, Rgba8::TRANSPARENT);
    }
}
