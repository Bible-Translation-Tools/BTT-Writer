package org.bibletranslationtools.writer.integration.core

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.bibletranslationtools.logger.Logger
import org.bibletranslationtools.resourcecatalog.ResourceCatalogClient
import org.bibletranslationtools.resourcecatalog.library.models.TargetLanguage
import org.bibletranslationtools.writer.DirectoryProvider
import org.bibletranslationtools.writer.TestDirectoryProvider
import org.bibletranslationtools.writer.core.ImportUsfmSession
import org.bibletranslationtools.writer.core.ProcessUSFM
import org.bibletranslationtools.writer.core.Profile
import org.bibletranslationtools.writer.core.TargetTranslation
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.di.platformModule
import org.bibletranslationtools.writer.di.sharedModule
import org.bibletranslationtools.writer.TestUtils
import org.bibletranslationtools.writer.usecases.ExportProjects
import org.bibletranslationtools.writer.utils.FileUtilities
import org.bibletranslationtools.writer.utils.Zip
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.context.GlobalContext.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.util.Locale
import java.util.regex.Matcher
import java.util.regex.Pattern

class ExportUsfmTest : KoinTest {

    private val directoryProvider: DirectoryProvider by inject()
    private val catalogClient: ResourceCatalogClient by inject()
    private val exportProjects: ExportProjects by inject()
    private val processUSFM: ProcessUSFM by inject()

    private var tempFolder: File? = null
    private var targetLanguage: TargetLanguage? = null
    private var usfmSession: ImportUsfmSession? = null
    private var outputFile: File? = null
    private var targetTranslation: TargetTranslation? = null
    private var errorLog: String? = null

    @Before
    fun setUp() {
        errorLog = null
        Logger.flush()
        startKoin {
            modules(
                sharedModule,
                platformModule,
                module { single<DirectoryProvider> { TestDirectoryProvider() } },
                module { single<Preference> { mockk(relaxed = true) } },
                module { single<Profile> { mockk(relaxed = true) } }
            )
        }
        runBlocking { directoryProvider.deployDefaultLibrary() }
        targetLanguage = catalogClient.library.getTargetLanguage("aae")
    }

    @After
    fun tearDown() {
        usfmSession?.cleanup()
        FileUtilities.deleteQuietly(tempFolder)
        stopKoin()
    }

    @Test
    @Throws(Exception::class)
    fun test01ValidExportMarkSingle() = runTest {
        //given
        val zipFileName: String? = null
        val separateChapters = false
        val source = "mrk.usfm"
        importTestTranslation(source)

        //when
        targetTranslation?.let { translation ->
            val result = exportProjects.exportUSFM(
                translation,
                PlatformFile(outputFile!!)
            )

            Assert.assertTrue(result.success)

            val usfmOutput = result.file

            //then
            verifyExportedUsfmFile(zipFileName, separateChapters, source, usfmOutput)
        }
    }

    @Test
    @Throws(Exception::class)
    fun test02ValidExportPsalmSingle() = runTest {
        //given
        val zipFileName: String? = null
        val separateChapters = false
        val source = "19-PSA.usfm"
        importTestTranslation(source)

        //when
        targetTranslation?.let { translation ->
            val result = exportProjects.exportUSFM(
                translation,
                PlatformFile(outputFile!!)
            )

            Assert.assertTrue(result.success)

            val usfmOutput = result.file

            //then
            verifyExportedUsfmFile(zipFileName, separateChapters, source, usfmOutput)
        }
    }

    @Test
    @Throws(Exception::class)
    fun test03ValidExportJudeSingle() = runTest {
        //given
        val zipFileName: String? = null
        val separateChapters = false
        val source = "66-JUD.usfm"
        importTestTranslation(source)

        //when
        targetTranslation?.let { translation ->
            val result = exportProjects.exportUSFM(
                translation,
                PlatformFile(outputFile!!)
            )

            Assert.assertTrue(result.success)

            val usfmOutput = result.file

            //then
            verifyExportedUsfmFile(zipFileName, separateChapters, source, usfmOutput)
        }
    }

    @Test
    @Throws(Exception::class)
    fun test04ValidExportJobSingle() = runTest {
        //given
        val zipFileName: String? = null
        val separateChapters = false
        val source = "18-JOB.usfm"
        importTestTranslation(source)

        //when
        targetTranslation?.let { translation ->
            val result = exportProjects.exportUSFM(
                translation,
                PlatformFile(outputFile!!)
            )

            Assert.assertTrue(result.success)

            val usfmOutput = result.file

            //then
            verifyExportedUsfmFile(zipFileName, separateChapters, source, usfmOutput)
        }
    }

    @Test
    @Throws(Exception::class)
    fun test08ValidExportIsaiahSingle() = runTest {
        //given
        val zipFileName: String? = null
        val separateChapters = false
        val source = "23-ISA.usfm"
        importTestTranslation(source)

        //when
        targetTranslation?.let { translation ->
            val result = exportProjects.exportUSFM(
                translation,
                PlatformFile(outputFile!!)
            )

            Assert.assertTrue(result.success)

            val usfmOutput = result.file

            //then
            verifyExportedUsfmFile(zipFileName, separateChapters, source, usfmOutput)
        }
    }

    @Test
    @Throws(Exception::class)
    fun test09ValidExportJeremiahSingle() = runTest {
        //given
        val zipFileName: String? = null
        val separateChapters = false
        val source = "24-JER.usfm"
        importTestTranslation(source)

        //when
        targetTranslation?.let { translation ->
            val result = exportProjects.exportUSFM(
                translation,
                PlatformFile(outputFile!!)
            )

            Assert.assertTrue(result.success)

            val usfmOutput = result.file

            //then
            verifyExportedUsfmFile(zipFileName, separateChapters, source, usfmOutput)
        }
    }

    @Test
    @Throws(Exception::class)
    fun test10ValidExportLukeSingle() = runTest {
        //given
        val zipFileName: String? = null
        val separateChapters = false
        val source = "43-LUK.usfm"
        importTestTranslation(source)

        //when
        targetTranslation?.let { translation ->
            val result = exportProjects.exportUSFM(
                translation,
                PlatformFile(outputFile!!)
            )

            Assert.assertTrue(result.success)

            val usfmOutput = result.file

            //then
            verifyExportedUsfmFile(zipFileName, separateChapters, source, usfmOutput)
        }
    }

    @Test
    @Throws(Exception::class)
    fun test11ValidExportJohnSingle() = runTest {
        //given
        val zipFileName: String? = null
        val separateChapters = false
        val source = "44-JHN.usfm"
        importTestTranslation(source)

        //when
        targetTranslation?.let { translation ->
            val result = exportProjects.exportUSFM(
                translation,
                PlatformFile(outputFile!!)
            )

            Assert.assertTrue(result.success)

            val usfmOutput = result.file

            //then
            verifyExportedUsfmFile(zipFileName, separateChapters, source, usfmOutput)
        }
    }

    /**
     * match all the book identifiers
     * 
     * @param input
     * @param output
     */
    private fun verifyBookID(input: String, output: String) {
        val bookTitle = extractString(input, ImportUsfmSession.PATTERN_BOOK_TITLE_MARKER)
        val bookLongName = extractString(input, ImportUsfmSession.PATTERN_BOOK_LONG_NAME_MARKER)
        val bookShortName = extractString(input, ImportUsfmSession.PATTERN_BOOK_ABBREVIATION_MARKER)
        val bookTitleOut = extractString(output, ImportUsfmSession.PATTERN_BOOK_TITLE_MARKER)
        val bookLongNameOut = extractString(output, ImportUsfmSession.PATTERN_BOOK_LONG_NAME_MARKER)
        val bookShortNameOut = extractString(
            output,
            ImportUsfmSession.PATTERN_BOOK_ABBREVIATION_MARKER
        )

        val bookID = extractString(input, ImportUsfmSession.ID_TAG_MARKER)
        val bookIdParts: Array<String?> =
            bookID!!.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val bookIDOut = extractString(output, ImportUsfmSession.ID_TAG_MARKER)
        val bookIdOutParts: Array<String?> =
            bookIDOut!!.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        Assert.assertEquals(
            "Input and output book titles (\\toc1) should equal",
            bookTitle!!.lowercase(Locale.getDefault()),
            bookTitleOut!!.lowercase(Locale.getDefault())
        )
        Assert.assertEquals(
            "Input and output book codes (\\toc3) should equal",
            bookShortName!!.lowercase(Locale.getDefault()),
            bookShortNameOut!!.lowercase(Locale.getDefault())
        )
        Assert.assertEquals(
            "Input and output book long name (\\toc2) should equal",
            bookLongName!!.lowercase(Locale.getDefault()),
            bookLongNameOut!!.lowercase(Locale.getDefault())
        )
        Assert.assertEquals(
            "Input and output book ID code (\\id) should equal",
            bookIdParts[0]!!.lowercase(Locale.getDefault()),
            bookIdOutParts[0]!!.lowercase(Locale.getDefault())
        )
    }

    /**
     * match regexPattern and get string in group 1 if present
     * 
     * @param text
     * @param regexPattern
     * @return
     */
    private fun extractString(text: CharSequence, regexPattern: Pattern): String? {
        if (text.isNotEmpty()) {
            // find instance
            val matcher = regexPattern.matcher(text)
            var foundItem: String? = null
            if (matcher.find()) {
                foundItem = matcher.group(1)
                return foundItem.trim { it <= ' ' }
            }
        }

        return null
    }

    /**
     * handles validation of exported USFM file by comparing to original imported USFM file
     * 
     * @param zipFileName      - to determine if zip file was expected
     * @param separateChapters
     * @param source
     * @param usfmOutput       - actual output file
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun verifyExportedUsfmFile(
        zipFileName: String?,
        separateChapters: Boolean,
        source: String?,
        usfmOutput: PlatformFile?
    ) {
        Assert.assertNotNull("exported file", usfmOutput)
        errorLog = ""

        val usfmOutputFile = File(usfmOutput!!.path)

        if (zipFileName == null) {
            if (!separateChapters) {
                verifySingleUsfmFile(source, usfmOutputFile)
            } else {
                Assert.fail("separate chapters without zip is not supported")
            }
        } else {
            if (separateChapters) {
                verifyUsfmZipFile(source, usfmOutputFile)
            } else {
                Assert.fail("single book with zip is not supported")
            }
        }

        if (!errorLog!!.isEmpty()) {
            Assert.fail("Errors found:\n$errorLog")
        }
    }

    /**
     * handles validation of exported USFM zip file containing chapters by comparing to original
     * imported USFM file
     * 
     * @param source
     * @param usfmOutput
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun verifyUsfmZipFile(source: String?, usfmOutput: File?) {
        val unzipFolder = File(tempFolder, "scratch_test_unzip")
        FileUtilities.forceMkdir(unzipFolder)

        val zipStream: InputStream = FileInputStream(usfmOutput)
        Zip.unzipFromStream(zipStream, unzipFolder)
        val usfmFiles = unzipFolder.listFiles()

        val usfmInputText: String = TestUtils.getResource("usfm/$source")

        val inputMatcher = ImportUsfmSession.PATTERN_CHAPTER_NUMBER_MARKER.matcher(usfmInputText)

        var lastInputChapterStart = -1
        var chapterIn: String? = ""
        var chapterInInt = -1
        while (inputMatcher.find()) {
            chapterIn = inputMatcher.group(1) // chapter number in input
            chapterInInt = chapterIn.toInt()

            if (usfmFiles!!.size < chapterInInt) {
                addErrorMsg(
                    "chapter count " + usfmFiles.size + "' should be greater than or " +
                            "equal to chapter number '" + chapterInInt + "'\n"
                )
            }

            if (chapterInInt > 1) {
                // verify verses in last chapter
                val inputChapter = usfmInputText.substring(
                    lastInputChapterStart,
                    inputMatcher.start()
                )
                val outputChapter: String =
                    FileUtilities.readFileToString(usfmFiles[chapterInInt - 1])
                verifyBookID(usfmInputText, outputChapter)
                compareVersesInChapter(chapterInInt - 1, inputChapter, outputChapter)
            }

            lastInputChapterStart = inputMatcher.end()
        }

        if (usfmFiles!!.size != chapterInInt + 1) {
            addErrorMsg("chapter count " + usfmFiles.size + "' should be  '" + (chapterInInt + 1) + "'\n")
        }

        // verify verses in last chapter
        val inputChapter = usfmInputText.substring(lastInputChapterStart)
        val outputChapter: String = FileUtilities.readFileToString(usfmFiles[chapterInInt])
        verifyBookID(usfmInputText, outputChapter)
        compareVersesInChapter(chapterInInt, inputChapter, outputChapter)
    }

    /**
     * queue up error messages
     * 
     * @param error
     */
    private fun addErrorMsg(error: String?) {
        errorLog = error + errorLog
    }

    /**
     * handles validation of exported USFM file by comparing to original imported USFM file
     * 
     * @param source
     * @param usfmOutput
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun verifySingleUsfmFile(source: String?, usfmOutput: File) {
        val usfmOutputText: String = FileUtilities.readFileToString(usfmOutput)

        val usfmInputText: String = TestUtils.getResource("usfm/$source")

        verifyBookID(usfmInputText, usfmOutputText)

        val inputMatcher = ImportUsfmSession.PATTERN_CHAPTER_NUMBER_MARKER.matcher(usfmInputText)
        val outputMatcher = ImportUsfmSession.PATTERN_CHAPTER_NUMBER_MARKER.matcher(usfmOutputText)

        var lastInputChapterStart = -1
        var lastOutputChapterStart = -1
        var chapterIn: String?
        var chapterInInt = -1
        while (inputMatcher.find()) {
            chapterIn = inputMatcher.group(1) // chapter number in input
            chapterInInt = chapterIn.toInt()

            if (outputMatcher.find()) {
                val chapterOut = outputMatcher.group(1) // chapter number in output
                val chapterOutInt = chapterOut!!.toInt()
                if (chapterInInt != chapterOutInt) {
                    addErrorMsg(
                        "chapter input: " + chapterInInt + "\n does not match chapter " +
                                "output:" + chapterOutInt + "\n"
                    )
                }
            } else {
                addErrorMsg("chapter '$chapterIn' missing in output\n")
                break
            }

            if (chapterInInt > 1) {
                // verify verses in last chapter
                val inputChapter = usfmInputText.substring(
                    lastInputChapterStart,
                    inputMatcher.start()
                )
                val outputChapter = usfmOutputText.substring(
                    lastOutputChapterStart,
                    outputMatcher.start()
                )
                compareVersesInChapter(chapterInInt - 1, inputChapter, outputChapter)
            }

            lastInputChapterStart = inputMatcher.end()
            lastOutputChapterStart = outputMatcher.end()
        }

        if (outputMatcher.find()) {
            addErrorMsg("extra chapter in output: " + outputMatcher.group(1) + "\n")
        }

        // verify verses in last chapter
        val inputChapter = usfmInputText.substring(lastInputChapterStart)
        val outputChapter = usfmOutputText.substring(lastOutputChapterStart)
        compareVersesInChapter(chapterInInt, inputChapter, outputChapter)
    }

    /**
     * compares the verses in exported chapter to make sure they are in same order and have same
     * contents as imported chapter
     * 
     * @param chapter
     * @param inputChapter
     * @param outputChapter
     */
    private fun compareVersesInChapter(chapter: Int, inputChapter: String, outputChapter: String) {
        val inputVerseMatcher = ImportUsfmSession.PATTERN_USFM_VERSE_SPAN.matcher(inputChapter)
        val outputVerseMatcher = ImportUsfmSession.PATTERN_USFM_VERSE_SPAN.matcher(outputChapter)
        var lastInputVerseStart = -1
        var lastOutputVerseStart = -1
        var verseIn: String? = ""
        while (inputVerseMatcher.find()) {
            verseIn = inputVerseMatcher.group(1) // verse number in input
            if (outputVerseMatcher.find()) {
                val verseOut = outputVerseMatcher.group(1) // verse number in output
                if (verseIn != verseOut) {
                    addErrorMsg(
                        "in chapter '" + chapter + "' verse input '" + verseIn + "'\n " +
                                "does not match verse output '" + verseOut + "'\n"
                    )
                    return
                }
            } else {
                addErrorMsg(
                    "in chapter '" + chapter + "', verse '" + verseIn + "' missing in " +
                            "output\n"
                )
                return
            }

            if (lastInputVerseStart > 0) {
                val inputVerse = inputChapter.substring(
                    lastInputVerseStart,
                    inputVerseMatcher.start()
                )
                val outputVerse = outputChapter.substring(
                    lastOutputVerseStart,
                    outputVerseMatcher.start()
                )
                compareVerses(chapter, verseIn, inputVerse, outputVerse)
            }

            lastInputVerseStart = inputVerseMatcher.end()
            lastOutputVerseStart = outputVerseMatcher.end()
        }

        if (outputVerseMatcher.find()) {
            addErrorMsg(
                "In chapter '$chapter' extra verse in output: '" + outputVerseMatcher.group(
                    1
                ) + "\n"
            )
        }

        val inputVerse = inputChapter.substring(lastInputVerseStart)
        val outputVerse = outputChapter.substring(lastOutputVerseStart)
        compareVerses(chapter, verseIn, inputVerse, outputVerse)
    }

    /**
     * compares contents of verses
     * 
     * @param chapterNum
     * @param verseIn
     * @param inputVerse
     * @param outputVerse
     */
    private fun compareVerses(
        chapterNum: Int, verseIn: String?, inputVerse: String,
        outputVerse: String
    ) {
        var input = inputVerse
        var output = outputVerse

        if (input == output) {
            return
        }

        //if not exact match, try stripping section marker and removing double new-lines

        //remove extra white space
        input = cleanUpVerse(input)
        output = cleanUpVerse(output)

        if (input != output) {
            if (output != input + "\n") {
                return
            }
            if (input != output + "\n") {
                return
            }
            addErrorMsg("In chapter '$chapterNum' verse '$verseIn' verse input:\n$input\n does not match output:\n$output\n")
        }
    }

    /**
     * clean up by stripping section marker and removing double new-lines
     * 
     * @param text
     * @return
     */
    private fun cleanUpVerse(text: String): String {
        var text = text
        val chapterLabelMatcher: Matcher = PATTERN_CHAPTER_LABEL_MARKER.matcher(text)
        if (chapterLabelMatcher.find()) {
            text = text.substring(0, chapterLabelMatcher.start())
        }

        text = text.replace("\\s5\n", "\n") // remove section markers
        text = text.replace("\\s5 \n", "\n") // remove section markers
        text = replaceAll(text, "\n\n", "\n") // remove double new-lines
        text = replaceAll(text, "\n\n", "\n") // remove double new-lines
        text = replaceAll(text, "\n \n", "\n") // remove double new-lines
        return text
    }

    /**
     * repeatedly replaces strings - useful
     * 
     * @param text
     * @param target
     * @param replacement
     * @return
     */
    private fun replaceAll(
        text: String,
        target: String,
        replacement: String
    ): String {
        var oldText: String? = null
        var newText: String = text

        while (newText != oldText) {
            oldText = newText
            newText = newText.replace(target, replacement)
        }

        return newText
    }

    /**
     * import a usfm file to be used for export testing.
     * 
     * @param source
     */
    private fun importTestTranslation(source: String?) = runTest {
        targetLanguage?.let { language ->
            val text = TestUtils.getResource("usfm/$source")
            val tempDir = File.createTempFile("usfm_export_test_", "").apply {
                delete()
                mkdirs()
                deleteOnExit()
            }
            val tempFile = File(tempDir, source!!).apply {
                writeText(text)
                deleteOnExit()
            }
            val platformFile = PlatformFile(tempFile)

            usfmSession = processUSFM.startImport(language, platformFile)

            Assert.assertNotNull(usfmSession)
            Assert.assertTrue("import usfm test file should succeed", usfmSession!!.isSuccess)
            val imports: List<File> = usfmSession!!.importedProjects
            Assert.assertEquals("import usfm test file should succeed", 1, imports.size.toLong())

            val projectFolder = imports[0]
            tempFolder = projectFolder.parentFile
            outputFile = File(tempFolder, "scratch_test")
            targetTranslation = TargetTranslation.open(projectFolder, null)
        }
    }

    companion object {
        const val CHAPTER_LABEL_MARKER: String = "\\\\cl\\s([^\\n]*)"
        val PATTERN_CHAPTER_LABEL_MARKER: Pattern = Pattern.compile(CHAPTER_LABEL_MARKER)
    }
}