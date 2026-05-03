package org.bibletranslationtools.writer.ui.home

import org.bibletranslationtools.resourcecontainer.Resource
import org.bibletranslationtools.writer.core.TargetTranslation

data class TranslationItem(
    val translation: TargetTranslation,
    val name: String,
    val progress: Float = 0f
) {
    val formattedProjectName: String
        get() = if (translation.resourceSlug != Resource.REGULAR_SLUG && translation.resourceSlug != "obs") {
            // display the resource type if not a regular resource e.g. this is for a gateway language
            name + " (" + translation.resourceSlug + ")"
        } else name
}