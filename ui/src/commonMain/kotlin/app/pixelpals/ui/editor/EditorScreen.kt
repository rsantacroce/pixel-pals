package app.pixelpals.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.pixelpals.model.Challenges
import app.pixelpals.model.EditorAction
import app.pixelpals.model.Tool
import app.pixelpals.ui.AppController
import app.pixelpals.ui.common.KidButton
import app.pixelpals.ui.common.KidConfirmDialog
import app.pixelpals.ui.common.Panel
import app.pixelpals.ui.common.PixelThumbnail
import app.pixelpals.ui.common.SectionTitle
import app.pixelpals.ui.common.str
import app.pixelpals.ui.platform.Sound
import app.pixelpals.ui.res.Res
import app.pixelpals.ui.res.canvas_description
import app.pixelpals.ui.res.challenge_copy_this
import app.pixelpals.ui.res.challenge_done
import app.pixelpals.ui.res.challenge_progress
import app.pixelpals.ui.res.clear
import app.pixelpals.ui.res.clear_no
import app.pixelpals.ui.res.clear_title
import app.pixelpals.ui.res.clear_yes
import app.pixelpals.ui.res.colors
import app.pixelpals.ui.res.export
import app.pixelpals.ui.res.fit_screen
import app.pixelpals.ui.res.frames
import app.pixelpals.ui.res.grid
import app.pixelpals.ui.res.help
import app.pixelpals.ui.res.home
import app.pixelpals.ui.res.layer_locked
import app.pixelpals.ui.res.layers
import app.pixelpals.ui.res.redo
import app.pixelpals.ui.res.undo
import app.pixelpals.ui.res.zoom_in
import app.pixelpals.ui.res.zoom_out
import app.pixelpals.ui.theme.PixelIcon
import kotlinx.coroutines.delay

/** Transient UI flags of the editor screen. */
private class EditorUi {
    var clear by mutableStateOf(false)
    var export by mutableStateOf(false)
    var mixer by mutableStateOf(false)
    var stamps by mutableStateOf(false)
    var shortcuts by mutableStateOf(false)
    var layersSheet by mutableStateOf(false)
    var palettes by mutableStateOf(false)
    var frames by mutableStateOf(false)
    var playing by mutableStateOf(false)
    var spaceHeld by mutableStateOf(false)
    var confetti by mutableStateOf(false)
}

/**
 * The picture editor. Adaptive: phones in portrait get canvas on top with tools at the bottom;
 * wider windows (tablets, landscape, desktop) get tools on the left and colors/layers on the right.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(app: AppController, controller: EditorController, lockedHint: Boolean, onHome: () -> Unit) {
    val ui = remember(controller) { EditorUi() }
    val d = controller.state.drawing
    val viewport = remember(controller) { Viewport(d.width, d.height) }
    var showGrid by remember(controller) { mutableStateOf(d.width <= 64 && d.height <= 64) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    LaunchedEffect(controller.challengeSolved) {
        if (controller.challengeSolved && controller.challenge != null) {
            ui.confetti = true
            app.play(Sound.TADA)
        }
    }
    val palette = controller.activePalette()

    fun onKey(e: KeyEvent): Boolean {
        if (e.key == Key.Spacebar) {
            ui.spaceHeld = e.type == KeyEventType.KeyDown
            return true
        }
        if (e.type != KeyEventType.KeyDown) return false
        val cmd = e.isCtrlPressed || e.isMetaPressed
        val action: EditorAction? =
            when {
                cmd && e.key == Key.Z && e.isShiftPressed -> EditorAction.Redo
                cmd && e.key == Key.Z -> EditorAction.Undo
                cmd && e.key == Key.Y -> EditorAction.Redo
                cmd -> return false
                e.key == Key.B -> EditorAction.SelectTool(Tool.PENCIL)
                e.key == Key.E -> EditorAction.SelectTool(Tool.ERASER)
                e.key == Key.G -> EditorAction.SelectTool(Tool.FILL)
                e.key == Key.I -> EditorAction.SelectTool(Tool.PICKER)
                e.key == Key.L -> EditorAction.SelectTool(Tool.LINE)
                e.key == Key.R -> EditorAction.SelectTool(Tool.RECTANGLE)
                e.key == Key.O -> EditorAction.SelectTool(Tool.ELLIPSE)
                e.key == Key.V -> EditorAction.SelectTool(Tool.MOVE)
                e.key == Key.M -> EditorAction.ToggleMirrorHorizontal
                e.key == Key.LeftBracket -> EditorAction.SetBrushSize(controller.state.settings.brushSize - 1)
                e.key == Key.RightBracket -> EditorAction.SetBrushSize(controller.state.settings.brushSize + 1)
                else -> null
            }
        if (action != null) {
            controller.dispatch(action)
            return true
        }
        val digit = DIGIT_KEYS.indexOf(e.key)
        when {
            digit >= 0 -> palette.getOrNull(digit)?.let { controller.dispatch(EditorAction.SetColor(it)) }
            e.key == Key.H -> showGrid = !showGrid
            e.key == Key.Slash && e.isShiftPressed -> ui.shortcuts = true
            e.key == Key.Equals || e.key == Key.Plus || e.key == Key.NumPadAdd -> viewport.zoom(1.25f)
            e.key == Key.Minus || e.key == Key.NumPadSubtract -> viewport.zoom(0.8f)
            e.key == Key.Zero -> viewport.fit()
            else -> return false
        }
        return true
    }

    val description = str(Res.string.canvas_description, d.width, d.height)
    var compact by remember { mutableStateOf(false) }
    val canvasArea: @Composable (Modifier) -> Unit = { mod ->
        Box(mod) {
            PixelCanvas(
                controller,
                viewport,
                showGrid,
                inputEnabled = !ui.playing,
                spaceHeld = { ui.spaceHeld },
                description = description,
                modifier = Modifier.fillMaxSize(),
            )
            ViewControls(viewport, showGrid, {
                showGrid = !showGrid
            }, compact, Modifier.align(if (compact) Alignment.BottomEnd else Alignment.TopEnd).padding(6.dp))
            controller.challenge?.let { ChallengeCard(controller, it, Modifier.align(Alignment.TopStart).padding(6.dp)) }
            if (lockedHint) {
                Surface(
                    Modifier.align(Alignment.BottomCenter).padding(12.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text(str(Res.string.layer_locked), Modifier.padding(12.dp), style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .onPreviewKeyEvent(::onKey)
            .focusRequester(focus)
            .focusable()
            .testTag("editor"),
    ) {
        // Side panels for tablets, desktop windows and phones in landscape; stacked layout for portrait phones.
        val wide = maxWidth >= 840.dp || (maxWidth >= 600.dp && maxWidth > maxHeight)
        compact = !wide || maxHeight < 560.dp
        val sidePanel = if (maxWidth >= 1000.dp) 300.dp else 250.dp
        Column(Modifier.fillMaxSize()) {
            TopBar(app, controller, ui, wide, onHome)
            if (wide) {
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    Panel(Modifier.fillMaxHeight().padding(6.dp)) {
                        ToolButtons(controller, vertical = true, onOpenStamps = { ui.stamps = true })
                    }
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        canvasArea(Modifier.weight(1f).fillMaxWidth())
                        ToolOptions(controller)
                        AnimatedVisibility(ui.frames || d.frames.size > 1) {
                            FramesBar(controller, ui.playing, { ui.playing = it })
                        }
                    }
                    Panel(Modifier.width(sidePanel).fillMaxHeight().padding(6.dp)) {
                        Column(Modifier.verticalScroll(rememberScrollState())) {
                            ColorPanel(controller, app.settings.customColors, onMix = { ui.mixer = true })
                            LayersSection(app, controller)
                        }
                    }
                }
            } else {
                canvasArea(Modifier.weight(1f).fillMaxWidth())
                ToolOptions(controller)
                AnimatedVisibility(ui.frames || d.frames.size > 1) {
                    FramesBar(controller, ui.playing, { ui.playing = it })
                }
                PaletteStrip(controller, app.settings.customColors, onMix = { ui.mixer = true }, onPalettes = { ui.palettes = true })
                Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                    ToolButtons(controller, vertical = false, onOpenStamps = { ui.stamps = true }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
        if (ui.confetti) ConfettiOverlay { ui.confetti = false }
    }

    if (ui.clear) {
        KidConfirmDialog(
            title = str(Res.string.clear_title),
            yes = str(Res.string.clear_yes),
            no = str(Res.string.clear_no),
            onYes = { controller.dispatch(EditorAction.Clear) },
            onDismiss = { ui.clear = false },
        )
    }
    if (ui.export) ExportDialog(app, controller) { ui.export = false }
    if (ui.stamps) StampPickerDialog(controller) { ui.stamps = false }
    if (ui.shortcuts) ShortcutsDialog { ui.shortcuts = false }
    if (ui.mixer) {
        ColorMixerDialog(
            controller.state.color,
            onUse = { controller.dispatch(EditorAction.SetColor(it)) },
            onAddToMyColors = {
                app.addCustomColor(it)
                controller.dispatch(EditorAction.SetColor(it))
            },
            onDismiss = { ui.mixer = false },
        )
    }
    if (ui.layersSheet) {
        ModalBottomSheet(onDismissRequest = { ui.layersSheet = false }) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) { LayersPanel(controller) }
        }
    }
    if (ui.palettes) {
        ModalBottomSheet(onDismissRequest = { ui.palettes = false }) {
            Column(Modifier.padding(16.dp).padding(bottom = 24.dp)) {
                SectionTitle(str(Res.string.colors))
                PaletteChooser(controller) { ui.palettes = false }
            }
        }
    }
}

private val DIGIT_KEYS = listOf(Key.One, Key.Two, Key.Three, Key.Four, Key.Five, Key.Six, Key.Seven, Key.Eight, Key.Nine)

@Composable
private fun TopBar(app: AppController, controller: EditorController, ui: EditorUi, wide: Boolean, onHome: () -> Unit) {
    val history = controller.state.history
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            KidButton(PixelIcon.HOME, str(Res.string.home), onHome)
            Row(
                Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (wide) {
                    Text(
                        controller.project.name,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 12.dp).width(220.dp),
                    )
                }
                KidButton(PixelIcon.TRASH, str(Res.string.clear), { ui.clear = true })
                KidButton(PixelIcon.EXPORT, str(Res.string.export), { ui.export = true })
                if (!wide) KidButton(PixelIcon.LAYERS, str(Res.string.layers), { ui.layersSheet = true })
                KidButton(PixelIcon.FILM, str(Res.string.frames), { ui.frames = !ui.frames }, selected = ui.frames)
                if (app.platform.isDesktop) KidButton(PixelIcon.HELP, str(Res.string.help), { ui.shortcuts = true })
            }
            val big = MaterialTheme.colorScheme.tertiaryContainer
            val size = if (wide) 72.dp else 64.dp
            KidButton(
                PixelIcon.UNDO,
                str(Res.string.undo),
                { controller.dispatch(EditorAction.Undo) },
                enabled = history.canUndo,
                iconSize = 36.dp,
                minSize = size,
                container = big,
                modifier = Modifier.testTag("undo"),
            )
            KidButton(
                PixelIcon.REDO,
                str(Res.string.redo),
                { controller.dispatch(EditorAction.Redo) },
                enabled = history.canRedo,
                iconSize = 36.dp,
                minSize = size,
                container = big,
                modifier = Modifier.padding(start = 4.dp).testTag("redo"),
            )
        }
    }
}

@Composable
private fun LayersSection(app: AppController, controller: EditorController) {
    val open = app.settings.layersPanelOpen
    Column(Modifier.padding(horizontal = 8.dp)) {
        KidButton(
            PixelIcon.LAYERS,
            str(Res.string.layers),
            { app.updateSettings { it.copy(layersPanelOpen = !open) } },
            selected = open,
            modifier = Modifier.fillMaxWidth(),
        )
        AnimatedVisibility(open) { LayersPanel(controller) }
    }
}

@Composable
private fun ViewControls(viewport: Viewport, showGrid: Boolean, onToggleGrid: () -> Unit, compact: Boolean, modifier: Modifier) {
    // On phones the controls sit in a slim icon-only row (pinch-zoom is the main way to zoom there).
    val buttons: @Composable () -> Unit = {
        KidButton(PixelIcon.ZOOM_IN, str(Res.string.zoom_in), { viewport.zoom(1.25f) }, iconSize = 22.dp, showLabel = !compact)
        KidButton(PixelIcon.ZOOM_OUT, str(Res.string.zoom_out), { viewport.zoom(0.8f) }, iconSize = 22.dp, showLabel = !compact)
        KidButton(PixelIcon.FIT, str(Res.string.fit_screen), { viewport.fit() }, iconSize = 22.dp, showLabel = !compact)
        KidButton(PixelIcon.GRID, str(Res.string.grid), onToggleGrid, selected = showGrid, iconSize = 22.dp, showLabel = !compact)
    }
    Surface(
        modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        tonalElevation = 2.dp,
    ) {
        if (compact) Row { buttons() } else Column { buttons() }
    }
}

@Composable
private fun ChallengeCard(controller: EditorController, challenge: Challenges.Challenge, modifier: Modifier) {
    val progress = (Challenges.progress(controller.state.drawing, controller.state.frameIndex, challenge.target) * 100).toInt()
    Surface(
        modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 3.dp,
    ) {
        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                str(if (controller.challengeSolved) Res.string.challenge_done else Res.string.challenge_copy_this),
                style = MaterialTheme.typography.titleSmall,
            )
            PixelThumbnail(challenge.target, Modifier.size(96.dp))
            Text(str(Res.string.challenge_progress, progress), style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** Shows the "layer is locked" hint briefly. */
@Composable
fun rememberLockedHint(trigger: Int): Boolean {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        visible = true
        delay(2500)
        visible = false
    }
    return visible
}
