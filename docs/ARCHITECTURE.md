# Architecture

## Modules

```
core-model  ←  core-io  ←  ui  ←  androidApp
                               ←  desktopApp
```

| Module | Targets | Contents |
|---|---|---|
| `core-model` | android, desktop (JVM) | Pure Kotlin with no Compose and no I/O. Covers `PixelBuffer`, `Drawing` (layers × frames), `EditorState`, `EditorAction`, `EditorReducer` and `StrokeEngine`, rasterization (`Raster`, `FloodFill`, `MirrorMode`), undo history, palettes, stamps, coloring pages, challenges and `Pixelate`. `explicitApi()` is on, and every public declaration has KDoc. |
| `core-io` | android, desktop | The `.pixelpals` codec and migrations, `ProjectRepository` (gallery, Recycle Bin, quarantine for corrupt files), `Autosaver`, `SettingsRepository`, and `PngEncoder`/`GifEncoder`/`PngExport`. Platform code uses `expect`/`actual`. JVM code shared by Android and desktop (`java.io`, `java.util.zip`) lives in a custom `jvmShared` source set. Image decoding is per platform (`BitmapFactory` / `ImageIO`). |
| `ui` | android, desktop | Compose Multiplatform screens, components, theme, the pixel icon set, string resources and the `PlatformServices` interface. `androidMain` and `desktopMain` implement it (share sheet, MediaStore/SAF, file dialogs, sound). |
| `androidApp` | Android app | `MainActivity`, the manifest, the FileProvider and the adaptive icon. |
| `desktopApp` | JVM app | `main()`, the window, and installer settings (`nativeDistributions`). |

AGP 9 no longer lets a Kotlin Multiplatform module apply `com.android.application`. So the shared
modules use `com.android.kotlin.multiplatform.library`, and the Android app is its own thin module.
That split is also what the spec asks for.

Adding iOS or Wasm later means adding the targets to the three shared modules and supplying `actual`s
for `Platform.kt` in `core-io` (file store, zlib, image decoding, zip) and a `PlatformServices` for the
UI. `core-model` already has no platform dependencies.

## State flow

The editor uses unidirectional data flow:

```
 pointer / keyboard / buttons
            │  EditorAction
            ▼
   EditorController.dispatch ──► EditorReducer.reduce(state, action) ──► Reduction(state', changed, effects)
            │                                (pure, unit-tested)
            ├─ state = state'                      → Compose recomposes
            ├─ CanvasRenderer.update(changed)      → repaints only the changed pixels
            ├─ effects → sounds / haptics
            └─ drawing changed and no stroke in progress → Autosaver.schedule(project)
```

- **`EditorState`** is an immutable data class. It holds the `Drawing`, selected frame and layer,
  tool settings, color, recent colors, `History`, and the in-progress `Stroke`.
- **`PixelBuffer`** is an immutable `IntArray` of ARGB values. Index value `0` means transparent.
  Edits return new buffers, so snapshots share memory safely.
- **Strokes:** `Press` records the layer's original buffer. Each `Drag` paints onto the current buffer
  (pencil, eraser, pattern) or repaints from the original (line, box, circle, stamp, move), which gives
  live previews. `Release` diffs the original against the result and pushes one `PixelDiff` (indices,
  before, after) onto the undo stack. A second finger, or an undo during a stroke, sends `CancelStroke`,
  which restores the original.
- **Undo:** `PixelDiff` is used for strokes, fills and imports. `CompoundCommand` covers Clear across
  layers. `StructureChange` (before/after snapshots that share buffers) covers layer and frame edits.
  The default limit is 200 steps.
- **Fill** works out the region from what the child *sees* (the composite of all visible layers) but
  paints on the current layer. That's why a coloring page's locked outline layer stops the bucket.
  The fill is an iterative scanline fill with no recursion; a 256×256 fill takes well under 50 ms
  (there's a test for this).

## Rendering

`CanvasRenderer` owns an `ImageBitmap` the size of the canvas. Each reduction reports which pixels
changed (`Changed.Pixels(indices)`, or `All` after frame switches, undoing a structural change, or
Move). Only those pixels are re-composited and drawn into the bitmap. Drawing uses
`BlendMode.Src`, no anti-aliasing, and one rectangle per run of identical pixels. A `version` state
then triggers a redraw.

The `PixelCanvas` composable draws, in order:

1. backdrop
2. checkerboard (one or two checks per canvas pixel, depending on zoom)
3. the onion-skin bitmap of the previous frame, at 30% opacity
4. the canvas bitmap, scaled with `FilterQuality.None` so pixels are never blurred
5. grid lines (when the cell is at least 6 px), dashed mirror axes, and the hover brush outline on
   desktop

`Viewport` holds the zoom and pan, and keeps at least a quarter of the canvas on screen.

Input:

- **Touch:** one finger draws. As soon as a second finger lands, the stroke is cancelled and the
  gesture becomes pinch-zoom plus pan. A one-finger drag never scrolls the canvas.
- **Mouse:** the left button draws. The middle button, or Space plus drag, pans. Ctrl/Cmd plus the
  wheel zooms around the cursor, and the plain wheel pans.

## Persistence

```
<app data>/
  projects/<id>.pixelpals        live drawings (JSON)
  trash/<id>.pixelpals           Recycle Bin
  trash/<id>.deleted             deletion time (epoch ms); entries older than 30 days are purged on start
  corrupt/<time>-<file>          unreadable files moved aside (never deleted automatically)
  settings.json                  AppSettings
```

`<app data>` is `filesDir/pixelpals` on Android. On desktop it's `~/Library/Application Support/PixelPals`
(macOS), `%APPDATA%\PixelPals` (Windows) or `$XDG_DATA_HOME/PixelPals` (Linux).

Every write goes to a temporary file, is `fsync`ed, and is then atomically renamed into place. A crash
mid-write therefore leaves the previous version intact.

Autosave is debounced by 1 s and serialized with a mutex. It's also flushed:

- in `Activity.onStop` on Android, which always runs before Android may kill the process
- when the window closes and in a JVM shutdown hook on desktop
- when the child leaves the editor

The current screen (including which drawing is open) is stored with `rememberSaveable`. After process
death, Android reopens the same drawing from disk.

## File format: `.pixelpals` (version 2)

UTF-8 JSON:

```jsonc
{
  "formatVersion": 2,            // bump + add a migration step when the schema changes
  "app": "PixelPals",
  "id": "lq3k9z-ab12cd34",
  "name": "Sunny Garden",
  "createdAt": 1760000000000,    // epoch ms
  "updatedAt": 1760000100000,
  "width": 32, "height": 32,     // 1..256
  "fps": 6,                      // animation speed 1..24
  "nextId": 9,                   // next free layer/frame id
  "paletteId": "bright",         // bright | rainbow | pastel | forest | ocean | retro
  "templateId": null,            // coloring page id, if started from one
  "challengeId": null,           // challenge id, if any
  "layers": [                    // bottom → top
    { "id": 1, "name": "Background", "visible": true, "locked": false }
  ],
  "frames": [
    { "id": 2, "cels": { "1": "<pixels>" } }   // one entry per layer id
  ],
  "thumbnail": { "width": 32, "height": 32, "pixels": "<pixels>" }  // frame 0 composite, ≤ 64 px
}
```

**`<pixels>`** is Base64 of a run-length encoding of the row-major ARGB array. The data is a sequence
of `(runLength: unsigned LEB128 varint, color: 4 bytes big-endian ARGB)` records whose run lengths add
up to exactly `width × height`. Colors use straight (non-premultiplied) alpha.

**Migrations:** `ProjectMigrations.migrate(JsonObject)` upgrades one version at a time before
decoding. Version 1 was the pre-animation format, with a `pixels` field on each layer and no frames.
Version 2 moves those pixels into one frame. A file from a *newer* version, or any malformed file,
raises `CorruptProjectException`. The repository then moves the file into `corrupt/`, and the UI shows
a friendly message instead of crashing.

## Export

- **PNG:** 8-bit RGBA with filter 0, streamed row by row through zlib. Scaling is nearest-neighbor by
  integer factors.
- **GIF:** GIF89a with a global table of up to 255 colors plus a transparent index, LZW using the
  classic code-size timing, a NETSCAPE loop extension, and per-frame delay `1000 / fps` ms.
- **Sprite sheet:** all frames side by side in one PNG.

Encoding runs on `Dispatchers.Default`. The bytes then go to `PlatformServices.export`. On Android
that's the share sheet, MediaStore (Pictures/PixelPals), or the Storage Access Framework; on desktop
it's a native `FileDialog`.

Both encoders are tested against `javax.imageio`, an independent decoder, down to exact pixel values.
