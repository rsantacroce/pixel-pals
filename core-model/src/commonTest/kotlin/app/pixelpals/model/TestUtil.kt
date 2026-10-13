package app.pixelpals.model

internal const val RED: Int = 0xFFFF0000.toInt()
internal const val BLUE: Int = 0xFF0000FF.toInt()

internal fun editor(size: Int = 16): EditorState = EditorState(Drawing.blank(size, size, "Background"), color = RED)

internal fun EditorState.run(vararg actions: EditorAction): EditorState = actions.fold(this) { s, a -> EditorReducer.reduce(s, a).state }

internal fun EditorState.pixel(x: Int, y: Int): Int = drawing.cel(frameIndex, layerId).at(x, y)

internal fun EditorState.tool(t: Tool): EditorState = run(EditorAction.SelectTool(t))

internal fun EditorState.tap(x: Int, y: Int): EditorState = run(EditorAction.Press(x, y), EditorAction.Release)

internal fun EditorState.drag(x0: Int, y0: Int, x1: Int, y1: Int): EditorState =
    run(EditorAction.Press(x0, y0), EditorAction.Drag(x1, y1), EditorAction.Release)

internal fun EditorState.painted(): Int = drawing.cel(frameIndex, layerId).toIntArray().count { it != 0 }
