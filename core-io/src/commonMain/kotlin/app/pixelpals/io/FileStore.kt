package app.pixelpals.io

/**
 * Minimal file access rooted at an app-private folder. Paths are relative and '/'-separated.
 * Implementations must make [writeAtomic] crash-safe: readers see either the old or the new content.
 */
public interface FileStore {
    /** File names (not paths) directly inside [dir]; empty when the folder does not exist. */
    public fun list(dir: String): List<String>

    /** File content, or null when missing. */
    public fun read(path: String): ByteArray?

    /** Replaces [path] with [bytes] atomically, creating parent folders as needed. */
    public fun writeAtomic(path: String, bytes: ByteArray)

    /** Deletes [path]; true when something was deleted. */
    public fun delete(path: String): Boolean

    /** Moves [from] to [to], replacing [to]; true on success. */
    public fun move(from: String, to: String): Boolean

    /** True when [path] exists. */
    public fun exists(path: String): Boolean = read(path) != null
}

/** In-memory [FileStore] for tests and previews. */
public class MemoryFileStore : FileStore {
    private val files = LinkedHashMap<String, ByteArray>()

    override fun list(dir: String): List<String> {
        val prefix = dir.trimEnd('/') + "/"
        return files.keys.filter { it.startsWith(prefix) && '/' !in it.removePrefix(prefix) }.map { it.removePrefix(prefix) }
    }

    override fun read(path: String): ByteArray? = files[path]?.copyOf()

    override fun writeAtomic(path: String, bytes: ByteArray) {
        files[path] = bytes.copyOf()
    }

    override fun delete(path: String): Boolean = files.remove(path) != null

    override fun move(from: String, to: String): Boolean {
        val b = files.remove(from) ?: return false
        files[to] = b
        return true
    }

    override fun exists(path: String): Boolean = path in files
}
