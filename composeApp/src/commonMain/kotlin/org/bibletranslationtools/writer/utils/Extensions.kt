package org.bibletranslationtools.writer.utils

import btt_writer.composeapp.generated.resources.Res
import btt_writer.composeapp.generated.resources.allStringResources
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

/**
 * Get formatted string blocking the current thread
 * Use with caution
 */
fun getStringBlocking(resource: StringResource, vararg formatArgs: Any) = runBlocking {
    getString(resource, *formatArgs)
}

fun resolveStringResource(stringResourceKey: String, defaultKey: String = "empty"): StringResource {
    return Res.allStringResources[stringResourceKey]
        ?: Res.allStringResources[defaultKey]
        ?: throw IllegalArgumentException("Resource by key $stringResourceKey or $defaultKey are not found.")
}