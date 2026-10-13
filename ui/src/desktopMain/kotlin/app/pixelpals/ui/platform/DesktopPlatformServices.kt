package app.pixelpals.ui.platform

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.FilenameFilter
import java.util.concurrent.Executors
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip

/** Desktop services: native file dialogs and Java Sound. [window] parents the dialogs. */
class DesktopPlatformServices(private val window: () -> Frame?) : PlatformServices {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override val isDesktop: Boolean = true
    override val defaultSoundOn: Boolean = false
    override val exportTargets: List<ExportTarget> = listOf(ExportTarget.SAVE_AS)
    override val sound: SoundPlayer = JavaSoundPlayer()

    override fun export(target: ExportTarget, bytes: ByteArray, fileName: String, mimeType: String, onResult: (Boolean) -> Unit) {
        val dialog = FileDialog(window(), null, FileDialog.SAVE).apply { file = fileName }
        dialog.isVisible = true
        val dir = dialog.directory
        val name = dialog.file
        if (dir == null || name == null) {
            onResult(false)
            return
        }
        val ext = fileName.substringAfterLast('.', "")
        val target = File(dir, if (ext.isNotEmpty() && !name.endsWith(".$ext", ignoreCase = true)) "$name.$ext" else name)
        scope.launch {
            val ok = withContext(Dispatchers.IO) { runCatching { target.writeBytes(bytes) }.isSuccess }
            onResult(ok)
        }
    }

    override fun pickImage(onResult: (ByteArray?) -> Unit) {
        val dialog =
            FileDialog(window(), null, FileDialog.LOAD).apply {
                filenameFilter =
                    FilenameFilter { _, n -> n.lowercase().substringAfterLast('.') in setOf("png", "jpg", "jpeg", "gif", "bmp") }
            }
        dialog.isVisible = true
        val file = dialog.files.firstOrNull()
        if (file == null) {
            onResult(null)
            return
        }
        scope.launch { onResult(withContext(Dispatchers.IO) { runCatching { file.readBytes() }.getOrNull() }) }
    }
}

/** Plays synthesized clips on a background thread; silently does nothing when no audio device exists. */
private class JavaSoundPlayer : SoundPlayer {
    private val executor = Executors.newSingleThreadExecutor { r -> Thread(r, "pixelpals-sound").apply { isDaemon = true } }
    private val clips = HashMap<Sound, Clip?>()

    override fun play(sound: Sound) {
        executor.execute {
            val clip = clips.getOrPut(sound) { open(sound) } ?: return@execute
            clip.stop()
            clip.framePosition = 0
            clip.start()
        }
    }

    private fun open(sound: Sound): Clip? =
        runCatching {
            val samples = SoundSynth.samples(sound)
            val bytes = ByteArray(samples.size * 2)
            for (i in samples.indices) {
                bytes[2 * i] = samples[i].toInt().toByte()
                bytes[2 * i + 1] = (samples[i].toInt() shr 8).toByte()
            }
            AudioSystem.getClip().apply { open(AudioFormat(SoundSynth.SAMPLE_RATE.toFloat(), 16, 1, true, false), bytes, 0, bytes.size) }
        }.getOrNull()
}
