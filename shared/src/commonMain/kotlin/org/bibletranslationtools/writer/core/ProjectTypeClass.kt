package org.bibletranslationtools.writer.core

/**
 * Groups translation types by editing workflow:
 * standard book text, helps (notes/questions) and extant (word lists).
 */
enum class ProjectTypeClass {
    STANDARD,
    HELPS,
    EXTANT;

    companion object {
        fun of(type: ResourceType): ProjectTypeClass = when (type) {
            ResourceType.TRANSLATION_WORD -> EXTANT
            ResourceType.TRANSLATION_NOTE, ResourceType.TRANSLATION_QUESTION -> HELPS
            else -> STANDARD
        }
    }
}
