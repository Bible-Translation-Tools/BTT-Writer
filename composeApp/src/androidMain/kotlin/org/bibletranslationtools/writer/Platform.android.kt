package org.bibletranslationtools.writer

import android.content.Context
import android.os.Build
import org.koin.mp.KoinPlatform.getKoin
import java.io.File

class AndroidPlatform : Platform {
    private val context: Context = getKoin().get()

    override val deviceId: String
        get() = Build.MODEL.lowercase().replace(" ", "_")

    override val appExternalDir: File
        get() = context.getExternalFilesDir(null)
            ?: throw NullPointerException("External storage is currently unavailable.")

    override val appInternalDir: File
        get() = context.filesDir
}

actual fun getPlatform(): Platform = AndroidPlatform()