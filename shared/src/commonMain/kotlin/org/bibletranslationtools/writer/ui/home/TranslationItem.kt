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
        get() = when {
            // helps and word projects have no resource slug; show the type
            translation.translationType != ResourceType.TEXT ->
                name + " (" + translation.translationType.title + ")"
            // display the resource type if not a regular resource e.g. this is for a gateway language
            translation.resourceSlug != Resource.REGULAR_SLUG && translation.resourceSlug != Resource.OBS_SLUG ->
                name + " (" + translation.resourceSlug + ")"
            else -> name
        }
}
