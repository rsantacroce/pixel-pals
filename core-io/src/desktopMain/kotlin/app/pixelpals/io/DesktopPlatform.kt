package app.pixelpals.io

import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO

public actual fun decodeImage(bytes: ByteArray): DecodedImage? =
    runCatching {
        val img = ImageIO.read(ByteArrayInputStream(bytes)) ?: return null
        val argb = img.getRGB(0, 0, img.width, img.height, null, 0, img.width)
        DecodedImage(img.width, img.height, argb)
    }.getOrNull()

/** Per-user app data folder: Application Support (macOS), %APPDATA% (Windows) or XDG data home (Linux). */
public fun desktopDataDir(): File {
    val os = System.getProperty("os.name").lowercase()
    val home = System.getProperty("user.home")
    val base =
        when {
            "mac" in os -> File(home, "Library/Application Support")
            "win" in os -> System.getenv("APPDATA")?.let(::File) ?: File(home, "AppData/Roaming")
            else -> System.getenv("XDG_DATA_HOME")?.let(::File) ?: File(home, ".local/share")
        }
    return File(base, "PixelPals").apply { mkdirs() }
}
