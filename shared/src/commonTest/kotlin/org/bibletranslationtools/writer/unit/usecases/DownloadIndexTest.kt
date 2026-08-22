package org.bibletranslationtools.writer.unit.usecases

import io.github.vinceglb.filekit.PlatformFile
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.bibletranslationtools.resourcecatalog.ResourceCatalogClient
import org.bibletranslationtools.writer.DirectoryProvider
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.inputStream
import org.bibletranslationtools.writer.network.HttpRequest
import org.bibletranslationtools.writer.outputStream
import org.bibletranslationtools.writer.usecases.ImportIndex
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DownloadIndexTest {

    @MockK private lateinit var preference: Preference
    @MockK private lateinit var directoryProvider: DirectoryProvider
    @MockK private lateinit var catalogClient: ResourceCatalogClient

    private val onProgress = mockk<(Float, String?) -> Unit>(relaxed = true)

    @get:Rule var tempFolder = TemporaryFolder()

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        mockkObject(HttpRequest)
        mockkStatic(PlatformFile::outputStream)

        every { onProgress(any(), any()) }.just(runs)
        every { preference.getPref(any(), any(), String::class) }.returns("http://localhost/index.sqlite")

        val dbFile = File.createTempFile("database", ".sqlite").also { it.deleteOnExit() }
        every { catalogClient.openLibrary() }.just(runs)
        every { catalogClient.closeLibrary() }.just(runs)
        every { directoryProvider.databaseFile }.returns(dbFile)
    }

    @After
    fun tearDown() {
        unmockkAll()
        tempFolder.delete()
    }

    @Test
    fun `test download index successful`() = runTest {
        coEvery { HttpRequest.download(any(), any(), any()) } answers {
            secondArg<File>().writeText("1234567890")
        }

        val success = ImportIndex(directoryProvider, preference, catalogClient).download(onProgress)

        assertTrue(success)
        assertEquals("1234567890", directoryProvider.databaseFile.readText())
        verify { onProgress(any(), "Downloading index.sqlite file. This may take a while. Please wait...") }
        verify { preference.getPref(any(), any(), String::class) }
        verify { catalogClient.closeLibrary() }
        verify { directoryProvider.databaseFile }
    }

    @Test
    fun `test an exception is thrown during download`() = runTest {
        every { catalogClient.closeLibrary() }.throws(Exception("An error occurred"))

        val success = ImportIndex(directoryProvider, preference, catalogClient).download(onProgress)

        assertFalse(success)
        verify { onProgress(any(), "Downloading index.sqlite file. This may take a while. Please wait...") }
        verify { catalogClient.closeLibrary() }
    }

    @Test
    fun `test server returned error code`() = runTest {
        coEvery { HttpRequest.download(any(), any(), any()) } throws Exception("HTTP 500")

        val success = ImportIndex(directoryProvider, preference, catalogClient).download(onProgress)

        assertFalse(success)
        verify { onProgress(any(), "Downloading index.sqlite file. This may take a while. Please wait...") }
        verify { catalogClient.closeLibrary() }
    }

    @Test
    fun `test import index successful`() {
        val file: PlatformFile = mockk()
        val indexFile = tempFolder.newFile().apply { writeText("1234567890") }
        every { file.inputStream() }.returns(indexFile.inputStream())

        val success = ImportIndex(directoryProvider, preference, catalogClient).import(file)

        assertTrue(success)
        assertEquals("1234567890", directoryProvider.databaseFile.readText())
        verify { catalogClient.closeLibrary() }
        verify { directoryProvider.databaseFile }
    }
}
