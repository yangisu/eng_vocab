package com.yangi.engvocab.core.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.ExifInterface
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import kotlin.math.max
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImagePreprocessorTest {
    @Test
    fun rotatesFromExifAndCapsLongEdgeAt2048() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val input = File(context.cacheDir, "preprocess_${System.nanoTime()}.jpg")
        createLandscapeJpegWithPortraitExif(input)

        val prepared = ImagePreprocessor(context).prepare(input.toPath())

        val bytes = Base64.decode(prepared.base64, Base64.NO_WRAP)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        assertEquals("image/jpeg", prepared.mimeType)
        assertTrue(max(bitmap.width, bitmap.height) <= 2048)
        assertTrue("EXIF rotation should produce portrait output", bitmap.height > bitmap.width)
        bitmap.recycle()
        input.delete()
    }

    @Test
    fun doesNotUpscaleSmallImage() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val input = File(context.cacheDir, "small_${System.nanoTime()}.jpg")
        Bitmap.createBitmap(320, 200, Bitmap.Config.ARGB_8888).useBitmap { bitmap ->
            input.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        }

        val prepared = ImagePreprocessor(context).prepare(input.toPath())
        val bytes = Base64.decode(prepared.base64, Base64.NO_WRAP)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        assertEquals(320, bitmap.width)
        assertEquals(200, bitmap.height)
        bitmap.recycle()
        input.delete()
    }

    private fun createLandscapeJpegWithPortraitExif(file: File) {
        Bitmap.createBitmap(3000, 1200, Bitmap.Config.ARGB_8888).useBitmap { bitmap ->
            bitmap.eraseColor(Color.WHITE)
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        }
        ExifInterface(file).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
            saveAttributes()
        }
    }
}

private inline fun Bitmap.useBitmap(block: (Bitmap) -> Unit) = try {
    block(this)
} finally {
    recycle()
}
