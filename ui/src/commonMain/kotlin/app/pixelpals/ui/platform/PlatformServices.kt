package app.pixelpals.ui.platform

/** Where an exported file can go. Android offers share + save; desktop offers a "Save as…" dialog. */
enum class ExportTarget { SHARE, SAVE_TO_DEVICE, SAVE_AS }

/** Platform-specific services the shared UI needs. Implemented in androidMain / desktopMain. */
interface PlatformServices {
    /** True on desktop: enables keyboard shortcut hints and mouse-specific affordances. */
    val isDesktop: Boolean

    /** Sound is on by default on Android and muted by default on desktop. */
    val defaultSoundOn: Boolean

    /** Export destinations offered on this platform, in display order. */
    val exportTargets: List<ExportTarget>

    /** Plays short UI sounds. */
    val sound: SoundPlayer

    /**
     * Sends [bytes] to [target]. Runs any UI (share sheet, file chooser) on the main thread and
     * file work off it. Calls [onResult] with true on success, false when cancelled or failed.
     */
    fun export(target: ExportTarget, bytes: ByteArray, fileName: String, mimeType: String, onResult: (Boolean) -> Unit)

    /** Lets the user choose a picture; returns its bytes, or null when cancelled. */
    fun pickImage(onResult: (ByteArray?) -> Unit)
}

/** Short synthesized UI sounds. */
enum class Sound { POP, SPLASH, WHOOSH, CLICK, WIPE, TADA }

/** Plays [Sound]s. Implementations must never block the caller. */
interface SoundPlayer {
    fun play(sound: Sound)
}
