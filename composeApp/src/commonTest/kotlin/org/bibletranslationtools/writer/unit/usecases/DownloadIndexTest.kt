package org.bibletranslationtools.writer.unit.usecases

import io.github.vinceglb.filekit.PlatformFile
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.bibletranslationtools.resourcecatalog.ResourceCatalogClient
import org.bibletranslationtools.writer.DirectoryProvider
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.inputStream
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

    private val server = MockWebServer()
    private val indexUrl = server.url("/index.sqlite").toString()

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        mockkStatic(PlatformFile::outputStream)

        every { onProgress(any(), any()) }.just(runs)

        every { preference.getPref(any(), any(), String::class) }
            .returns(indexUrl)

        val dbFile = File.createTempFile("database", ".sqlite").also {
            it.deleteOnExit()
        }

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
        server.enqueue(createDownloadResponse())

        val success = ImportIndex(directoryProvider, preference, catalogClient)
            .download(onProgress)

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

        val success = ImportIndex(directoryProvider, preference, catalogClient)
            .download(onProgress)

        assertFalse(success)

        verify { onProgress(any(), "Downloading index.sqlite file. This may take a while. Please wait...") }
        verify { catalogClient.closeLibrary() }
    }

    @Test
    fun `test server returned error code`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))

        val success = ImportIndex(directoryProvider, preference, catalogClient)
            .download(onProgress)

        assertFalse(success)

        verify { onProgress(any(), "Downloading index.sqlite file. This may take a while. Please wait...") }
        verify { catalogClient.closeLibrary() }
    }

    @Test
    fun `test import index successful`() {
        val file: PlatformFile = mockk()
        val indexFile = tempFolder.newFile().apply {
            writeText("1234567890")
        }

        every { file.inputStream() }.returns(indexFile.inputStream())

        val success = ImportIndex(directoryProvider, preference, catalogClient)
            .import(file)

        assertTrue(success)
        assertEquals("1234567890", directoryProvider.databaseFile.readText())

        verify { catalogClient.closeLibrary() }
        verify { directoryProvider.databaseFile }
    }

    private fun createDownloadResponse(): MockResponse {
        val buffer = Buffer().apply {
            write("1234567890".toByteArray())
        }
        return MockResponse().setBody(buffer).setResponseCode(200)
    }
}