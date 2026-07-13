package org.bibletranslationtools.writer.ui.home

import org.bibletranslationtools.resourcecontainer.Resource
import org.bibletranslationtools.writer.core.ResourceType
import org.bibletranslationtools.writer.core.TargetTranslation

data class TranslationItem(
    val translation: TargetTranslation,
    val name: String,
    val progress: Float = 0f
) {
    val formattedProjectName: String
        get() = name

    // matches the desktop Type column: "Text", "Text ulb", "Notes", "Words", ...
    val formattedTypeName: String
        get() {
            val type = translation.translationType.title
            val slug = translation.resourceSlug
            return if (
                translation.translationType == ResourceType.TEXT &&
                slug != Resource.REGULAR_SLUG &&
                slug != Resource.OBS_SLUG &&
                slug != null
            ) "$type $slug" else type
        }
}
