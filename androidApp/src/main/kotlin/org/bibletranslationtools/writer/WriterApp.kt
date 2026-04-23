package org.bibletranslationtools.writer

import android.app.Application
import org.bibletranslationtools.writer.di.initKoin
import org.koin.android.ext.koin.androidContext

class WriterApp : Application() {

    override fun onCreate() {
        super.onCreate()

        AppLogger.setupGlobalExceptionHandler()

        initKoin {
            androidContext(applicationContext)
        }
    }
}