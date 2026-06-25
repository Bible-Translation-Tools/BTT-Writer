package org.bibletranslationtools.writer.utils

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import java.io.File
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.time.Clock
import kotlin.time.Instant

object DateUtils {

    private val defaultDateTimeFormat = LocalDateTime.Format {
        year()
        char('-')
        monthNumber(Padding.ZERO)
        char('-')
        day(padding = Padding.ZERO)
        char(' ')
        hour(Padding.ZERO)
        char(':')
        minute(Padding.ZERO)
        char(':')
        second(Padding.ZERO)
    }

    /**
     * Retrieves the current system date and time formatted as a string
     * using the type-safe DSL format.
     */
    fun getCurrentDateTime(timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
        val currentInstant = Clock.System.now()
        val localDateTime = currentInstant.toLocalDateTime(timeZone)
        return localDateTime.format(defaultDateTimeFormat)
    }

    /**
     * Formats a [LocalDateTime] into a date-time string matching a medium localized style.
     * * *Example output:* `Jan 12, 1952, 3:30:32 PM`
     */
    fun dateToDateTime(
        dateTime: LocalDateTime,
        locale: Locale = Locale.getDefault()
    ): String {
        val formatter = DateTimeFormatter
            .ofLocalizedDateTime(FormatStyle.MEDIUM)
            .withLocale(locale)
        return dateTime.toJavaLocalDateTime().format(formatter)
    }

    /**
     * Parses the given [dateString] to a [LocalDateTime] object using a type-safe Kotlin DSL format.
     * @return Returns [LocalDateTime] object or null if [dateString] was invalid.
     */
    fun parseDateString(dateString: String): LocalDateTime? {
        return try {
            defaultDateTimeFormat.parse(dateString)
        } catch (_: Exception) {
            null
        }
    }
}

fun File.getLastModified(
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): LocalDateTime {
    val instant = Instant.fromEpochMilliseconds(this.lastModified())
    return instant.toLocalDateTime(timeZone)
}