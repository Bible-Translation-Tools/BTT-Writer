package org.unfoldingword.door43client.models

import org.unfoldingword.resourcecontainer.Language
import kotlinx.serialization.Serializable

/**
 * Represents a language that a resource exists in (for the purpose of source content)
 */
@Serializable
data class SourceLanguage(
    val slug: String,
    val name: String,
    val direction: String
)

fun Language.toSource(): SourceLanguage {
    return SourceLanguage(
        slug = slug,
        name = name,
        direction = direction
    )
}