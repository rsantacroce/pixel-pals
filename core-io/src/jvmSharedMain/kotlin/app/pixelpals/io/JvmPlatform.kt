package app.pixelpals.io

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

internal actual val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

public actual fun currentTimeMillis(): Long = System.currentTimeMillis()

public actual class ZlibCompressor actual constructor() {
    private val deflater = Deflater(Deflater.BEST_COMPRESSION)
    private val out = ByteArrayOutputStream()
    private val buf = ByteArray(16 * 1024)

    public actual fun write(data: ByteArray, offset: Int, len: Int) {
        deflater.setInput(data, offset, len)
        while (!deflater.needsInput()) drain()
    }

    public actual fun finish(): ByteArray {
        deflater.finish()
        while (!deflater.finished()) drain()
        deflater.end()
        return out.toByteArray()
    }

    private fun drain() {
        val n = deflater.deflate(buf)
        out.write(buf, 0, n)
    }
}

public actual fun zip(entries: List<Pair<String, ByteArray>>): ByteArray {
    val bytes = ByteArrayOutputStream()
    ZipOutputStream(bytes).use { z ->
        for ((name, data) in entries) {
            z.putNextEntry(ZipEntry(name))
            z.write(data)
            z.closeEntry()
        }
    }
    return bytes.toByteArray()
}
