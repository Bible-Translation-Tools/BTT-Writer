package org.bibletranslationtools.writer.di

import org.bibletranslationtools.writer.DesktopDirectoryProvider
import org.bibletranslationtools.writer.DirectoryProvider
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

actual val platformModule = module {
    singleOf(::DesktopDirectoryProvider).bind<DirectoryProvider>()
}