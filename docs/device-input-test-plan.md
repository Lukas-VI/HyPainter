# Device Input Test Plan

This checklist verifies the stylus-first MVP input path on a real Android tablet.

## Setup

1. Build and install the debug APK from Android Studio or PowerShell:

```powershell
.\gradlew.bat :android:app:assembleDebug
```

2. Open Logcat for optional input diagnostics:

```powershell
adb logcat -s HyPainterInput
```

3. In the app toolbar, tap `Debug` only when collecting diagnostics. Keep it off for baseline performance checks.

## Stylus Priority

- Draw a long continuous line for at least 10 seconds with the stylus.
- While drawing, rest a palm or one finger on the canvas.
- Expected:
  - The stroke continues without switching to pan, zoom, or rotation.
  - With `Debug` enabled, route remains `stylus` during the stroke.
  - Logcat does not show route changes to `single-finger` or `two-finger` while the stylus is active.

## Screen To Canvas Mapping

- Draw short marks near all four screen corners with the default viewport.
- Pan, zoom, and rotate the canvas using two fingers.
- Draw marks near all four screen corners again.
- Expected:
  - Marks appear under the stylus tip after pan, zoom, and rotation.
  - Full-screen drawing affects the visible canvas location, not only the original top-left document area.
  - With `Debug` enabled, `Screen` and `Canvas` coordinates both change smoothly as expected.

## Touch Layering

- Drag one finger on the canvas.
- Expected:
  - The canvas does not pan, zoom, or rotate.
  - With `Debug` enabled, route shows `single-finger`.

- Place two fingers on the canvas and move them together.
- Expected:
  - The canvas pans.
  - With `Debug` enabled, route shows `two-finger`.

- Rotate two fingers around their midpoint, including when the midpoint is outside the document rectangle.
- Expected:
  - Rotation anchors around the two-finger centroid, not the screen center.
  - The canvas motion feels stable without sudden jumps.

## Long Stroke Performance

- Turn `Debug` off.
- Draw repeated long strokes for 30 seconds.
- Tap brush color, brush size, layer, undo, and clear controls repeatedly between strokes.
- Expected:
  - Stylus drawing remains responsive.
  - Toolbar taps do not freeze the app for noticeable periods.
  - Strokes do not unexpectedly end and convert into touch gestures.

## Evidence To Capture

If a failure occurs, record:

- Device model and Android version.
- Whether `Debug` was on or off.
- The visible overlay route/action/tool values.
- A short Logcat excerpt from `adb logcat -s HyPainterInput`.
- Whether the failure happened during stylus down, stylus move, stylus up, one-finger touch, or two-finger touch.
