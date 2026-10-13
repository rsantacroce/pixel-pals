package app.pixelpals.model

/** Complete, immutable state of the picture editor. */
public data class EditorState(
    val drawing: Drawing,
    val frameIndex: Int = 0,
    /** Layer that tools draw on. */
    val layerId: Int = defaultLayer(drawing),
    val settings: ToolSettings = ToolSettings(),
    val color: Int = Palettes.Bright.colors.first(),
    val recentColors: List<Int> = emptyList(),
    val history: History = History(),
    /** In-progress pointer gesture, or null. */
    val stroke: Stroke? = null,
    /** Show the previous frame faintly beneath the current one. */
    val onionSkin: Boolean = true,
) {
    /** The selected layer. */
    val layer: LayerInfo get() = drawing.layer(layerId) ?: drawing.layers.last()

    /** The current frame. */
    val frame: Frame get() = drawing.frames[frameIndex]

    public companion object {
        /** Topmost unlocked layer, or the top layer when all are locked. */
        public fun defaultLayer(drawing: Drawing): Int = (drawing.layers.lastOrNull { !it.locked } ?: drawing.layers.last()).id
    }
}

/** An in-progress gesture. The [original] buffer allows live previews and a single undo step per stroke. */
public class Stroke internal constructor(
    internal val tool: Tool,
    internal val frameIndex: Int,
    internal val layerId: Int,
    internal val original: PixelBuffer,
    internal val startX: Int,
    internal val startY: Int,
    internal val lastX: Int,
    internal val lastY: Int,
    /** Indices painted by the previous preview step (shapes and stamps). */
    internal val previewIndices: IntArray,
)

/** Input to [EditorReducer]. Pointer coordinates are canvas pixels and may be out of bounds. */
public sealed interface EditorAction {
    public data class Press(val x: Int, val y: Int) : EditorAction

    public data class Drag(val x: Int, val y: Int) : EditorAction

    public data object Release : EditorAction

    /** Abandons the current gesture (e.g. a second finger touched down to zoom). */
    public data object CancelStroke : EditorAction

    public data class SelectTool(val tool: Tool) : EditorAction

    public data class SetBrushSize(val size: Int) : EditorAction

    public data object ToggleMirrorHorizontal : EditorAction

    public data object ToggleMirrorVertical : EditorAction

    public data class SetShapeFilled(val filled: Boolean) : EditorAction

    public data class SelectStamp(val stampId: String) : EditorAction

    public data class SetColor(val color: Int) : EditorAction

    public data object Undo : EditorAction

    public data object Redo : EditorAction

    /** Wipes every unlocked layer of the current frame (undoable). */
    public data object Clear : EditorAction

    public data class AddLayer(val name: String) : EditorAction

    public data class DeleteLayer(val layerId: Int) : EditorAction

    /** Moves a layer up (+1) or down (-1) in the stack. */
    public data class MoveLayer(val layerId: Int, val delta: Int) : EditorAction

    public data class ToggleLayerVisible(val layerId: Int) : EditorAction

    public data class RenameLayer(val layerId: Int, val name: String) : EditorAction

    public data class SelectLayer(val layerId: Int) : EditorAction

    public data object AddFrame : EditorAction

    public data object DuplicateFrame : EditorAction

    public data object DeleteFrame : EditorAction

    public data class SelectFrame(val index: Int) : EditorAction

    /** Moves the current frame earlier (-1) or later (+1). */
    public data class MoveFrame(val delta: Int) : EditorAction

    public data class SetFps(val fps: Int) : EditorAction

    public data object ToggleOnionSkin : EditorAction

    /** Replaces the current layer with [pixels] (already sized to the canvas). */
    public class ImportPixels(public val pixels: IntArray) : EditorAction

    /** Replaces every [from] pixel of the current layer with [to]. */
    public data class ReplaceColor(val from: Int, val to: Int) : EditorAction
}

/** Feedback hints for the UI (sounds, haptics). */
public enum class EditorEffect { POP, SPLASH, WHOOSH, PICK, WIPE, TOOL, BLOCKED }

/** Which pixels of the current frame need repainting after a reduction. */
public sealed interface Changed {
    public data object None : Changed

    public data object All : Changed

    public class Pixels(public val indices: IntArray) : Changed
}

/** Result of [EditorReducer.reduce]. */
public class Reduction(
    public val state: EditorState,
    public val changed: Changed = Changed.None,
    public val effects: Set<EditorEffect> = emptySet(),
)
