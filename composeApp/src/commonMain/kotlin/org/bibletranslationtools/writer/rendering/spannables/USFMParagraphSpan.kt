package org.bibletranslationtools.writer.rendering.spannables

class USFMParagraphSpan : ParagraphSpan("\n", "\\p ") {

    companion object {
        const val PATTERN = "\\\\p\\W?"
    }
}
