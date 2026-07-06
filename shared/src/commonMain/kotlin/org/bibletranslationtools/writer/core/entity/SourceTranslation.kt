package org.bibletranslationtools.writer.core.entity

import org.bibletranslationtools.resourcecatalog.library.models.Translation
import org.bibletranslationtools.resourcecontainer.Language
import org.bibletranslationtools.resourcecontainer.Project
import org.bibletranslationtools.resourcecontainer.Resource

data class SourceTranslation(
    val language: Language,
    val project: Project,
    val resource: Resource,
    val modifiedTimestamp: Int = -1
)

fun Translation.toSourceTranslation(modifiedTimestamp: Int): SourceTranslation =
    SourceTranslation(
        this.language,
        this.project,
        this.resource,
        modifiedTimestamp
    )