package org.bibletranslationtools.writer.integration.usecases

import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.bibletranslationtools.writer.AppInfo
import org.bibletranslationtools.writer.BaseIntegrationTest
import org.bibletranslationtools.writer.Platform
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.usecases.UpdateApp
import org.junit.Before
import org.junit.Test
import org.koin.core.component.inject


class UpdateAppTest : BaseIntegrationTest() {

    private val updateApp: UpdateApp by inject()
    private val preference: Preference by inject()
    private val platform: Platform = mockk()
    private val appInfo: AppInfo = mockk()

    override val needsLibrary = true

    @Before
    fun setUp() {
        runBlocking { directoryProvider.deleteLibrary() }

        every {
            preference.getPref(Preference.LAST_VERSION_CODE, any(), Int::class)
        } returns 0

        every { appInfo.versionCode }.returns(0)
        every { platform.info }.returns(appInfo)
    }

    @Test
    fun testUpdateAppNewInstall() = runTest {
        var progressMessage: String? = null
        val onProgress: (Float, String?) -> Unit = { _, message ->
            progressMessage = message
        }

        updateApp.execute(onProgress)

        assertNull("Progress message should be null", progressMessage)
    }

    @Test
    fun testUpdateAppCurrentVersion() = runTest {
        val currentVersion = platform.info.versionCode

        every {
            preference.getPref(Preference.LAST_VERSION_CODE, any(), Int::class)
        } returns currentVersion

        updateApp.execute()
    }
}
