package org.bibletranslationtools.writer.core

/**
 * Reads the human-readable font name from the sfnt `name` table of a TrueType/OpenType
 * font. Returns the full font name (nameID 4) when present, otherwise the family name
 * (nameID 1), or null when the bytes aren't a parseable sfnt font.
 *
 * See https://learn.microsoft.com/typography/opentype/spec/name
 */
object FontNameReader {

    private const val NAME_ID_FAMILY = 1
    private const val NAME_ID_FULL = 4

    fun readDisplayName(bytes: ByteArray): String? {
        return runCatching { parse(bytes) }.getOrNull()
    }

    private fun parse(b: ByteArray): String? {
        if (b.size < 12) return null

        val numTables = u16(b, 4)
        var recordOffset = 12
        var nameTableOffset = -1
        repeat(numTables) {
            if (recordOffset + 16 > b.size) return null
            val tag = b.decodeToString(recordOffset, recordOffset + 4)
            if (tag == "name") {
                nameTableOffset = u32(b, recordOffset + 8)
            }
            recordOffset += 16
        }
        if (nameTableOffset < 0 || nameTableOffset + 6 > b.size) return null

        val count = u16(b, nameTableOffset + 2)
        val stringStorage = nameTableOffset + u16(b, nameTableOffset + 4)
        val firstRecord = nameTableOffset + 6

        // Track best candidate; prefer full name (4) over family (1).
        var family: String? = null
        var full: String? = null

        repeat(count) { i ->
            val rec = firstRecord + i * 12
            if (rec + 12 > b.size) return@repeat
            val platformId = u16(b, rec)
            val nameId = u16(b, rec + 6)
            if (nameId != NAME_ID_FAMILY && nameId != NAME_ID_FULL) return@repeat

            val length = u16(b, rec + 8)
            val offset = stringStorage + u16(b, rec + 10)
            if (offset + length > b.size) return@repeat

            val value = decode(b, offset, length, platformId) ?: return@repeat
            when (nameId) {
                NAME_ID_FULL -> if (full == null) full = value
                NAME_ID_FAMILY -> if (family == null) family = value
            }
        }
        return (full ?: family)?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun decode(b: ByteArray, offset: Int, length: Int, platformId: Int): String? {
        val slice = b.copyOfRange(offset, offset + length)
        return when (platformId) {
            // Windows (3) and Unicode (0) store UTF-16BE.
            0, 3 -> if (length % 2 != 0) null else buildString {
                var i = 0
                while (i < slice.size) {
                    val code = ((slice[i].toInt() and 0xFF) shl 8) or (slice[i + 1].toInt() and 0xFF)
                    append(code.toChar())
                    i += 2
                }
            }
            // Macintosh (1) Roman — treat as Latin-1/ASCII.
            else -> buildString {
                for (byte in slice) append((byte.toInt() and 0xFF).toChar())
            }
        }
    }

    private fun u16(b: ByteArray, i: Int): Int =
        ((b[i].toInt() and 0xFF) shl 8) or (b[i + 1].toInt() and 0xFF)

    private fun u32(b: ByteArray, i: Int): Int =
        ((b[i].toInt() and 0xFF) shl 24) or
            ((b[i + 1].toInt() and 0xFF) shl 16) or
            ((b[i + 2].toInt() and 0xFF) shl 8) or
            (b[i + 3].toInt() and 0xFF)
}
