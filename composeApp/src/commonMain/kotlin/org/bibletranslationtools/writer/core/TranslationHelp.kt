package org.bibletranslationtools.writer.core

/**
 * Represents a single translation question
 */
data class TranslationHelp(
    /** title the question title */
    val title: String,
    /** the question body */
    val body: String
)
