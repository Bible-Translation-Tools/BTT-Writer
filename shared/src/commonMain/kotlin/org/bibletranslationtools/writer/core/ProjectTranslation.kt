package org.bibletranslationtools.writer.core

data class ProjectTranslation(
    val title: String,
    val isTitleFinished: Boolean,
    val description: String = ""
)