package org.bibletranslationtools.writer.usecases

import btt_writer.shared.generated.resources.Res
import btt_writer.shared.generated.resources.pref_default_translation_typeface
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bibletranslationtools.logger.Logger
import org.bibletranslationtools.resourcecatalog.ResourceCatalogClient
import org.bibletranslationtools.writer.DirectoryProvider
import org.bibletranslationtools.writer.Platform
import org.bibletranslationtools.writer.core.ArchiveGenerator
import org.bibletranslationtools.writer.core.ArchiveManifest
import org.bibletranslationtools.writer.core.ArchiveMigrator
import org.bibletranslationtools.writer.core.ArchiveTranslation
import org.bibletranslationtools.writer.core.FrameTranslation
import org.bibletranslationtools.writer.core.PdfPrinter
import org.bibletranslationtools.writer.core.TargetTranslation
import org.bibletranslationtools.writer.core.TranslationFormat
import org.bibletranslationtools.writer.core.TranslationType
import org.bibletranslationtools.writer.core.Translator.Companion.TSTUDIO_EXTENSION
import org.bibletranslationtools.writer.core.Translator.Companion.ZIP_EXTENSION
import org.bibletranslationtools.writer.core.Typography
import org.bibletranslationtools.writer.outputStream
import org.bibletranslationtools.writer.utils.FileUtilities
import org.bibletranslationtools.writer.utils.RepoUtils
import org.bibletranslationtools.writer.utils.Util
import org.bibletranslationtools.writer.utils.Zip
import org.eclipse.jgit.errors.TransportException
import org.jetbrains.compose.resources.getString
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream
import java.util.Locale

class ExportProjects(
    private val directoryProvider: DirectoryProvider,
    private val catalogClient: ResourceCatalogClient,
    private val typography: Typography,
    private val platform: Platform
) {

    /**
     * Exports a single target translation in .tstudio format to File
     * @param targetTranslation
     * @param outputFile
     */
    @Throws(Exception::class)
    suspend fun exportProject(targetTranslation: TargetTranslation, outputFile: File) {
        exportProject(targetTranslation, PlatformFile(outputFile))
    }

    /**
     * Exports a single target translation in .tstudio format to OutputStream
     * @param targetTranslation
     * @param platformFile
     */
    suspend fun exportProject(
        targetTranslation: TargetTranslation,
        platformFile: PlatformFile,
        recoverBadRepo: Boolean = true
    ): Result {
        var success = false
        val tempDir = directoryProvider.createTempDir()
        try {
            targetTranslation.commitSync(".", false)

            val manifest = buildArchiveManifest(targetTranslation)
            val manifestFile = File(tempDir, "manifest.json")
            manifestFile.createNewFile()
            directoryProvider.writeStringToFile(
                manifestFile,
                ArchiveMigrator.json.encodeToString(manifest)
            )

            platformFile.outputStream().use { out ->
                Zip.zipToStream(
                    files = arrayOf(manifestFile, targetTranslation.path),
                    dest = out
                )
                success = true
            }
        } catch (_: TransportException) {
            if (recoverBadRepo) {
                // fix corrupt repo and try again
                RepoUtils.recover(targetTranslation)
                return exportProject(targetTranslation, platformFile, false)
            }
            success = true
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to export project", e)
        } finally {
            FileUtilities.deleteQuietly(tempDir)
        }

        return Result(platformFile, success, ExportType.PROJECT)
    }

    @Throws(Exception::class)
    fun exportProject(projectDir: File, outputFile: File) {
        if (!isValidArchiveExtension(outputFile.path)) {
            throw Exception("Output file must have '$TSTUDIO_EXTENSION' or '$ZIP_EXTENSION' extension")
        }
        if (!projectDir.exists()) {
            throw Exception("Project directory doesn't exist.")
        }

        FileOutputStream(outputFile).use { outputStream ->
            Zip.zipToStream(arrayOf(projectDir), outputStream)
        }
    }

    /**
     * Exports a target translation as a USFM file
     * @param targetTranslation
     * @param platformFile
     */
    suspend fun exportUSFM(
        targetTranslation: TargetTranslation,
        platformFile: PlatformFile
    ): Result {
        val tempDir = directoryProvider.createTempDir()

        val success = try {
            val chapters = targetTranslation.chapterTranslations

            val bookData = BookData.generate(targetTranslation, catalogClient)
            val bookCode = bookData.bookCode
            val bookTitle = bookData.bookTitle
            val bookName = bookData.bookName
            val languageId = bookData.languageId
            val languageName = bookData.languageName

            val tempFile = directoryProvider.createTempFile("output", dir = tempDir)

            withContext(Dispatchers.IO) {
                PrintStream(tempFile).use { ps ->
                    val id = "\\id $bookCode $bookTitle, $bookName, $languageId, $languageName"
                    ps.println(id)
                    val ide = "\\ide usfm"
                    ps.println(ide)
                    val h = "\\h $bookTitle"
                    ps.println(h)
                    val bookID = "\\toc1 $bookTitle"
                    ps.println(bookID)
                    val bookNameID = "\\toc2 $bookName"
                    ps.println(bookNameID)
                    val shortBookID = "\\toc3 $bookCode"
                    ps.println(shortBookID)
                    val mt = "\\mt $bookTitle"
                    ps.println(mt)

                    for (chapter in chapters) {
                        // TRICKY: the translation format doesn't matter for exporting
                        val frames = targetTranslation.getFrameTranslations(
                            chapter.id,
                            TranslationFormat.DEFAULT
                        )
                        if (frames.isEmpty()) continue

                        val chapterInt = Util.strToInt(chapter.id, 0)
                        if (chapterInt != 0) {
                            ps.println("\\s5") // section marker
                            val chapterNumber = "\\c " + chapter.id
                            ps.println(chapterNumber)
                        }

                        if (chapter.title.isNotEmpty()) {
                            val chapterTitle = "\\cl " + chapter.title
                            ps.println(chapterTitle)
                        }

                        if (chapter.reference.isNotEmpty()) {
                            val chapterRef = "\\cd " + chapter.reference
                            ps.println(chapterRef)
                        }

                        ps.println("\\p") // paragraph marker

                        val frameList = sortFrameTranslations(frames)
                        var startChunk = 0
                        if (frameList.isNotEmpty()) {
                            val frame = frameList[0]
                            val verseID = Util.strToInt(frame.id, 0)
                            if ((verseID == 0)) {
                                startChunk++
                            }
                        }

                        for (i in startChunk until frameList.size) {
                            val frame = frameList[i]
                            val text = frame.body

                            if (i > startChunk) {
                                ps.println("\\s5") // section marker
                            }
                            ps.print(text)
                        }
                    }
                    platformFile.outputStream().use { output ->
                        tempFile.inputStream().use { input ->
                            val buffer = ByteArray(1024)
                            var length: Int
                            while ((input.read(buffer).also { length = it }) > 0) {
                                output.write(buffer, 0, length)
                            }
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to export USFM file", e)
            false
        } finally {
            FileUtilities.deleteQuietly(tempDir)
        }

        return Result(platformFile, success, ExportType.USFM)
    }

    /**
     * Exports a target translation as a PDF file
     * @param targetTranslation
     * @param platformFile
     * @return output file
     */
    suspend fun exportPDF(
        targetTranslation: TargetTranslation,
        platformFile: PlatformFile,
        includeImages: Boolean,
        includeIncompleteFrames: Boolean,
        imagesDir: File?
    ): Result {
        val success = try {
            val fontPath = typography.getAssetPath(TranslationType.TARGET)
            val fontFile = directoryProvider.getAssetAsFile(fontPath)
            val fontSize = typography.getFontSize(TranslationType.TARGET)
            val licenseFontName = getString(Res.string.pref_default_translation_typeface)
            val licenseFontPath = directoryProvider.getAssetAsFile("font/$licenseFontName").absolutePath
            val targetLanguageRtl = "rtl" == targetTranslation.targetLanguageDirection
            val printer = PdfPrinter(
                targetTranslation, targetTranslation.format, fontFile.absolutePath,
                fontSize, targetLanguageRtl, licenseFontPath, imagesDir, directoryProvider,
                catalogClient
            )
            printer.includeMedia(includeImages)
            printer.includeIncomplete(includeIncompleteFrames)
            val pdf = printer.print()
            if (pdf.exists()) {
                platformFile.outputStream().use { output ->
                    pdf.inputStream().use { input ->
                        val buffer = ByteArray(1024)
                        var length: Int
                        while ((input.read(buffer).also { length = it }) > 0) {
                            output.write(buffer, 0, length)
                        }
                    }
                }
                FileUtilities.deleteQuietly(pdf)
            }
            true
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to export PDF file", e)
            false
        }

        return Result(platformFile, success, ExportType.PDF)
    }

    /**
     * creates an archive manifest.
     * @param targetTranslation
     * @return
     */
    private fun buildArchiveManifest(targetTranslation: TargetTranslation): ArchiveManifest {
        targetTranslation.commit()

        // build manifest
        return ArchiveManifest(
            packageVersion = TSTUDIO_PACKAGE_VERSION,
            timestamp = Util.unixTime,
            generator = ArchiveGenerator(
                name = platform.info.generator,
                build = platform.info.versionCode.toString()
            ),
            targetTranslations = listOf(
                ArchiveTranslation(
                    id = targetTranslation.id,
                    path = targetTranslation.id,
                    commitHash = targetTranslation.commitHash,
                    direction = targetTranslation.targetLanguageDirection
                )
            )
        )
    }

    /**
     * Check if file extension is supported archive extension
     * @param fileName
     * @return boolean
     */
    private fun isValidArchiveExtension(fileName: String): Boolean {
        val isTstudio = FileUtilities.getExtension(fileName)
            .equals(TSTUDIO_EXTENSION, ignoreCase = true)
        val isZip = FileUtilities.getExtension(fileName)
            .equals(ZIP_EXTENSION, ignoreCase = true)

        return isTstudio || isZip
    }

    data class Result(
        val file: PlatformFile,
        val success: Boolean,
        val exportType: ExportType
    )

    enum class ExportType {
        PROJECT,
        USFM,
        PDF
    }

    /**
     * class to extract book data as well as default USFM output file name
     */
    class BookData private constructor(
        targetTranslation: TargetTranslation,
        catalogClient: ResourceCatalogClient,
    ) {
        val defaultUSFMFileName: String
        val bookCode: String = targetTranslation.projectId.uppercase(Locale.getDefault())
        val languageId: String = targetTranslation.targetLanguageId
        val languageName: String = targetTranslation.targetLanguageName
        var bookName: String
            private set
        var bookTitle: String
            private set

        init {
            val projectTranslation = targetTranslation.projectTranslation
            val project = catalogClient.library.getProject(
                languageId,
                targetTranslation.projectId,
                true
            )

            bookName = bookCode
            if (project != null) {
                bookName = project.name
            }

            bookTitle = ""
            val title = projectTranslation.title
            bookTitle = title.trim()
            if (bookTitle.isEmpty()) {
                bookTitle = bookName
            }

            if (bookTitle.isEmpty()) {
                bookTitle = bookCode
            }

            // generate file name
            defaultUSFMFileName = languageId + "_" + bookCode + "_" + bookName + ".usfm"
        }

        companion object {
            fun generate(
                targetTranslation: TargetTranslation,
                catalogClient: ResourceCatalogClient
            ): BookData {
                return BookData(targetTranslation, catalogClient)
            }
        }
    }

    companion object {
        private const val TAG = "ExportProjects"
        private const val TSTUDIO_PACKAGE_VERSION = 3

        /**
         * sort the frames
         * @param frames
         * @return
         */
        fun sortFrameTranslations(frames: Array<FrameTranslation>): ArrayList<FrameTranslation> {
            // sort frames
            val frameList = ArrayList(listOf(*frames))
            frameList.sortWith { lhs, rhs ->
                val lhInt = getChunkOrder(lhs.id)
                val rhInt = getChunkOrder(rhs.id)
                lhInt.compareTo(rhInt)
            }
            return frameList
        }

        /**
         *
         * @param chunkID
         * @return
         */
        private fun getChunkOrder(chunkID: String): Int {
            // special treatment for chunk 00 to move to end of list
            if ("00" == chunkID) {
                return 99999
            }
            if ("back".equals(chunkID, ignoreCase = true)) {
                // back is moved to very end
                return 9999999
            }
            // if not numeric, then will move to top of list and leave order unchanged
            return Util.strToInt(chunkID, -1)
        }
    }
}