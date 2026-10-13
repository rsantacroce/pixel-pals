package app.pixelpals.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import app.pixelpals.io.JvmFileStore
import app.pixelpals.io.ProjectRepository
import app.pixelpals.ui.editor.EditorController
import app.pixelpals.ui.platform.ExportTarget
import app.pixelpals.ui.platform.PlatformServices
import app.pixelpals.ui.platform.Sound
import app.pixelpals.ui.platform.SoundPlayer
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal object FakePlatform : PlatformServices {
    override val isDesktop = true
    override val defaultSoundOn = false
    override val exportTargets = listOf(ExportTarget.SAVE_AS)
    val played = mutableListOf<Sound>()
    override val sound =
        object : SoundPlayer {
            override fun play(sound: Sound) {
                played += sound
            }
        }

    override fun export(target: ExportTarget, bytes: ByteArray, fileName: String, mimeType: String, onResult: (Boolean) -> Unit) =
        onResult(
            true,
        )

    override fun pickImage(onResult: (ByteArray?) -> Unit) = onResult(null)
}

@OptIn(ExperimentalTestApi::class)
class EditorFlowUiTest {
    @Test
    fun createDrawUndoAutosaveReopen() =
        runComposeUiTest {
            val dir = Files.createTempDirectory("pixelpals-ui").toFile()
            val app = AppController(JvmFileStore(dir), FakePlatform, autosaveDelayMs = 200)
            var editor: EditorController? = null
            setContent { PixelPalsApp(app) { editor = it } }

            // Create a Tiny drawing.
            onNodeWithTag("new-drawing").performClick()
            onNodeWithTag("size-16").performClick()
            waitUntil(timeoutMillis = 5_000) { editor != null }
            val first = assertNotNull(editor)
            val id = first.project.id

            // Draw two strokes, then undo the second.
            onNodeWithTag("canvas").performTouchInput {
                down(center)
                moveTo(center + Offset(60f, 0f))
                up()
            }
            // A single finished stroke is autosaved on its own (no other action needed).
            waitForIdle()
            val afterOne = first.state.drawing.composite(0)
            waitUntil(timeoutMillis = 5_000) { savedPixels(dir, id)?.contentEquals(afterOne) == true }

            onNodeWithTag("canvas").performTouchInput {
                down(center + Offset(0f, 80f))
                moveTo(center + Offset(60f, 80f))
                up()
            }
            waitForIdle()
            val afterTwo = first.state.drawing.composite(0).count { it != 0 }
            assertTrue(afterTwo > 2, "both strokes painted pixels ($afterTwo)")
            onNodeWithTag("undo").performClick()
            waitForIdle()
            val expected = first.state.drawing.composite(0)
            val painted = expected.count { it != 0 }
            assertTrue(painted in 1 until afterTwo, "undo removed the second stroke ($painted of $afterTwo)")

            // Autosave kicks in on its own after the debounce.
            waitUntil(timeoutMillis = 5_000) { savedPixels(dir, id)?.contentEquals(expected) == true }

            // Close the drawing, then reopen it from the gallery.
            onNodeWithTag("editor").assertExists()
            editor = null
            onNode(hasContentDescription("Home")).performClick()
            waitUntil(timeoutMillis = 5_000) { runCatching { onNodeWithTag("gallery").assertExists() }.isSuccess }
            onNodeWithTag("drawing-$id").assertExists()
            onNodeWithTag("drawing-$id", useUnmergedTree = true).performTouchInput { click(Offset(width / 2f, height / 3f)) }
            waitUntil(timeoutMillis = 5_000) { editor != null }
            val reopened = assertNotNull(editor)
            assertEquals(id, reopened.project.id)
            assertContentEquals(expected, reopened.state.drawing.composite(0))
            dir.deleteRecursively()
        }
}

private fun savedPixels(dir: java.io.File, id: String): IntArray? =
    runCatching { runBlocking { ProjectRepository(JvmFileStore(dir)).load(id) }.drawing.composite(0) }.getOrNull()
