package app.pixelpals.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EditorReducerTest {
    @Test
    fun pencilStrokeIsOneUndoStep() {
        val s = editor().drag(0, 0, 5, 0)
        assertEquals(6, s.painted())
        assertEquals(1, s.history.undoStack.size)
        val undone = s.run(EditorAction.Undo)
        assertEquals(0, undone.painted())
        val redone = undone.run(EditorAction.Redo)
        assertEquals(s.drawing.cel(0, s.layerId), redone.drawing.cel(0, redone.layerId))
    }

    @Test
    fun undoStoresDiffsNotSnapshots() {
        val s = editor(64).tap(3, 3)
        val diff = s.history.undoStack.single() as PixelDiff
        assertEquals(1, diff.indices.size)
        assertEquals(0, diff.before[0])
        assertEquals(RED, diff.after[0])
    }

    @Test
    fun keepsAtLeastHundredUndoLevels() {
        var s = editor(16)
        for (i in 0 until 150) s = s.tap(i % 16, i / 16)
        assertTrue(s.history.undoStack.size >= 100)
        repeat(150) { s = s.run(EditorAction.Undo) }
        assertTrue(s.painted() <= 150 - History.DEFAULT_LIMIT.coerceAtMost(150))
    }

    @Test
    fun newEditClearsRedo() {
        val s = editor().tap(0, 0).run(EditorAction.Undo).tap(1, 1)
        assertFalse(s.history.canRedo)
    }

    @Test
    fun eraserRemovesPixels() {
        val s = editor().drag(0, 0, 3, 0).tool(Tool.ERASER).tap(1, 0)
        assertEquals(0, s.pixel(1, 0))
        assertEquals(3, s.painted())
    }

    @Test
    fun brushSizeThreePaintsNine() {
        assertEquals(9, editor().run(EditorAction.SetBrushSize(3)).tap(5, 5).painted())
        assertEquals(3, editor().run(EditorAction.SetBrushSize(9)).settings.brushSize)
    }

    @Test
    fun linePreviewReplacesPreviousPreview() {
        val s = editor().tool(Tool.LINE).run(EditorAction.Press(0, 0), EditorAction.Drag(5, 0))
        assertEquals(6, s.painted())
        val moved = s.run(EditorAction.Drag(0, 5))
        assertEquals(6, moved.painted())
        assertEquals(0, moved.pixel(5, 0))
        val done = moved.run(EditorAction.Release)
        assertEquals(1, done.history.undoStack.size)
    }

    @Test
    fun rectangleFilledAndOutline() {
        assertEquals(12, editor().tool(Tool.RECTANGLE).drag(0, 0, 3, 3).painted())
        assertEquals(16, editor().tool(Tool.RECTANGLE).run(EditorAction.SetShapeFilled(true)).drag(0, 0, 3, 3).painted())
    }

    @Test
    fun ellipseTool() {
        val s = editor().tool(Tool.ELLIPSE).drag(0, 0, 7, 7)
        assertTrue(s.painted() > 0)
        assertEquals(0, s.pixel(0, 0))
    }

    @Test
    fun fillRespectsVisibleOutlineOnOtherLayer() {
        var s = editor(8).run(EditorAction.SetColor(BLUE)).tool(Tool.RECTANGLE).drag(1, 1, 5, 5)
        s = s.run(EditorAction.AddLayer("Layer 2")).run(EditorAction.SetColor(RED)).tool(Tool.FILL).tap(3, 3)
        assertEquals(9, s.painted())
        assertEquals(RED, s.pixel(3, 3))
        assertEquals(0, s.pixel(0, 0))
    }

    @Test
    fun fillWithSameColorIsNoOp() {
        val s = editor(4).tool(Tool.FILL).tap(0, 0).tap(0, 0)
        assertEquals(1, s.history.undoStack.size)
    }

    @Test
    fun pickerPicksAndReturnsToPreviousTool() {
        val s = editor().tap(2, 2).run(EditorAction.SetColor(BLUE)).tool(Tool.PICKER).tap(2, 2)
        assertEquals(RED, s.color)
        assertEquals(Tool.PENCIL, s.settings.tool)
    }

    @Test
    fun moveShiftsLayerAsOneStep() {
        val s =
            editor().tap(
                2,
                2,
            ).tool(Tool.MOVE).run(EditorAction.Press(0, 0), EditorAction.Drag(3, 3), EditorAction.Drag(1, 1), EditorAction.Release)
        assertEquals(RED, s.pixel(3, 3))
        assertEquals(0, s.pixel(2, 2))
        assertEquals(2, s.history.undoStack.size)
    }

    @Test
    fun stampPlacesCenteredStamp() {
        val heart = Stamps.byId("heart")!!.pixels
        val s = editor(32).run(EditorAction.SelectStamp("heart")).tap(16, 16)
        assertEquals(heart.toIntArray().count { it != 0 }, s.painted())
        assertEquals(Tool.STAMP, s.settings.tool)
    }

    @Test
    fun ditherPaintsCheckerboard() {
        val s = editor(4).tool(Tool.DITHER).run(EditorAction.SetBrushSize(3)).tap(1, 1)
        assertEquals(RED, s.pixel(1, 1))
        assertEquals(0, s.pixel(1, 0))
        assertEquals(5, s.painted())
    }

    @Test
    fun replaceColorSwapsEveryPixel() {
        val s = editor(4).drag(0, 0, 3, 0).tap(0, 3).run(EditorAction.SetColor(BLUE)).tool(Tool.REPLACE).tap(1, 0)
        assertEquals(5, s.drawing.cel(0, s.layerId).toIntArray().count { it == BLUE })
        assertEquals(0, s.drawing.cel(0, s.layerId).toIntArray().count { it == RED })
    }

    @Test
    fun clearIsUndoable() {
        val s = editor().drag(0, 0, 5, 5).run(EditorAction.Clear)
        assertEquals(0, s.painted())
        assertEquals(6, s.run(EditorAction.Undo).painted())
    }

    @Test
    fun cancelStrokeRestoresPixels() {
        val s = editor().run(EditorAction.Press(0, 0), EditorAction.Drag(5, 0), EditorAction.CancelStroke)
        assertEquals(0, s.painted())
        assertNull(s.stroke)
        assertFalse(s.history.canUndo)
    }

    @Test
    fun undoDuringStrokeCancelsStrokeFirst() {
        val s = editor().tap(0, 0).run(EditorAction.Press(5, 5), EditorAction.Drag(6, 6), EditorAction.Undo)
        assertEquals(0, s.painted())
    }

    @Test
    fun outOfBoundsStrokeIsClipped() {
        val s = editor(4).drag(-5, 1, 10, 1)
        assertEquals(4, s.painted())
    }

    @Test
    fun drawingOnLockedLayerIsBlocked() {
        val s = EditorState(ColoringPages.drawing("house", "Color", "Outline"))
        val outline = s.drawing.layers.first { it.locked }
        val onOutline = s.run(EditorAction.SelectLayer(outline.id))
        val r = EditorReducer.reduce(onOutline, EditorAction.Press(10, 10))
        assertTrue(EditorEffect.BLOCKED in r.effects)
        assertEquals(onOutline.drawing, r.state.drawing)
    }

    @Test
    fun recentColorsTracksPaintColors() {
        val s = editor().tap(0, 0).run(EditorAction.SetColor(BLUE)).tap(1, 1)
        assertEquals(listOf(BLUE, RED), s.recentColors)
    }

    @Test
    fun changedReportsOnlyTouchedPixels() {
        val s = editor()
        val r = EditorReducer.reduce(s, EditorAction.Press(2, 3))
        val changed = r.changed as Changed.Pixels
        assertEquals(listOf(3 * 16 + 2), changed.indices.toList())
    }
}
