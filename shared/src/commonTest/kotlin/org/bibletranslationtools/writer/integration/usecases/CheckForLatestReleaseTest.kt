package org.bibletranslationtools.writer.integration.usecases

import kotlinx.coroutines.runBlocking
import org.bibletranslationtools.writer.BaseIntegrationTest
import org.bibletranslationtools.writer.Platform
import org.bibletranslationtools.writer.usecases.CheckForLatestRelease
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.koin.test.inject

class CheckForLatestReleaseTest : BaseIntegrationTest() {

    private val checkForLatestRelease: CheckForLatestRelease by inject()
    private val platform: Platform by inject()

    @Test
    fun checkForLatestRelease() {
        val result = runBlocking { checkForLatestRelease.execute() }

        if (result.release != null) {
            assertFalse(
                "Release name should not be empty",
                result.release.name.isEmpty()
            )
            assertTrue(
                "Release file should be .apk",
                result.release.downloadUrl.endsWith(".apk")
            )
            assertTrue(
                "Release build should be greater than 0",
                result.release.build > 0
            )
            assertTrue(
                "Release build should be greater than current version",
                result.release.build > platform.info.versionCode
            )
            assertTrue(
                "Release size should be greater than 0",
                result.release.downloadSize > 0
            )
        }

        assertNotNull(result)
    }
}
