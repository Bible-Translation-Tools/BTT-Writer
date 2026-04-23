package org.unfoldingword.door43client

import kotlinx.serialization.json.Json

val JsonLenient = Json {
    isLenient = true
    ignoreUnknownKeys = true
    coerceInputValues = true
    encodeDefaults = true
}