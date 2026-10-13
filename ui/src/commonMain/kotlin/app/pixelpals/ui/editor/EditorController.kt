package app.pixelpals.ui.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import app.pixelpals.io.Autosaver
import app.pixelpals.io.Project
import app.pixelpals.io.currentTimeMillis
import app.pixelpals.model.Challenges
import app.pixelpals.model.Changed
import app.pixelpals.model.EditorAction
import app.pixelpals.model.EditorEffect
import app.pixelpals.model.EditorReducer
import app.pixelpals.model.EditorState
import app.pixelpals.model.Palettes
import app.pixelpals.model.Tool
import app.pixelpals.model.ToolSettings

/**
 * State holder for one open drawing (unidirectional data flow): the UI sends [EditorAction]s to
 * [dispatch]; the pure [EditorReducer] returns the next immutable [EditorState]; the controller then
 * updates the bitmap incrementally, plays feedback and schedules an autosave.
 */
class EditorController(
    initial: Project,
    private val autosaver: Autosaver,
    private val onEffects: (Set<EditorEffect>) -> Unit,
) {
    var project: Project by mutableStateOf(initial)
        private set

    var state: EditorState by mutableStateOf(
        EditorState(
            drawing = initial.drawing,
            color =
                (initial.challengeId?.let { Challenges.byId(it)?.palette?.firstOrNull() })
                    ?: Palettes.byId(initial.paletteId).colors.first(),
            // Coloring pages start with the bucket: tap a shape to color it in.
            settings = ToolSettings(tool = if (initial.templateId != null) Tool.FILL else Tool.PENCIL),
        ),
    )
        private set

    val renderer = CanvasRenderer(initial.drawing.width, initial.drawing.height)

    /** The drawing last handed to the autosaver. */
    private var savedDrawing = initial.drawing

    /** Faint copy of the previous frame for onion skinning; null when not applicable. */
    var onion: ImageBitmap? by mutableStateOf(null)
        private set

    /** The challenge being attempted, if any. */
    val challenge: Challenges.Challenge? = initial.challengeId?.let { Challenges.byId(it) }

    /** True once the challenge picture has been matched (shows the celebration once). */
    var challengeSolved: Boolean by mutableStateOf(false)
        private set

    init {
        renderer.update(state.drawing, state.frameIndex, Changed.All)
        refreshOnion()
        challengeSolved = challenge?.let { Challenges.isMatched(state.drawing, 0, it.target) } ?: false
    }

    fun dispatch(action: EditorAction) {
        val r = EditorReducer.reduce(state, action)
        state = r.state
        renderer.update(r.state.drawing, r.state.frameIndex, r.changed)
        if (r.changed == Changed.All) refreshOnion()
        if (r.effects.isNotEmpty()) onEffects(r.effects)
        // Strokes change the drawing during drags; save once the gesture has finished.
        if (r.state.stroke == null && r.state.drawing !== savedDrawing) {
            savedDrawing = r.state.drawing
            changed()
        }
    }

    /** Switches the shown palette (persisted with the drawing). */
    fun setPalette(id: String) {
        if (id == project.paletteId) return
        project = project.copy(paletteId = id)
        autosaver.schedule(project.copy(drawing = state.drawing))
    }

    /** Renames the drawing. */
    fun rename(name: String) {
        project = project.copy(name = name)
        autosaver.schedule(project.copy(drawing = state.drawing, updatedAt = currentTimeMillis()))
    }

    /** Current drawing as a project ready to save. */
    fun snapshot(): Project = project.copy(drawing = state.drawing)

    private fun changed() {
        project = project.copy(drawing = state.drawing, updatedAt = currentTimeMillis())
        autosaver.schedule(project)
        val ch = challenge ?: return
        if (!challengeSolved && Challenges.isMatched(state.drawing, state.frameIndex, ch.target)) {
            challengeSolved = true
        }
    }

    private fun refreshOnion() {
        val s = state
        onion =
            if (s.onionSkin && s.frameIndex > 0) {
                CanvasRenderer.bitmapOf(s.drawing.width, s.drawing.height, s.drawing.composite(s.frameIndex - 1))
            } else {
                null
            }
    }
}
