package dev.jahir.harmonic.colors.demo

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Decodes [uri] scaled down by powers of two until neither side is over [maxSize].
 * Returns null if the image can't be read, for example when the picker grant expired.
 */
internal suspend fun loadBitmap(contentResolver: ContentResolver, uri: Uri, maxSize: Int): Bitmap? =
    withContext(Dispatchers.IO) {
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sampleSize = 1
            while (bounds.outWidth / sampleSize > maxSize || bounds.outHeight / sampleSize > maxSize) {
                sampleSize *= 2
            }
            val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        } catch (_: IOException) {
            null
        } catch (_: SecurityException) {
            null
        }
    }
