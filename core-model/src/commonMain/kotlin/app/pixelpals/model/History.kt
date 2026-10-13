package app.pixelpals.model

/** A reversible edit. Commands are applied to and reverted from whole [Drawing]s. */
public sealed interface Command {
    /** Re-applies the edit. */
    public fun redo(drawing: Drawing): Drawing

    /** Reverts the edit. */
    public fun undo(drawing: Drawing): Drawing
}

/**
 * Pixel diff for one layer of one frame: every changed [indices] entry with its [before] and [after] color.
 * This is what a single stroke produces.
 */
public class PixelDiff(
    public val frameId: Int,
    public val layerId: Int,
    public val indices: IntArray,
    public val before: IntArray,
    public val after: IntArray,
) : Command {
    override fun redo(drawing: Drawing): Drawing = apply(drawing, after)

    override fun undo(drawing: Drawing): Drawing = apply(drawing, before)

    private fun apply(drawing: Drawing, values: IntArray): Drawing {
        val fi = drawing.frameIndexOf(frameId)
        if (fi < 0 || drawing.layer(layerId) == null) return drawing
        return drawing.withCel(fi, layerId, drawing.cel(fi, layerId).withChanges(indices, values))
    }

    public companion object {
        /** Diff between two buffers of the same size, or null when they are identical. */
        public fun between(frameId: Int, layerId: Int, before: PixelBuffer, after: PixelBuffer): PixelDiff? {
            var count = 0
            for (i in 0 until before.size) if (before[i] != after[i]) count++
            if (count == 0) return null
            val idx = IntArray(count)
            val b = IntArray(count)
            val a = IntArray(count)
            var n = 0
            for (i in 0 until before.size) {
                if (before[i] != after[i]) {
                    idx[n] = i
                    b[n] = before[i]
                    a[n] = after[i]
                    n++
                }
            }
            return PixelDiff(frameId, layerId, idx, b, a)
        }
    }
}

/** Several commands undone/redone as a single step (e.g. "Clear" across layers). */
public class CompoundCommand(public val commands: List<Command>) : Command {
    override fun redo(drawing: Drawing): Drawing = commands.fold(drawing) { d, c -> c.redo(d) }

    override fun undo(drawing: Drawing): Drawing = commands.asReversed().fold(drawing) { d, c -> c.undo(d) }
}

/**
 * Structural change (layers, frames) stored as two snapshots. Snapshots share their pixel buffers
 * with the live drawing, so this is cheap.
 */
public class StructureChange(public val before: Drawing, public val after: Drawing) : Command {
    override fun redo(drawing: Drawing): Drawing = after

    override fun undo(drawing: Drawing): Drawing = before
}

/** Immutable undo/redo stacks holding at most [limit] undo steps. */
public data class History(
    val undoStack: List<Command> = emptyList(),
    val redoStack: List<Command> = emptyList(),
    val limit: Int = DEFAULT_LIMIT,
) {
    /** True when there is something to undo. */
    val canUndo: Boolean get() = undoStack.isNotEmpty()

    /** True when there is something to redo. */
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    /** Records [command] as the newest step and clears the redo stack. */
    public fun push(command: Command): History = copy(undoStack = (undoStack + command).takeLast(limit), redoStack = emptyList())

    /** Undoes the newest step, returning the new drawing and history, or null when empty. */
    public fun undo(drawing: Drawing): Pair<Drawing, History>? {
        val cmd = undoStack.lastOrNull() ?: return null
        return cmd.undo(drawing) to copy(undoStack = undoStack.dropLast(1), redoStack = redoStack + cmd)
    }

    /** Redoes the most recently undone step, or null when there is none. */
    public fun redo(drawing: Drawing): Pair<Drawing, History>? {
        val cmd = redoStack.lastOrNull() ?: return null
        return cmd.redo(drawing) to copy(undoStack = undoStack + cmd, redoStack = redoStack.dropLast(1))
    }

    public companion object {
        /** Default number of undo levels (spec minimum is 100). */
        public const val DEFAULT_LIMIT: Int = 200
    }
}
