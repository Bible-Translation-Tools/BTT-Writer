package org.bibletranslationtools.writer.rendering.spannables

import java.util.regex.Pattern

class MarkdownTitledLinkSpan(val title: String, val address: String) : Span(
    title,
    address
) {
    companion object {
        val PATTERN: Pattern = Pattern.compile("\\[(((?!]).)*)]\\(((((?!\\\\)).)*)\\)")
    }
}
