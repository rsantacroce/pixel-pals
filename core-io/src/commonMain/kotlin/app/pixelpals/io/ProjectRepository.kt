package app.pixelpals.io

import kotlinx.coroutines.withContext

/** Result of listing the gallery. [movedAside] names files that were unreadable and moved to `corrupt/`. */
public data class GalleryListing(val projects: List<ProjectSummary>, val movedAside: List<String>)

/**
 * Gallery storage. Layout under the [FileStore] root:
 * - `projects/<id>.pixelpals` — live drawings
 * - `trash/<id>.pixelpals` + `trash/<id>.deleted` (deletion time) — Recycle Bin, kept [TRASH_DAYS] days
 * - `corrupt/` — unreadable files kept aside for a parent to inspect
 */
public class ProjectRepository(
    private val fs: FileStore,
    private val clock: () -> Long = ::currentTimeMillis,
) {
    private fun projectPath(id: String) = "$PROJECTS/$id.${ProjectCodec.EXTENSION}"

    private fun trashPath(id: String) = "$TRASH/$id.${ProjectCodec.EXTENSION}"

    private fun idOf(fileName: String) = fileName.removeSuffix(".${ProjectCodec.EXTENSION}")

    private fun isProject(fileName: String) = fileName.endsWith(".${ProjectCodec.EXTENSION}")

    /** Lists drawings, newest first. Unreadable files are moved aside instead of crashing. */
    public suspend fun list(): GalleryListing =
        withContext(ioDispatcher) {
            val moved = mutableListOf<String>()
            val items =
                fs.list(PROJECTS).filter(::isProject).mapNotNull { name ->
                    try {
                        ProjectCodec.decodeSummary(fs.read("$PROJECTS/$name")!!.decodeToString())
                    } catch (e: Exception) {
                        moveAside("$PROJECTS/$name", name)
                        moved += name
                        null
                    }
                }
            GalleryListing(items.sortedByDescending { it.updatedAt }, moved)
        }

    /** Loads a drawing. Throws [CorruptProjectException] (after moving the file aside) or [NoSuchElementException]. */
    public suspend fun load(id: String): Project =
        withContext(ioDispatcher) {
            val path = projectPath(id)
            val bytes = fs.read(path) ?: throw NoSuchElementException("No drawing $id")
            try {
                ProjectCodec.decode(bytes.decodeToString())
            } catch (e: CorruptProjectException) {
                moveAside(path, "$id.${ProjectCodec.EXTENSION}")
                throw e
            }
        }

    /** Writes [project] atomically. Safe to call often. */
    public suspend fun save(project: Project): Unit = withContext(ioDispatcher) { saveNow(project) }

    /** Blocking variant of [save] for shutdown paths. */
    public fun saveNow(project: Project) {
        fs.writeAtomic(projectPath(project.id), ProjectCodec.encode(project).encodeToByteArray())
    }

    /** Copies a drawing under a new id and [name]. */
    public suspend fun duplicate(id: String, name: String): Project {
        val now = clock()
        val copy = load(id).copy(id = Project.newId(), name = name, createdAt = now, updatedAt = now)
        save(copy)
        return copy
    }

    /** Renames a drawing. */
    public suspend fun rename(id: String, name: String) {
        val p = load(id)
        save(p.copy(name = name, updatedAt = clock()))
    }

    /** Moves a drawing to the Recycle Bin. */
    public suspend fun moveToTrash(id: String): Unit =
        withContext(ioDispatcher) {
            if (fs.move(projectPath(id), trashPath(id))) fs.writeAtomic("$TRASH/$id.deleted", clock().toString().encodeToByteArray())
        }

    /** Drawings in the Recycle Bin, most recently deleted first. */
    public suspend fun listTrash(): List<TrashedProject> =
        withContext(ioDispatcher) {
            fs.list(TRASH).filter(::isProject).mapNotNull { name ->
                val id = idOf(name)
                runCatching {
                    val summary = ProjectCodec.decodeSummary(fs.read("$TRASH/$name")!!.decodeToString())
                    TrashedProject(summary, deletedAt(id))
                }.getOrNull()
            }.sortedByDescending { it.deletedAt }
        }

    /** Brings a drawing back from the Recycle Bin. */
    public suspend fun restore(id: String): Unit =
        withContext(ioDispatcher) {
            if (fs.move(trashPath(id), projectPath(id))) fs.delete("$TRASH/$id.deleted")
        }

    /** Permanently deletes everything in the Recycle Bin (parent-only). */
    public suspend fun emptyTrash(): Unit = withContext(ioDispatcher) { fs.list(TRASH).forEach { fs.delete("$TRASH/$it") } }

    /** Permanently deletes Recycle Bin entries older than [TRASH_DAYS]. */
    public suspend fun purgeExpiredTrash(): Unit =
        withContext(ioDispatcher) {
            val cutoff = clock() - TRASH_DAYS * DAY_MS
            fs.list(TRASH).filter(::isProject).map(::idOf).filter { deletedAt(it) < cutoff }.forEach {
                fs.delete(trashPath(it))
                fs.delete("$TRASH/$it.deleted")
            }
        }

    /** Every live drawing as a ZIP of `.pixelpals` files plus 8× PNGs (parent export). */
    public suspend fun exportAllZip(): ByteArray =
        withContext(ioDispatcher) {
            val entries = mutableListOf<Pair<String, ByteArray>>()
            val used = HashSet<String>()
            for (name in fs.list(PROJECTS).filter(::isProject)) {
                val bytes = fs.read("$PROJECTS/$name") ?: continue
                val project = runCatching { ProjectCodec.decode(bytes.decodeToString()) }.getOrNull() ?: continue
                var base = safeFileName(project.name)
                var n = 2
                while (!used.add(base)) base = "${safeFileName(project.name)} ${n++}"
                entries += "$base.${ProjectCodec.EXTENSION}" to bytes
                entries += "$base.png" to PngExport.render(project.drawing, 0, scale = 8, whiteBackground = false)
            }
            zip(entries)
        }

    /** Deletes every drawing, the Recycle Bin and corrupt files (parent "reset app"). */
    public suspend fun deleteEverything(): Unit =
        withContext(ioDispatcher) {
            for (dir in listOf(PROJECTS, TRASH, CORRUPT)) fs.list(dir).forEach { fs.delete("$dir/$it") }
        }

    private fun deletedAt(id: String): Long = fs.read("$TRASH/$id.deleted")?.decodeToString()?.trim()?.toLongOrNull() ?: 0L

    private fun moveAside(path: String, name: String) {
        fs.move(path, "$CORRUPT/${clock()}-$name")
    }

    public companion object {
        public const val PROJECTS: String = "projects"
        public const val TRASH: String = "trash"
        public const val CORRUPT: String = "corrupt"
        public const val TRASH_DAYS: Long = 30
        private const val DAY_MS = 24L * 60 * 60 * 1000

        /** A file-name-safe version of a drawing name. */
        public fun safeFileName(name: String): String =
            name.map {
                if (it.isLetterOrDigit() ||
                    it == ' ' ||
                    it == '-' ||
                    it == '_'
                ) {
                    it
                } else {
                    '_'
                }
            }.joinToString("").trim().ifEmpty { "drawing" }
    }
}
