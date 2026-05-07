package org.bibletranslationtools.writer.rendering.spannables

import org.bibletranslationtools.logger.Logger
import org.bibletranslationtools.writer.utils.StringUtilities
import java.util.regex.Pattern

class ShortReferenceSpan(reference: String) : Span(reference, reference) {

    val chapter: String
    val verse: String

    companion object {
        val TAG = ShortReferenceSpan::javaClass.name
        val PATTERN: Pattern = Pattern.compile("\\b(\\d+):(\\d+)\\b")
    }

    init {
        val pieces = reference.split(":")

        chapter = try {
            StringUtilities.normalizeSlug(pieces[0])
        } catch (e: Exception) {
            Logger.w(TAG, "Failed to normalize chapter slug", e)
            pieces.getOrElse(0) { "" }
        }

        verse = try {
            StringUtilities.normalizeSlug(pieces[1])
        } catch (e: Exception) {
            Logger.w(TAG, "Failed to normalize chunk slug", e)
            pieces.getOrElse(1) { "" }
        }
    }
}