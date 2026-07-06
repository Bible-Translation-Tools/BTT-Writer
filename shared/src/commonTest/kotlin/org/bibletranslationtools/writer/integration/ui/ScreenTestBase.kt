package org.bibletranslationtools.writer.integration.ui

import btt_writer.shared.generated.resources.Res
import btt_writer.shared.generated.resources.noto_sans_multi_language_regular
import io.mockk.every
import io.mockk.mockk
import org.bibletranslationtools.writer.Platform
import org.bibletranslationtools.writer.core.Profile
import org.bibletranslationtools.writer.core.Typography
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.rendering.RenderingProvider
import org.junit.After
import org.junit.Before
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.context.GlobalContext.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest

abstract class ScreenTestBase : KoinTest {

    protected val fakeProfile: Profile = mockk(relaxed = true)
    protected val fakeTypography: Typography = mockk(relaxed = true)
    protected val fakePreference: Preference = mockk(relaxed = true)
    protected val fakePlatform: Platform = mockk(relaxed = true)

    @Before
    fun screenTestSetUp() {
        every { fakeTypography.resolveFontResource(any()) } returns Res.font.noto_sans_multi_language_regular
        startKoin {
            modules(
                module {
                    single<Profile> { fakeProfile }
                    single<Typography> { fakeTypography }
                    single<Preference> { fakePreference }
                    single<Platform> { fakePlatform }
                    single { RenderingProvider() }
                }
            )
        }
    }

    @After
    fun screenTestTearDown() {
        stopKoin()
    }
}
