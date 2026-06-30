use hyp_core::{BrushSettings, Point, StylusSample};
use hyp_tiles::TileGrid;

#[derive(Clone, Debug)]
pub struct Stroke {
    samples: Vec<StylusSample>,
}

impl Stroke {
    pub fn new() -> Self {
        Self {
            samples: Vec::new(),
        }
    }

    pub fn append(&mut self, samples: impl IntoIterator<Item = StylusSample>) {
        self.samples.extend(samples);
        self.samples.sort_by_key(|sample| sample.timestamp_ms);
    }

    pub fn rasterize(&self, target: &mut TileGrid, settings: BrushSettings) {
        if self.samples.is_empty() {
            return;
        }

        let mut previous = self.samples[0];
        paint_dab(target, previous, settings);

        for sample in self.samples.iter().copied().skip(1) {
            rasterize_segment(target, previous, sample, settings);
            previous = sample;
        }
    }
}

impl Default for Stroke {
    fn default() -> Self {
        Self::new()
    }
}

fn rasterize_segment(
    target: &mut TileGrid,
    from: StylusSample,
    to: StylusSample,
    settings: BrushSettings,
) {
    let distance = distance(from.position, to.position);
    let spacing_px = (settings.radius_px * 2.0 * settings.spacing.max(0.05)).max(1.0);
    let steps = (distance / spacing_px).ceil().max(1.0) as u32;

    for step in 1..=steps {
        let t = step as f32 / steps as f32;
        let sample = interpolate_sample(from, to, t);
        paint_dab(target, sample, settings);
    }
}

fn paint_dab(target: &mut TileGrid, sample: StylusSample, settings: BrushSettings) {
    let pressure = sample.pressure.clamp(0.05, 1.0);
    let radius = settings.radius_px * pressure;
    let color = settings.color.with_alpha(settings.opacity * pressure);
    target.draw_circle(sample.position.x, sample.position.y, radius, color);
}

fn interpolate_sample(from: StylusSample, to: StylusSample, t: f32) -> StylusSample {
    StylusSample {
        position: Point::new(
            from.position.x + (to.position.x - from.position.x) * t,
            from.position.y + (to.position.y - from.position.y) * t,
        ),
        pressure: from.pressure + (to.pressure - from.pressure) * t,
        tilt_x: from.tilt_x + (to.tilt_x - from.tilt_x) * t,
        tilt_y: from.tilt_y + (to.tilt_y - from.tilt_y) * t,
        timestamp_ms: from.timestamp_ms + ((to.timestamp_ms - from.timestamp_ms) as f32 * t) as u64,
    }
}

fn distance(from: Point, to: Point) -> f32 {
    let dx = to.x - from.x;
    let dy = to.y - from.y;
    (dx * dx + dy * dy).sqrt()
}

#[cfg(test)]
mod tests {
    use hyp_core::{CanvasSize, Rgba8};

    use super::*;

    #[test]
    fn stroke_rasterizes_between_samples() {
        let mut grid = TileGrid::new(CanvasSize::new(128, 128));
        let mut stroke = Stroke::new();
        stroke.append([
            StylusSample::new(10.0, 10.0, 1.0, 0),
            StylusSample::new(80.0, 10.0, 1.0, 16),
        ]);

        stroke.rasterize(&mut grid, BrushSettings::ink(4.0, Rgba8::BLACK));

        assert_ne!(grid.get_pixel(10, 10), Rgba8::TRANSPARENT);
        assert_ne!(grid.get_pixel(45, 10), Rgba8::TRANSPARENT);
        assert_ne!(grid.get_pixel(80, 10), Rgba8::TRANSPARENT);
    }
}
