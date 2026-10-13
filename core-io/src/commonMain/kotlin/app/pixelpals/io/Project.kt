package app.pixelpals.io

import app.pixelpals.model.Drawing
import app.pixelpals.model.PixelBuffer
import kotlin.random.Random

/** A saved drawing plus gallery metadata. */
public data class Project(
    val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val drawing: Drawing,
    val paletteId: String = "bright",
    /** Coloring page template this drawing started from, if any. */
    val templateId: String? = null,
    /** Challenge this drawing is attempting, if any. */
    val challengeId: String? = null,
) {
    public companion object {
        /** A new random project id (file-name safe). */
        public fun newId(): String =
            buildString {
                append(currentTimeMillis().toString(36))
                append('-')
                repeat(8) { append("0123456789abcdefghijklmnopqrstuvwxyz"[Random.nextInt(36)]) }
            }
    }
}

/** What the gallery needs to show one drawing. */
public data class ProjectSummary(
    val id: String,
    val name: String,
    val updatedAt: Long,
    val width: Int,
    val height: Int,
    val frameCount: Int,
    val thumbnail: PixelBuffer,
)

/** A drawing in the Recycle Bin. */
public data class TrashedProject(val summary: ProjectSummary, val deletedAt: Long)

/** Thrown when a project file cannot be read. The file has already been moved aside. */
public class CorruptProjectException(message: String, cause: Throwable? = null) : Exception(message, cause)
