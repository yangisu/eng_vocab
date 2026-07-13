package com.yangi.engvocab.core.image

import android.content.Context
import android.net.Uri
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.util.UUID

class TempImageStore(context: Context) {
    private val appContext = context.applicationContext
    private val importsDirectory: Path = appContext.cacheDir.toPath().resolve(DIRECTORY_NAME)

    fun createCapturePath(): Path {
        Files.createDirectories(importsDirectory)
        return importsDirectory.resolve("${UUID.randomUUID()}.jpg")
    }

    fun createCaptureUri(): Uri = Uri.fromFile(createCapturePath().toFile())

    fun copyFrom(uri: Uri): Path {
        val destination = createCapturePath()
        try {
            val source = requireNotNull(appContext.contentResolver.openInputStream(uri)) {
                "선택한 사진을 열 수 없습니다."
            }
            source.use { input ->
                Files.newOutputStream(destination).use(input::copyTo)
            }
            return destination
        } catch (failure: Exception) {
            Files.deleteIfExists(destination)
            throw failure
        }
    }

    fun delete(path: Path?) {
        if (path == null || !path.normalize().startsWith(importsDirectory.normalize())) return
        Files.deleteIfExists(path)
    }

    fun cleanupOlderThan(
        age: Duration = Duration.ofHours(24),
        now: Instant = Instant.now(),
    ) {
        if (!Files.exists(importsDirectory)) return
        Files.newDirectoryStream(importsDirectory, "*.jpg").use { paths ->
            paths.forEach { path ->
                val modified = Files.getLastModifiedTime(path).toInstant()
                if (modified.isBefore(now.minus(age))) Files.deleteIfExists(path)
            }
        }
    }

    private companion object {
        const val DIRECTORY_NAME = "imports"
    }
}
