package org.bibletranslationtools.writer.core

import java.util.regex.Pattern

const val WORDS_CHAPTER = "01"
val WORD_PATTERN: Pattern = Pattern.compile("#+([^\\n]+)\\n+([\\s\\S]*)")