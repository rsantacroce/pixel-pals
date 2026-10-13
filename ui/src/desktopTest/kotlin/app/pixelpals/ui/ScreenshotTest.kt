package app.pixelpals.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.Density
import app.pixelpals.io.JvmFileStore
import app.pixelpals.io.Project
import app.pixelpals.io.ProjectRepository
import app.pixelpals.io.ThemeMode
import app.pixelpals.model.Challenges
import app.pixelpals.model.ColoringPages
import app.pixelpals.model.Drawing
import app.pixelpals.model.EditorAction
import app.pixelpals.model.EditorReducer
import app.pixelpals.model.EditorState
import app.pixelpals.model.Palettes
import app.pixelpals.model.Tool
import app.pixelpals.ui.editor.EditorController
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.test.Test

/**
 * Renders key screens to `ui/build/screenshots` (phone and desktop sizes) for visual review and docs.
 * Not an assertion test; it fails only if rendering throws.
 */
@OptIn(ExperimentalTestApi::class)
class ScreenshotTest {
    private val out = File("build/screenshots").apply { mkdirs() }

    private fun sample(): Drawing {
        val c = Palettes.Bright.colors
        var s = EditorState(Drawing.blank(32, 32, "Background"), color = c[11])

        fun run(vararg a: EditorAction) {
            for (x in a) s = EditorReducer.reduce(s, x).state
        }
        run(EditorAction.SelectTool(Tool.FILL), EditorAction.Press(0, 0), EditorAction.Release)
        run(EditorAction.SetColor(c[7]), EditorAction.SelectTool(Tool.RECTANGLE), EditorAction.SetShapeFilled(true))
        run(EditorAction.Press(0, 26), EditorAction.Drag(31, 31), EditorAction.Release)
        run(
            EditorAction.SetColor(c[4]),
            EditorAction.SelectTool(Tool.ELLIPSE),
            EditorAction.Press(22, 2),
            EditorAction.Drag(29, 9),
            EditorAction.Release,
        )
        run(EditorAction.ToggleMirrorHorizontal, EditorAction.SelectStamp("flower"), EditorAction.Press(5, 21), EditorAction.Release)
        run(EditorAction.SelectStamp("heart"), EditorAction.Press(8, 8), EditorAction.Release)
        run(EditorAction.SelectStamp("cat"), EditorAction.Press(15, 19), EditorAction.Release)
        return s.drawing
    }

    private fun seed(dir: File): String {
        val repo = ProjectRepository(JvmFileStore(dir))
        val main = Project("main", "Sunny Garden", 1, 10, sample())
        runBlocking {
            repo.save(main)
            repo.save(Project("tpl", "Robot", 1, 9, ColoringPages.drawing("robot", "Colors", "Outline"), templateId = "robot"))
            val heart = Challenges.byId("star")!!
            repo.save(
                Project("ch", "Star", 1, 8, Drawing.blank(heart.target.width, heart.target.height, "Background"), challengeId = "star"),
            )
            var s = EditorState(Drawing.blank(16, 16, "Background"), color = Palettes.Bright.colors[0])
            for (a in listOf(EditorAction.SelectStamp("mushroom"), EditorAction.Press(8, 8), EditorAction.Release)) {
                s =
                    EditorReducer.reduce(s, a).state
            }
            repo.save(Project("m", "Mushroom", 1, 7, s.drawing))
        }
        return main.id
    }

    private fun shoot(name: String, width: Int, height: Int, density: Float, dark: Boolean, open: String?) {
        val dir = Files.createTempDirectory("shots").toFile()
        seed(dir)
        val app = AppController(JvmFileStore(dir), FakePlatform)
        if (dark) app.updateSettings { it.copy(theme = ThemeMode.DARK, layersPanelOpen = true) }
        runDesktopComposeUiTest(width, height) {
            var editor: EditorController? = null
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(density)) { PixelPalsApp(app) { editor = it } }
            }
            waitForIdle()
            if (open == "new") {
                onNodeWithTag("new-drawing").performClick()
            } else if (open != null) {
                waitUntil(timeoutMillis = 5000) { runCatching { onNodeWithTag("drawing-$open").assertExists() }.isSuccess }
                onNode(
                    hasContentDescription("Open", substring = true).and(hasContentDescription(nameOf(open), substring = true)),
                ).performClick()
                waitUntil(timeoutMillis = 5000) { editor != null }
            } else {
                waitUntil(timeoutMillis = 5000) { runCatching { onNodeWithTag("drawing-main").assertExists() }.isSuccess }
            }
            waitForIdle()
            val roots = onAllNodes(isRoot()).fetchSemanticsNodes().size
            ImageIO.write(onAllNodes(isRoot())[roots - 1].captureToImage().toAwtImage(), "png", File(out, "$name.png"))
        }
        dir.deleteRecursively()
    }

    private fun nameOf(id: String) = mapOf("main" to "Sunny Garden", "tpl" to "Robot", "ch" to "Star", "m" to "Mushroom").getValue(id)

    @Test
    fun render() {
        val only = System.getProperty("shots")?.split(",")

        fun want(n: String) = only == null || n in only
        if (want("desktop-gallery")) shoot("desktop-gallery", 1280, 800, 1f, false, null)
        if (want("desktop-editor")) shoot("desktop-editor", 1280, 800, 1f, false, "main")
        if (want("desktop-editor-dark")) shoot("desktop-editor-dark", 1280, 800, 1f, true, "main")
        if (want("desktop-coloring")) shoot("desktop-coloring", 1280, 800, 1f, false, "tpl")
        if (want("desktop-challenge")) shoot("desktop-challenge", 1280, 800, 1f, false, "ch")
        if (want("desktop-new")) shoot("desktop-new", 1280, 800, 1f, false, "new")
        if (want("phone-gallery")) shoot("phone-gallery", 1080, 2280, 2.625f, false, null)
        if (want("phone-editor")) shoot("phone-editor", 1080, 2280, 2.625f, false, "main")
        if (want("tablet-editor")) shoot("tablet-editor", 2560, 1600, 2f, false, "main")
    }
}
