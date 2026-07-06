package org.bibletranslationtools.writer.utils

import kotlinx.serialization.json.Json

val JsonLenient = Json {
    isLenient = true
    ignoreUnknownKeys = true
    coerceInputValues = true
    encodeDefaults = false
}