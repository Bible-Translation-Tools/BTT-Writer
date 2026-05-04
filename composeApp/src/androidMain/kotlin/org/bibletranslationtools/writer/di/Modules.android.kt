package org.bibletranslationtools.writer.di

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.toBlockingObservableSettings
import com.russhwolf.settings.datastore.DataStoreSettings
import org.bibletranslationtools.writer.AndroidDirectoryProvider
import org.bibletranslationtools.writer.AndroidPlatform
import org.bibletranslationtools.writer.DirectoryProvider
import org.bibletranslationtools.writer.Platform
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import java.io.File



actual val platformModule = module {
    singleOf(::AndroidPlatform).bind<Platform>()
    singleOf(::AndroidDirectoryProvider).bind<DirectoryProvider>()

    @OptIn(ExperimentalSettingsApi::class, ExperimentalSettingsImplementation::class)
    single<ObservableSettings> {
        val directoryProvider: DirectoryProvider = get()
        val dataStore = PreferenceDataStoreFactory.create {
            File(directoryProvider.internalAppDir, "settings.preferences_pb")
        }
        DataStoreSettings(dataStore).toBlockingObservableSettings()
    }
}