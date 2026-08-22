package org.bibletranslationtools.writer.core

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.bibletranslationtools.logger.Logger

/**
 * Represents a single translation help (note, question or word entry)
 */
@Serializable
data class TranslationHelp(
    /** title the question title */
    val title: String,
    /** the question body */
    val body: String
) {
    companion object {
        // helps target translations store frames as a JSON array of {title, body};
        @OptIn(ExperimentalSerializationApi::class)
        private val json = Json {
            prettyPrint = true
            prettyPrintIndent = "\t"
            ignoreUnknownKeys = true
        }

        fun toJson(helps: List<TranslationHelp>): String {
            return if (helps.isEmpty()) "" else json.encodeToString(helps)
        }

        fun fromJson(text: String): List<TranslationHelp> {
            if (text.isBlank()) return emptyList()
            return try {
                json.decodeFromString(text)
            } catch (e: Exception) {
                Logger.e("TranslationHelp", "Data Parsing Error", e)
                listOf(TranslationHelp("Data Parsing Error", "Data Parsing Error"))
            }
        }
    }
}
