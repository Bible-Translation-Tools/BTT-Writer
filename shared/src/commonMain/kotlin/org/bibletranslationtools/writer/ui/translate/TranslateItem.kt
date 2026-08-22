package org.bibletranslationtools.writer.ui.translate

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.text.AnnotatedString
import btt_writer.shared.generated.resources.Res
import btt_writer.shared.generated.resources.project_chapter_title
import btt_writer.shared.generated.resources.project_title
import btt_writer.shared.generated.resources.reference
import org.bibletranslationtools.writer.core.ChapterTranslation
import org.bibletranslationtools.writer.core.Chunk
import org.bibletranslationtools.writer.core.FileHistory
import org.bibletranslationtools.writer.core.Frame
import org.bibletranslationtools.writer.core.FrameTranslation
import org.bibletranslationtools.writer.core.MergeConflictsHandler
import org.bibletranslationtools.writer.core.ProjectTranslation
import org.bibletranslationtools.writer.core.ProjectTypeClass
import org.bibletranslationtools.writer.core.TranslationHelp
import org.bibletranslationtools.writer.ui.translate.review.TargetMode
import org.bibletranslationtools.writer.usecases.ParseMergeConflicts
import org.jetbrains.compose.resources.stringResource


interface Swipable {
    val sourceOnTop: Boolean
    fun selfCopy(sourceOnTop: Boolean = this.sourceOnTop): Swipable
}

@Stable
abstract class TranslateItem {
    abstract val id: String
    abstract val chunk: Chunk
    abstract val sourceText: String
    abstract val targetText: String
    abstract val renderedSourceText: AnnotatedString
    abstract val renderedTargetText: AnnotatedString
    abstract val pt: ProjectTranslation
    abstract val ct: ChapterTranslation
    abstract val ft: FrameTranslation

    open val sourceTitle: String
        get() {
            return if (chunk.isProjectTitle) {
                ""
            } else if (chunk.isChapter) {
                chunk.source.project.name.trim()
            } else {
                // TODO: we should read the title from a cache instead of doing file io again
                var title = chunk.source.readChunk(chunk.chapterSlug, "title").trim()
                if (title.isEmpty()) {
                    title = try {
                        "${chunk.source.project.name.trim()} ${chunk.chapterSlug.toInt()}"
                    } catch (_: Exception) {
                        "${chunk.source.project.name.trim()} ${chunk.chapterSlug}"
                    }
                }
                val verseSpan = Frame.parseVerseTitle(sourceText, chunk.sourceTranslationFormat)
                title += if (verseSpan.isEmpty()) {
                    try {
                        ":${chunk.chunkSlug.toInt()}"
                    } catch (_: Exception) {
                        ":${chunk.chunkSlug}"
                    }
                } else ":$verseSpan"
                title
            }
        }

    // row labels are always derived from the source (desktop style:
    // "Project Title", "<book> <ch> Title", "<book> <ch>:<verses>") —
    // never from pt/ct content, which for helps projects holds JSON
    open val targetTitle: String
        @Composable
        get() {
            val language = chunk.target.targetLanguage.name
            val book = chunk.source.project.name.trim()
            val chapter = chunk.chapterSlug.toIntOrNull() ?: chunk.chapterSlug

            return when {
                chunk.isProjectTitle ->
                    "${stringResource(Res.string.project_title)} - $language"
                chunk.isChapterTitle -> "${stringResource(Res.string.project_chapter_title, book, chapter)} - $language"
                chunk.isChapterReference ->
                    "$book $chapter ${stringResource(Res.string.reference)} - $language"
                else -> {
                    val verseSpan = Frame.parseVerseTitle(sourceText, chunk.sourceTranslationFormat)
                    val span = verseSpan.ifEmpty {
                        (chunk.chunkSlug.toIntOrNull() ?: chunk.chunkSlug).toString()
                    }
                    "$book $chapter:$span - $language"
                }
            }
        }

    val isComplete: Boolean
        get() = when (chunk.chapterSlug) {
            "front" -> {
                // project stuff
                if (chunk.chunkSlug == "title") {
                    pt.isTitleFinished
                } else false
            }
            "back" -> false
            else -> {
                // chapter stuff
                when (chunk.chunkSlug) {
                    "title" -> ct.titleFinished
                    "reference" -> ct.referenceFinished
                    else -> ft.finished
                }
            }
        }

    val hasMergeConflict: Boolean
        get() = MergeConflictsHandler.isMergeConflicted(targetText)

    val mergeItems: List<CharSequence>
        get() = if (hasMergeConflict) {
            ParseMergeConflicts.execute(targetText)
        } else emptyList()

    fun saveTranslation(text: String) {
        if (chunk.isProjectTitle) {
            chunk.target.applyProjectTitleTranslation(text)
        } else if (chunk.isChapterReference) {
            chunk.target.applyChapterReferenceTranslation(ct, text)
        } else if (chunk.isChapterTitle) {
            chunk.target.applyChapterTitleTranslation(ct, text)
        } else {
            chunk.target.applyFrameTranslation(ft, text)
        }
    }

}

data class ReadItem(
    override val id: String,
    override val chunk: Chunk,
    override val sourceText: String,
    override val targetText: String,
    override val renderedSourceText: AnnotatedString,
    override val renderedTargetText: AnnotatedString,
    override val pt: ProjectTranslation,
    override val ct: ChapterTranslation,
    override val ft: FrameTranslation,
    override val sourceOnTop: Boolean
) : TranslateItem(), Swipable {

    override fun selfCopy(sourceOnTop: Boolean): Swipable {
        return copy(sourceOnTop = sourceOnTop)
    }

    override val sourceTitle: String
        get() {
            var title = chunk.source.readChunk(chunk.chapterSlug, "title")
                .trim()
            if (title.isEmpty()) {
                title = chunk.source.readChunk("front", "title")
                    .trim()
                if (chunk.chapterSlug != "front") {
                    title += " " + chunk.chapterSlug.toInt()
                }
            }
            return title
        }

    override val targetTitle: String
        @Composable
        get() = "${sourceTitle.trim()} - ${chunk.target.targetLanguage.name}"
}

data class ChunkItem(
    override val id: String,
    override val chunk: Chunk,
    override val sourceText: String,
    override val targetText: String,
    override val renderedSourceText: AnnotatedString,
    override val renderedTargetText: AnnotatedString,
    override val pt: ProjectTranslation,
    override val ct: ChapterTranslation,
    override val ft: FrameTranslation,
    override val sourceOnTop: Boolean
) : TranslateItem(), Swipable {

    override fun selfCopy(sourceOnTop: Boolean): Swipable {
        return copy(sourceOnTop = sourceOnTop)
    }
}

data class ReviewItem(
    override val id: String,
    override val chunk: Chunk,
    override val sourceText: String,
    override val targetText: String,
    override val renderedSourceText: AnnotatedString,
    override val renderedTargetText: AnnotatedString,
    override val pt: ProjectTranslation,
    override val ct: ChapterTranslation,
    override val ft: FrameTranslation,
    val helps: Map<String, Any> = emptyMap(),
    val targetMode: TargetMode,
    val fileHistory: FileHistory? = null,
    val helpsContent: List<TranslationHelp> = emptyList(),
    val bookTranslationText: String = "",
    val renderedBookTranslationText: AnnotatedString = AnnotatedString(""),
    val customSourceTitle: String? = null
) : TranslateItem() {

    val projectTypeClass: ProjectTypeClass
        get() = chunk.target.projectTypeClass

    override val sourceTitle: String
        get() = customSourceTitle ?: super.sourceTitle

    override val targetTitle: String
        @Composable
        get() = if (projectTypeClass == ProjectTypeClass.EXTANT) {
            // words are titled by the word itself, not chapter:verse
            "$sourceTitle - ${chunk.target.targetLanguage.name}"
        } else super.targetTitle
}