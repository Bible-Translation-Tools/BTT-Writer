package org.bibletranslationtools.writer.unit.core

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.name
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.bibletranslationtools.resourcecatalog.ResourceCatalogClient
import org.bibletranslationtools.resourcecatalog.library.Index
import org.bibletranslationtools.resourcecatalog.library.models.ChunkMarker
import org.bibletranslationtools.resourcecatalog.library.models.TargetLanguage
import org.bibletranslationtools.resourcecatalog.library.models.Versification
import org.bibletranslationtools.writer.DirectoryProvider
import org.bibletranslationtools.writer.Platform
import org.bibletranslationtools.writer.core.ImportUsfmSession
import org.bibletranslationtools.writer.core.NativeSpeaker
import org.bibletranslationtools.writer.core.ProcessUSFM
import org.bibletranslationtools.writer.core.Profile
import org.bibletranslationtools.writer.core.TargetTranslation
import org.bibletranslationtools.writer.unit.TestUtils
import org.bibletranslationtools.writer.utils.FileUtilities
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ProcessUSFMTest {

    @MockK private lateinit var directoryProvider: DirectoryProvider
    @MockK private lateinit var platform: Platform
    @MockK private lateinit var profile: Profile
    @MockK private lateinit var catalogClient: ResourceCatalogClient
    @MockK private lateinit var targetLanguage: TargetLanguage
    @MockK private lateinit var index: Index

    private val onProgress = mockk<(Float, String?) -> Unit>(relaxed = true)

    private lateinit var processUSFM: ProcessUSFM

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        mockkObject(FileUtilities)
        mockkObject(TargetTranslation.Companion)

        every { directoryProvider.cacheDir } returns File("/cache")

        every { index.getVersifications("en") } returns listOf(
            Versification("en", "English")
        )
        every { catalogClient.library } returns index

        every { FileUtilities.forceMkdir(any()) } just runs
        every { FileUtilities.writeStringToFile(any(), any()) } just runs
        every { FileUtilities.deleteQuietly(any()) } returns true

        every { profile.nativeSpeaker } returns NativeSpeaker("tester")

        coEvery { TargetTranslation.create(
            any(), any(), any(), any(), any(),
            any(), any(), any(), any()
        ) } returns mockk()

        processUSFM = ProcessUSFM(
            platform = platform,
            directoryProvider = directoryProvider,
            profile = profile,
            catalogClient = catalogClient
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test successful file processing`() = runTest {
        val usfmContent = TestUtils.getResource("mrk.usfm") ?: ""
        val file = createTempUsfmFile("mrk.usfm", usfmContent)
        mockChunkMarkers()
        every { targetLanguage.slug } returns "aa"

        val session = processUSFM.startImport(targetLanguage, file, onProgress)

        assertNotNull(session)
        verifyBookResult(session)

        val summary = session.summary()
        assertTrue(summary.contains("No errors found"))
        session.cleanup()
    }

    @Test
    fun `test processing bad usfm file fails`() = runTest {
        val usfmContent = TestUtils.getResource("mrk-bad-file.usfm") ?: ""
        val file = createTempUsfmFile("mrk.usfm", usfmContent)
        mockChunkMarkers()
        every { targetLanguage.slug } returns "aa"

        val session = processUSFM.startImport(targetLanguage, file, onProgress)

        assertNotNull(session)
        val summary = session.summary()

        assertFalse(summary.contains("No errors found"))
        assertTrue(summary.contains("Missing book short name"))
        assertEquals(1, session.booksMissingNames.size)
        assertEquals(file.name, session.booksMissingNames.first().description)
        assertEquals("This is not a usfm file", session.booksMissingNames.first().contents)
        assertTrue(session.importedProjects.isEmpty())

        // Resolve the missing name
        val missingItem = session.booksMissingNames.first()
        session.processText(
            missingItem.contents!!,
            missingItem.description!!,
            "mrk"
        )

        val updatedSummary = session.summary()
        assertFalse(updatedSummary.contains("No errors found"))
        assertTrue(updatedSummary.contains("No verse markers found"))
        assertTrue(session.importedProjects.isEmpty())

        session.cleanup()
    }

    @Test
    fun `test book with single chapter fails`() = runTest {
        val usfmContent = TestUtils.getResource("mrk-single-chapter.usfm") ?: ""
        val file = createTempUsfmFile("mrk.usfm", usfmContent)
        mockChunkMarkers()
        every { targetLanguage.slug } returns "aa"

        val session = processUSFM.startImport(targetLanguage, file, onProgress)

        assertNotNull(session)
        verifyBookResult(session)

        val summary = session.summary()
        assertFalse(summary.contains("No errors found"))
        assertTrue(summary.contains("No verses in range 1 to 4 in chapter: 02"))
        assertTrue(summary.contains("No verses in range 5 to 8 in chapter: 02"))

        session.cleanup()
    }

    @Test
    fun `test book with missing verse fails`() = runTest {
        val usfmContent = TestUtils.getResource("mrk-missing-verse.usfm") ?: ""
        val file = createTempUsfmFile("mrk.usfm", usfmContent)
        mockChunkMarkers()
        every { targetLanguage.slug } returns "aa"

        val session = processUSFM.startImport(targetLanguage, file, onProgress)

        assertNotNull(session)
        verifyBookResult(session)

        val summary = session.summary()
        assertFalse(summary.contains("No errors found"))
        assertTrue(summary.contains("Missing 1 verse(s) in range 4 to 6 in chapter: 01"))

        session.cleanup()
    }

    @Test
    fun `test book with missing verse range fails`() = runTest {
        val usfmContent = TestUtils.getResource("mrk-missing-range.usfm") ?: ""
        val file = createTempUsfmFile("mrk.usfm", usfmContent)
        mockChunkMarkers()
        every { targetLanguage.slug } returns "aa"

        val session = processUSFM.startImport(targetLanguage, file, onProgress)

        assertNotNull(session)
        verifyBookResult(session)

        val summary = session.summary()
        assertFalse(summary.contains("No errors found"))
        assertTrue(summary.contains("No verses in range 4 to 6 in chapter: 01"))

        session.cleanup()
    }

    @Test
    fun `test book with extra verse fails`() = runTest {
        val usfmContent = TestUtils.getResource("mrk-extra-verse.usfm") ?: ""
        val file = createTempUsfmFile("mrk.usfm", usfmContent)
        mockChunkMarkers()
        every { targetLanguage.slug } returns "aa"

        val session = processUSFM.startImport(targetLanguage, file, onProgress)

        assertNotNull(session)
        verifyBookResult(session)

        val summary = session.summary()
        assertFalse(summary.contains("No errors found"))
        assertTrue(summary.contains("Extra 1 verse(s) in range 5 to 8 in chapter: 02"))

        session.cleanup()
    }

    @Test
    fun `test processing usfm file without header`() = runTest {
        val usfmContent = TestUtils.getResource("mrk-no-header.usfm") ?: ""
        val file = createTempUsfmFile("mrk.usfm", usfmContent)
        mockChunkMarkers()
        every { targetLanguage.slug } returns "aa"

        val session = processUSFM.startImport(targetLanguage, file, onProgress)

        assertNotNull(session)

        val summary = session.summary()
        assertFalse(summary.contains("No errors found"))
        assertTrue(summary.contains("Missing book short name"))
        assertEquals(1, session.booksMissingNames.size)
        assertTrue(session.importedProjects.isEmpty())

        // Resolve the missing name
        val missingItem = session.booksMissingNames.first()
        session.processText(
            missingItem.contents!!,
            missingItem.description!!,
            "mrk"
        )

        verifyBookResult(session)

        val updatedSummary = session.summary()
        assertTrue(updatedSummary.contains("Missing book name"))
        assertTrue(session.importedProjects.isNotEmpty())

        session.cleanup()
    }

    @Test
    fun `test cleanup temp directory`() = runTest {
        val file = createTempUsfmFile("empty.usfm", "")
        every { targetLanguage.slug } returns "aa"

        val session = processUSFM.startImport(targetLanguage, file, onProgress)

        assertNotNull(session)
        session.cleanup()

        verify { FileUtilities.deleteQuietly(any()) }
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private fun createTempUsfmFile(name: String, content: String): PlatformFile {
        val tempFile = File.createTempFile(
            name.substringBeforeLast("."),
            ".${name.substringAfterLast(".")}"
        )
        tempFile.writeText(content)
        tempFile.deleteOnExit()
        return PlatformFile(tempFile)
    }

    private fun mockChunkMarkers() {
        every { index.getChunkMarkers("mrk", "en") } returns listOf(
            ChunkMarker("1", "1"),
            ChunkMarker("1", "4"),
            ChunkMarker("1", "7"),
            ChunkMarker("1", "16"),
            ChunkMarker("1", "24"),
            ChunkMarker("1", "28"),
            ChunkMarker("2", "1"),
            ChunkMarker("2", "5"),
            ChunkMarker("2", "9"),
        )
    }

    private fun verifyBookResult(session: ImportUsfmSession) {
        assertTrue(session.isSuccess, "Import should be successful")
        assertEquals(1, session.importedProjects.size)
        assertTrue(session.importedProjects.first().name.endsWith("mrk-aa"))
        assertTrue(session.booksMissingNames.isEmpty())

        verify { index.getChunkMarkers("mrk", "en") }
        coVerify { TargetTranslation.create(
            any(), any(), any(), any(), any(),
            any(), any(), any(), any()
        ) }
        verify { directoryProvider.cacheDir }
        verify { onProgress(any(), any()) }
        verify { index.getVersifications("en") }
        verify { FileUtilities.forceMkdir(any()) }
        verify { FileUtilities.writeStringToFile(any(), any()) }
        verify { profile.nativeSpeaker }
    }
}