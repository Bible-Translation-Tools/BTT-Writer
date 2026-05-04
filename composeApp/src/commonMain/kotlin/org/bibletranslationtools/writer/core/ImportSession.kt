package org.bibletranslationtools.writer.core

import btt_writer.composeapp.generated.resources.Res
import btt_writer.composeapp.generated.resources.building_manifest
import btt_writer.composeapp.generated.resources.chapter_count_invalid
import btt_writer.composeapp.generated.resources.chapter_out_of_order
import btt_writer.composeapp.generated.resources.could_not_find_chapter
import btt_writer.composeapp.generated.resources.could_not_find_verses_in_chapter
import btt_writer.composeapp.generated.resources.could_not_parse
import btt_writer.composeapp.generated.resources.could_not_parse_chapter
import btt_writer.composeapp.generated.resources.error_prefix
import btt_writer.composeapp.generated.resources.error_reading_file
import btt_writer.composeapp.generated.resources.extra_verses_in_chapter
import btt_writer.composeapp.generated.resources.file_read_error_detail
import btt_writer.composeapp.generated.resources.file_write_error
import btt_writer.composeapp.generated.resources.file_write_for_verse
import btt_writer.composeapp.generated.resources.finished_loading
import btt_writer.composeapp.generated.resources.found_book
import btt_writer.composeapp.generated.resources.initializing_import
import btt_writer.composeapp.generated.resources.missing_book_name
import btt_writer.composeapp.generated.resources.missing_book_short_name
import btt_writer.composeapp.generated.resources.missing_chapter_n
import btt_writer.composeapp.generated.resources.missing_verses_in_chapter
import btt_writer.composeapp.generated.resources.no_chapter
import btt_writer.composeapp.generated.resources.no_chunk_list
import btt_writer.composeapp.generated.resources.no_error
import btt_writer.composeapp.generated.resources.no_verse
import btt_writer.composeapp.generated.resources.processing_chapter
import btt_writer.composeapp.generated.resources.warning_prefix
import btt_writer.composeapp.generated.resources.zip_read_error
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.readString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bibletranslationtools.logger.Logger
import org.bibletranslationtools.resourcecatalog.ResourceCatalogClient
import org.bibletranslationtools.resourcecatalog.library.models.ChunkMarker
import org.bibletranslationtools.resourcecatalog.library.models.TargetLanguage
import org.bibletranslationtools.resourcecontainer.Resource
import org.bibletranslationtools.writer.DirectoryProvider
import org.bibletranslationtools.writer.Platform
import org.bibletranslationtools.writer.core.Translator.Companion.TXT_EXTENSION
import org.bibletranslationtools.writer.core.Translator.Companion.USFM_EXTENSION
import org.bibletranslationtools.writer.inputStream
import org.bibletranslationtools.writer.rendering.spannables.USFMNoteSpan
import org.bibletranslationtools.writer.rendering.spannables.USFMVerseSpan
import org.bibletranslationtools.writer.utils.FileUtilities
import org.bibletranslationtools.writer.utils.Util
import org.bibletranslationtools.writer.utils.Zip
import org.bibletranslationtools.writer.utils.sortedNumerically
import org.bibletranslationtools.writer.utils.toNumericallySortedMap
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import java.io.File
import java.io.InputStream
import java.util.Locale
import java.util.regex.Pattern

/**
 * Single-use session for importing one PlatformFile into a project structure.
 * All mutable state lives here so [ProcessUSFM] can stay stateless and singleton.
 */
class ImportSession internal constructor(
    private val platform: Platform,
    private val directoryProvider: DirectoryProvider,
    private val profile: Profile,
    private val catalogClient: ResourceCatalogClient,
    private val targetLanguage: TargetLanguage,
    private val onProgress: (Float, String?) -> Unit
) {
    // Workspace folders
    private val tempDir: File = File(
        directoryProvider.cacheDir,
        System.currentTimeMillis().toString()
    ).also { it.mkdirs() }
    private val tempSrc: File = File(tempDir, "source").also { it.mkdirs() }
    private val projectsFolder: File = File(tempDir, "output").also { it.mkdirs() }

    private var projectFolder: File? = null

    // Book-level state
    private val sourceFiles = mutableListOf<File>()
    private val foundBooks = mutableListOf<String>()
    private val errorsByBook = mutableListOf<String>()
    private val _importedProjects = mutableListOf<File>()
    private val _booksMissingNames = mutableListOf<MissingNameItem>()
    private var currentBook = 0

    // Chapter-level state
    private val chunks = HashMap<String, List<String>>()
    private val chapters = mutableListOf<String>()
    private var bookName: String? = null
    private var bookShortName: String? = null
    private var chapter: String? = null
    private var lastChapter = 0
    private var currentChapter = 0
    private var chapterCount = 1

    /**
     * True if the overall import succeeded. May be false even after [run] completes
     * if some books had errors or are awaiting names.
     */
    var isSuccess: Boolean = false
        private set

    /**
     * Books that were parsed but lack a usable name; the caller should resolve
     * these via [processText] before proceeding to the actual import.
     */
    val booksMissingNames: List<MissingNameItem>
        get() = _booksMissingNames.toList()

    /**
     * Project folders ready to be imported into the app's translation store.
     * Empty until [run] completes successfully.
     */
    val importedProjects: List<File>
        get() = _importedProjects.toList()

    // ---------------------------------------------------------------
    // Entry points
    // ---------------------------------------------------------------

    /**
     * Multi-line summary of the import results (per-book status & errors).
     */
    suspend fun summary(): String = buildSummary()

    /**
     * Strips temp-folder paths from a file path for display.
     */
    fun shortPath(path: String): String = getShortFilePath(path)

    internal suspend fun run(file: PlatformFile) {
        updateStatus(Res.string.initializing_import)
        isSuccess = try {
            readFile(file)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to read ${file.path}", e)
            addError(Res.string.file_read_error_detail, file.path)
            false
        }
        updateStatus(Res.string.finished_loading)
    }

    /**
     * Re-processes a single book with a user-supplied name. Used to resolve
     * entries from [booksMissingNames] one at a time.
     *
     * Returns the updated [isSuccess] state after this book is processed.
     */
    suspend fun processText(
        book: String,
        name: String,
        useName: String
    ): Boolean = withContext(Dispatchers.IO) {
        // Position the session as if processing this book directly,
        // bypassing the missing-name prompt.
        val ok = processBook(book, name, promptForName = false, useName = useName)
        // Recompute overall success — true only if every book has succeeded
        isSuccess = isSuccess && ok && _booksMissingNames.isEmpty()
        ok
    }

    fun cleanup() {
        FileUtilities.deleteQuietly(tempDir)
    }

    suspend fun runText(text: String, name: String, useName: String?): ImportResult {
        currentBook = 0
        _booksMissingNames.clear()
        errorsByBook.clear()
        foundBooks.clear()
        val success = processBook(text, name, promptForName = true, useName = useName)
        return toResult(success)
    }

    suspend fun toResult(success: Boolean): ImportResult = ImportResult(
        success = success,
        importedProjects = importedProjects.toList(),
        booksMissingNames = booksMissingNames.toList(),
        summary = buildSummary()
    )

    // ---------------------------------------------------------------
    // File reading
    // ---------------------------------------------------------------

    private suspend fun readFile(file: PlatformFile): Boolean {
        return if (file.extension.equals("zip", ignoreCase = true)) {
            try {
                file.inputStream().use { readZipStream(it) }
            } catch (e: Exception) {
                Logger.e(TAG, "error reading zip", e)
                addError(Res.string.zip_read_error)
                false
            }
        } else {
            processBook(file.readString(), file.path)
        }
    }

    private suspend fun readZipStream(stream: InputStream): Boolean {
        Zip.unzipFromStream(stream, tempSrc)
        addFilesInFolder(tempSrc)
        Logger.i(TAG, "found files: ${sourceFiles.joinToString("\n")}")

        var allSucceeded = true
        sourceFiles.forEachIndexed { index, file ->
            currentBook = index
            currentChapter = 0
            updateStatus(Res.string.found_book, file.name)

            val ok = try {
                processBook(file)
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to process ${file.name}", e)
                addError(Res.string.could_not_parse, getShortFilePath(file.toString()))
                false
            }
            allSucceeded = allSucceeded && ok
        }
        currentBook = (sourceFiles.size - 1).coerceAtLeast(0)
        return allSucceeded
    }

    private suspend fun processBook(file: File): Boolean {
        return try {
            val text = FileUtilities.readFileToString(file)
            processBook(text, file.name)
        } catch (e: Exception) {
            Logger.e(TAG, "error reading book $file", e)
            addError(Res.string.error_reading_file, file.name)
            false
        }
    }

    // ---------------------------------------------------------------
    // Core USFM processing
    // ---------------------------------------------------------------

    private suspend fun processBook(
        book: String,
        name: String,
        promptForName: Boolean = true,
        useName: String? = null
    ): Boolean {
        bookShortName = ""
        val description = getShortFilePath(name)
        setBookName("", description)

        return try {
            currentChapter = 0
            chapterCount = 1

            extractBookID(book)
            if (useName != null) bookShortName = useName

            if (bookShortName.isNullOrEmpty()) {
                addError(Res.string.missing_book_short_name)
                _booksMissingNames.add(MissingNameItem(name, null, book))
                return promptForName
            }
            bookShortName = bookShortName?.lowercase(Locale.getDefault())
            setBookName(bookShortName!!, description)

            if (!hasVerses(book)) {
                addError(Res.string.no_verse)
                return false
            }

            projectFolder = File(
                File(projectsFolder, bookShortName!!),
                "${bookShortName}-${targetLanguage.slug}"
            )

            if (bookName.isNullOrEmpty()) {
                addError(Res.string.missing_book_name)
                bookName = bookShortName
            }

            val markers = loadChunkMarkers()
            if (markers.isEmpty()) {
                addWarning(Res.string.no_chunk_list, bookShortName!!)
                _booksMissingNames.add(MissingNameItem(bookName, bookShortName, book))
                return promptForName
            }

            applyChunks(markers)

            if (!extractChaptersFromBook(book)) return false

            currentChapter = chapterCount + 1
            updateStatus(Res.string.building_manifest)
            if (!buildManifest()) return false

            _importedProjects.add(projectFolder!!)
            true
        } catch (e: Exception) {
            Logger.e(TAG, "error parsing book", e)
            false
        }
    }

    private fun loadChunkMarkers(): List<ChunkMarker> {
        val versifications = catalogClient.library.getVersifications("en")
        return bookShortName?.let { shortName ->
            catalogClient.library.getChunkMarkers(shortName, versifications[0].slug)
        } ?: emptyList()
    }

    private fun applyChunks(markers: List<ChunkMarker>) {
        val parsed = parseChunks(markers)
        chapters.clear()
        chapters.addAll(parsed.chapters.sortedNumerically())

        chunks.clear()
        chunks.putAll(
            parsed.chunks
                .toNumericallySortedMap()
                .mapValues { (_, value) -> value.sortedNumerically() }
        )
        chapterCount = chapters.size
    }

    private fun extractBookID(book: String) {
        bookName = extractString(book, PATTERN_BOOK_TITLE_MARKER)
        bookShortName = extractString(book, PATTERN_BOOK_ABBREVIATION_MARKER)

        extractString(book, ID_TAG_MARKER)?.let { idString ->
            val tags = idString.split(" ".toRegex())
            if (tags.isNotEmpty()) {
                bookShortName = tags[0]
            }
        }
    }

    private suspend fun buildManifest(): Boolean {
        return try {
            TargetTranslation.create(
                directoryProvider,
                platform,
                profile.nativeSpeaker,
                TranslationFormat.USFM,
                targetLanguage,
                bookShortName!!,
                ResourceType.TEXT,
                Resource.REGULAR_SLUG,
                projectFolder!!
            )
            true
        } catch (e: Exception) {
            addError(Res.string.file_write_error)
            Logger.e(TAG, "failed to build manifest", e)
            false
        }
    }

    // ---------------------------------------------------------------
    // Chapter extraction
    // ---------------------------------------------------------------

    private suspend fun extractChaptersFromBook(text: CharSequence): Boolean {
        chapter = null
        lastChapter = 0

        val matcher = PATTERN_CHAPTER_NUMBER_MARKER.matcher(text)
        var lastIndex = 0
        var foundChapter = false
        var success = true

        while (matcher.find()) {
            foundChapter = true
            val section = text.subSequence(lastIndex, matcher.start())
            val chapterNumber = matcher.group(1) ?: "0"
            currentChapter = chapterNumber.toInt()

            if (currentChapter > chapters.size) break
            if (currentChapter <= 0) continue

            val expectedChapter = lastChapter + 1
            success = if (currentChapter != expectedChapter) {
                if (currentChapter > expectedChapter) {
                    val gapResult = processChapterGap(section, lastChapter, currentChapter)
                    lastChapter = currentChapter - 1
                    gapResult
                } else {
                    if (currentChapter == lastChapter) continue
                    Logger.e(TAG, "out of order chapter $chapter after $lastChapter")
                    addError(
                        Res.string.chapter_out_of_order,
                        chapter ?: "",
                        lastChapter.toString()
                    )
                    return false
                }
            } else {
                breakUpChapter(section, chapter ?: "")
            }

            if (!success) break

            lastChapter++
            chapter = chapterNumber
            lastIndex = matcher.end()
        }

        if (!foundChapter) {
            Logger.e(TAG, "no chapters")
            addError(Res.string.no_chapter)
            return false
        }

        if (success) {
            val finalSection = text.subSequence(lastIndex, text.length)
            success = breakUpChapter(finalSection, chapter ?: "")
            chapter?.let { lastChapter = it.toInt() }
        }

        if (success) {
            val ch = chapter
            currentChapter = ch?.toInt() ?: 0
            if (ch == null || currentChapter != chapters.size) {
                if (currentChapter < chapters.size) {
                    success = processChapterGap("", currentChapter + 1, chapters.size + 1)
                } else {
                    addWarning(
                        Res.string.chapter_count_invalid,
                        chapters.size.toString(),
                        ch ?: "(null)"
                    )
                    return false
                }
            }
        }
        return success
    }

    private suspend fun processChapterGap(
        section: CharSequence,
        missingStart: Int,
        missingEnd: Int
    ): Boolean {
        val start = if (missingStart <= 0) {
            Logger.w(TAG, "missing chapter 1")
            addWarning(Res.string.missing_chapter_n, "1")
            1
        } else missingStart

        val success = breakUpChapter(section, start.toString())

        for (i in start + 1 until missingEnd) {
            Logger.w(TAG, "missing chapter $i")
            addWarning(Res.string.missing_chapter_n, i.toString())
            breakUpChapter("", i.toString())
        }
        return success
    }

    private suspend fun breakUpChapter(text: CharSequence, currentChapterStr: String): Boolean {
        val cleaned = text.toString().replace("\r\n".toRegex(), "\n")

        if (currentChapterStr.isEmpty()) {
            return bookName?.let { saveSection("front", "title", it) } == true
        }

        return try {
            val chapter = getChapterFolderName(currentChapterStr) ?: run {
                addError(Res.string.could_not_find_chapter, currentChapterStr)
                return false
            }

            val verseBreaks = getVerseBreaks(chapter)
            val chapterNum = chapter.toInt()
            updateStatus(
                Res.string.processing_chapter,
                (chapterCount - chapterNum + 1).toString()
            )

            var lastFirst: String? = null
            var success = true
            var i = 0
            while (i < verseBreaks.size && success) {
                val first = verseBreaks[i]
                success = extractVerses(chapter, cleaned, lastFirst, first)
                lastFirst = first
                i++
            }
            if (success) {
                success = extractVerses(chapter, cleaned, lastFirst, END_MARKER.toString())
            }
            success
        } catch (e: Exception) {
            Logger.e(TAG, "error parsing chapter $currentChapterStr", e)
            addError(Res.string.could_not_parse_chapter, currentChapterStr)
            false
        }
    }

    private suspend fun getChapterFolderName(findChapter: String): String? {
        try {
            val target = Util.strToInt(findChapter, -1)
            if (target > 0) {
                val expected = chapters[target - 1]
                if (Util.strToInt(expected, -1) == target) {
                    return getRightFileNameLength(expected)
                }
            }
            for (chapterN in chapters) {
                if (Util.strToInt(chapterN, -1) == target) {
                    return getRightFileNameLength(chapterN)
                }
            }
        } catch (e: Exception) {
            Logger.e(TAG, "error resolving chapter $findChapter", e)
        }
        addError(Res.string.could_not_find_chapter, findChapter)
        return null
    }

    private suspend fun getChunkFileName(findChapter: String, firstVerse: String): String {
        val verseChunks = getVerseBreaks(findChapter)
        for (firstVerseFile in verseChunks) {
            if (Util.strToInt(firstVerse, 0) == Util.strToInt(firstVerseFile, 0)) {
                return getRightFileNameLength(firstVerseFile)
            }
        }
        return firstVerse
    }

    private suspend fun getVerseBreaks(findChapter: String): List<String> {
        chunks[findChapter]?.let { return it }
        chunks["0$findChapter"]?.let { return it }
        chunks["00$findChapter"]?.let { return it }

        var stripped = findChapter
        while (stripped.isNotEmpty() && stripped[0] == '0') {
            stripped = stripped.substring(1)
            chunks[stripped]?.let { return it }
        }

        addError(Res.string.could_not_find_chapter, findChapter)
        return emptyList()
    }

    // ---------------------------------------------------------------
    // Verse extraction
    // ---------------------------------------------------------------

    private suspend fun extractVerses(
        chapter: String,
        text: CharSequence,
        start: String?,
        end: String
    ): Boolean {
        if (start == null) {
            if (chapter.isNotEmpty()) {
                val matcher = PATTERN_USFM_VERSE_SPAN.matcher(text)
                if (matcher.find() && matcher.start() > 0) {
                    val titleMatcher = PATTERN_CHAPTER_TITLE_MARKER.matcher(text)
                    val chapterTitle = if (titleMatcher.find()) {
                        titleMatcher.group(1)?.let { removeKnownUSFMTags(it) }
                    } else null

                    getChapterFolderName(chapter)?.let {
                        saveSection(it, "title", chapterTitle ?: "")
                    }
                }
            }
            return true
        }
        return extractVerseRange(chapter, text, start.toInt(), end.toInt(), start)
    }

    private suspend fun extractVerseRange(
        chapter: String,
        text: CharSequence,
        start: Int,
        end: Int,
        firstVerse: String
    ): Boolean {
        if (chapter.isEmpty()) return true

        val matcher = PATTERN_USFM_VERSE_SPAN.matcher(text)
        var lastIndex = 0
        var section = ""
        var currentVerse = 0
        var foundVerseCount = 0
        var endVerseRange = 0
        var done = false
        var matchesFound = false
        var pretext: CharSequence = ""

        while (matcher.find()) {
            matchesFound = true
            if (currentVerse >= end) {
                done = true
                break
            }

            if (currentVerse >= start) {
                while (true) {
                    foundVerseCount += if (endVerseRange > 0) {
                        endVerseRange - currentVerse + 1
                    } else 1

                    val verse = matcher.group(1)
                    val verseRange = verse?.let(::getVerseRange) ?: break
                    currentVerse = verseRange[0]
                    endVerseRange = verseRange[1]

                    var results = splitAtVerseEnd(text, lastIndex, matcher.start())
                    section = section + pretext + results.verse
                    pretext = results.extra
                    lastIndex = matcher.start()

                    if (currentVerse >= end) break

                    if (!matcher.find()) {
                        results = splitAtVerseEnd(text, lastIndex, text.length)
                        section = section + pretext + results.verse
                        pretext = ""
                        foundVerseCount++
                        break
                    }
                }
                done = true
                break
            }

            val verse = matcher.group(1)
            val verseRange = verse?.let(::getVerseRange) ?: return false
            currentVerse = verseRange[0]
            endVerseRange = verseRange[1]

            val results = splitAtVerseEnd(text, lastIndex, matcher.start())
            pretext = results.extra
            lastIndex = matcher.start()
        }

        if (!done && matchesFound && currentVerse in start until end) {
            val results = splitAtVerseEnd(text, lastIndex, text.length)
            section = section + pretext + results.verse
        }

        if (start != 0) {
            val delta = foundVerseCount - (end - start)
            when {
                section.isEmpty() -> addWarning(
                    getString(
                        Res.string.could_not_find_verses_in_chapter,
                        start, end - 1, chapter
                    )
                )
                end != END_MARKER && delta != 0 -> {
                    val msg = if (delta < 0) {
                        getString(Res.string.missing_verses_in_chapter, -delta, start, end - 1, chapter)
                    } else {
                        getString(Res.string.extra_verses_in_chapter, delta, start, end - 1, chapter)
                    }
                    addWarning(msg)
                }
            }
        }

        val chunkFileName = getChunkFileName(chapter, firstVerse)
        return getChapterFolderName(chapter)?.let {
            saveSection(it, chunkFileName, section)
        } == true
    }

    private fun splitAtVerseEnd(text: CharSequence, start: Int, end: Int): VerseSplitResults {
        val verseStr = text.subSequence(start, end).toString()
        val sectionEnd = "\\s5\n"
        val sectionPos = verseStr.indexOf(sectionEnd)
        val footnoteMatcher = PATTERN_FOOTNOTE_MARKER.matcher(verseStr)

        return when {
            sectionPos >= 0 -> VerseSplitResults(
                verseStr.substring(0, sectionPos),
                verseStr.substring(sectionPos + sectionEnd.length)
            )
            footnoteMatcher.find() -> VerseSplitResults(
                verseStr.substring(0, footnoteMatcher.start()),
                verseStr.substring(footnoteMatcher.start())
            )
            else -> VerseSplitResults(verseStr, "")
        }
    }

    private fun getVerseRange(verse: String): IntArray? {
        return try {
            intArrayOf(verse.toInt(), 0)
        } catch (e: NumberFormatException) {
            val range = verse.split("-".toRegex())
            if (range.size < 2) null
            else intArrayOf(range[0].toInt(), range[1].toInt())
        }
    }

    private suspend fun saveSection(
        chapter: String,
        fileName: String,
        section: CharSequence
    ): Boolean {
        val chapterFolder = File(projectFolder, chapter)
        return try {
            val cleanChunk = removePattern(section)
            FileUtilities.forceMkdir(chapterFolder)
            val output = File(chapterFolder, "$fileName.txt")
            FileUtilities.writeStringToFile(output, cleanChunk)
            true
        } catch (e: Exception) {
            Logger.e(TAG, "error saving chapter ${this.chapter}", e)
            addError(Res.string.file_write_for_verse, "$chapter/$fileName")
            false
        }
    }

    // ---------------------------------------------------------------
    // Text utilities
    // ---------------------------------------------------------------

    private fun extractString(text: CharSequence, regexPattern: Pattern): String? {
        if (text.isEmpty()) return null
        val matcher = regexPattern.matcher(text)
        return if (matcher.find()) matcher.group(1)?.trim() else null
    }

    private fun removeKnownUSFMTags(text: CharSequence): String {
        if (text.isEmpty()) return ""
        val knownTags = setOf("c", "id", "ide", "h", "toc1", "toc2", "toc3", "mt", "p")
        val regex = Pattern.compile("\\\\(\\w+)(?:\\s([^\\n\\\\]*))?")
        val matcher = regex.matcher(text)

        val builder = StringBuilder()
        var lastPos = 0
        while (matcher.find()) {
            val tag = matcher.group(1)
            if (tag in knownTags) {
                builder.append(text.subSequence(lastPos, matcher.start()))
                lastPos = matcher.end()
                if (lastPos < text.length && text[lastPos] == '\n') lastPos++
            }
        }
        if (lastPos < text.length) {
            builder.append(text.subSequence(lastPos, text.length))
        }
        return trimWhiteSpace(builder).toString()
    }

    private fun trimWhiteSpace(text: CharSequence): CharSequence {
        var start = 0
        while (start < text.length && (text[start] == ' ' || text[start] == '\n')) start++
        if (start >= text.length) return ""

        var end = text.length
        while (end > start && (text[end - 1] == ' ' || text[end - 1] == '\n')) end--
        if (end == start) return ""

        val trimmed = text.subSequence(start, end)
        return if (trimmed.isNotEmpty() && trimmed[trimmed.length - 1] != '\n') {
            "$trimmed \n"
        } else trimmed
    }

    private fun removePattern(text: CharSequence): String {
        val matcher = PATTERN_SECTION_MARKER.matcher(text)
        val builder = StringBuilder()
        var lastIndex = 0
        while (matcher.find()) {
            builder.append(text.subSequence(lastIndex, matcher.start()))
            lastIndex = matcher.end()
        }
        builder.append(text.subSequence(lastIndex, text.length))
        return builder.toString()
    }

    private fun hasVerses(text: CharSequence): Boolean {
        return text.isNotEmpty() && PATTERN_USFM_VERSE_SPAN.matcher(text).find()
    }

    // ---------------------------------------------------------------
    // File scanning & chunk parsing
    // ---------------------------------------------------------------

    private fun addFilesInFolder(folder: File?) {
        Logger.i(TAG, "processing folder: $folder")
        folder?.walk()
            ?.filter {
                val isUSFM = it.extension.contains(USFM_EXTENSION, ignoreCase = true)
                val isTXT = it.extension.contains(TXT_EXTENSION, ignoreCase = true)
                it.isFile && (isUSFM || isTXT)
            }
            ?.forEach { sourceFiles.add(it) }
    }

    private fun parseChunks(markers: List<ChunkMarker>): ParsedChunks {
        val chunkMap = HashMap<String, MutableList<String>>()
        for (marker in markers) {
            chunkMap.getOrPut(marker.chapter) { mutableListOf() }.add(marker.verse)
        }
        val foundChapters = chunkMap.keys
            .filter { Util.strToInt(it, 0) > 0 }
            .sortedNumerically()

        val finalChunks = HashMap<String, List<String>>()
        chunkMap.forEach { (key, value) -> finalChunks[key] = value }

        return ParsedChunks(
            chunks = finalChunks,
            chapters = foundChapters,
            success = foundChapters.isNotEmpty() && finalChunks.isNotEmpty()
        )
    }

    private fun getRightFileNameLength(fileName: String): String {
        val numericalValue = Util.strToInt(fileName, -1)
        if (numericalValue !in 0..99 || fileName.length == 2) return fileName
        val padded = "00$fileName"
        return padded.substring(padded.length - 2)
    }

    fun getShortFilePath(name: String): String {
        val tempPath = tempSrc.toString()
        val pos = name.indexOf(tempPath)
        return if (pos >= 0) {
            name.substring(pos + tempPath.length + 1)
        } else {
            val parts = name.split("/".toRegex())
            if (parts.isNotEmpty()) parts.last() else name
        }
    }

    // ---------------------------------------------------------------
    // Status & messages
    // ---------------------------------------------------------------

    private fun updateStatus(text: String) {
        val fileCount = sourceFiles.size.coerceAtLeast(1)
        val importDone = currentBook.toFloat() / fileCount
        val bookDone = currentChapter.toFloat() / (chapterCount + 2)
        val progress = importDone + bookDone / fileCount

        val status = bookShortName
            ?.takeIf { it.isNotEmpty() }
            ?.let { "$it - $text" }
            ?: text
        onProgress(progress, status)
    }

    private suspend fun updateStatus(resource: StringResource) =
        updateStatus(getString(resource))

    private suspend fun updateStatus(resource: StringResource, data: String) =
        updateStatus(getString(resource, data))

    private suspend fun addError(resource: StringResource) =
        addMessage(getString(resource), isError = true)

    private suspend fun addError(resource: StringResource, vararg args: String) =
        addMessage(getString(resource, *args), isError = true)

    private suspend fun addError(message: String) =
        addMessage(message, isError = true)

    private suspend fun addWarning(message: String) =
        addMessage(message, isError = false)

    private suspend fun addWarning(resource: StringResource, vararg args: String) =
        addMessage(getString(resource, *args), isError = false)

    private suspend fun addMessage(message: String, isError: Boolean) {
        ensureBookSlot()
        val prefix = getString(
            if (isError) Res.string.error_prefix else Res.string.warning_prefix,
            message
        )
        val existing = errorsByBook[currentBook]
        errorsByBook[currentBook] = if (existing.isEmpty()) prefix else "$existing\n$prefix"
        if (isError) Logger.e(TAG, prefix) else Logger.w(TAG, prefix)
    }

    private fun setBookName(shortName: String, description: String) {
        ensureBookSlot()
        foundBooks[currentBook] = if (shortName.isNotEmpty()) "$shortName = $description" else description
    }

    private fun ensureBookSlot() {
        while (foundBooks.size <= currentBook) foundBooks.add("")
        while (errorsByBook.size <= currentBook) errorsByBook.add("")
    }

    private suspend fun buildSummary(): String = buildString {
        for (i in foundBooks.indices) {
            val bookLine = getString(Res.string.found_book, foundBooks[i])
            val errorText = errorsByBook.getOrNull(i)
                ?.takeIf { it.isNotEmpty() }
                ?: getString(Res.string.no_error)
            append("\n ${i + 1} - $bookLine \n $errorText\n")
        }
    }

    // ---------------------------------------------------------------
    // Inner types & companion
    // ---------------------------------------------------------------

    private data class VerseSplitResults(val verse: String, val extra: String)

    private data class ParsedChunks(
        val chunks: HashMap<String, List<String>>,
        val chapters: List<String>,
        val success: Boolean
    )

    companion object {
        private val TAG: String = ImportSession::class.java.simpleName

        private const val CHAPTER_TITLE_MARKER = "\\\\cl\\s([^\\n]*)"
        val PATTERN_CHAPTER_TITLE_MARKER: Pattern = Pattern.compile(CHAPTER_TITLE_MARKER)
        val PATTERN_FOOTNOTE_MARKER: Pattern = Pattern.compile(USFMNoteSpan.PATTERN)

        private const val BOOK_TITLE_MARKER = "\\\\toc1\\s([^\\n]*)"
        val PATTERN_BOOK_TITLE_MARKER: Pattern = Pattern.compile(BOOK_TITLE_MARKER)

        private const val ID_TAG = "\\\\id\\s([^\\n]*)"
        val ID_TAG_MARKER: Pattern = Pattern.compile(ID_TAG)

        private const val BOOK_LONG_NAME_MARKER = "\\\\toc2\\s([^\\n]*)"
        val PATTERN_BOOK_LONG_NAME_MARKER: Pattern = Pattern.compile(BOOK_LONG_NAME_MARKER)

        private const val BOOK_ABBREVIATION_MARKER = "\\\\toc3\\s([^\\n]*)"
        val PATTERN_BOOK_ABBREVIATION_MARKER: Pattern = Pattern.compile(BOOK_ABBREVIATION_MARKER)

        private const val SECTION_MARKER = "\\\\s5([^\\n]*)"
        private val PATTERN_SECTION_MARKER: Pattern = Pattern.compile(SECTION_MARKER)

        private const val CHAPTER_NUMBER_MARKER = "\\\\c\\s(\\d+(-\\d+)?)\\s"
        val PATTERN_CHAPTER_NUMBER_MARKER: Pattern = Pattern.compile(CHAPTER_NUMBER_MARKER)

        val PATTERN_USFM_VERSE_SPAN: Pattern = Pattern.compile(USFMVerseSpan.PATTERN)

        const val END_MARKER: Int = 999999
    }
}