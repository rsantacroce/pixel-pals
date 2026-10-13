# Build "Pixel Pals": A Kid-Friendly Pixel Art Editor (Android + Desktop)

You are a senior Kotlin Multiplatform engineer. Build a complete, polished, kid-friendly pixel art editor that runs on **Android** and **Desktop (Windows, macOS, Linux)** from a single shared codebase. The app is for children aged roughly 5–12, used with a parent nearby. It must be fun, safe, fast, forgiving of mistakes, and work fully offline.

Work in phases (see "Delivery Plan"). At the end of each phase the app must build, run, and pass its tests on both platforms. Do not leave TODO stubs in shipped features.

---

## 1. Tech Stack (required)

- **Language:** Kotlin (latest stable).
- **UI:** Compose Multiplatform (JetBrains), latest stable. Shared UI in `commonMain`.
- **Targets:** `androidTarget()` and `jvm("desktop")`. Structure the project so iOS and Web (Wasm) can be added later without rewriting shared code, but do not implement them now.
- **Build:** Gradle with Kotlin DSL and a version catalog (`gradle/libs.versions.toml`). Use the current official Compose Multiplatform project template layout as the starting point. Verify library versions are current and mutually compatible before pinning them.
- **Android:** minSdk 26, target the latest stable SDK. Single-activity app.
- **Desktop:** Package native installers with the Compose Desktop `nativeDistributions` task (MSI/DMG/DEB).
- **Serialization:** `kotlinx.serialization` (JSON) for project files.
- **Coroutines:** `kotlinx.coroutines` for autosave and export work.
- **Persistence/files:** Use `expect/actual` for platform file access. On Android use app-private storage plus the system share sheet / Storage Access Framework for exports. On desktop use a per-user app data folder and a native file chooser for export.
- **No** analytics, ads, network calls, accounts, in-app purchases, or third-party tracking SDKs. Do not request the INTERNET permission on Android.
- Prefer few dependencies. Justify every dependency you add in `docs/DEPENDENCIES.md`.

## 2. Architecture

- Modules (or clearly separated source packages if a single module is simpler):
  - `core-model` – pure Kotlin: canvas, layers, palette, history, tools logic. No Compose imports. Fully unit-tested.
  - `core-io` – project save/load (JSON), PNG export, GIF export, `expect/actual` file access.
  - `ui` – Compose screens, components, theme.
  - `androidApp` and `desktopApp` – thin entry points.
- Pattern: unidirectional data flow (state + intents/actions + reducer-style ViewModel/StateHolder). The editor state is immutable; tool operations produce new state or diffs.
- Pixel storage: a flat `IntArray` of ARGB per layer (width × height). Index 0 = fully transparent.
- Rendering: draw the canvas with Compose `Canvas`, rendering into an `ImageBitmap` that is updated incrementally (only dirty regions) so drawing stays smooth at 128×128. Use nearest-neighbor scaling — never blur pixels.
- Undo/redo: command pattern storing **pixel diffs** (index, before, after) per stroke, not full snapshots. One finger/mouse stroke = one undo step. Minimum 100 undo levels.

## 3. Core Features (MVP)

### 3.1 Canvas
- Sizes offered as friendly choices: 16×16 ("Tiny"), 32×32 ("Small"), 64×64 ("Medium"), 128×128 ("Big"). Also allow a custom size up to 256×256 under a parent-only "More" option.
- Toggleable pixel grid (on by default for sizes ≤ 64).
- Checkerboard background shows transparency.
- Zoom: pinch on Android, Ctrl+scroll / +/− buttons on desktop. Pan: two-finger drag on Android, space+drag or middle mouse on desktop. "Fit to screen" button.
- Canvas never accidentally scrolls while drawing with one finger.

### 3.2 Tools (big, clear icons with text labels)
1. **Pencil** – 1 px, with optional sizes 1/2/3.
2. **Eraser** – same sizes.
3. **Bucket fill** – flood fill (4-way), iterative (no recursion; must not stack-overflow on 256×256).
4. **Color picker (eyedropper)**.
5. **Line** – with live preview while dragging (Bresenham).
6. **Rectangle** and **Circle/Ellipse** – outline or filled, live preview.
7. **Mirror mode** – toggle horizontal and/or vertical symmetry; strokes are mirrored live. Show the mirror axis as a dashed line. (Kids love this — make it prominent.)
8. **Move** – shift the whole layer by whole pixels.
9. **Stamps** – a small built-in set of original pixel stamps (heart, star, smiley, flower, cat face, sword, mushroom, rocket, etc.). Draw these yourself as data; do not copy any existing game sprites or copyrighted characters.

### 3.3 Colors
- A default kid-friendly palette of 16–24 bright colors plus black, white, and greys.
- Several built-in palettes: "Rainbow", "Pastel", "Forest", "Ocean", "Retro 8-bit" (your own selection of colors, not a trademarked console palette).
- A simple color mixer (hue wheel + brightness slider) to add custom colors to a "My Colors" row (max 16, persisted).
- Recently used colors row.
- The current color is always visible in a large swatch.

### 3.4 Layers (simple version)
- Up to 4 layers, shown as a small vertical list with thumbnails.
- Add, delete (with confirmation), reorder, toggle visibility, rename with a kid-friendly default name ("Background", "Layer 2"…).
- Keep this panel collapsible so it does not overwhelm young kids.

### 3.5 Undo / Redo / Clear
- Huge, always-visible Undo and Redo buttons. Keyboard: Ctrl/Cmd+Z, Ctrl/Cmd+Shift+Z, Ctrl/Cmd+Y.
- "Clear" asks for confirmation with a playful dialog ("Wipe the whole picture?" with a big "Yes, wipe it!" and "No, keep it!"), and is itself undoable.

### 3.6 Gallery & Saving
- Home screen = **Gallery** of the child's drawings as a grid of thumbnails, plus a big "+ New Drawing" card.
- **Autosave**: every change is saved automatically (debounced ~1 s) and on app pause/close. There is no "Save" button for kids to forget. Never lose work, including after a crash or process death on Android.
- Project format: `.pixelpals` (JSON) containing version, size, palette, layers (base64 or run-length-encoded pixels), and a thumbnail. Include a `formatVersion` field and a migration hook.
- Duplicate, rename, and delete drawings (delete moves to a "Recycle Bin" kept for 30 days; emptying it is behind the parent gate).

### 3.7 Export & Share
- Export PNG at the chosen scale: 1×, 4×, 8×, 16× (default 8× so it's not tiny). Nearest-neighbor upscaling only.
- Option for transparent or white background.
- Android: share via system share sheet and save to Pictures. Desktop: "Save as…" dialog.
- Export work runs off the main thread.

## 4. Kid-Friendly UX Requirements

- Touch targets ≥ 56 dp; primary buttons larger. Usable by small hands on a phone and with a mouse on desktop.
- Icons always paired with short text labels (support kids who are still learning to read; labels can be hidden in settings).
- Bright, cheerful, high-contrast theme with rounded shapes. Also include a dark theme. Respect system font scaling.
- Gentle sound effects (pop on pixel place, splash on fill, whoosh on undo) with a clear mute toggle; muted by default on desktop. Generate or create original sounds; record their source/license in `docs/ASSETS.md`.
- Light haptics on Android for tool switch and fill (respect system settings).
- No timers, scores, streaks, or engagement pressure. No external links in the kid area.
- No text input required to make a drawing.
- Adaptive layout:
  - Phone portrait: canvas on top, tools in a bottom bar, palette in a scrollable row, layers in a bottom sheet.
  - Tablet / landscape / desktop: tools on the left, palette and layers on the right.
- Desktop keyboard shortcuts: B pencil, E eraser, G fill, I picker, L line, R rectangle, O circle, M mirror, V move, [ / ] brush size, H toggle grid, number keys 1–9 pick palette colors. Show a shortcuts cheat sheet (?).
- Accessibility: content descriptions on all controls, sufficient contrast, keyboard navigation on desktop.
- Localization-ready: all strings in Compose Multiplatform resources. Ship English; structure so other languages (e.g. Portuguese, Spanish) can be added by dropping in a strings file.

### 4.1 Parent Gate
- A settings area for parents behind a simple gate (e.g. "Hold this button for 3 seconds" or a small arithmetic question like "What is 7 × 6?").
- Parent settings: sound on/off, labels on/off, custom canvas sizes, empty Recycle Bin, export all drawings as a ZIP, reset app.

## 5. Phase 2 Features (after MVP is solid)

1. **Animation / frames**: add frames to a drawing, onion skinning (previous frame faint), play preview at adjustable FPS, export animated **GIF** (implement a small GIF encoder or use a lightweight, permissively licensed one) and a sprite sheet PNG.
2. **Coloring pages**: built-in original line-art templates (locked outline layer + coloring layer) — e.g. house, fish, dinosaur, robot, flower.
3. **Pixel challenges**: "Copy the picture" mode showing a small reference to recreate, with a gentle celebration (confetti) when matched. No scoring pressure.
4. **Image import**: import a photo and pixelate it to the canvas size with palette reduction (nearest palette color), as a starting point.
5. **Dither / pattern brush** (checkerboard pattern).
6. **Replace color**: swap every pixel of one color for another.

## 6. Quality Requirements

- **Tests:**
  - Unit tests in `commonTest` for: flood fill (including edge cases and large canvases), Bresenham line, ellipse, mirror mapping, undo/redo diffs, layer compositing, palette management, project serialization round-trip, format migration, PNG export dimensions.
  - Compose UI tests for: create drawing → draw → undo → autosave → reopen shows same pixels.
- **Performance:** drawing on a 128×128 canvas at max zoom stays smooth (target 60 fps) on a mid-range Android phone. Fill on 256×256 completes in under ~50 ms on desktop.
- **Robustness:** handle Android configuration changes and process death without losing the current drawing. Corrupt project files must not crash the app — show a friendly message and keep the file aside.
- **Code quality:** enable ktlint or detekt; no warnings in CI. Small, focused files. KDoc on public APIs in `core-model`.
- **CI:** a GitHub Actions workflow that builds Android debug APK and desktop distributions and runs all tests on Linux (and desktop build on macOS/Windows if practical).

## 7. Deliverables

1. Full source code in a Git repository with clean, meaningful commits per phase.
2. `README.md` with: what the app is, screenshots/GIF, how to build and run on Android and desktop, how to package installers, project structure overview.
3. `docs/ARCHITECTURE.md` – modules, state flow, rendering approach, file format spec.
4. `docs/DEPENDENCIES.md` and `docs/ASSETS.md` – every dependency/asset with its license.
5. App icon (original pixel-art style) for Android (adaptive icon) and desktop.
6. All stamps, templates, and palettes are original work authored in code or data files.

## 8. Delivery Plan

- **Phase 0 – Setup:** project skeleton, both targets launch a "Hello" screen, CI green.
- **Phase 1 – Core model:** canvas, layers, tools logic, undo/redo, with unit tests. No UI yet beyond a debug view.
- **Phase 2 – Editor UI:** canvas rendering, zoom/pan, tool bar, palette, adaptive layouts, shortcuts.
- **Phase 3 – Persistence:** gallery, autosave, project format, recycle bin.
- **Phase 4 – Export & polish:** PNG export/share, sounds, haptics, parent gate, theme, icon, accessibility pass.
- **Phase 5 – Phase 2 features:** animation + GIF, coloring pages, challenges, import, extra tools.

After each phase, report: what was built, how to run it, test results, known issues, and what's next. If any requirement conflicts with a platform limitation, pick the closest reasonable behavior, document the decision in `docs/DECISIONS.md`, and keep going.

## 9. Definition of Done (MVP)

A child can open the app, tap "+ New Drawing", pick a size, draw with pencil, fill, mirror mode and stamps, undo mistakes, close the app, reopen it to find the drawing safely in the gallery, and (with a parent) export it as a crisp PNG — on both an Android phone and a desktop computer.
