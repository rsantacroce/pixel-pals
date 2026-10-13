package app.pixelpals.io

import app.pixelpals.model.ColoringPages
import app.pixelpals.model.Drawing
import app.pixelpals.model.EditorAction
import app.pixelpals.model.EditorReducer
import app.pixelpals.model.EditorState
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal const val RED: Int = 0xFFFF0000.toInt()

internal fun sampleDrawing(): Drawing {
    var s = EditorState(Drawing.blank(32, 24, "Background"), color = RED)
    val actions =
        listOf(
            EditorAction.Press(1, 1),
            EditorAction.Drag(20, 10),
            EditorAction.Release,
            EditorAction.AddLayer("Layer 2"),
            EditorAction.SetColor(0x800000FF.toInt()),
            EditorAction.Press(5, 5),
            EditorAction.Release,
            EditorAction.AddFrame,
            EditorAction.Press(7, 7),
            EditorAction.Release,
            EditorAction.ToggleLayerVisible(1),
        )
    for (a in actions) s = EditorReducer.reduce(s, a).state
    return s.drawing
}

internal fun sampleProject(id: String = "abc", drawing: Drawing = sampleDrawing()) =
    Project(id, "My Cat", createdAt = 1000, updatedAt = 2000, drawing = drawing, paletteId = "pastel")

class ProjectCodecTest {
    @Test
    fun rleRoundTrip() {
        val px =
            IntArray(1000) {
                if (it in 100..899) {
                    RED
                } else if (it % 7 == 0) {
                    -1
                } else {
                    0
                }
            }
        assertContentEquals(px, PixelCodec.decode(PixelCodec.encode(px), px.size))
        assertContentEquals(IntArray(256 * 256), PixelCodec.decode(PixelCodec.encode(IntArray(256 * 256)), 256 * 256))
    }

    @Test
    fun rleRejectsWrongCounts() {
        val text = PixelCodec.encode(IntArray(10))
        assertFailsWith<IllegalArgumentException> { PixelCodec.decode(text, 11) }
        assertFailsWith<IllegalArgumentException> { PixelCodec.decode(text, 9) }
    }

    @Test
    fun projectRoundTrip() {
        val p = sampleProject()
        val back = ProjectCodec.decode(ProjectCodec.encode(p))
        assertEquals(p, back)
        assertEquals(2, back.drawing.frames.size)
        assertEquals(false, back.drawing.layers.first().visible)
    }

    @Test
    fun lockedLayersAndTemplateSurvive() {
        val p = sampleProject(drawing = ColoringPages.drawing("robot", "Color", "Outline")).copy(templateId = "robot")
        assertEquals(p, ProjectCodec.decode(ProjectCodec.encode(p)))
    }

    @Test
    fun summaryHasThumbnail() {
        val s = ProjectCodec.decodeSummary(ProjectCodec.encode(sampleProject()))
        assertEquals("My Cat", s.name)
        assertEquals(32, s.thumbnail.width)
        assertEquals(2, s.frameCount)
    }

    @Test
    fun bigDrawingThumbnailIsDownscaled() {
        val t = ProjectCodec.thumbnail(Drawing.blank(256, 100, "L"))
        assertEquals(64, t.width)
        assertEquals(25, t.height)
    }

    @Test
    fun corruptInputThrowsFriendlyException() {
        val good = ProjectCodec.encode(sampleProject())
        for (bad in listOf(
            "",
            "{",
            "[]",
            "{\"formatVersion\":2}",
            good.replace("\"width\":32", "\"width\":999"),
            good.take(good.length / 2),
        )) {
            assertFailsWith<CorruptProjectException>(bad.take(30)) { ProjectCodec.decode(bad) }
        }
        assertFailsWith<CorruptProjectException> { ProjectCodec.decode(good.replace("\"formatVersion\":2", "\"formatVersion\":99")) }
    }

    @Test
    fun migratesVersion1Files() {
        val px = IntArray(4 * 4).also { it[5] = RED }
        val thumb = PixelCodec.encode(px)
        val v1 =
            """
            {"formatVersion":1,"id":"old","name":"Old","createdAt":1,"updatedAt":2,"width":4,"height":4,
             "layers":[{"id":1,"name":"Background","visible":true,"pixels":"${PixelCodec.encode(px)}"}],
             "thumbnail":{"width":4,"height":4,"pixels":"$thumb"}}
            """.trimIndent()
        val p = ProjectCodec.decode(v1)
        assertEquals(1, p.drawing.frames.size)
        assertEquals(RED, p.drawing.cel(0, 1)[5])
        assertEquals(Drawing.DEFAULT_FPS, p.drawing.fps)
        assertEquals(2, p.drawing.frames.single().id)
        // Re-saving writes the current version.
        assertEquals(p, ProjectCodec.decode(ProjectCodec.encode(p)))
        // Base64 sanity: the pixel text is plain Base64.
        Base64.decode(PixelCodec.encode(px))
    }
}
