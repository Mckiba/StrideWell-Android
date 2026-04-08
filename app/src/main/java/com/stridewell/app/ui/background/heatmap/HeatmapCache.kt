package com.stridewell.app.ui.background.heatmap

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HeatmapCache @Inject constructor(
    @ApplicationContext context: Context
) {
    private val cacheDirectory: File = File(context.cacheDir, "heatmaps").apply {
        mkdirs()
    }

    fun load(userId: String, runCount: Int, hasLocation: Boolean, isDark: Boolean): Bitmap? {
        val key = cacheKey(userId, runCount, hasLocation, isDark)
        val file = File(cacheDirectory, "$key.jpg")
        if (!file.exists()) return null
        return BitmapFactory.decodeFile(file.absolutePath)
    }

    fun save(image: Bitmap, userId: String, runCount: Int, hasLocation: Boolean, isDark: Boolean) {
        val key = cacheKey(userId, runCount, hasLocation, isDark)
        val file = File(cacheDirectory, "$key.jpg")

        runCatching {
            FileOutputStream(file).use { out ->
                image.compress(Bitmap.CompressFormat.JPEG, 85, out)
                out.flush()
            }
            pruneOldFiles(userId = userId, currentKey = key)
        }
    }

    fun clearAll(userId: String) {
        cacheDirectory.listFiles()?.forEach { file ->
            if (file.nameWithoutExtension.startsWith(userId)) {
                file.delete()
            }
        }
    }

    private fun cacheKey(userId: String, runCount: Int, hasLocation: Boolean, isDark: Boolean): String {
        val locSuffix = if (hasLocation) "_loc" else "_noloc"
        val darkSuffix = if (isDark) "_dark" else "_light"
        return "${userId}_${runCount}_v$CACHE_VERSION$locSuffix$darkSuffix"
    }

    private fun pruneOldFiles(userId: String, currentKey: String) {
        cacheDirectory.listFiles()?.forEach { file ->
            if (file.nameWithoutExtension.startsWith(userId) &&
                file.nameWithoutExtension != currentKey) {
                file.delete()
            }
        }
    }

    companion object {
        private const val CACHE_VERSION = 3
    }
}
