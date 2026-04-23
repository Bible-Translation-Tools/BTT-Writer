package org.unfoldingword.door43client.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.unfoldingword.resourcecontainer.Language

/**
 * Represents a language that a resource will be translated into
 */
@Serializable
data class TargetLanguage(
    @SerialName("lc")
    val slug: String,
    @SerialName("ln")
    val name: String,
    @SerialName("ang")
    val anglicizedName: String,
    @SerialName("ld")
    val direction: String,
    @SerialName("lr")
    val region: String,
    @SerialName("gl")
    val isGatewayLanguage: Boolean
)

fun Language.toTarget(): TargetLanguage {
    return TargetLanguage(
        slug = slug,
        name = name,
        anglicizedName = "",
        direction = direction,
        region = "",
        isGatewayLanguage = false
    )
}