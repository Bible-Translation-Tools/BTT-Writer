package org.bibletranslationtools.writer

import android.content.Context
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

}