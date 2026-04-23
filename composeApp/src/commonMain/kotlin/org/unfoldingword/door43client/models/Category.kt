package org.unfoldingword.door43client.models

import kotlinx.serialization.Serializable

/**
 * Represents a project category. e.g. a group of projects.
 */
@Serializable
class Category(
    /** the category code */
    val slug: String,
    /** the name of the category */
    val name: String
)
