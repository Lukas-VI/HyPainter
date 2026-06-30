use std::{
    fs::File,
    io::{BufWriter, Write},
    path::Path,
};

use hyp_compositor::{composite_rgba, RasterLayer};
use hyp_core::CanvasSize;

pub fn export_png(
    path: impl AsRef<Path>,
    canvas_size: CanvasSize,
    layers: &[RasterLayer],
) -> Result<(), ExportError> {
    let file = File::create(path)?;
    let writer = BufWriter::new(file);
    let pixels = composite_rgba(layers, canvas_size);
    write_png_rgba(writer, canvas_size, &pixels)?;

    Ok(())
}

#[derive(Debug)]
pub enum ExportError {
    Io(std::io::Error),
}

impl From<std::io::Error> for ExportError {
    fn from(error: std::io::Error) -> Self {
        Self::Io(error)
    }
}

impl std::fmt::Display for ExportError {
    fn fmt(&self, formatter: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            ExportError::Io(error) => write!(formatter, "I/O export error: {error}"),
        }
    }
}

impl std::error::Error for ExportError {}

pub fn export_rgba_bytes(canvas_size: CanvasSize, layers: &[RasterLayer]) -> Vec<u8> {
    composite_rgba(layers, canvas_size)
}

pub fn write_rgba_dump(
    path: impl AsRef<Path>,
    canvas_size: CanvasSize,
    layers: &[RasterLayer],
) -> Result<(), std::io::Error> {
    let mut file = File::create(path)?;
    file.write_all(&canvas_size.width.to_le_bytes())?;
    file.write_all(&canvas_size.height.to_le_bytes())?;
    file.write_all(&composite_rgba(layers, canvas_size))?;
    Ok(())
}

fn write_png_rgba(
    mut writer: impl Write,
    canvas_size: CanvasSize,
    rgba: &[u8],
) -> Result<(), std::io::Error> {
    writer.write_all(&[137, 80, 78, 71, 13, 10, 26, 10])?;

    let mut ihdr = Vec::with_capacity(13);
    ihdr.extend_from_slice(&canvas_size.width.to_be_bytes());
    ihdr.extend_from_slice(&canvas_size.height.to_be_bytes());
    ihdr.extend_from_slice(&[8, 6, 0, 0, 0]);
    write_chunk(&mut writer, b"IHDR", &ihdr)?;

    let mut scanlines = Vec::with_capacity(
        (canvas_size.width * canvas_size.height * 4 + canvas_size.height) as usize,
    );
    let row_bytes = (canvas_size.width * 4) as usize;
    for row in rgba.chunks(row_bytes) {
        scanlines.push(0);
        scanlines.extend_from_slice(row);
    }

    let compressed = zlib_store(&scanlines);
    write_chunk(&mut writer, b"IDAT", &compressed)?;
    write_chunk(&mut writer, b"IEND", &[])?;
    Ok(())
}

fn write_chunk(
    writer: &mut impl Write,
    chunk_type: &[u8; 4],
    data: &[u8],
) -> Result<(), std::io::Error> {
    writer.write_all(&(data.len() as u32).to_be_bytes())?;
    writer.write_all(chunk_type)?;
    writer.write_all(data)?;

    let mut crc_data = Vec::with_capacity(chunk_type.len() + data.len());
    crc_data.extend_from_slice(chunk_type);
    crc_data.extend_from_slice(data);
    writer.write_all(&crc32(&crc_data).to_be_bytes())?;
    Ok(())
}

fn zlib_store(data: &[u8]) -> Vec<u8> {
    let mut output = Vec::with_capacity(data.len() + 16);
    output.extend_from_slice(&[0x78, 0x01]);

    let mut remaining = data;
    while !remaining.is_empty() {
        let block_len = remaining.len().min(u16::MAX as usize);
        let is_final = block_len == remaining.len();
        output.push(if is_final { 0x01 } else { 0x00 });
        output.extend_from_slice(&(block_len as u16).to_le_bytes());
        output.extend_from_slice(&(!(block_len as u16)).to_le_bytes());
        output.extend_from_slice(&remaining[..block_len]);
        remaining = &remaining[block_len..];
    }

    if data.is_empty() {
        output.extend_from_slice(&[0x01, 0x00, 0x00, 0xff, 0xff]);
    }

    output.extend_from_slice(&adler32(data).to_be_bytes());
    output
}

fn adler32(data: &[u8]) -> u32 {
    const MOD: u32 = 65_521;
    let mut a = 1;
    let mut b = 0;

    for byte in data {
        a = (a + *byte as u32) % MOD;
        b = (b + a) % MOD;
    }

    (b << 16) | a
}

fn crc32(data: &[u8]) -> u32 {
    let mut crc = 0xffff_ffff;
    for byte in data {
        crc ^= *byte as u32;
        for _ in 0..8 {
            let mask = 0u32.wrapping_sub(crc & 1);
            crc = (crc >> 1) ^ (0xedb8_8320 & mask);
        }
    }
    !crc
}

#[cfg(test)]
mod tests {
    use hyp_brush::Stroke;
    use hyp_compositor::RasterLayer;
    use hyp_core::{BrushSettings, Rgba8, StylusSample};

    use super::*;

    #[test]
    fn export_rgba_bytes_has_expected_size() {
        let canvas_size = CanvasSize::new(16, 16);
        let mut layer = RasterLayer::new(1, "Ink", canvas_size);
        let mut stroke = Stroke::new();
        stroke.append([StylusSample::new(8.0, 8.0, 1.0, 0)]);
        stroke.rasterize(&mut layer.tiles, BrushSettings::ink(3.0, Rgba8::BLACK));

        let bytes = export_rgba_bytes(canvas_size, &[layer]);

        assert_eq!(bytes.len(), 16 * 16 * 4);
        assert!(bytes.iter().any(|value| *value != 0));
    }

    #[test]
    fn png_export_has_png_signature() {
        let canvas_size = CanvasSize::new(4, 4);
        let layer = RasterLayer::new(1, "Empty", canvas_size);
        let mut bytes = Vec::new();

        write_png_rgba(
            &mut bytes,
            canvas_size,
            &export_rgba_bytes(canvas_size, &[layer]),
        )
        .expect("PNG export should succeed");

        assert_eq!(&bytes[..8], &[137, 80, 78, 71, 13, 10, 26, 10]);
    }
}
