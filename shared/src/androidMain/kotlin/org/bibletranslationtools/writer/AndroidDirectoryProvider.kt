package org.bibletranslationtools.writer

import android.content.Context
import btt_writer.shared.generated.resources.Res
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class AndroidDirectoryProvider(
    private val context: Context
) : DirectoryProvider {

    override val internalAppDir: File
        get() = context.filesDir

    override val externalAppDir: File
        get() = context.getExternalFilesDir(null)
            ?: throw NullPointerException("External storage is currently unavailable.")

    override val cacheDir: File
        get() = context.cacheDir

    override suspend fun getAssetAsFile(path: String): File {
        return withContext(Dispatchers.IO) {
            val cacheFile = File(cacheDir, "assets/$path")
            if (!cacheFile.exists()) {
                cacheFile.parentFile?.mkdirs()
                val assetPath = Res.getUri(path).removePrefix("file:///android_asset/")
                context.assets.open(assetPath).use { input ->
                    cacheFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
            cacheFile
        }
    }
}
