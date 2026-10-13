package app.pixelpals.model

/** Metadata for one layer. Layers are shared by every frame of a drawing. */
public data class LayerInfo(
    val id: Int,
    val name: String,
    val visible: Boolean = true,
    /** Locked layers (e.g. coloring-page outlines) cannot be drawn on, moved or deleted. */
    val locked: Boolean = false,
)

/** One animation frame: a pixel buffer ("cel") for every layer, keyed by [LayerInfo.id]. */
public data class Frame(val id: Int, val cels: Map<Int, PixelBuffer>)

/**
 * A complete picture: size, layers (bottom first), and one or more animation frames.
 * Instances are immutable; edit them through [EditorReducer] or the copy helpers below.
 */
public data class Drawing(
    val width: Int,
    val height: Int,
    val layers: List<LayerInfo>,
    val frames: List<Frame>,
    /** Playback speed for animation previews and GIF export. */
    val fps: Int = DEFAULT_FPS,
    /** Next unused id for layers and frames. */
    val nextId: Int,
) {
    init {
        require(layers.isNotEmpty() && frames.isNotEmpty()) { "A drawing needs at least one layer and one frame" }
    }

    /** Pixels of layer [layerId] in frame [frameIndex]. */
    public fun cel(frameIndex: Int, layerId: Int): PixelBuffer = frames[frameIndex].cels.getValue(layerId)

    /** Returns a copy with layer [layerId] of frame [frameIndex] replaced by [buffer]. */
    public fun withCel(frameIndex: Int, layerId: Int, buffer: PixelBuffer): Drawing {
        val frame = frames[frameIndex]
        val newFrame = frame.copy(cels = frame.cels + (layerId to buffer))
        return copy(frames = frames.toMutableList().also { it[frameIndex] = newFrame })
    }

    /** Index of the frame with [frameId], or -1. */
    public fun frameIndexOf(frameId: Int): Int = frames.indexOfFirst { it.id == frameId }

    /** The layer with [layerId], or null. */
    public fun layer(layerId: Int): LayerInfo? = layers.firstOrNull { it.id == layerId }

    /** Flattens the visible layers of frame [frameIndex] into one ARGB array. */
    public fun composite(frameIndex: Int): IntArray {
        val out = IntArray(width * height)
        val buffers = visibleBuffers(frameIndex)
        for (i in out.indices) out[i] = compositeAt(buffers, i)
        return out
    }

    /** Composited color of linear pixel [index] in frame [frameIndex]. */
    public fun compositePixel(frameIndex: Int, index: Int): Int = compositeAt(visibleBuffers(frameIndex), index)

    /** Visible layer buffers of a frame, bottom first. */
    public fun visibleBuffers(frameIndex: Int): List<PixelBuffer> =
        layers.filter { it.visible }.map { cel(frameIndex, it.id) }

    public companion object {
        /** Default animation speed in frames per second. */
        public const val DEFAULT_FPS: Int = 6

        /** Maximum number of layers a child can create. */
        public const val MAX_LAYERS: Int = 4

        /** Maximum number of animation frames. */
        public const val MAX_FRAMES: Int = 24

        /** Largest allowed width or height. */
        public const val MAX_SIZE: Int = 256

        /** A new drawing with one empty layer called [layerName] and one frame. */
        public fun blank(width: Int, height: Int, layerName: String): Drawing {
            require(width in 1..MAX_SIZE && height in 1..MAX_SIZE) { "Unsupported size ${width}x$height" }
            return Drawing(
                width = width,
                height = height,
                layers = listOf(LayerInfo(id = 1, name = layerName)),
                frames = listOf(Frame(id = 2, cels = mapOf(1 to PixelBuffer.blank(width, height)))),
                nextId = 3,
            )
        }

        /** Composites linear pixel [index] across [buffers] (bottom first). */
        public fun compositeAt(buffers: List<PixelBuffer>, index: Int): Int {
            var c = Argb.TRANSPARENT
            for (b in buffers) {
                val p = b[index]
                if (p != Argb.TRANSPARENT) c = Argb.over(p, c)
            }
            return c
        }
    }
}
