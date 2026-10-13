package app.pixelpals.model

/** Handles pointer gestures for every tool. One gesture = at most one undo step. */
internal object StrokeEngine {
    fun press(state: EditorState, x: Int, y: Int): Reduction {
        val base = EditorReducer.cancelled(state)
        val tool = base.settings.tool
        if (tool == Tool.PICKER) return pick(base, x, y)
        if (base.layer.locked) return Reduction(base, effects = setOf(EditorEffect.BLOCKED))
        val d = base.drawing
        // Drawing on a hidden layer would be invisible and confusing, so reveal it first.
        val drawing =
            if (base.layer.visible) {
                d
            } else {
                d.copy(layers = d.layers.map { if (it.id == base.layerId) it.copy(visible = true) else it })
            }
        val paints = tool != Tool.ERASER && tool != Tool.MOVE
        val ready =
            base.copy(
                drawing = drawing,
                recentColors = if (paints) ColorMemory.pushRecent(base.recentColors, base.color) else base.recentColors,
            )
        val revealed = drawing !== d
        return when (tool) {
            Tool.FILL -> fill(ready, x, y, revealed)
            Tool.REPLACE -> replace(ready, x, y, revealed)
            else -> {
                val stroke =
                    Stroke(
                        tool = tool,
                        frameIndex = ready.frameIndex,
                        layerId = ready.layerId,
                        original = drawing.cel(ready.frameIndex, ready.layerId),
                        startX = x,
                        startY = y,
                        lastX = x,
                        lastY = y,
                        previewIndices = IntArray(0),
                    )
                val r = step(ready.copy(stroke = stroke), x, y, first = true)
                val effects = if (tool == Tool.MOVE) emptySet() else setOf(EditorEffect.POP)
                Reduction(r.state, if (revealed) Changed.All else r.changed, effects)
            }
        }
    }

    fun drag(state: EditorState, x: Int, y: Int): Reduction {
        val stroke = state.stroke ?: return Reduction(state)
        if (stroke.tool == Tool.PICKER) return pick(state, x, y)
        if (x == stroke.lastX && y == stroke.lastY) return Reduction(state)
        return step(state, x, y, first = false)
    }

    fun release(state: EditorState): Reduction {
        val stroke = state.stroke ?: return Reduction(state)
        if (stroke.tool == Tool.PICKER) {
            val s = state.settings
            return Reduction(state.copy(stroke = null, settings = s.copy(tool = s.previousTool)), effects = setOf(EditorEffect.TOOL))
        }
        val current = state.drawing.cel(stroke.frameIndex, stroke.layerId)
        val diff = PixelDiff.between(state.drawing.frames[stroke.frameIndex].id, stroke.layerId, stroke.original, current)
        val history = if (diff != null) state.history.push(diff) else state.history
        val effects = if (stroke.tool == Tool.STAMP && diff != null) setOf(EditorEffect.POP) else emptySet()
        return Reduction(state.copy(stroke = null, history = history), effects = effects)
    }

    fun cancel(state: EditorState): Reduction {
        val stroke = state.stroke ?: return Reduction(state)
        if (stroke.tool == Tool.PICKER) return Reduction(state.copy(stroke = null))
        val current = state.drawing.cel(stroke.frameIndex, stroke.layerId)
        val restored = state.copy(stroke = null, drawing = state.drawing.withCel(stroke.frameIndex, stroke.layerId, stroke.original))
        return Reduction(restored, changedBetween(current, stroke.original, null))
    }

    private fun step(state: EditorState, x: Int, y: Int, first: Boolean): Reduction {
        val stroke = state.stroke!!
        val d = state.drawing
        val s = state.settings
        val current = d.cel(stroke.frameIndex, stroke.layerId)
        val color = if (stroke.tool == Tool.ERASER) Argb.TRANSPARENT else state.color
        val (next, candidates, preview) =
            when (stroke.tool) {
                Tool.PENCIL, Tool.ERASER, Tool.DITHER -> {
                    val path = if (first) listOf(Point(x, y)) else Raster.line(stroke.lastX, stroke.lastY, x, y)
                    val pts = s.mirror.mapAll(path.flatMap { Raster.brush(it.x, it.y, s.brushSize) }, d.width, d.height)
                    val idx = paint(current, pts, color, dither = stroke.tool == Tool.DITHER)
                    Triple(current.withChanges(idx, IntArray(idx.size) { color }), idx, IntArray(0))
                }
                Tool.LINE, Tool.RECTANGLE, Tool.ELLIPSE -> {
                    val shape =
                        when (stroke.tool) {
                            Tool.LINE -> Raster.line(stroke.startX, stroke.startY, x, y).flatMap { Raster.brush(it.x, it.y, s.brushSize) }
                            Tool.RECTANGLE -> Raster.rectangle(stroke.startX, stroke.startY, x, y, s.shapeFilled)
                            else -> Raster.ellipse(stroke.startX, stroke.startY, x, y, s.shapeFilled)
                        }
                    previewFrom(stroke, s.mirror.mapAll(shape, d.width, d.height), color)
                }
                Tool.STAMP -> {
                    val stamp = Stamps.byId(s.stampId) ?: Stamps.all.first()
                    previewStamp(stroke, stamp.pixels, x, y, s.mirror)
                }
                Tool.MOVE -> {
                    val moved = stroke.original.shifted(x - stroke.startX, y - stroke.startY)
                    Triple(moved, null, IntArray(0))
                }
                Tool.FILL, Tool.PICKER, Tool.REPLACE -> return Reduction(state)
            }
        val newStroke =
            Stroke(
                stroke.tool,
                stroke.frameIndex,
                stroke.layerId,
                stroke.original,
                stroke.startX,
                stroke.startY,
                x,
                y,
                preview,
            )
        val newState = state.copy(drawing = d.withCel(stroke.frameIndex, stroke.layerId, next), stroke = newStroke)
        val changed = if (stroke.tool == Tool.MOVE) Changed.All else changedBetween(current, next, candidates)
        return Reduction(newState, changed)
    }

    /** Paints [shape] onto the stroke's original buffer, returning (buffer, candidate indices, preview indices). */
    private fun previewFrom(stroke: Stroke, shape: List<Point>, color: Int): Triple<PixelBuffer, IntArray, IntArray> {
        val idx = paint(stroke.original, shape, color, dither = false)
        val next = stroke.original.withChanges(idx, IntArray(idx.size) { color })
        return Triple(next, stroke.previewIndices + idx, idx)
    }

    private fun previewStamp(
        stroke: Stroke,
        stamp: PixelBuffer,
        cx: Int,
        cy: Int,
        mirror: MirrorMode,
    ): Triple<PixelBuffer, IntArray, IntArray> {
        val base = stroke.original
        val w = base.width
        val h = base.height
        val ox = cx - stamp.width / 2
        val oy = cy - stamp.height / 2
        val changes = LinkedHashMap<Int, Int>()
        for (sy in 0 until stamp.height) {
            for (sx in 0 until stamp.width) {
                val c = stamp[sy * stamp.width + sx]
                if (c == Argb.TRANSPARENT) continue
                for (p in mirror.map(Point(ox + sx, oy + sy), w, h)) {
                    if (base.contains(p.x, p.y)) changes[p.y * w + p.x] = c
                }
            }
        }
        val idx = changes.keys.toIntArray()
        val next = base.withChanges(idx, changes.values.toIntArray())
        return Triple(next, stroke.previewIndices + idx, idx)
    }

    private fun fill(state: EditorState, x: Int, y: Int, revealed: Boolean): Reduction {
        val d = state.drawing
        if (x !in 0 until d.width || y !in 0 until d.height) return Reduction(state, if (revealed) Changed.All else Changed.None)
        // Regions follow what the child sees (all visible layers), but paint goes on the current layer.
        val composite = d.composite(state.frameIndex)
        val seeds = state.settings.mirror.map(Point(x, y), d.width, d.height)
        val region = LinkedHashSet<Int>()
        for (p in seeds) {
            if (composite[p.y * d.width + p.x] == state.color) continue
            FloodFill.region(composite, d.width, d.height, p.x, p.y).forEach { region += it }
        }
        val before = d.cel(state.frameIndex, state.layerId)
        val idx = region.toIntArray()
        val after = before.withChanges(idx, IntArray(idx.size) { state.color })
        val diff =
            PixelDiff.between(state.frame.id, state.layerId, before, after)
                ?: return Reduction(state, if (revealed) Changed.All else Changed.None)
        val next =
            state.copy(
                drawing = diff.redo(d),
                history = state.history.push(diff),
            )
        return Reduction(next, if (revealed) Changed.All else Changed.Pixels(diff.indices), setOf(EditorEffect.SPLASH))
    }

    private fun replace(state: EditorState, x: Int, y: Int, revealed: Boolean): Reduction {
        val buffer = state.drawing.cel(state.frameIndex, state.layerId)
        if (!buffer.contains(x, y)) return Reduction(state)
        val r = EditorReducer.replaceColor(state, buffer.at(x, y), state.color)
        return if (revealed) Reduction(r.state, Changed.All, r.effects) else r
    }

    private fun pick(state: EditorState, x: Int, y: Int): Reduction {
        val d = state.drawing
        val stroke =
            state.stroke
                ?: Stroke(Tool.PICKER, state.frameIndex, state.layerId, d.cel(state.frameIndex, state.layerId), x, y, x, y, IntArray(0))
        val withStroke = state.copy(stroke = stroke)
        if (x !in 0 until d.width || y !in 0 until d.height) return Reduction(withStroke)
        val c = d.compositePixel(state.frameIndex, y * d.width + x)
        if (c == Argb.TRANSPARENT || c == state.color) return Reduction(withStroke)
        return Reduction(withStroke.copy(color = c), effects = setOf(EditorEffect.PICK))
    }

    /** Indices in [points] (clipped to [buffer]) that the paint would change. */
    private fun paint(buffer: PixelBuffer, points: List<Point>, color: Int, dither: Boolean): IntArray {
        val out = IntList(points.size)
        val seen = HashSet<Int>()
        for (p in points) {
            if (!buffer.contains(p.x, p.y)) continue
            if (dither && (p.x + p.y) % 2 != 0) continue
            val i = p.y * buffer.width + p.x
            if (buffer[i] != color && seen.add(i)) out.add(i)
        }
        return out.toIntArray()
    }

    private fun changedBetween(a: PixelBuffer, b: PixelBuffer, candidates: IntArray?): Changed {
        if (candidates == null) return Changed.All
        val out = IntList(candidates.size)
        for (i in candidates) if (a[i] != b[i]) out.add(i)
        return if (out.size == 0) Changed.None else Changed.Pixels(out.toIntArray())
    }
}
