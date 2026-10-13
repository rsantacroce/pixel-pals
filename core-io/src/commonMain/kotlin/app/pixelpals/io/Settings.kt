package app.pixelpals.io

import app.pixelpals.model.ColorMemory
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Theme choice. */
@Serializable
public enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Persisted app preferences (parent settings plus a few remembered choices). */
@Serializable
public data class AppSettings(
    /** Null means "platform default" (on for Android, muted on desktop). */
    val soundOn: Boolean? = null,
    val showLabels: Boolean = true,
    val allowCustomSizes: Boolean = false,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val paletteId: String = "bright",
    val customColors: List<Int> = emptyList(),
    val exportScale: Int = PngExport.DEFAULT_SCALE,
    val exportWhiteBackground: Boolean = false,
    val layersPanelOpen: Boolean = false,
)

/** Reads and writes [AppSettings] as `settings.json`; falls back to defaults if missing or unreadable. */
public class SettingsRepository(private val fs: FileStore) {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    /** Loads settings, never throws. */
    public fun load(): AppSettings =
        runCatching {
            fs.read(FILE)?.let { json.decodeFromString(AppSettings.serializer(), it.decodeToString()) }
        }.getOrNull()?.let { it.copy(customColors = it.customColors.takeLast(ColorMemory.MAX_CUSTOM)) } ?: AppSettings()

    /** Saves settings atomically. */
    public fun save(settings: AppSettings) {
        fs.writeAtomic(FILE, json.encodeToString(AppSettings.serializer(), settings).encodeToByteArray())
    }

    private companion object {
        const val FILE = "settings.json"
    }
}
