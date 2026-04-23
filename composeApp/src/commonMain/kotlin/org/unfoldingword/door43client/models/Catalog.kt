package org.unfoldingword.door43client.models

import kotlinx.serialization.Serializable

/**
 * Represents a global catalog
 */
@Serializable
data class Catalog(
    /** the catalog code */
    val slug: String,
    /** the url where the catalog exists */
    val url: String,
    /** when the catalog was last modified */
    val modifiedAt: Int
)
