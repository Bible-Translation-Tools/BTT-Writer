package org.unfoldingword.door43client.models

import kotlinx.serialization.Serializable

/**
 * Represents a versification system.
 * This is what chunk markers are based on.
 */
@Serializable
data class Versification(
    var slug: String,
    var name: String
) {
    var rowId: Long = -1L
}