package app.pixelpals.model

/** Structural edits to layers and frames. Each one is a single undoable [StructureChange]. */
internal object LayerFrameOps {
    fun reduce(state: EditorState, action: EditorAction): Reduction {
        val d = state.drawing
        val result: Pair<Drawing, EditorState.() -> EditorState>? =
            when (action) {
                is EditorAction.AddLayer -> addLayer(state, action.name)
                is EditorAction.DeleteLayer -> deleteLayer(d, action.layerId)
                is EditorAction.MoveLayer -> moveLayer(d, action.layerId, action.delta)
                is EditorAction.ToggleLayerVisible -> mapLayer(d, action.layerId) { it.copy(visible = !it.visible) }
                is EditorAction.RenameLayer -> {
                    val name = action.name.trim().take(MAX_NAME)
                    if (name.isEmpty()) null else mapLayer(d, action.layerId) { it.copy(name = name) }
                }
                EditorAction.AddFrame -> addFrame(state, duplicate = false)
                EditorAction.DuplicateFrame -> addFrame(state, duplicate = true)
                EditorAction.DeleteFrame -> deleteFrame(state)
                is EditorAction.MoveFrame -> moveFrame(state, action.delta)
                else -> null
            }
        result ?: return Reduction(state)
        val (drawing, select) = result
        val command = StructureChange(d, drawing)
        val next = EditorReducer.reconcile(state.copy(drawing = drawing, history = state.history.push(command)).select())
        return Reduction(next, Changed.All)
    }

    private fun addLayer(state: EditorState, name: String): Pair<Drawing, EditorState.() -> EditorState>? {
        val d = state.drawing
        if (d.layers.size >= Drawing.MAX_LAYERS) return null
        val id = d.nextId
        val at = d.layers.indexOfFirst { it.id == state.layerId } + 1
        val layers = d.layers.toMutableList().apply { add(at.coerceIn(0, size), LayerInfo(id, name.take(MAX_NAME))) }
        val blank = PixelBuffer.blank(d.width, d.height)
        val frames = d.frames.map { it.copy(cels = it.cels + (id to blank)) }
        return d.copy(layers = layers, frames = frames, nextId = id + 1) to { copy(layerId = id) }
    }

    private fun deleteLayer(d: Drawing, layerId: Int): Pair<Drawing, EditorState.() -> EditorState>? {
        val index = d.layers.indexOfFirst { it.id == layerId }
        if (index < 0 || d.layers.size <= 1 || d.layers[index].locked) return null
        val layers = d.layers.filterNot { it.id == layerId }
        val frames = d.frames.map { it.copy(cels = it.cels - layerId) }
        val neighbor = layers[(index - 1).coerceAtLeast(0)].id
        return d.copy(layers = layers, frames = frames) to { if (this.layerId == layerId) copy(layerId = neighbor) else this }
    }

    private fun moveLayer(d: Drawing, layerId: Int, delta: Int): Pair<Drawing, EditorState.() -> EditorState>? {
        val from = d.layers.indexOfFirst { it.id == layerId }
        val to = from + delta
        if (from < 0 || to !in d.layers.indices) return null
        val layers = d.layers.toMutableList().apply { add(to, removeAt(from)) }
        return d.copy(layers = layers) to { this }
    }

    private fun mapLayer(d: Drawing, layerId: Int, f: (LayerInfo) -> LayerInfo): Pair<Drawing, EditorState.() -> EditorState>? {
        if (d.layer(layerId) == null) return null
        return d.copy(layers = d.layers.map { if (it.id == layerId) f(it) else it }) to { this }
    }

    private fun addFrame(state: EditorState, duplicate: Boolean): Pair<Drawing, EditorState.() -> EditorState>? {
        val d = state.drawing
        if (d.frames.size >= Drawing.MAX_FRAMES) return null
        val current = state.frame
        val blank = PixelBuffer.blank(d.width, d.height)
        // Locked layers (coloring-page outlines) carry over so every frame keeps its outline.
        val cels = d.layers.associate { l -> l.id to if (duplicate || l.locked) current.cels.getValue(l.id) else blank }
        val frame = Frame(d.nextId, cels)
        val at = state.frameIndex + 1
        val frames = d.frames.toMutableList().apply { add(at, frame) }
        return d.copy(frames = frames, nextId = d.nextId + 1) to { copy(frameIndex = at) }
    }

    private fun deleteFrame(state: EditorState): Pair<Drawing, EditorState.() -> EditorState>? {
        val d = state.drawing
        if (d.frames.size <= 1) return null
        val frames = d.frames.toMutableList().apply { removeAt(state.frameIndex) }
        return d.copy(frames = frames) to { copy(frameIndex = (frameIndex - 1).coerceAtLeast(0)) }
    }

    private fun moveFrame(state: EditorState, delta: Int): Pair<Drawing, EditorState.() -> EditorState>? {
        val d = state.drawing
        val to = state.frameIndex + delta
        if (to !in d.frames.indices) return null
        val frames = d.frames.toMutableList().apply { add(to, removeAt(state.frameIndex)) }
        return d.copy(frames = frames) to { copy(frameIndex = to) }
    }

    const val MAX_NAME = 24
}
