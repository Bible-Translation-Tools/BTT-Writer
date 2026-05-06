package org.bibletranslationtools.writer.unit.usecases

import com.itextpdf.text.pdf.BaseFont
import io.github.vinceglb.filekit.PlatformFile
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.bibletranslationtools.resourcecatalog.ResourceCatalogClient
import org.bibletranslationtools.resourcecatalog.library.Index
import org.bibletranslationtools.resourcecontainer.Project
import org.bibletranslationtools.resourcecontainer.Resource
import org.bibletranslationtools.writer.AppInfo
import org.bibletranslationtools.writer.DirectoryProvider
import org.bibletranslationtools.writer.Platform
import org.bibletranslationtools.writer.core.ChapterTranslation
import org.bibletranslationtools.writer.core.FrameTranslation
import org.bibletranslationtools.writer.core.PdfPrinter
import org.bibletranslationtools.writer.core.TargetTranslation
import org.bibletranslationtools.writer.core.TranslationFormat
import org.bibletranslationtools.writer.core.Translator.Companion.TSTUDIO_EXTENSION
import org.bibletranslationtools.writer.core.Translator.Companion.ZIP_EXTENSION
import org.bibletranslationtools.writer.core.Typography
import org.bibletranslationtools.writer.outputStream
import org.bibletranslationtools.writer.usecases.ExportProjects
import org.bibletranslationtools.writer.utils.FileUtilities
import org.bibletranslationtools.writer.utils.RepoUtils
import org.bibletranslationtools.writer.utils.Zip
import org.eclipse.jgit.errors.TransportException
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.OutputStream


class ExportProjectsTest {

    @JvmField
    @Rule
    var tempDir: TemporaryFolder = TemporaryFolder()

    @MockK private lateinit var directoryProvider: DirectoryProvider
    @MockK private lateinit var catalogClient: ResourceCatalogClient
    @MockK private lateinit var typography: Typography
    @MockK private lateinit var targetTranslation: TargetTranslation
    @MockK private lateinit var index: Index
    @MockK private lateinit var platform: Platform
    @MockK private lateinit var info: AppInfo

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        mockkObject(Zip)
        mockkObject(RepoUtils)
        mockkObject(FileUtilities)
        mockkObject(ExportProjects.BookData)
        mockkConstructor(PdfPrinter::class)
        mockkStatic(BaseFont::class)
        mockkStatic(PlatformFile::outputStream)

        every { catalogClient.library } returns index
        val project: Project = mockk {
            every { slug }.returns("mrk")
            every { name }.returns("Gospel of Mark")
            every { languageSlug }.returns("en")
        }
        val resource: Resource = mockk {
            every { slug }.returns("ulb")
        }

        every { info.versionCode }.returns(11)
        every { platform.info }.returns(info)

        every { index.getProject(any(), any(), any()) }.returns(project)
        every { index.getResources(any(), any()) }.returns(listOf(resource))
        every { catalogClient.openResourceContainer(any(), any(), any()) }.returns(mockk(relaxed = true))

        every { targetTranslation.commitSync(any(), any()) }.returns(true)
        every { targetTranslation.commit() }.just(runs)
        every { targetTranslation.id }.returns("aa_mrk_text_ulb")
        every { targetTranslation.commitHash }.returns("abc123")
        every { targetTranslation.targetLanguageDirection }.returns("ltr")
        every { targetTranslation.targetLanguageName }.returns("aa")
        every { targetTranslation.targetLanguageId }.returns("aa")
        every { targetTranslation.path }.returns(mockk())
        every { targetTranslation.format }.returns(TranslationFormat.DEFAULT)
        every { targetTranslation.projectId }.returns("mrk")
        every { targetTranslation.projectTranslation }.returns(mockk(relaxed = true))

        coEvery { directoryProvider.writeStringToFile(any(), any()) }.just(runs)
        coEvery { directoryProvider.getAssetAsFile(any()) }.returns(File("font.ttf"))

        every { Zip.zipToStream(any(), any()) }.just(runs)
        every { RepoUtils.recover(any()) }.returns(true)
        every { FileUtilities.deleteQuietly(any()) }.returns(true)

        every { typography.getAssetPath(any()) }.returns("/fonts/font.ttf")
        every { typography.getFontSize(any()) }.returns(16f)

        every { anyConstructed<PdfPrinter>().includeMedia(any()) }.just(runs)
        every { anyConstructed<PdfPrinter>().includeIncomplete(any()) }.just(runs)
        every { BaseFont.createFont(any(), any(), any()) }.returns(mockk())
    }

    @After
    fun tearDown() {
        unmockkAll()
        tempDir.delete()
    }

    @Test
    fun `test export project from file`() = runTest {
        val platformFile: PlatformFile = mockk()
        coEvery { directoryProvider.createTempDir(any()) }.returns(tempDir.root)

        val outputStream: OutputStream = mockk()
        every { platformFile.outputStream() }.returns(outputStream)
        every { outputStream.close() } just runs

        ExportProjects(
            directoryProvider,
            catalogClient,
            typography,
            platform
        ).exportProject(targetTranslation, platformFile)

        coVerify { directoryProvider.createTempDir(any()) }
        verify { platformFile.outputStream() }
        verify { outputStream.close() }
        verify { Zip.zipToStream(any(), any()) }
        verify { FileUtilities.deleteQuietly(any()) }
        verify { targetTranslation.commitSync(any(), any()) }
        verify { targetTranslation.commit() }
    }

    @Test
    fun `test export project from uri recovering bad repo`() = runTest {
        val platformFile: PlatformFile = mockk()
        coEvery { directoryProvider.createTempDir(any()) }.returns(tempDir.root)

        val outputStream: OutputStream = mockk()
        every { platformFile.outputStream() }.returns(outputStream)
        every { outputStream.close() } just runs

        var called = false
        every { targetTranslation.commit() }.answers {
            if (!called) {
                called = true
                throw TransportException("An error occurred.")
            } else Unit
        }

        ExportProjects(
            directoryProvider,
            catalogClient,
            typography,
            platform
        ).exportProject(targetTranslation, platformFile)

        coVerify(exactly = 2) { directoryProvider.createTempDir(any()) }
        verify(exactly = 1) { platformFile.outputStream() }
        verify(exactly = 1) { outputStream.close() }
        verify { RepoUtils.recover(any()) }
        verify { FileUtilities.deleteQuietly(any()) }
        verify { targetTranslation.commitSync(any(), any()) }
        verify { targetTranslation.commit() }
    }

    @Test
    fun `test exporting directory to the specified file`() {
        val projectDir = tempDir.newFolder("project")
        val outFile = tempDir.newFile("output.tstudio")

        ExportProjects(
            directoryProvider,
            catalogClient,
            typography,
            platform
        ).exportProject(projectDir, outFile)

        verify { Zip.zipToStream(any(), any()) }
    }

    @Test
    fun `test exporting directory to invalid file`() {
        val projectDir = tempDir.newFolder("project")
        val outFile = tempDir.newFile("output.pdf")

        assertThrows(
            "Output file must have '$TSTUDIO_EXTENSION' or '$ZIP_EXTENSION' extension",
            Exception::class.java
        ) {
            ExportProjects(
                directoryProvider,
                catalogClient,
                typography,
                platform
            ).exportProject(projectDir, outFile)
        }

        verify(exactly = 0) { Zip.zipToStream(any(), any()) }
    }

    @Test
    fun `test exporting nonexistent directory fails`() {
        val projectDir = File("project")
        val outFile = tempDir.newFile("output.zip")

        assertThrows(
            "Project directory doesn't exist.",
            Exception::class.java
        ) {
            ExportProjects(
                directoryProvider,
                catalogClient,
                typography,
                platform
            ).exportProject(projectDir, outFile)
        }

        verify(exactly = 0) { Zip.zipToStream(any(), any()) }
    }

    @Test
    fun `test export project as USFM file`() = runTest {
        val platformFile: PlatformFile = mockk()

        val dir = tempDir.newFolder("project")
        coEvery { directoryProvider.createTempDir() }.returns(dir)
        val file = tempDir.newFile("output.usfm")
        coEvery { directoryProvider.createTempFile(any(), any(), any()) }.returns(file)

        mockTranslationContents()
        val bookData = mockBookData()

        val outputText = StringBuffer()
        val outputStream: OutputStream = mockk {
            every { write(any(), any(), any()) }.answers {
                val arr = firstArg<ByteArray>()
                outputText.append(String(arr))
            }
        }
        every { platformFile.outputStream() }.returns(outputStream)
        every { outputStream.close() } just runs

        val result = ExportProjects(
            directoryProvider,
            catalogClient,
            typography,
            platform
        ).exportUSFM(targetTranslation, platformFile)

        assertTrue(result.success)
        assertEquals(platformFile, result.file)
        assertEquals(ExportProjects.ExportType.USFM, result.exportType)

        assertTrue(file.length() > 0)

        val text = outputText.toString()

        assertTrue(text.contains("\\id MRK Gospel of Mark, Mark, aa, Afar"))
        assertTrue(text.contains("\\ide usfm"))
        assertTrue(text.contains("\\h Gospel of Mark"))
        assertTrue(text.contains("\\toc1 Gospel of Mark"))
        assertTrue(text.contains("\\toc2 Mark"))
        assertTrue(text.contains("\\toc3 MRK"))
        assertTrue(text.contains("\\mt Gospel of Mark"))
        assertTrue(text.contains("\\c 01"))
        assertTrue(text.contains("\\cl Chapter 1"))
        assertTrue(text.contains("\\cd Chapter reference"))
        assertTrue(text.contains("This is a test verse contents"))

        verifyUSFMExport(platformFile, bookData)

        verify { outputStream.write(any(), any(), any()) }
        verify { outputStream.close() }
    }

    @Test
    fun `test export project as USFM file fails with bad uri`() = runTest {
        val platformFile: PlatformFile = mockk()

        val dir = tempDir.newFolder("project")
        coEvery { directoryProvider.createTempDir() }.returns(dir)
        val file = tempDir.newFile("output.usfm")
        coEvery { directoryProvider.createTempFile(any(), any(), any()) }.returns(file)

        mockTranslationContents()
        val bookData = mockBookData()

        val outputStream: OutputStream = mockk {
            every { write(any(), any(), any()) }.answers {
                val arr = firstArg<ByteArray>()
                val outputText = StringBuffer()
                outputText.append(String(arr))
            }
        }
        every { platformFile.outputStream() }.throws(Exception("Bad uri"))
        every { outputStream.close() } just runs

        val result = ExportProjects(
            directoryProvider,
            catalogClient,
            typography,
            platform
        ).exportUSFM(targetTranslation, platformFile)

        assertFalse(result.success)
        assertEquals(platformFile, result.file)
        assertEquals(ExportProjects.ExportType.USFM, result.exportType)

        assertTrue(file.length() > 0)

        verifyUSFMExport(platformFile, bookData)

        verify(exactly = 0) { outputStream.write(any(), any(), any()) }
        verify(exactly = 0) { outputStream.close() }
    }

    @Test
    fun `test export project as PDF file`() = runTest {
        val platformFile: PlatformFile = mockk()

        val file = tempDir.newFile("test.pdf")
        coEvery { anyConstructed<PdfPrinter>().print() }.returns(file)

        val outputStream: OutputStream = mockk()
        every { platformFile.outputStream() }.returns(outputStream)
        every { outputStream.close() } just runs

        val result = ExportProjects(
            directoryProvider,
            catalogClient,
            typography,
            platform
        ).exportPDF(
            targetTranslation,
            platformFile,
            includeImages = true,
            includeIncompleteFrames = true,
            null
        )

        assertTrue(result.success)
        assertEquals(platformFile, result.file)
        assertEquals(ExportProjects.ExportType.PDF, result.exportType)

        verifyPDFExport(platformFile)

        verify { outputStream.close() }
        verify { FileUtilities.deleteQuietly(any()) }
    }

    @Test
    fun `test export project as PDF file with bad uri`() = runTest {
        val platformFile: PlatformFile = mockk()

        val file = tempDir.newFile("test.pdf")
        coEvery { anyConstructed<PdfPrinter>().print() }.returns(file)

        val outputStream: OutputStream = mockk()
        every { platformFile.outputStream() }.throws(Exception("Bad uri"))
        every { outputStream.close() } just runs

        val result = ExportProjects(
            directoryProvider,
            catalogClient,
            typography,
            platform
        ).exportPDF(
            targetTranslation,
            platformFile,
            includeImages = true,
            includeIncompleteFrames = true,
            null
        )

        assertFalse(result.success)
        assertEquals(platformFile, result.file)
        assertEquals(ExportProjects.ExportType.PDF, result.exportType)

        verifyPDFExport(platformFile)

        verify(exactly = 0) { outputStream.close() }
        verify(exactly = 0) { FileUtilities.deleteQuietly(any()) }
    }

    private fun verifyUSFMExport(
        file: PlatformFile,
        bookData: ExportProjects.BookData
    ) {
        coVerify { directoryProvider.createTempDir() }
        coVerify { directoryProvider.createTempFile(any(), any(), any()) }
        verify { targetTranslation.chapterTranslations }
        verify { targetTranslation.getFrameTranslations(any(), any()) }
        verify { bookData.bookCode }
        verify { bookData.bookTitle }
        verify { bookData.bookName }
        verify { bookData.languageId }
        verify { bookData.languageName }

        verify { ExportProjects.BookData.generate(any(), any()) }
        verify { file.outputStream() }
        verify { FileUtilities.deleteQuietly(any()) }
    }

    private fun verifyPDFExport(file: PlatformFile) {
        coVerify { anyConstructed<PdfPrinter>().print() }
        verify { file.outputStream() }
        verify { typography.getAssetPath(any()) }
        verify { typography.getFontSize(any()) }
        verify { anyConstructed<PdfPrinter>().includeMedia(any()) }
        verify { anyConstructed<PdfPrinter>().includeIncomplete(any()) }
    }

    private fun mockTranslationContents() {
        val chapterTranslation: ChapterTranslation = mockk {
            every { id } returns "01"
            every { title } returns "Chapter 1"
            every { reference } returns "Chapter reference"
            every { titleFinished } returns true
            every { referenceFinished } returns true
        }
        every { targetTranslation.chapterTranslations }.returns(arrayOf(chapterTranslation))

        val frameTranslation: FrameTranslation = mockk {
            every { id } returns "01"
            every { complexId } returns "01-01"
            every { body } returns "This is a test verse contents"
            every { finished } returns true
        }
        every { targetTranslation.getFrameTranslations(any(), any()) }
            .returns(arrayOf(frameTranslation))
    }

    private fun mockBookData(): ExportProjects.BookData {
        val bookData: ExportProjects.BookData = mockk {
            every { bookCode }.returns("MRK")
            every { bookTitle }.returns("Gospel of Mark")
            every { bookName }.returns("Mark")
            every { languageId }.returns("aa")
            every { languageName }.returns("Afar")
        }
        every { ExportProjects.BookData.generate(any(), any()) }
            .returns(bookData)

        return bookData
    }
}