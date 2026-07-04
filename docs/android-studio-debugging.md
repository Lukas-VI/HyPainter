# Android Studio Debugging

## Run The App

1. Open `D:\Java\P\HyPainter` in Android Studio.
2. Select the `app` run configuration.
3. Select a connected Android tablet.
4. Click `Run` or `Debug`.

The `io.github.lukasvi.hypainter.debug` package is not a separate app or launch target. It is compiled into the normal debug APK and is activated from the in-app `Debug` toolbar chip.

## Use The Input Overlay

1. Start the app.
2. Tap the toolbar `Debug` chip.
3. Watch the overlay in the bottom-right corner.

Expected route values:

- `stylus`: stylus or eraser owns input.
- `single-finger`: one finger touched the canvas; it should not pan or rotate the canvas.
- `two-finger`: two fingers are driving pan, zoom, and rotation.
- `ignored`: an unsupported or mixed tool stream was ignored.

Keep `Debug` off when measuring drawing performance. The overlay and input logs are diagnostic tools and should not be part of baseline latency testing.

## Use Logcat In Android Studio

1. Open `View > Tool Windows > Logcat`.
2. Select the connected device.
3. Select the `io.github.lukasvi.hypainter` app process.
4. Search for:

```text
tag:HyPainterInput
```

Or use the command line:

```powershell
adb logcat -s HyPainterInput
```

`HyPainterInput` lines are emitted only after the in-app `Debug` chip is enabled.

## Diagnose GC And Input Latency

Useful Logcat searches:

```text
package:io.github.lukasvi.hypainter (GC OR Choreographer OR Davey OR MIUIInput OR AnrScout)
```

Warning signs:

- `Skipped ... frames`
- `Davey! duration=...`
- `input event latency is ...`
- Sticky or frequent GC during stylus down/move/up

For baseline latency, turn the in-app `Debug` chip off, draw long strokes, then inspect GC and frame logs.
