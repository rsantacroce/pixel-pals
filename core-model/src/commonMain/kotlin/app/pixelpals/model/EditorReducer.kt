package app.pixelpals.model

/**
 * Pure state transitions for the editor: `reduce(state, action) -> Reduction`.
 * Drawing gestures go through [StrokeEngine]; everything else is handled here.
 */
public object EditorReducer {
    /** Applies [action] to [state]. Never throws for out-of-range input; invalid requests are ignored. */
    public fun reduce(state: EditorState, action: EditorAction): Reduction =
        when (action) {
            is EditorAction.Press -> StrokeEngine.press(state, action.x, action.y)
            is EditorAction.Drag -> StrokeEngine.drag(state, action.x, action.y)
            EditorAction.Release -> StrokeEngine.release(state)
            EditorAction.CancelStroke -> StrokeEngine.cancel(state)
            is EditorAction.SelectTool -> selectTool(state, action.tool)
            is EditorAction.SetBrushSize ->
                settings(state) { it.copy(brushSize = action.size.coerceIn(ToolSettings.BRUSH_SIZES)) }
            EditorAction.ToggleMirrorHorizontal ->
                settings(state) { it.copy(mirror = it.mirror.copy(horizontal = !it.mirror.horizontal)) }
            EditorAction.ToggleMirrorVertical ->
                settings(state) { it.copy(mirror = it.mirror.copy(vertical = !it.mirror.vertical)) }
            is EditorAction.SetShapeFilled -> settings(state) { it.copy(shapeFilled = action.filled) }
            is EditorAction.SelectStamp ->
                if (Stamps.byId(action.stampId) == null) {
                    Reduction(state)
                } else {
                    Reduction(
                        state.copy(settings = state.settings.copy(tool = Tool.STAMP, stampId = action.stampId)),
                        effects = setOf(EditorEffect.TOOL),
                    )
                }
            is EditorAction.SetColor -> Reduction(state.copy(color = action.color))
            EditorAction.Undo -> undoRedo(state, undo = true)
            EditorAction.Redo -> undoRedo(state, undo = false)
            EditorAction.Clear -> clear(state)
            is EditorAction.ImportPixels -> importPixels(state, action.pixels)
            is EditorAction.ReplaceColor -> replaceColor(state, action.from, action.to)
            EditorAction.ToggleOnionSkin -> Reduction(state.copy(onionSkin = !state.onionSkin), Changed.All)
            is EditorAction.SetFps -> Reduction(state.copy(drawing = state.drawing.copy(fps = action.fps.coerceIn(1, 24))))
            is EditorAction.SelectLayer ->
                if (state.drawing.layer(action.layerId) == null) Reduction(state) else Reduction(state.copy(layerId = action.layerId))
            is EditorAction.SelectFrame ->
                if (action.index !in state.drawing.frames.indices) {
                    Reduction(state)
                } else {
                    Reduction(cancelled(state).copy(frameIndex = action.index), Changed.All)
                }
            else -> LayerFrameOps.reduce(cancelled(state), action)
        }

    private fun settings(state: EditorState, f: (ToolSettings) -> ToolSettings) = Reduction(state.copy(settings = f(state.settings)))

    private fun selectTool(state: EditorState, tool: Tool): Reduction {
        val s = state.settings
        val previous = if (s.tool != Tool.PICKER) s.tool else s.previousTool
        return Reduction(
            cancelled(state).copy(settings = s.copy(tool = tool, previousTool = previous)),
            effects = setOf(EditorEffect.TOOL),
        )
    }

    /** Returns [state] with any in-progress gesture rolled back. */
    internal fun cancelled(state: EditorState): EditorState = StrokeEngine.cancel(state).state

    private fun undoRedo(state: EditorState, undo: Boolean): Reduction {
        val base = cancelled(state)
        val result = if (undo) base.history.undo(base.drawing) else base.history.redo(base.drawing)
        result ?: return Reduction(base, if (base === state) Changed.None else Changed.All)
        val (drawing, history) = result
        val command = if (undo) base.history.undoStack.last() else base.history.redoStack.last()
        val currentFrameId = base.frame.id
        val changed =
            if (command is PixelDiff && command.frameId == currentFrameId && drawing.frames.size == base.drawing.frames.size) {
                Changed.Pixels(command.indices)
            } else {
                Changed.All
            }
        return Reduction(reconcile(base.copy(drawing = drawing, history = history)), changed, setOf(EditorEffect.WHOOSH))
    }

    private fun clear(state: EditorState): Reduction {
        val base = cancelled(state)
        val d = base.drawing
        val blank = PixelBuffer.blank(d.width, d.height)
        val diffs =
            d.layers.filter {
                !it.locked
            }.mapNotNull { PixelDiff.between(base.frame.id, it.id, d.cel(base.frameIndex, it.id), blank) }
        if (diffs.isEmpty()) return Reduction(base)
        val command = CompoundCommand(diffs)
        return Reduction(
            base.copy(drawing = command.redo(d), history = base.history.push(command)),
            Changed.All,
            setOf(EditorEffect.WIPE),
        )
    }

    private fun importPixels(state: EditorState, pixels: IntArray): Reduction {
        val base = cancelled(state)
        val d = base.drawing
        if (pixels.size != d.width * d.height || base.layer.locked) return Reduction(base)
        val diff =
            PixelDiff.between(
                base.frame.id,
                base.layer.id,
                d.cel(base.frameIndex, base.layer.id),
                PixelBuffer.of(d.width, d.height, pixels),
            )
                ?: return Reduction(base)
        return Reduction(base.copy(drawing = diff.redo(d), history = base.history.push(diff)), Changed.All, setOf(EditorEffect.SPLASH))
    }

    internal fun replaceColor(state: EditorState, from: Int, to: Int): Reduction {
        val base = cancelled(state)
        if (from == to || base.layer.locked) return Reduction(base)
        val d = base.drawing
        val before = d.cel(base.frameIndex, base.layer.id)
        val after = before.edit { px -> for (i in px.indices) if (px[i] == from) px[i] = to }
        val diff = PixelDiff.between(base.frame.id, base.layer.id, before, after) ?: return Reduction(base)
        return Reduction(
            base.copy(drawing = diff.redo(d), history = base.history.push(diff)),
            Changed.Pixels(diff.indices),
            setOf(EditorEffect.SPLASH),
        )
    }

    /** Fixes up selections after the drawing structure changed (e.g. by undo). */
    internal fun reconcile(state: EditorState): EditorState {
        val d = state.drawing
        val frameIndex = state.frameIndex.coerceIn(0, d.frames.size - 1)
        val layerId = if (d.layer(state.layerId) != null) state.layerId else EditorState.defaultLayer(d)
        return if (frameIndex == state.frameIndex &&
            layerId == state.layerId
        ) {
            state
        } else {
            state.copy(frameIndex = frameIndex, layerId = layerId)
        }
    }
}
