# Decisions

These are places where a requirement ran into a platform limit or was ambiguous. Each entry says what
I chose and why.

1. **The Android app is a separate module.** AGP 9 doesn't allow `com.android.application` in a Kotlin
   Multiplatform module. The shared modules use `com.android.kotlin.multiplatform.library`, and
   `androidApp` is a plain Android application module. This matches the "thin entry points" layout
   the spec asks for.

2. **compileSdk and targetSdk are 37.** The current Compose artifacts (`runtime-saveable-android
   1.12.1`) require compileSdk 37 or newer, so both are 37, the latest stable Android SDK. minSdk is 26.

3. **The JVM toolchain is JDK 21.** Gradle 9.8 and AGP 9.4 run on it, the desktop installers need
   jpackage from JDK 17+, and Android desugars the bytecode.

4. **Configuration changes on Android.** The activity declares `configChanges` for rotation, resize,
   dark mode and similar, so Compose re-lays out in place and the undo history survives. After
   *process death*, the open drawing comes back from disk (the screen route is in `rememberSaveable`,
   and the autosave is flushed in `onStop`). The undo history doesn't survive process death. Storing
   it would mean writing every diff to disk, which costs a lot of writes for little gain.

5. **Desktop bitmap upload.** On every platform, the composite is recomputed only for the changed
   pixels, and only those pixels are drawn into the `ImageBitmap`. I did this with Compose's common
   `Canvas(ImageBitmap)` API so the incremental path is shared code. How the GPU texture is
   refreshed underneath is up to Skia on each platform.

6. **Fill uses what the child sees.** The fill region is computed from the visible composite and
   painted on the current layer. So filling inside an outline that sits on another layer, such as a
   coloring page, does what a child expects.

7. **Drawing on a hidden layer makes it visible.** Otherwise strokes would seem to vanish.
   Drawing on a *locked* layer (a coloring-page outline) is blocked, and the app shows a short hint.

8. **Mirror mode has a tool button and option toggles.** The big "Mirror" button switches left-right
   symmetry, because that's the most common use. The options row under the canvas has separate
   Left-Right and Top-Bottom toggles, so both kinds of symmetry are available.

9. **The eyedropper is one-shot.** After picking a color, the app goes back to the previous tool.
   Younger kids found staying in picker mode confusing.

10. **Custom sizes need a parent to turn them on.** The "More" size option appears only after a
    grown-up enables "Allow custom canvas sizes" behind the parent gate. The size is set with
    steppers, so no typing is needed.

11. **The parent gate offers two ways in.** A grown-up can either answer a 6–9 × 6–9 multiplication
    on an on-screen keypad or hold a button for 3 seconds. Both are easy for an adult and unlikely
    for young children to do by accident.

12. **Sounds are synthesized.** The sounds are generated in code at runtime, so there are no audio
    files or licenses to track. Sound is muted by default on desktop and on by default on Android,
    and a parent can change this.

13. **Haptics use Compose's `LocalHapticFeedback`.** On Android it calls
    `View.performHapticFeedback`, which follows the system's touch-feedback setting.

14. **Exporting on Android.** "Share" writes to the cache and goes through a `FileProvider` and the
    share sheet. "Save to Pictures" uses MediaStore on Android 10+ (API 29), which needs no storage
    permission. On API 26–28 it falls back to the Storage Access Framework. The parent ZIP export
    offers Share or a SAF document.

15. **Photo import is a new-drawing option.** You pick the canvas size first. The photo is
    downscaled, centered with its aspect ratio kept, and snapped to the Bright palette. It then
    becomes the background layer, and the child can undo or paint over it.

16. **Animation frames share the layer list.** Every frame has one cel per layer. Locked layers
    (outlines) are copied into new frames, so coloring pages can be animated. Playback is limited to
    1–24 fps, and a drawing can have at most 24 frames.

17. **Challenges compare pixels exactly.** The palette is limited to the target's colors, which keeps
    exact matching fair. The progress indicator ("NN% matching") is only a hint. There are no scores
    or timers.

18. **Installers are built per OS.** jpackage can't cross-compile installers, so CI builds the DEB on
    Linux, the DMG on macOS and the MSI on Windows. The macOS build isn't signed or notarized, which
    needs a developer account.

19. **Android UI test.** The required Compose UI test (create → draw → undo → autosave → reopen) runs
    on the desktop JVM in `ui/src/desktopTest`, because it's shared code and runs headless in CI.
    Android behaviour is covered by the core tests on the Android host and by a manual emulator
    smoke test (see the README).
