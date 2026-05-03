package org.bibletranslationtools.writer.utils

import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

/**
 * Get formatted string blocking the current thread
 * Use with caution
 */
fun getStringBlocking(resource: StringResource, vararg formatArgs: Any) = runBlocking {
    getString(resource, formatArgs)
}