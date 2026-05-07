package org.bibletranslationtools.writer

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.bibletranslationtools.writer.core.Profile
import org.bibletranslationtools.writer.core.Typography
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.di.platformModule
import org.bibletranslationtools.writer.di.sharedModule
import org.junit.After
import org.junit.Before
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.context.GlobalContext.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject

abstract class BaseIntegrationTest : KoinTest {

    protected val directoryProvider: DirectoryProvider by inject()
    private val typography: Typography by inject()
    private val preference: Preference by inject()

    open val needsLibrary: Boolean = false

    @Before
    fun baseSetUp() {
        startKoin {
            modules(
                sharedModule,
                platformModule,
                module { single<DirectoryProvider> { TestDirectoryProvider() } },
                module { single<Preference> { mockk(relaxed = true) } },
                module { single<Profile> { mockk(relaxed = true) } }
            )
        }
        // Relaxed mock can't infer generic T for getPref → returns Object → ClassCastException.
        // Stub to always return the defaultValue (args[1]).
        every { preference.getPref(any(), any(), any()) } answers { args[1]!! }
        runBlocking {
            typography.init()
            if (needsLibrary) directoryProvider.deployDefaultLibrary()
        }
    }

    @After
    fun baseTearDown() {
        runBlocking { directoryProvider.clearCache() }
        stopKoin()
    }
}
