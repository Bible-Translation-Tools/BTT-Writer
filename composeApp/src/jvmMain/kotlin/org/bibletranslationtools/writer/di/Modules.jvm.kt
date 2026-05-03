package org.bibletranslationtools.writer.di

import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.PreferencesSettings
import org.bibletranslationtools.writer.DesktopPlatform
import org.bibletranslationtools.writer.DirectoryProvider
import org.bibletranslationtools.writer.JvmDirectoryProvider
import org.bibletranslationtools.writer.Platform
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import java.util.prefs.Preferences

actual val platformModule = module {
    singleOf(::DesktopPlatform).bind<Platform>()
    singleOf(::JvmDirectoryProvider).bind<DirectoryProvider>()
    single<ObservableSettings> {
        PreferencesSettings(Preferences.userRoot().node("BTT-Writer"))
    }
}