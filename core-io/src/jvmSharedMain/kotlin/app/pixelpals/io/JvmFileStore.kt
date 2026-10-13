package app.pixelpals.io

import java.io.File
import java.io.FileOutputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** [FileStore] backed by a real folder. Writes go to a temp file, are fsynced, then atomically renamed. */
public class JvmFileStore(public val root: File) : FileStore {
    private fun file(path: String) = File(root, path)

    override fun list(dir: String): List<String> =
        file(dir).listFiles()?.filter {
            it.isFile && !it.name.endsWith(".tmp")
        }?.map { it.name }.orEmpty()

    override fun read(path: String): ByteArray? = file(path).takeIf { it.isFile }?.readBytes()

    override fun writeAtomic(path: String, bytes: ByteArray) {
        val target = file(path)
        target.parentFile?.mkdirs()
        val tmp = File(target.parentFile, ".${target.name}.tmp")
        FileOutputStream(tmp).use { out ->
            out.write(bytes)
            out.fd.sync()
        }
        try {
            Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (e: AtomicMoveNotSupportedException) {
            Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    override fun delete(path: String): Boolean = file(path).delete()

    override fun move(from: String, to: String): Boolean {
        val src = file(from)
        if (!src.isFile) return false
        val dst = file(to)
        dst.parentFile?.mkdirs()
        Files.move(src.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING)
        return true
    }

    override fun exists(path: String): Boolean = file(path).isFile
}
