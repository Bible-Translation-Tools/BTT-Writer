package org.bibletranslationtools.writer.unit.usecases

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.bibletranslationtools.resourcecatalog.ResourceCatalogClient
import org.bibletranslationtools.resourcecatalog.library.Index
import org.bibletranslationtools.resourcecatalog.library.models.SourceLanguage
import org.bibletranslationtools.resourcecatalog.library.models.toRcLanguage
import org.bibletranslationtools.resourcecontainer.PackageInfo
import org.bibletranslationtools.resourcecontainer.ResourceContainer
import org.bibletranslationtools.writer.core.ChapterTranslation
import org.bibletranslationtools.writer.core.FrameTranslation
import org.bibletranslationtools.writer.core.MergeConflictsHandler
import org.bibletranslationtools.writer.core.TargetTranslation
import org.bibletranslationtools.writer.core.Translator
import org.bibletranslationtools.writer.core.Validation
import org.bibletranslationtools.writer.usecases.ValidateProject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ValidateProjectTest {

    @MockK private lateinit var catalogClient: ResourceCatalogClient
    @MockK private lateinit var translator: Translator
    @MockK private lateinit var index: Index
    @MockK private lateinit var sourceContainer: ResourceContainer
    @MockK private lateinit var sourceLanguage: SourceLanguage

    private val sourceTranslationId = "en_mrk_text_ulb"
    private val targetTranslationId = "id_mrk_text_reg"

    private val sourceTranslation: TargetTranslation = mockk(relaxed = true)
    val targetTranslation: TargetTranslation = mockk(relaxed = true)

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        every { catalogClient.library } returns index

        coEvery { translator.getTargetTranslation(sourceTranslationId) }
            .returns(sourceTranslation)
        coEvery { translator.getTargetTranslation(targetTranslationId) }
            .returns(targetTranslation)

        every { sourceLanguage.slug }.returns("en")
        every { sourceLanguage.name }.returns("mrk")
        every { sourceLanguage.direction }.returns("ltr")
        every { index.getTargetLanguage(any()) }.returns(mockk())
        every { index.getSourceLanguage(any()) }.returns(sourceLanguage)

        val info: PackageInfo = mockk {
            every { contentMimeType }.returns("text/usfm")
        }
        every { sourceContainer.info }.returns(info)
        every { sourceContainer.language }.returns(sourceLanguage.toRcLanguage())

        every { catalogClient.openResourceContainer(any()) }.returns(sourceContainer)

        mockkObject(MergeConflictsHandler)
        every { MergeConflictsHandler.isMergeConflicted(any()) }.returns(false)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test validate project, passes successfully`() = runTest {
        mockTranslation()

        val bookChapter: ChapterTranslation = mockk(relaxed = true) {
            every { titleFinished }.returns(true)
        }
        val chapter01: ChapterTranslation = mockk(relaxed = true) {
            every { titleFinished }.returns(true)
        }
        val chapter02: ChapterTranslation = mockk(relaxed = true)

        mockChapters(bookChapter, chapter01, chapter02)

        val chunk0101: FrameTranslation = mockk(relaxed = true) {
            every { finished }.returns(true)
        }
        val chunk0104: FrameTranslation = mockk(relaxed = true) {
            every { finished }.returns(true)
        }
        val chunk0201: FrameTranslation = mockk(relaxed = true) {
            every { finished }.returns(true)
        }
        val chunk0203: FrameTranslation = mockk(relaxed = true) {
            every { finished }.returns(true)
        }

        mockChunks(chunk0101, chunk0104, chunk0201, chunk0203)

        val items = ValidateProject(
            catalogClient,
            translator
        ).execute(
            targetTranslationId,
            sourceTranslationId
        )

        assertEquals(1, items.size)
        assertTrue(items.first().isRange)
        assertTrue(items.first() is Validation.ValidGroup)

        verifyCommonStuff()
        verifyChapterAndChunks()
    }

    @Test
    fun `test validate project, invalid project title`() = runTest {
        mockTranslation()

        val bookChapter: ChapterTranslation = mockk(relaxed = true) {
            every { titleFinished }.returns(false)
        }
        val chapter01: ChapterTranslation = mockk(relaxed = true) {
            every { titleFinished }.returns(true)
        }
        val chapter02: ChapterTranslation = mockk(relaxed = true)

        mockChapters(bookChapter, chapter01, chapter02)

        val chunk0101: FrameTranslation = mockk(relaxed = true) {
            every { finished }.returns(true)
        }
        val chunk0104: FrameTranslation = mockk(relaxed = true) {
            every { finished }.returns(true)
        }
        val chunk0201: FrameTranslation = mockk(relaxed = true) {
            every { finished }.returns(true)
        }
        val chunk0203: FrameTranslation = mockk(relaxed = true) {
            every { finished }.returns(true)
        }

        mockChunks(chunk0101, chunk0104, chunk0201, chunk0203)

        val items = ValidateProject(
            catalogClient,
            translator
        ).execute(
            targetTranslationId,
            sourceTranslationId
        )

        assertEquals(3, items.size)
        assertEquals("'Book of Mark' has warnings:", items.first().title)
        assertEquals("front", (items[1] as Validation.InvalidFrame).chapterId)
        assertTrue(items[1] is Validation.InvalidFrame)
        assertFalse(items[1].isRange)
        assertTrue(items[2].isRange)
        assertTrue(items[2] is Validation.ValidFrame)

        verifyCommonStuff()
        verifyChapterAndChunks()
    }

    @Test
    fun `test validate project, invalid chapter title`() = runTest {
        mockTranslation()

        val bookChapter: ChapterTranslation = mockk(relaxed = true) {
            every { titleFinished }.returns(true)
        }
        val chapter01: ChapterTranslation = mockk(relaxed = true) {
            every { titleFinished }.returns(false)
        }
        val chapter02: ChapterTranslation = mockk(relaxed = true)

        mockChapters(bookChapter, chapter01, chapter02)

        val chunk0101: FrameTranslation = mockk(relaxed = true) {
            every { finished }.returns(true)
        }
        val chunk0104: FrameTranslation = mockk(relaxed = true) {
            every { finished }.returns(true)
        }
        val chunk0201: FrameTranslation = mockk(relaxed = true) {
            every { finished }.returns(true)
        }
        val chunk0203: FrameTranslation = mockk(relaxed = true) {
            every { finished }.returns(true)
        }

        mockChunks(chunk0101, chunk0104, chunk0201, chunk0203)

        val items = ValidateProject(
            catalogClient,
            translator
        ).execute(
            targetTranslationId,
            sourceTranslationId
        )

        assertEquals(4, items.size)
        assertTrue(items.first() is Validation.ValidGroup)
        assertEquals("Book of Mark front", items.first().title)
        assertEquals("'Chapter 1' has warnings:", items[1].title)
        assertEquals("01", (items[2] as Validation.InvalidFrame).chapterId)
        assertEquals("Chapter 1 - Title", items[2].title)
        assertTrue(items[2] is Validation.InvalidFrame)
        assertFalse(items[2].isRange)
        assertTrue(items[3].isRange)
        assertTrue(items[3] is Validation.ValidFrame)

        verifyCommonStuff()
        verifyChapterAndChunks()
    }

    @Test
    fun `test validate project, invalid first chapter chunk`() = runTest {
        mockTranslation()

        val bookChapter: ChapterTranslation = mockk(relaxed = true) {
            every { titleFinished }.returns(true)
        }
        val chapter01: ChapterTranslation = mockk(relaxed = true) {
            every { titleFinished }.returns(true)
        }
        val chapter02: ChapterTranslation = mockk(relaxed = true)

        mockChapters(bookChapter, chapter01, chapter02)

        val chunk0101: FrameTranslation = mockk(relaxed = true) {
            every { finished }.returns(false)
        }
        val chunk0104: FrameTranslation = mockk(relaxed = true) {
            every { finished }.returns(true)
        }
        val chunk0201: FrameTranslation = mockk(relaxed = true) {
            every { finished }.returns(true)
        }
        val chunk0203: FrameTranslation = mockk(relaxed = true) {
            every { finished }.returns(true)
        }

        mockChunks(chunk0101, chunk0104, chunk0201, chunk0203)

        val items = ValidateProject(
            catalogClient,
            translator
        ).execute(
            targetTranslationId,
            sourceTranslationId
        )

        assertEquals(3, items.size)
        assertTrue(items.first() is Validation.ValidGroup)
        assertEquals("Book of Mark front", items.first().title)
        assertEquals("\'Chapter 1\' has warnings:", items[1].title)
        assertEquals("01", (items[2] as Validation.InvalidFrame).chapterId)
        assertEquals("01", (items[2] as Validation.InvalidFrame).frameId)
        assertEquals("Book of Mark 1:", items[2].title)
        assertTrue(items[2] is Validation.InvalidFrame)
        assertFalse(items[2].isRange)

        verifyCommonStuff()
        verifyChapterAndChunks()
    }

    @Test
    fun `test validate project, invalid second chapter chunk`() = runTest {
        mockTranslation()

        val bookChapter: ChapterTranslation = mockk(relaxed = true) {
            every { titleFinished }.returns(true)
        }
        val chapter01: ChapterTranslation = mockk(relaxed = true) {
            every { titleFinished }.returns(true)
        }
        val chapter02: ChapterTranslation = mockk(relaxed = true)

        mockChapters(bookChapter, chapter01, chapter02)

        val chunk0101: FrameTranslation = mockk(relaxed = true) {
            every { finished }.returns(true)
        }
        val chunk0104: FrameTranslation = mockk(relaxed = true) {
            every { finished }.returns(true)
        }
        val chunk0201: FrameTranslation = mockk(relaxed = true) {
            every { finished }.returns(true)
        }
        val chunk0203: FrameTranslation = mockk(relaxed = true) {
            every { finished }.returns(false)
        }

        mockChunks(chunk0101, chunk0104, chunk0201, chunk0203)

        val items = ValidateProject(
            catalogClient,
            translator
        ).execute(
            targetTranslationId,
            sourceTranslationId
        )

        assertEquals(4, items.size)
        assertTrue(items.first() is Validation.ValidFrame)
        assertEquals("Book of Mark front-1", items.first().title)
        assertEquals("'Chapter 1' has warnings:", items[1].title)
        assertEquals("Book of Mark 2:", items[2].title)
        assertTrue(items[2] is Validation.ValidFrame)
        assertFalse(items[2].isRange)
        assertEquals("02", (items[3] as Validation.InvalidFrame).chapterId)
        assertEquals("03", (items[3] as Validation.InvalidFrame).frameId)
        assertEquals("Book of Mark 2:", items[3].title)
        assertTrue(items[3] is Validation.InvalidFrame)
        assertFalse(items[3].isRange)

        verifyCommonStuff()
        verifyChapterAndChunks()
    }

    @Test
    fun `test validate project, target translation not found`() = runTest {
        coEvery { translator.getTargetTranslation(targetTranslationId) }.returns(null)

        val items = ValidateProject(
            catalogClient,
            translator
        ).execute(
            targetTranslationId,
            sourceTranslationId
        )

        assertEquals(0, items.size)

        coVerify { translator.getTargetTranslation(targetTranslationId) }
        // should not be called if there is no target translation
        verify(inverse = true) { index.getTargetLanguage(any()) }
    }

    @Test
    fun `test validate project, source translation not found`() = runTest {
        every { catalogClient.openResourceContainer(sourceTranslationId) }.throws(Exception("Not found."))

        val items = ValidateProject(
            catalogClient,
            translator
        ).execute(
            targetTranslationId,
            sourceTranslationId
        )

        assertEquals(0, items.size)

        coVerify { translator.getTargetTranslation(targetTranslationId) }
        verify { index.getTargetLanguage(any()) }
        verify { catalogClient.openResourceContainer(sourceTranslationId) }
        // should not be called if there is no source translation
        verify(inverse = true) { sourceContainer.info }
    }

    @Test
    fun `test validate project, failed to parse format`() = runTest {
        every { sourceContainer.info }.throws(Exception("Bad format."))

        val items = ValidateProject(
            catalogClient,
            translator
        ).execute(
            targetTranslationId,
            sourceTranslationId
        )

        assertEquals(0, items.size)

        coVerify { translator.getTargetTranslation(targetTranslationId) }
        verify { index.getTargetLanguage(any()) }
        verify { catalogClient.openResourceContainer(sourceTranslationId) }
        verify { sourceContainer.info }
        // should not be called if failed to parse format
        verify(inverse = true) { sourceContainer.readChunk("front", "title") }
    }

    private fun mockTranslation() {
        every { sourceContainer.readChunk(any(), any()) }.answers {
            val chapterSlug = firstArg<String>()
            val chunkSlug = secondArg<String>()
            when {
                chapterSlug == "front" -> "Book of Mark"
                chunkSlug == "title" -> "Chapter 1"
                chapterSlug == "01" && chunkSlug == "01" -> "Mark 1:1-3"
                chapterSlug == "01" && chunkSlug == "04" -> "Mark 1:4-6"
                chapterSlug == "02" && chunkSlug == "01" -> "Mark 2:1-2"
                chapterSlug == "02" && chunkSlug == "03" -> "Mark 2:3-5"
                else -> ""
            }
        }
        every { sourceContainer.chapters() }.returns(listOf("front", "01", "02"))
        every { sourceContainer.chunks(any()) }.answers {
            val chapterSlug = firstArg<String>()
            when (chapterSlug) {
                "front" -> listOf("title")
                "01" -> listOf("title", "01", "04")
                "02" -> listOf("01", "03")
                else -> listOf()
            }
        }
    }

    private fun mockChapters(vararg chapters: ChapterTranslation) {
        every { targetTranslation.getChapterTranslation(any()) }.answers {
            val chapterSlug = firstArg<String>()
            when (chapterSlug) {
                "front" -> chapters[0]
                "01" -> chapters[1]
                "02" -> chapters[2]
                else -> mockk()
            }
        }
    }

    private fun mockChunks(vararg chunks: FrameTranslation) {
        every { targetTranslation.getFrameTranslation(any(), any(), any()) }.answers {
            val chapterSlug = firstArg<String>()
            val chunkSlug = secondArg<String>()
            when (chapterSlug) {
                "01" if chunkSlug == "01" -> chunks[0]
                "01" if chunkSlug == "04" -> chunks[1]
                "02" if chunkSlug == "01" -> chunks[2]
                "02" if chunkSlug == "03" -> chunks[3]
                else -> mockk()
            }
        }
    }

    private fun verifyChapterAndChunks() {
        verify { targetTranslation.getChapterTranslation("front") }
        verify { targetTranslation.getChapterTranslation("01") }
        verify { targetTranslation.getChapterTranslation("02") }

        verify { targetTranslation.getFrameTranslation("01", "01", any()) }
        verify { targetTranslation.getFrameTranslation("01", "04", any()) }
        verify { targetTranslation.getFrameTranslation("02", "01", any()) }
        verify { targetTranslation.getFrameTranslation("02", "03", any()) }
    }

    private fun verifyCommonStuff() {
        coVerify { translator.getTargetTranslation(any()) }
        verify { index.getTargetLanguage(any()) }
        verify { catalogClient.openResourceContainer(any()) }
        verify { sourceContainer.chapters() }
        verify { sourceContainer.chunks(any()) }
        verify { sourceContainer.info }
        verify { sourceContainer.readChunk(any(), any()) }
        verify { index.getSourceLanguage(any()) }
        verify { MergeConflictsHandler.isMergeConflicted(any()) }
    }
}