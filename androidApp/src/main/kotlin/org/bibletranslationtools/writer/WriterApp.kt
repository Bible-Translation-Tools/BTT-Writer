package org.bibletranslationtools.writer

import android.app.Application
import org.bibletranslationtools.resourcecatalog.initAndroid
import org.bibletranslationtools.writer.android.BuildConfig
import org.bibletranslationtools.writer.di.initKoin
import org.bibletranslationtools.writer.di.platformModule
import org.bibletranslationtools.writer.di.sharedModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.logger.Level

class WriterApp : Application() {

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
    }
}