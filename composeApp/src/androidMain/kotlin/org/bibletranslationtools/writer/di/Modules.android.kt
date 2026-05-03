package org.bibletranslationtools.writer.di

import android.content.Context
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.SharedPreferencesSettings
import org.bibletranslationtools.writer.AndroidDirectoryProvider
import org.bibletranslationtools.writer.AndroidPlatform
import org.bibletranslationtools.writer.DirectoryProvider
import org.bibletranslationtools.writer.Platform
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

actual val platformModule = module {
    singleOf(::AndroidPlatform).bind<Platform>()
    singleOf(::AndroidDirectoryProvider).bind<DirectoryProvider>()
    single<ObservableSettings> {
        SharedPreferencesSettings(
            androidContext().getSharedPreferences(
                "app_prefs",
                Context.MODE_PRIVATE
            )
        )
    }
}