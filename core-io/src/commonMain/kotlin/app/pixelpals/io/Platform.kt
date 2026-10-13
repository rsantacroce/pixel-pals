package app.pixelpals.io

import kotlinx.coroutines.CoroutineDispatcher

/** Dispatcher for blocking file work. */
internal expect val ioDispatcher: CoroutineDispatcher

/** Wall-clock time in epoch milliseconds. */
public expect fun currentTimeMillis(): Long

/** Streaming zlib (RFC 1950) compressor used by the PNG encoder. */
public expect class ZlibCompressor() {
    /** Feeds [len] bytes of [data] starting at [offset]. */
    public fun write(data: ByteArray, offset: Int = 0, len: Int = data.size)

    /** Finishes the stream and returns all compressed bytes. */
    public fun finish(): ByteArray
}

/** A decoded raster image in straight ARGB. */
public class DecodedImage(public val width: Int, public val height: Int, public val argb: IntArray)

/** Decodes PNG/JPEG/etc. bytes using the platform's image codecs; null when unsupported or corrupt. */
public expect fun decodeImage(bytes: ByteArray): DecodedImage?

/** Writes a ZIP archive containing [entries] (name → bytes). */
public expect fun zip(entries: List<Pair<String, ByteArray>>): ByteArray
