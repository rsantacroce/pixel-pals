package app.pixelpals.ui.platform

import android.content.ContentValues
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors

/**
 * Android services. Must be constructed in [ComponentActivity.onCreate] (before STARTED) because it
 * registers activity-result launchers. Exports go to the share sheet, to Pictures via MediaStore
 * (API 29+), or through the Storage Access Framework.
 */
class AndroidPlatformServices(private val activity: ComponentActivity, private val fileProviderAuthority: String) : PlatformServices {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var pendingPick: ((ByteArray?) -> Unit)? = null
    private var pendingCreate: Pair<ByteArray, (Boolean) -> Unit>? = null

    private val pickLauncher =
        activity.registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            val callback = pendingPick ?: return@registerForActivityResult
            pendingPick = null
            if (uri == null) {
                callback(null)
                return@registerForActivityResult
            }
            scope.launch { callback(withContext(Dispatchers.IO) { read(uri) }) }
        }

    private val createLauncher =
        activity.registerForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
            val (bytes, callback) = pendingCreate ?: return@registerForActivityResult
            pendingCreate = null
            if (uri == null) {
                callback(false)
                return@registerForActivityResult
            }
            scope.launch {
                val ok =
                    withContext(Dispatchers.IO) {
                        runCatching { activity.contentResolver.openOutputStream(uri)!!.use { it.write(bytes) } }.isSuccess
                    }
                callback(ok)
            }
        }

    override val isDesktop: Boolean = false
    override val defaultSoundOn: Boolean = true
    override val exportTargets: List<ExportTarget> = listOf(ExportTarget.SHARE, ExportTarget.SAVE_TO_DEVICE)
    override val sound: SoundPlayer = AudioTrackPlayer()

    override fun export(target: ExportTarget, bytes: ByteArray, fileName: String, mimeType: String, onResult: (Boolean) -> Unit) {
        when {
            target == ExportTarget.SHARE -> share(bytes, fileName, mimeType, onResult)
            mimeType.startsWith(
                "image/",
            ) &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> saveToPictures(bytes, fileName, mimeType, onResult)
            else -> {
                pendingCreate = bytes to onResult
                createLauncher.launch(fileName)
            }
        }
    }

    override fun pickImage(onResult: (ByteArray?) -> Unit) {
        pendingPick = onResult
        pickLauncher.launch("image/*")
    }

    private fun share(bytes: ByteArray, fileName: String, mimeType: String, onResult: (Boolean) -> Unit) {
        scope.launch {
            val file =
                withContext(Dispatchers.IO) {
                    runCatching {
                        val dir = File(activity.cacheDir, "exports").apply { mkdirs() }
                        dir.listFiles()?.forEach { it.delete() }
                        File(dir, fileName).apply { writeBytes(bytes) }
                    }.getOrNull()
                }
            if (file == null) {
                onResult(false)
                return@launch
            }
            val uri = FileProvider.getUriForFile(activity, fileProviderAuthority, file)
            val send =
                Intent(Intent.ACTION_SEND).apply {
                    type = mimeType
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            val ok = runCatching { activity.startActivity(Intent.createChooser(send, null)) }.isSuccess
            onResult(ok)
        }
    }

    private fun saveToPictures(bytes: ByteArray, fileName: String, mimeType: String, onResult: (Boolean) -> Unit) {
        scope.launch {
            val ok =
                withContext(Dispatchers.IO) {
                    runCatching {
                        val values =
                            ContentValues().apply {
                                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PixelPals")
                                put(MediaStore.Images.Media.IS_PENDING, 1)
                            }
                        val resolver = activity.contentResolver
                        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!
                        resolver.openOutputStream(uri)!!.use { it.write(bytes) }
                        values.clear()
                        values.put(MediaStore.Images.Media.IS_PENDING, 0)
                        resolver.update(uri, values, null, null)
                    }.isSuccess
                }
            onResult(ok)
        }
    }

    private fun read(uri: Uri): ByteArray? =
        runCatching {
            activity.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull()
}

/** Plays synthesized sounds with static AudioTracks on a background thread. */
private class AudioTrackPlayer : SoundPlayer {
    private val executor = Executors.newSingleThreadExecutor()
    private val tracks = HashMap<Sound, AudioTrack?>()

    override fun play(sound: Sound) {
        executor.execute {
            val track = tracks.getOrPut(sound) { create(sound) } ?: return@execute
            runCatching {
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) track.stop()
                track.reloadStaticData()
                track.play()
            }
        }
    }

    private fun create(sound: Sound): AudioTrack? =
        runCatching {
            val samples = SoundSynth.samples(sound)
            val track =
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder().setUsage(
                            AudioAttributes.USAGE_GAME,
                        ).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build(),
                    )
                    .setAudioFormat(
                        AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(SoundSynth.SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build(),
                    )
                    .setBufferSizeInBytes(samples.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
            track.write(samples, 0, samples.size)
            track
        }.getOrNull()
}
