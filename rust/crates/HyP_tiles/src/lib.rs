use std::collections::{HashMap, HashSet};

use hyp_core::{CanvasSize, Rgba8, TileCoord, TILE_SIZE};

#[derive(Clone, Debug)]
pub struct Tile {
    pixels: Vec<Rgba8>,
}

impl Tile {
    pub fn new() -> Self {
        Self {
            pixels: vec![Rgba8::TRANSPARENT; (TILE_SIZE * TILE_SIZE) as usize],
        }
    }

    pub fn pixel(&self, x: u32, y: u32) -> Rgba8 {
        self.pixels[(y * TILE_SIZE + x) as usize]
    }

    pub fn set_pixel(&mut self, x: u32, y: u32, color: Rgba8) {
        self.pixels[(y * TILE_SIZE + x) as usize] = color;
    }

    pub fn pixels(&self) -> &[Rgba8] {
        &self.pixels
    }
}

impl Default for Tile {
    fn default() -> Self {
        Self::new()
    }
}

#[derive(Clone, Debug)]
pub struct TileGrid {
    canvas_size: CanvasSize,
    tiles: HashMap<TileCoord, Tile>,
    dirty_tiles: HashSet<TileCoord>,
}

impl TileGrid {
    pub fn new(canvas_size: CanvasSize) -> Self {
        Self {
            canvas_size,
            tiles: HashMap::new(),
            dirty_tiles: HashSet::new(),
        }
    }

    pub fn canvas_size(&self) -> CanvasSize {
        self.canvas_size
    }

    pub fn get_pixel(&self, x: i32, y: i32) -> Rgba8 {
        if !self.canvas_size.contains(x, y) {
            return Rgba8::TRANSPARENT;
        }

        let coord = tile_coord(x, y);
        let local = local_coord(x, y);
        self.tiles
            .get(&coord)
            .map(|tile| tile.pixel(local.0, local.1))
            .unwrap_or(Rgba8::TRANSPARENT)
    }

    pub fn blend_pixel(&mut self, x: i32, y: i32, color: Rgba8) {
        if !self.canvas_size.contains(x, y) || color.a == 0 {
            return;
        }

        let coord = tile_coord(x, y);
        let local = local_coord(x, y);
        let tile = self.tiles.entry(coord).or_default();
        let destination = tile.pixel(local.0, local.1);
        tile.set_pixel(local.0, local.1, source_over(destination, color));
        self.dirty_tiles.insert(coord);
    }

    pub fn draw_circle(&mut self, center_x: f32, center_y: f32, radius: f32, color: Rgba8) {
        let radius = radius.max(0.5);
        let min_x = (center_x - radius).floor() as i32;
        let max_x = (center_x + radius).ceil() as i32;
        let min_y = (center_y - radius).floor() as i32;
        let max_y = (center_y + radius).ceil() as i32;
        let radius_sq = radius * radius;

        for y in min_y..=max_y {
            for x in min_x..=max_x {
                let dx = x as f32 + 0.5 - center_x;
                let dy = y as f32 + 0.5 - center_y;
                let distance_sq = dx * dx + dy * dy;
                if distance_sq <= radius_sq {
                    let edge = 1.0 - (distance_sq.sqrt() / radius).clamp(0.0, 1.0);
                    let coverage = edge.max(0.15);
                    self.blend_pixel(x, y, color.with_alpha(coverage));
                }
            }
        }
    }

    pub fn dirty_tiles(&self) -> impl Iterator<Item = TileCoord> + '_ {
        self.dirty_tiles.iter().copied()
    }

    pub fn clear_dirty(&mut self) {
        self.dirty_tiles.clear();
    }
}

pub fn source_over(destination: Rgba8, source: Rgba8) -> Rgba8 {
    let src_a = source.a as f32 / 255.0;
    let dst_a = destination.a as f32 / 255.0;
    let out_a = src_a + dst_a * (1.0 - src_a);

    if out_a <= f32::EPSILON {
        return Rgba8::TRANSPARENT;
    }

    let channel = |src: u8, dst: u8| {
        (((src as f32 * src_a) + (dst as f32 * dst_a * (1.0 - src_a))) / out_a).round() as u8
    };

    Rgba8::new(
        channel(source.r, destination.r),
        channel(source.g, destination.g),
        channel(source.b, destination.b),
        (out_a * 255.0).round() as u8,
    )
}

fn tile_coord(x: i32, y: i32) -> TileCoord {
    TileCoord {
        x: x.div_euclid(TILE_SIZE as i32),
        y: y.div_euclid(TILE_SIZE as i32),
    }
}

fn local_coord(x: i32, y: i32) -> (u32, u32) {
    (
        x.rem_euclid(TILE_SIZE as i32) as u32,
        y.rem_euclid(TILE_SIZE as i32) as u32,
    )
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn circle_marks_dirty_tile_and_writes_pixels() {
        let mut grid = TileGrid::new(CanvasSize::new(512, 512));

        grid.draw_circle(12.0, 10.0, 6.0, Rgba8::BLACK);

        assert_ne!(grid.get_pixel(12, 10), Rgba8::TRANSPARENT);
        assert_eq!(grid.dirty_tiles().count(), 1);
    }

    #[test]
    fn source_over_preserves_opaque_source() {
        let result = source_over(Rgba8::WHITE, Rgba8::BLACK);

        assert_eq!(result, Rgba8::BLACK);
    }
}
