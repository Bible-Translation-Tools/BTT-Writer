package org.unfoldingword.door43client.models

import kotlinx.serialization.Serializable

/**
 * Represents the beginning of a chunk in a chapter
 */
@Serializable
class ChunkMarker(
    /** the chapter this chunk exists in */
    val chapter: String,
    /** the verse at which this chunk starts */
    val verse: String
)
