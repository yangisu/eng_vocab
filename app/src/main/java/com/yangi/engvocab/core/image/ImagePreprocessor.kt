package com.yangi.engvocab.core.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.util.Base64
import com.yangi.engvocab.core.openai.ImageInput
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.max
import kotlin.math.roundToInt

class ImagePreprocessor(context: Context) {
    @Suppress("unused")
    private val appContext = context.applicationContext

    fun prepare(path: Path): ImageInput {
        require(Files.isRegularFile(path)) { "사진 파일을 찾을 수 없습니다." }
        val file = path.toFile()
        val orientation = ExifInterface(file).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "사진을 읽을 수 없습니다." }

        val sampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight)
        val decoded = requireNotNull(
            BitmapFactory.decodeFile(
                file.absolutePath,
                BitmapFactory.Options().apply { inSampleSize = sampleSize },
            ),
        ) { "사진을 읽을 수 없습니다." }

        val oriented = applyOrientation(decoded, orientation)
        if (oriented !== decoded) decoded.recycle()
        val scaled = scaleDown(oriented)
        if (scaled !== oriented) oriented.recycle()

        return try {
            val bytes = ByteArrayOutputStream().use { output ->
                check(scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)) {
                    "사진을 압축할 수 없습니다."
                }
                output.toByteArray()
            }
            ImageInput(
                mimeType = "image/jpeg",
                base64 = Base64.encodeToString(bytes, Base64.NO_WRAP),
            )
        } finally {
            scaled.recycle()
        }
    }

    private fun calculateSampleSize(width: Int, height: Int): Int {
        var sample = 1
        while (max(width / sample, height / sample) > DECODE_EDGE_LIMIT) sample *= 2
        return sample
    }

    private fun applyOrientation(source: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(270f)
            else -> return source
        }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun scaleDown(source: Bitmap): Bitmap {
        val longEdge = max(source.width, source.height)
        if (longEdge <= MAX_LONG_EDGE) return source
        val ratio = MAX_LONG_EDGE.toDouble() / longEdge
        val width = (source.width * ratio).roundToInt().coerceAtLeast(1)
        val height = (source.height * ratio).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, width, height, true)
    }

    private companion object {
        const val MAX_LONG_EDGE = 2048
        const val DECODE_EDGE_LIMIT = 4096
        const val JPEG_QUALITY = 85
    }
}
