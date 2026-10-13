package app.pixelpals.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LayerFrameTest {
    @Test
    fun addLayerUpToFour() {
        var s = editor()
        repeat(6) { s = s.run(EditorAction.AddLayer("Layer ${it + 2}")) }
        assertEquals(Drawing.MAX_LAYERS, s.drawing.layers.size)
        assertEquals("Layer 4", s.layer.name)
    }

    @Test
    fun deleteLayerSelectsNeighborAndIsUndoable() {
        val s = editor().run(EditorAction.AddLayer("Layer 2"))
        val added = s.layerId
        val deleted = s.run(EditorAction.DeleteLayer(added))
        assertEquals(1, deleted.drawing.layers.size)
        assertEquals(deleted.drawing.layers.single().id, deleted.layerId)
        assertEquals(2, deleted.run(EditorAction.Undo).drawing.layers.size)
    }

    @Test
    fun cannotDeleteLastLayer() {
        val s = editor()
        assertEquals(1, s.run(EditorAction.DeleteLayer(s.layerId)).drawing.layers.size)
    }

    @Test
    fun reorderAndVisibilityAndRename() {
        val s = editor().run(EditorAction.AddLayer("Top"))
        val top = s.layerId
        val moved = s.run(EditorAction.MoveLayer(top, -1))
        assertEquals(top, moved.drawing.layers.first().id)
        val hidden = moved.run(EditorAction.ToggleLayerVisible(top))
        assertFalse(hidden.drawing.layer(top)!!.visible)
        assertEquals("Sky", hidden.run(EditorAction.RenameLayer(top, "  Sky ")).drawing.layer(top)!!.name)
        assertEquals("Top", hidden.run(EditorAction.RenameLayer(top, "   ")).drawing.layer(top)!!.name)
    }

    @Test
    fun compositingRespectsOrderVisibilityAndAlpha() {
        val s = editor(2).tap(0, 0).run(EditorAction.AddLayer("L2"), EditorAction.SetColor(BLUE)).tap(0, 0)
        assertEquals(BLUE, s.drawing.composite(0)[0])
        val hidden = s.run(EditorAction.ToggleLayerVisible(s.layerId))
        assertEquals(RED, hidden.drawing.composite(0)[0])
        val half = Argb.of(128, 0, 0, 255)
        val blended = Argb.over(half, RED)
        assertEquals(255, Argb.alpha(blended))
        assertTrue(Argb.red(blended) in 120..130 && Argb.blue(blended) in 125..130)
        assertEquals(half, Argb.over(half, 0))
    }

    @Test
    fun drawingOnHiddenLayerRevealsIt() {
        val s = editor().run(EditorAction.ToggleLayerVisible(1)).tap(0, 0)
        assertTrue(s.layer.visible)
    }

    @Test
    fun framesAddDuplicateDeleteMove() {
        var s = editor().tap(0, 0)
        s = s.run(EditorAction.DuplicateFrame)
        assertEquals(2, s.drawing.frames.size)
        assertEquals(1, s.frameIndex)
        assertEquals(1, s.painted())
        s = s.run(EditorAction.AddFrame)
        assertEquals(0, s.painted())
        s = s.run(EditorAction.MoveFrame(-2))
        assertEquals(0, s.frameIndex)
        assertEquals(0, s.painted())
        s = s.run(EditorAction.DeleteFrame)
        assertEquals(2, s.drawing.frames.size)
        assertEquals(3, s.run(EditorAction.Undo).drawing.frames.size)
    }

    @Test
    fun undoAcrossFramesKeepsSelectionValid() {
        val s = editor().run(EditorAction.AddFrame).tap(1, 1).run(EditorAction.Undo, EditorAction.Undo)
        assertEquals(1, s.drawing.frames.size)
        assertEquals(0, s.frameIndex)
    }

    @Test
    fun coloringPageKeepsOutlineLockedAcrossFrames() {
        val s = EditorState(ColoringPages.drawing("fish", "Color", "Outline")).run(EditorAction.AddFrame)
        val outline = s.drawing.layers.first { it.locked }
        assertEquals(s.drawing.cel(0, outline.id), s.drawing.cel(1, outline.id))
        assertEquals(s, s.run(EditorAction.DeleteLayer(outline.id)).copy(history = s.history))
    }
}
