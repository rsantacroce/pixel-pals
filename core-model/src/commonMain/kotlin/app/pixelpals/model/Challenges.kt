package app.pixelpals.model

/** "Copy the picture" challenges: small reference pictures to recreate. No scores, just a celebration. */
public object Challenges {
    /** A challenge built from a stamp. */
    public class Challenge(public val id: String, public val target: PixelBuffer) {
        /** Exactly the colors used by the target, so a child can match it with the palette. */
        public val palette: List<Int> = (0 until target.size).map { target[it] }.filter { it != Argb.TRANSPARENT }.distinct()
    }

    /** All challenges, easiest (smallest) first. */
    public val all: List<Challenge> = Stamps.all.sortedBy { it.pixels.width * it.pixels.height }.map { Challenge(it.id, it.pixels) }

    /** Looks up a challenge by id. */
    public fun byId(id: String): Challenge? = all.firstOrNull { it.id == id }

    /** True when the composite of [drawing]'s frame [frameIndex] equals [target] pixel for pixel. */
    public fun isMatched(drawing: Drawing, frameIndex: Int, target: PixelBuffer): Boolean {
        if (drawing.width != target.width || drawing.height != target.height) return false
        val buffers = drawing.visibleBuffers(frameIndex)
        for (i in 0 until target.size) if (Drawing.compositeAt(buffers, i) != target[i]) return false
        return true
    }

    /** Fraction (0..1) of target pixels already correct; used for a gentle progress hint, not a score. */
    public fun progress(drawing: Drawing, frameIndex: Int, target: PixelBuffer): Float {
        if (drawing.width != target.width || drawing.height != target.height) return 0f
        val buffers = drawing.visibleBuffers(frameIndex)
        var ok = 0
        var total = 0
        for (i in 0 until target.size) {
            val c = Drawing.compositeAt(buffers, i)
            if (target[i] != Argb.TRANSPARENT || c != Argb.TRANSPARENT) {
                total++
                if (c == target[i]) ok++
            }
        }
        return if (total == 0) 0f else ok.toFloat() / total
    }
}
