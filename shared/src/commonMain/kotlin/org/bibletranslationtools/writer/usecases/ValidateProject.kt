package org.bibletranslationtools.writer.usecases

import btt_writer.shared.generated.resources.Res
import btt_writer.shared.generated.resources.has_warnings
import btt_writer.shared.generated.resources.reference
import btt_writer.shared.generated.resources.title
import org.bibletranslationtools.logger.Logger
import org.bibletranslationtools.resourcecatalog.ResourceCatalogClient
import org.bibletranslationtools.resourcecontainer.ResourceContainer
import org.bibletranslationtools.writer.core.Frame
import org.bibletranslationtools.writer.core.MergeConflictsHandler
import org.bibletranslationtools.writer.core.ProjectTypeClass
import org.bibletranslationtools.writer.core.TranslationFormat
import org.bibletranslationtools.writer.core.TranslationHelp
import org.bibletranslationtools.writer.core.Translator
import org.bibletranslationtools.writer.core.Validation
import org.bibletranslationtools.writer.core.WORDS_CHAPTER
import org.bibletranslationtools.writer.core.WORD_PATTERN
import org.bibletranslationtools.writer.utils.StringUtilities
import org.bibletranslationtools.writer.utils.sortedNumerically
import org.jetbrains.compose.resources.getString

class ValidateProject(
    private val catalogClient: ResourceCatalogClient,
    private val translator: Translator
) {
    suspend fun execute(targetTranslationId: String, sourceTranslationId: String): List<Validation> {
        val validations = arrayListOf<Validation>()

        translator.getTargetTranslation(targetTranslationId)?.let { targetTranslation ->
            val targetLanguage = catalogClient.library.getTargetLanguage(
                targetTranslation.targetLanguageId
            ) ?: return validations

            val container = try {
                catalogClient.openResourceContainer(sourceTranslationId)
            } catch (e: Exception) {
                Logger.e(
                    "ValidationTask",
                    "Failed to load resource container",
                    e
                )
                return listOf()
            }

            val sourceFormat = try {
                TranslationFormat.parse(container.info.contentMimeType)
            } catch (e: Exception) {
                Logger.e(
                    "ValidationTask",
                    "Failed to read the translation format from the container",
                    e
                )
                return listOf()
            }

            val projectTitle = container.readChunk("front", "title")
            val sourceLanguage = catalogClient.library.getSourceLanguage(
                container.language.slug
            ) ?: return validations
            val isExtant = targetTranslation.projectTypeClass == ProjectTypeClass.EXTANT
            val isHelps = targetTranslation.projectTypeClass == ProjectTypeClass.HELPS
            val chapters = if (isExtant) {
                container.chapters().sortedBy { wordTitle(container, it).lowercase() }
            } else {
                container.chapters().sortedNumerically()
            }

            // validate chapters
            var lastValidChapterIndex = -1
            val chapterValidations = arrayListOf<Validation>()

            for (i in chapters.indices) {
                val chapterSlug = chapters[i]
                val chunks = container.chunks(chapterSlug).sortedNumerically()

                // validate frames
                var lastValidFrameIndex = -1
                var chapterIsValid = true
                val frameValidations = arrayListOf<Validation>()

                val chapterTranslation = targetTranslation.getChapterTranslation(chapterSlug)
                if (MergeConflictsHandler.isMergeConflicted(chapterTranslation.title) ||
                    chunks.contains("title") &&
                    !chapterTranslation.titleFinished
                ) {
                    chapterIsValid = false
                    frameValidations.add(
                        Validation.InvalidFrame(
                            title = getChunkTitle(
                                container,
                                chapterSlug,
                                "title",
                                getString(Res.string.title)
                            ),
                            titleLanguage = sourceLanguage,
                            body = if (isHelps) {
                                firstHelpTitle(chapterTranslation.title)
                            } else {
                                chapterTranslation.title
                            },
                            bodyLanguage = targetLanguage,
                            bodyFormat = TranslationFormat.DEFAULT,
                            targetTranslationId = targetTranslationId,
                            chapterId = chapterSlug,
                            frameId = "00"
                        )
                    )
                }

                if (MergeConflictsHandler.isMergeConflicted(chapterTranslation.reference) ||
                    chunks.contains("reference") &&
                    !chapterTranslation.referenceFinished
                ) {
                    chapterIsValid = false
                    frameValidations.add(
                        Validation.InvalidFrame(
                            title = getChunkTitle(
                                container,
                                chapterSlug,
                                "reference",
                                getString(Res.string.reference)
                            ),
                            titleLanguage = sourceLanguage,
                            body = if (isHelps) {
                                firstHelpTitle(chapterTranslation.reference)
                            } else {
                                chapterTranslation.reference
                            },
                            bodyLanguage = targetLanguage,
                            bodyFormat = TranslationFormat.DEFAULT,
                            targetTranslationId = targetTranslationId,
                            chapterId = chapterSlug,
                            frameId = "00"
                        )
                    )
                }

                for (j in chunks.indices) {
                    val chunkSlug = chunks[j]
                    // if chunk types we have already handled, then skip
                    if (chunkSlug == "title" || chunkSlug == "reference") {
                        continue
                    }

                    val frameTranslation = if (isExtant) {
                        targetTranslation.getFrameTranslation(
                            WORDS_CHAPTER,
                            chapterSlug,
                            TranslationFormat.DEFAULT
                        )
                    } else {
                        targetTranslation.getFrameTranslation(
                            chapterSlug,
                            chunkSlug,
                            TranslationFormat.DEFAULT
                        )
                    }
                    val chunkText = container.readChunk(chapterSlug, chunkSlug)
                    val finishedOrEmpty = frameTranslation.finished || chunkText.isEmpty()
                    val mergeConflicted = MergeConflictsHandler.isMergeConflicted(
                        frameTranslation.body
                    )
                    val isLastChunk = j == chunks.size - 1

                    if (lastValidFrameIndex == -1 && finishedOrEmpty) {
                        // start new valid range
                        lastValidFrameIndex = j
                    } else if (mergeConflicted || !finishedOrEmpty || isLastChunk) {
                        // close valid range
                        if (lastValidFrameIndex > -1) {
                            var previousFrameIndex = j - 1
                            if (finishedOrEmpty) {
                                previousFrameIndex = j
                            }
                            if (lastValidFrameIndex < previousFrameIndex) {
                                // range
                                val previousFrame = container.readChunk(
                                    chapterSlug,
                                    chunks[previousFrameIndex]
                                )
                                val lastValidText = container.readChunk(
                                    chapterSlug,
                                    chunks[lastValidFrameIndex]
                                )
                                val formattedChapter = StringUtilities.formatNumber(
                                    chapterSlug
                                )
                                var frameTitle = "$projectTitle $formattedChapter"
                                val frameStartVerse = Frame.getStartVerse(
                                    lastValidText,
                                    sourceFormat
                                )
                                val frameEndVerse = Frame.getEndVerse(
                                    previousFrame,
                                    sourceFormat
                                )
                                frameTitle += ":$frameStartVerse-$frameEndVerse"

                                frameValidations.add(
                                    Validation.ValidFrame(
                                        frameTitle,
                                        sourceLanguage,
                                        true
                                    )
                                )
                            } else {
                                val lastValidText = container.readChunk(
                                    chapterSlug,
                                    chunks[lastValidFrameIndex]
                                )
                                val formattedChapter = StringUtilities.formatNumber(
                                    chapterSlug
                                )
                                var frameTitle = "$projectTitle $formattedChapter"
                                val frameStartVerse = Frame.getStartVerse(
                                    lastValidText,
                                    sourceFormat
                                )
                                val frameEndVerse = Frame.getEndVerse(
                                    lastValidText,
                                    sourceFormat
                                )
                                frameTitle += ":$frameStartVerse"

                                if (frameStartVerse != frameEndVerse) {
                                    frameTitle += "-$frameEndVerse"
                                }
                                frameValidations.add(
                                    Validation.ValidFrame(
                                        frameTitle,
                                        sourceLanguage,
                                        false
                                    )
                                )
                            }
                            lastValidFrameIndex = -1
                        }

                        // add invalid frame
                        if (!finishedOrEmpty) {
                            chapterIsValid = false
                            val frameTitle: String
                            if (isExtant) {
                                frameTitle = wordTitle(container, chapterSlug)
                            } else {
                                val formattedChapter = StringUtilities.formatNumber(chapterSlug)
                                var title = "$projectTitle $formattedChapter"
                                val frameStartVerse = Frame.getStartVerse(
                                    chunkText,
                                    sourceFormat
                                )
                                val frameEndVerse = Frame.getEndVerse(
                                    chunkText,
                                    sourceFormat
                                )
                                title += ":$frameStartVerse"

                                if (frameStartVerse != frameEndVerse) {
                                    title += "-$frameEndVerse"
                                }
                                frameTitle = title
                            }

                            val frameBody = if (isExtant || isHelps) {
                                firstHelpTitle(frameTranslation.body)
                            } else {
                                frameTranslation.body
                            }

                            frameValidations.add(
                                Validation.InvalidFrame(
                                    title = frameTitle,
                                    titleLanguage = sourceLanguage,
                                    body = frameBody,
                                    bodyLanguage = targetLanguage,
                                    bodyFormat = frameTranslation.format,
                                    targetTranslationId = targetTranslationId,
                                    chapterId = if (isExtant) WORDS_CHAPTER else chapterSlug,
                                    frameId = if (isExtant) chapterSlug else chunkSlug
                                )
                            )
                        }
                    }
                }
                if (lastValidChapterIndex == -1 && chapterIsValid) {
                    // start new valid range
                    lastValidChapterIndex = i
                } else if (!chapterIsValid || i == chapters.size - 1) {
                    // close valid range
                    if (lastValidChapterIndex > -1) {
                        var previousChapterIndex = i - 1
                        if (chapterIsValid) {
                            previousChapterIndex = i
                        }
                        if (lastValidChapterIndex < previousChapterIndex) {
                            // range
                            val previousChapterSlug = chapters[previousChapterIndex]
                            val lastValidChapterSlug = chapters[lastValidChapterIndex]
                            val lastChapter = if (isExtant) {
                                wordTitle(container, lastValidChapterSlug)
                            } else {
                                StringUtilities.formatNumber(lastValidChapterSlug)
                            }
                            val prevChapter = if (isExtant) {
                                wordTitle(container, previousChapterSlug)
                            } else {
                                StringUtilities.formatNumber(previousChapterSlug)
                            }
                            val chapterTitle = "$projectTitle $lastChapter-$prevChapter"

                            chapterValidations.add(
                                Validation.ValidFrame(
                                    chapterTitle,
                                    sourceLanguage,
                                    true
                                )
                            )
                        } else {
                            val lastValidChapter = chapters[lastValidChapterIndex]
                            val lastChapter = if (isExtant) {
                                wordTitle(container, lastValidChapter)
                            } else {
                                StringUtilities.formatNumber(lastValidChapter)
                            }
                            val chapterTitle = "$projectTitle $lastChapter"

                            chapterValidations.add(
                                Validation.ValidGroup(
                                    chapterTitle,
                                    sourceLanguage,
                                    false
                                )
                            )
                        }
                        lastValidChapterIndex = -1
                    }

                    // add invalid chapter
                    if (!chapterIsValid) {
                        var chapterTitle: String
                        chapterTitle = if (isExtant) {
                            wordTitle(container, chapterSlug)
                        } else {
                            container.readChunk(chapterSlug, "title")
                        }
                        if (chapterTitle.isEmpty()) {
                            val formattedChapter = StringUtilities.formatNumber(chapterSlug)
                            chapterTitle = "$projectTitle $formattedChapter"
                        }
                        chapterTitle = getString(
                            Res.string.has_warnings,
                            chapterTitle.trim()
                        )

                        chapterValidations.add(
                            Validation.InvalidGroup(
                                chapterTitle,
                                sourceLanguage
                            )
                        )

                        // add frame validations
                        chapterValidations.addAll(frameValidations)
                    }
                }
            }

            // close validations
            if (chapterValidations.size > 1) {
                validations.addAll(chapterValidations)
            } else {
                validations.add(
                    Validation.ValidGroup(
                        title = projectTitle,
                        titleLanguage = sourceLanguage,
                        isRange = true
                    )
                )
            }
        }

        return validations
    }

    private fun firstHelpTitle(body: String): String {
        // conflicted text is not valid JSON; keep it raw so the warning stays visible
        if (MergeConflictsHandler.isMergeConflicted(body)) return body
        return TranslationHelp.fromJson(body).firstOrNull()?.title ?: ""
    }

    private fun wordTitle(container: ResourceContainer, wordSlug: String): String {
        val match = WORD_PATTERN.matcher(container.readChunk(wordSlug, "01"))
        return if (match.find()) {
            match.group(1)?.trim() ?: wordSlug
        } else wordSlug
    }

    /**
     * get the text from the source for title and add the chunk type as a tip
     * @param container
     * @param chapterSlug
     * @param chunkSlug
     * @return
     */
    private fun getChunkTitle(
        container: ResourceContainer,
        chapterSlug: String,
        chunkSlug: String,
        type: String
    ): String {
        val title = container.readChunk(chapterSlug, chunkSlug)
        return title.trim() + " - " + type
    }
}