package org.bibletranslationtools.writer

import android.content.Context
import btt_writer.composeapp.generated.resources.Res
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

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

    override suspend fun openAssetStream(path: String): InputStream {
        return withContext(Dispatchers.IO) {
            val assetPath = Res.getUri(path).removePrefix("file:///android_asset/")
            context.assets.open(assetPath)
        }
    }
}