package org.bibletranslationtools.writer.rendering

import org.bibletranslationtools.writer.rendering.spannables.USFMChar
import org.bibletranslationtools.writer.rendering.spannables.USFMNoteSpan
import org.bibletranslationtools.writer.rendering.spannables.USFMVerseSpan
import org.bibletranslationtools.writer.rendering.spannables.USXChar
import org.bibletranslationtools.writer.rendering.spannables.USXNoteSpan
import org.bibletranslationtools.writer.rendering.spannables.USXVerseSpan

/**
 * Converts USX to USFM
 */
open class USXtoUSFMConverter {

    companion object {
        fun doConversion(input: CharSequence): CharSequence {
            val converter = USXtoUSFMConverter()
            return converter.convert(input)
        }

        /**
         * Returns a regex that matches a para tag pair e.g. <para style=""></para>
         */
        private fun paraPattern(style: String): Regex {
            return Regex("<para\\s+style=\"$style\"\\s*>\\s*(((?!</para>).)*)</para>", RegexOption.DOT_MATCHES_ALL)
        }

        /**
         * Returns a regex that matches a single para tag e.g. <para style=""/>
         */
        private fun paraShortPattern(style: String): Regex {
            return Regex("<para\\s+style=\"$style\"\\s*/>", RegexOption.DOT_MATCHES_ALL)
        }
    }

    /**
     * Renders the usx input into usfm
     * @param input the raw input string
     */
    fun convert(input: CharSequence): CharSequence {
        var out = input

        out = trimWhitespace(out)
        out = renderLineBreaks(out)
        // TODO: this will strip out new lines. Eventually we may want to convert these to paragraphs.
        out = renderWhiteSpace(out)
        out = renderMajorSectionHeading(out)
        out = renderSectionHeading(out)
        out = renderParagraph(out)
        out = renderBlankLine(out)
        out = renderPoeticLine(out)
        out = renderRightAlignedPoeticLine(out)
        out = renderVerse(out)
        out = renderNote(out)
        out = renderChapterLabel(out)
        out = renderChar(out)

        return out
    }

    /**
     * Converts USX character-style tags to USFM paired markers.
     * <char style="X">content</char> → \X content\X*
     */
    private fun renderChar(input: CharSequence): CharSequence {
        var out = input.toString()
        out = Regex("<char\\s+style=\"([^\"]+)\"\\s*>(.*?)</char>", RegexOption.DOT_MATCHES_ALL)
            .replace(out) { m ->
                val style = m.groupValues[1]
                val content = m.groupValues[2].trim()
                "\\$style $content\\$style*"
            }
        // strip malformed/unmatched char tags
        out = Regex("</?char\\b[^>]*/?>").replace(out, "")
        return out
    }

    /**
     * Strips leading and trailing whitespace
     */
    fun trimWhitespace(input: CharSequence): CharSequence {
        return input.toString().trim()
    }

    fun renderSectionHeading(input: CharSequence): CharSequence = input

    fun renderMajorSectionHeading(input: CharSequence): CharSequence = input

    /**
     * Strips out extra whitespace from the text, collapsing runs of whitespace into a single space.
     */
    fun renderWhiteSpace(input: CharSequence): CharSequence {
        return Regex("\\s+").replace(input.toString(), " ")
    }

    /**
     * Strips out new lines and replaces them with a single space
     */
    fun renderLineBreaks(input: CharSequence): CharSequence {
        return Regex("\\s*\\n+\\s*").replace(input.toString(), " ")
    }

    /**
     * Renders all note tags
     */
    fun renderNote(input: CharSequence): CharSequence {
        val source = input.toString()
        val sb = StringBuilder()
        val regex = Regex(USXNoteSpan.PATTERN)
        var lastIndex = 0

        for (match in regex.findAll(source)) {
            val note = USXNoteSpan.parseNote(match.value)
            sb.append(source, lastIndex, match.range.first)
            if (note != null) {
                val usfmNote = convertNoteSpanUSXtoUSFM(note)
                sb.append(usfmNote.machineReadable)
            } else {
                sb.append(match.value)
            }
            lastIndex = match.range.last + 1
        }
        sb.append(source, lastIndex, source.length)
        return sb.toString()
    }

    protected fun convertNoteSpanUSXtoUSFM(note: USXNoteSpan): USFMNoteSpan {
        val usfmChars = convertUsxChars(note)
        return USFMNoteSpan(note.style, note.caller, usfmChars)
    }

    private fun convertUsxChars(note: USXNoteSpan): List<USFMChar> {
        val usfmChars = mutableListOf<USFMChar>()
        for (c in note.chars) {
            val style = when (c.style) {
                USXChar.STYLE_PASSAGE_TEXT -> USFMChar.STYLE_PASSAGE_TEXT
                USXChar.STYLE_FOOTNOTE_REFERENCE -> USFMChar.STYLE_FOOTNOTE_REFERENCE
                USXChar.STYLE_FOOTNOTE_TEXT -> USFMChar.STYLE_FOOTNOTE_TEXT
                USXChar.STYLE_FOOTNOTE_KEYWORD -> USFMChar.STYLE_FOOTNOTE_KEYWORD
                USXChar.STYLE_FOOTNOTE_QUOTATION -> USFMChar.STYLE_FOOTNOTE_QUOTATION
                USXChar.STYLE_FOOTNOTE_ALT_QUOTATION -> USFMChar.STYLE_FOOTNOTE_ALT_QUOTATION
                USXChar.STYLE_FOOTNOTE_LABEL -> USFMChar.STYLE_FOOTNOTE_LABEL
                USXChar.STYLE_FOOTNOTE_PARAGRAPH -> USFMChar.STYLE_FOOTNOTE_PARAGRAPH
                USXChar.STYLE_FOOTNOTE_VERSE -> USFMChar.STYLE_FOOTNOTE_VERSE
                USXChar.STYLE_FOOTNOTE_DEUTEROCANONICAL_APOCRYPHA -> USFMChar.STYLE_FOOTNOTE_DEUTEROCANONICAL_APOCRYPHA
                else -> c.style
            }
            usfmChars.add(USFMChar(style, c.value))
        }
        return usfmChars
    }

    /**
     * Renders all verse tags
     */
    fun renderVerse(input: CharSequence): CharSequence {
        val source = input.toString()
        val sb = StringBuilder()
        val regex = Regex(USXVerseSpan.PATTERN)
        var lastIndex = 0
        val foundVerses = mutableListOf<Int>()

        for (match in regex.findAll(source)) {
            val verseStr = match.groupValues.getOrNull(1)?.takeIf { it.isNotEmpty() }
            sb.append(source, lastIndex, match.range.first)

            if (verseStr != null) {
                val verse = USFMVerseSpan(verseStr)
                val startVerse = verse.startVerseNumber
                val endVerse = verse.endVerseNumber
                var alreadyRendered = false

                if (endVerse > startVerse) {
                    for (i in startVerse..endVerse) {
                        if (!foundVerses.contains(i)) {
                            foundVerses.add(i)
                        } else {
                            alreadyRendered = true
                        }
                    }
                } else {
                    if (!foundVerses.contains(startVerse)) {
                        foundVerses.add(startVerse)
                    } else {
                        alreadyRendered = true
                    }
                }

                if (!alreadyRendered) {
                    sb.append(verse.machineReadable)
                }
            } else {
                sb.append(match.value)
            }
            lastIndex = match.range.last + 1
        }
        sb.append(source, lastIndex, source.length)
        return sb.toString()
    }

    fun renderParagraph(input: CharSequence): CharSequence {
        var out = input.toString()
        // <para style="X">content</para> → \n\X content
        out = Regex("<para\\s+style=\"([^\"]+)\"\\s*>(.*?)</para>", RegexOption.DOT_MATCHES_ALL)
            .replace(out) { m -> "\n\\${m.groupValues[1]} ${m.groupValues[2].trim()}" }
        // <para style="X"/> → \n\X
        out = Regex("<para\\s+style=\"([^\"]+)\"\\s*/>")
            .replace(out) { m -> "\n\\${m.groupValues[1]}" }
        // strip any malformed/unmatched para tags
        out = Regex("</?para\\b[^>]*/?>").replace(out, "")
        // trim leading newline if first marker was at start
        out = out.trimStart('\n', ' ')
        return out
    }

    fun renderBlankLine(input: CharSequence): CharSequence = input

    fun renderChapterLabel(input: CharSequence): CharSequence = input

    fun renderPoeticLine(input: CharSequence): CharSequence = input

    fun renderRightAlignedPoeticLine(input: CharSequence): CharSequence = input
}