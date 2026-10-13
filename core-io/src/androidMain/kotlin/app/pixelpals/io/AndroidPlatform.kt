package app.pixelpals.io

import android.graphics.BitmapFactory

public actual fun decodeImage(bytes: ByteArray): DecodedImage? =
    runCatching {
        // Downsample very large photos while decoding; the result is pixelated to at most 256 px anyway.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 1024) sample *= 2
        val bmp =
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
        val px = IntArray(bmp.width * bmp.height)
        bmp.getPixels(px, 0, bmp.width, 0, 0, bmp.width, bmp.height)
        DecodedImage(bmp.width, bmp.height, px).also { bmp.recycle() }
    }.getOrNull()
