package org.bibletranslationtools.writer

import android.app.Application
import org.bibletranslationtools.logger.Logger
import org.bibletranslationtools.resourcecatalog.initAndroid
import org.bibletranslationtools.writer.android.BuildConfig
import org.bibletranslationtools.writer.di.initKoin
import org.bibletranslationtools.writer.di.platformModule
import org.bibletranslationtools.writer.di.sharedModule
import org.bibletranslationtools.writer.utils.FileUtilities
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.logger.Level
import java.io.File
import java.io.IOException

class WriterApp : Application() {

    private val directoryProvider: DirectoryProvider by inject()
    private val platform: Platform by inject()

    override fun onCreate() {
        super.onCreate()

        initAndroid(this)

        AppConfig.init(
            versionName = BuildConfig.VERSION_NAME,
            versionCode = BuildConfig.VERSION_CODE,
            githubToken = ""
        )

        initKoin {
            androidLogger(Level.WARNING)
            androidContext(applicationContext)
            modules(sharedModule, platformModule)
        }

        val dir = File(directoryProvider.externalAppDir, "crashes")
        if (!dir.exists()) {
            try {
                FileUtilities.forceMkdir(dir)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        Logger.registerGlobalExceptionHandler(dir)
    }
}