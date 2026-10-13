package app.pixelpals.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.pixelpals.io.AppSettings
import app.pixelpals.io.Autosaver
import app.pixelpals.io.FileStore
import app.pixelpals.io.ProjectRepository
import app.pixelpals.io.SettingsRepository
import app.pixelpals.model.ColorMemory
import app.pixelpals.ui.platform.PlatformServices
import app.pixelpals.ui.platform.Sound
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * App-wide state and services, created once by each platform entry point. Owns the repositories,
 * the autosaver and the persisted settings.
 */
class AppController(
    fs: FileStore,
    val platform: PlatformServices,
    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
    autosaveDelayMs: Long = Autosaver.DEFAULT_DELAY_MS,
) {
    val repository = ProjectRepository(fs)
    private val settingsRepository = SettingsRepository(fs)
    val autosaver = Autosaver(scope, autosaveDelayMs) { repository.saveNow(it) }

    var settings: AppSettings by mutableStateOf(settingsRepository.load())
        private set

    /** Whether sounds should play, honoring the platform default until a parent changes it. */
    val soundOn: Boolean get() = settings.soundOn ?: platform.defaultSoundOn

    /** Set when an autosave failed, so the UI can show a gentle warning. */
    var saveError: Boolean by mutableStateOf(false)

    init {
        autosaver.onError = { saveError = true }
        scope.launch { runCatching { repository.purgeExpiredTrash() } }
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val next = transform(settings)
        if (next == settings) return
        settings = next
        scope.launch(Dispatchers.Default) { runCatching { settingsRepository.save(next) } }
    }

    fun addCustomColor(color: Int) = updateSettings { it.copy(customColors = ColorMemory.addCustom(it.customColors, color)) }

    fun play(sound: Sound) {
        if (soundOn) platform.sound.play(sound)
    }

    /** Writes any pending autosave now (call on app pause/close). */
    suspend fun flush() = autosaver.flush()
}
