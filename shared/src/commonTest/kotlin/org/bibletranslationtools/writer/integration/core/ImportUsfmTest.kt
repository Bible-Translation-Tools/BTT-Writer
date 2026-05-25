package org.bibletranslationtools.writer.integration.core

import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.test.runTest
import org.bibletranslationtools.logger.Logger
import org.bibletranslationtools.resourcecatalog.ResourceCatalogClient
import org.bibletranslationtools.resourcecatalog.library.models.ChunkMarker
import org.bibletranslationtools.writer.BaseIntegrationTest
import org.bibletranslationtools.writer.core.ImportUsfmSession
import org.bibletranslationtools.writer.core.ProcessUSFM
import org.bibletranslationtools.writer.rendering.spannables.USFMVerseSpan
import org.bibletranslationtools.writer.TestUtils
import org.bibletranslationtools.writer.utils.FileUtilities
import org.bibletranslationtools.writer.utils.Util
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.test.inject
import java.io.File
import java.util.regex.Pattern
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ImportUsfmTest : BaseIntegrationTest() {

    override val needsLibrary = true

    private val catalogClient: ResourceCatalogClient by inject()
    private val processUSFM: ProcessUSFM by inject()

    private var session: ImportUsfmSession? = null
    private var chunks: HashMap<String, MutableList<String>> = HashMap()
    private var chapters: Array<String>? = null

    @Before
    fun setUp() {
        Logger.flush()
    }

    @After
    fun tearDown() {
        session?.cleanup()
    }

    @Test
    fun test01ValidImportMark() = runTest {
        val source = "usfm/mrk.usfm"
        session = importFromResource(source)
        assertNotNull(session)

        verifyResults(
            session = session!!,
            expectSuccess = true,
            expectedBook = ExpectedBook("mrk.usfm", "mrk", success = true, missingName = false),
            noEmptyChunks = true,
            expectAllVerses = true,
            expectedVerseCount = 678
        )
    }

    @Test
    fun test02ImportMarkMissingName() = runTest {
        val source = "usfm/mrk_no_id.usfm"
        session = importFromResource(source)
        assertNotNull(session)

        verifyResults(
            session = session!!,
            expectSuccess = true,
            expectedBook = ExpectedBook("mrk_no_id.usfm", "", success = false, missingName = true),
            noEmptyChunks = true,
            expectAllVerses = true,
            expectedVerseCount = 0
        )
    }

    @Test
    fun test03ImportMarkMissingNameForce() = runTest {
        val source = "usfm/mrk_no_id.usfm"
        val useName = "Mrk"
        session = importFromResource(source)
        assertNotNull(session)

        // Resolve missing name
        val text = TestUtils.getResource(source)
        session!!.processText(text, "mrk_no_id.usfm", useName)

        verifyResults(
            session = session!!,
            expectSuccess = true,
            expectedBook = ExpectedBook("mrk_no_id.usfm", useName, success = true, missingName = false),
            noEmptyChunks = true,
            expectAllVerses = true,
            expectedVerseCount = 678
        )
    }

    @Test
    fun test04ValidImportPsalms() = runTest {
        val source = "usfm/19-PSA.usfm"
        session = importFromResource(source)
        assertNotNull(session)

        verifyResults(
            session = session!!,
            expectSuccess = true,
            expectedBook = ExpectedBook("19-PSA.usfm", "psa", success = true, missingName = false),
            noEmptyChunks = true,
            expectAllVerses = false,
            expectedVerseCount = 2461
        )
    }

    @Test
    fun test05ImportMarkNoChapters() = runTest {
        val source = "usfm/mrk_no_chapter.usfm"
        session = importFromResource(source)
        assertNotNull(session)

        verifyResults(
            session = session!!,
            expectSuccess = false,
            expectedBook = ExpectedBook("mrk_no_chapter.usfm", "mrk", success = false, missingName = false),
            noEmptyChunks = true,
            expectAllVerses = true
        )
    }

    @Test
    fun test06ImportMarkMissingChapters() = runTest {
        val source = "usfm/mrk_one_chapter.usfm"
        session = importFromResource(source)
        assertNotNull(session)

        verifyResults(
            session = session!!,
            expectSuccess = true,
            expectedBook = ExpectedBook("mrk_one_chapter.usfm", "mrk", success = false, missingName = false),
            noEmptyChunks = false,
            expectAllVerses = true,
            expectedVerseCount = 45
        )
    }

    @Test
    fun test07ImportMarkNoVerses() = runTest {
        val source = "usfm/mrk_no_verses.usfm"
        session = importFromResource(source)
        assertNotNull(session)

        verifyResults(
            session = session!!,
            expectSuccess = false,
            expectedBook = ExpectedBook("mrk_no_verses.usfm", "mrk", success = false, missingName = false),
            noEmptyChunks = false,
            expectAllVerses = true,
            expectedVerseCount = 0
        )
    }

    @Test
    fun test08ImportMarkMissingVerse() = runTest {
        val source = "usfm/mrk_missing_verse.usfm"
        session = importFromResource(source)
        assertNotNull(session)

        verifyResults(
            session = session!!,
            expectSuccess = true,
            expectedBook = ExpectedBook("mrk_missing_verse.usfm", "mrk", success = false, missingName = false),
            noEmptyChunks = true,
            expectAllVerses = false,
            expectedVerseCount = 677
        )
    }

    @Test
    fun test09ImportMarkEmptyChapter() = runTest {
        val source = "usfm/mrk_empty_chapter.usfm"
        session = importFromResource(source)
        assertNotNull(session)

        verifyResults(
            session = session!!,
            expectSuccess = true,
            expectedBook = ExpectedBook("mrk_empty_chapter.usfm", "mrk", success = false, missingName = false),
            noEmptyChunks = false,
            expectAllVerses = true,
            expectedVerseCount = 633
        )
    }

    @Test
    fun test10ImportJudeNoVerses() = runTest {
        val source = "usfm/jude.no_verses.usfm"
        session = importFromResource(source)
        assertNotNull(session)

        verifyResults(
            session = session!!,
            expectSuccess = false,
            expectedBook = ExpectedBook("jude.no_verses.usfm", "jud", success = false, missingName = false),
            noEmptyChunks = true,
            expectAllVerses = true,
            expectedVerseCount = 0
        )
    }

    @Test
    fun test11ImportJudeNoChapter() = runTest {
        val source = "usfm/jude.no_chapter_or_verses.usfm"
        session = importFromResource(source)
        assertNotNull(session)

        verifyResults(
            session = session!!,
            expectSuccess = false,
            expectedBook = ExpectedBook("jude.no_chapter_or_verses.usfm", "jud", success = false, missingName = false),
            noEmptyChunks = true,
            expectAllVerses = true,
            expectedVerseCount = 0
        )
    }

    @Test
    fun test12ImportPhpNoChapter1() = runTest {
        val source = "usfm/php_usfm_NoC1.usfm"
        session = importFromResource(source)
        assertNotNull(session)

        verifyResults(
            session = session!!,
            expectSuccess = true,
            expectedBook = ExpectedBook("php_usfm_NoC1.usfm", "php", success = false, missingName = false),
            noEmptyChunks = false,
            expectAllVerses = true,
            expectedVerseCount = 74
        )
    }

    @Test
    fun test13ImportPhpNoChapter2() = runTest {
        val source = "usfm/php_usfm_NoC2.usfm"
        session = importFromResource(source)
        assertNotNull(session)

        verifyResults(
            session = session!!,
            expectSuccess = true,
            expectedBook = ExpectedBook("php_usfm_NoC2.usfm", "php", success = false, missingName = false),
            noEmptyChunks = false,
            expectAllVerses = true,
            expectedVerseCount = 74
        )
    }

    @Test
    fun test14ImportPhpChapter3OutOfOrder() = runTest {
        val source = "usfm/php_usfm_C3_out_of_order.usfm"
        session = importFromResource(source)
        assertNotNull(session)

        verifyResults(
            session = session!!,
            expectSuccess = false,
            expectedBook = ExpectedBook("php_usfm_C3_out_of_order.usfm", "php", success = false, missingName = false),
            noEmptyChunks = true,
            expectAllVerses = true
        )
    }

    @Test
    fun test15ImportPhpMissingLastChapter() = runTest {
        val source = "usfm/php_usfm_missing_last_chapter.usfm"
        session = importFromResource(source)
        assertNotNull(session)

        verifyResults(
            session = session!!,
            expectSuccess = true,
            expectedBook = ExpectedBook("php_usfm_missing_last_chapter.usfm", "php", success = false, missingName = false),
            noEmptyChunks = false,
            expectAllVerses = true,
            expectedVerseCount = 81
        )
    }

    @Test
    fun test16ImportPhpNoChapter1Marker() = runTest {
        val source = "usfm/php_usfm_NoC1_marker.usfm"
        session = importFromResource(source)
        assertNotNull(session)

        verifyResults(
            session = session!!,
            expectSuccess = true,
            expectedBook = ExpectedBook("php_usfm_NoC1_marker.usfm", "php", success = false, missingName = false),
            noEmptyChunks = true,
            expectAllVerses = true,
            expectedVerseCount = 104
        )
    }

    @Test
    fun test17ImportPhpNoChapter2Marker() = runTest {
        val source = "usfm/php_usfm_NoC2_marker.usfm"
        session = importFromResource(source)
        assertNotNull(session)

        verifyResults(
            session = session!!,
            expectSuccess = true,
            expectedBook = ExpectedBook("php_usfm_NoC2_marker.usfm", "php", success = false, missingName = false),
            noEmptyChunks = false,
            expectAllVerses = true,
            expectedVerseCount = 104
        )
    }

    @Test
    fun test18ImportPhpMissingLastChapterMarker() = runTest {
        val source = "usfm/php_usfm_missing_last_chapter_marker.usfm"
        session = importFromResource(source)
        assertNotNull(session)

        verifyResults(
            session = session!!,
            expectSuccess = true,
            expectedBook = ExpectedBook("php_usfm_missing_last_chapter_marker.usfm", "php", success = true, missingName = false),
            noEmptyChunks = false,
            expectAllVerses = true,
            expectedVerseCount = 104
        )
    }

    @Test
    fun test19ImportJudeOutOfOrderVerses() = runTest {
        val source = "usfm/jude.out_order_verses.usfm"
        session = importFromResource(source)
        assertNotNull(session)

        verifyResults(
            session = session!!,
            expectSuccess = true,
            expectedBook = ExpectedBook("jude.out_order_verses.usfm", "jud", success = false, missingName = false),
            noEmptyChunks = true,
            expectAllVerses = false,
            expectedVerseCount = 25
        )
    }

    @Test
    fun test20ImportPhpMissingInitialAndFinalVerses() = runTest {
        val source = "usfm/php_usfm_missing_initial_and_final_vs.usfm"
        session = importFromResource(source)
        assertNotNull(session)

        verifyResults(
            session = session!!,
            expectSuccess = true,
            expectedBook = ExpectedBook("php_usfm_missing_initial_and_final_vs.usfm", "php", success = false, missingName = false),
            noEmptyChunks = false,
            expectAllVerses = false,
            expectedVerseCount = 6
        )
    }

    private data class ExpectedBook(
        val filename: String,
        val book: String,
        val success: Boolean,
        val missingName: Boolean
    )

    private suspend fun importFromResource(resourcePath: String): ImportUsfmSession {
        val text = TestUtils.getResource(resourcePath)
        val filename = resourcePath.substringAfterLast("/")
        val tempDir = File.createTempFile("usfm_test_dir_", "").apply {
            delete()
            mkdirs()
            deleteOnExit()
        }
        val tempFile = File(tempDir, filename).apply {
            writeText(text)
            deleteOnExit()
        }
        val platformFile = PlatformFile(tempFile)
        val targetLanguage = catalogClient.library.getTargetLanguage("es")!!
        return processUSFM.startImport(targetLanguage, platformFile)
    }

    private suspend fun verifyResults(
        session: ImportUsfmSession,
        expectSuccess: Boolean,
        expectedBook: ExpectedBook,
        noEmptyChunks: Boolean,
        expectAllVerses: Boolean,
        expectedVerseCount: Int = -1
    ) {
        val summary = session.summary()
        assertFalse(summary.isEmpty(), "Summary should not be empty")
        assertEquals(expectSuccess, session.isSuccess, "Overall success")

        verifyBookResults(
            session = session,
            summary = summary,
            filename = expectedBook.filename,
            book = expectedBook.book,
            noErrorsExpected = expectedBook.success,
            noEmptyChunks = noEmptyChunks,
            overallSuccess = expectSuccess,
            expectAllVerses = expectAllVerses,
            expectedVerseCount = expectedVerseCount
        )

        if (expectedBook.missingName) {
            val found = session.booksMissingNames.any {
                it.description?.contains(expectedBook.filename) == true
            }
            assertTrue(found, "${expectedBook.filename} should be in missing names")
        }

        val expectedMissingCount = if (expectedBook.missingName) 1 else 0
        assertEquals(
            expectedMissingCount,
            session.booksMissingNames.size,
            "Missing name count"
        )
    }

    private fun verifyBookResults(
        session: ImportUsfmSession,
        summary: String,
        filename: String,
        book: String,
        noErrorsExpected: Boolean,
        noEmptyChunks: Boolean,
        overallSuccess: Boolean,
        expectAllVerses: Boolean,
        expectedVerseCount: Int
    ) {
        val bookLine = if (book.isNotEmpty()) {
            "${book.lowercase()} = $filename"
        } else filename

        val foundBookMarker = "Found book: "
        val expectLine = foundBookMarker + bookLine
        val resultLines = summary.split("\n")

        // Find the book in results
        var bookFound = false
        for (i in resultLines.indices) {
            if (resultLines[i].contains(expectLine)) {
                var noErrorsFound = false
                for (j in i + 1 until resultLines.size) {
                    if (resultLines[j].contains(foundBookMarker)) break
                    if (resultLines[j].contains("No errors found")) {
                        noErrorsFound = true
                        break
                    }
                }
                assertEquals(
                    noErrorsExpected, noErrorsFound,
                    "$bookLine found, no errors expected $noErrorsExpected"
                )
                bookFound = true
                break
            }
        }
        assertTrue(bookFound, "$bookLine not found in summary")

        // Verify chapters and verses
        var verseCount = 0
        if (overallSuccess && book.isNotEmpty()) {
            val projects = session.importedProjects
            assertTrue(projects.isNotEmpty(), "Import projects should not be empty")

            for (project in projects) {
                val chunksList = catalogClient.library.getChunkMarkers(
                    book.lowercase(), "en-US"
                )
                assertFalse(chunksList.isEmpty(), "Chunk list should not be empty")
                parseChunks(chunksList)

                for (chapter in chapters!!) {
                    val chapterPath = File(project, getRightChapterLength(chapter))
                    assertTrue(chapterPath.exists(), "Chapter missing $chapterPath")

                    val chapterFrameSlugs = chunks[chapter]!!
                    for (i in chapterFrameSlugs.indices) {
                        val slug = chapterFrameSlugs[i]
                        var expectCount = -1
                        if (i + 1 < chapterFrameSlugs.size) {
                            val nextStart = chapterFrameSlugs[i + 1].toInt()
                            if (nextStart > 0) {
                                expectCount = nextStart - slug.toInt()
                            }
                        }

                        val chunkPath = File(
                            chapterPath,
                            getRightFileNameLength(slug) + ".txt"
                        )
                        assertTrue(chunkPath.exists(), "Chunk missing $chunkPath")

                        val chunk = FileUtilities.readFileToString(chunkPath)
                        val count = getVerseCount(chunk)
                        verseCount += count

                        if (noEmptyChunks) {
                            assertFalse(chunk.isEmpty(), "Chunk is empty $chunkPath")
                            assertTrue(count > 0, "Verse count should not be zero in $chunkPath")
                            if (expectCount >= 0 && expectAllVerses) {
                                assertEquals(expectCount, count, "Verse count in $chunkPath")
                            }
                        }
                    }
                }
            }
        }

        if (expectedVerseCount >= 0) {
            assertEquals(expectedVerseCount, verseCount, "Total verse count")
        }
    }

    private fun parseChunks(chunksList: List<ChunkMarker>): Boolean {
        chunks = HashMap()
        return try {
            for (marker in chunksList) {
                chunks.getOrPut(marker.chapter) { mutableListOf() }.add(marker.verse)
            }
            chapters = chunks.keys.sorted().toTypedArray()
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun getRightChapterLength(chapterN: String): String {
        val n = Util.strToInt(chapterN, -1)
        if (n in 0..99) {
            val padded = "00$chapterN"
            return padded.substring(padded.length - 2)
        }
        return chapterN
    }

    private fun getRightFileNameLength(fileName: String): String {
        val n = Util.strToInt(fileName, -1)
        if (n in 0..99 && fileName.length != 2) {
            val padded = "00$fileName"
            return padded.substring(padded.length - 2)
        }
        return fileName
    }

    private fun getVerseCount(text: String): Int {
        var count = 0
        val matcher = PATTERN_USFM_VERSE_SPAN.matcher(text)
        while (matcher.find()) {
            val verse = matcher.group(1) ?: continue
            val range = getVerseRange(verse) ?: break
            count += if (range[1] > 0) range[1] - range[0] + 1 else 1
        }
        return count
    }

    private fun getVerseRange(verse: String): IntArray? {
        return try {
            intArrayOf(verse.toInt(), 0)
        } catch (_: NumberFormatException) {
            val parts = verse.split("-")
            if (parts.size < 2) null
            else intArrayOf(parts[0].toInt(), parts[1].toInt())
        }
    }

    companion object {
        private val PATTERN_USFM_VERSE_SPAN = Pattern.compile(USFMVerseSpan.PATTERN)
    }
}