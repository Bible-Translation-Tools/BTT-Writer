package org.bibletranslationtools.writer.unit.core

import org.bibletranslationtools.writer.core.FontNameReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FontNameReaderTest {

    @Test
    fun readsWindowsFullName_nameId4_utf16be() {
        val bytes = sfntWithNames(
            NameRecord(platformId = 3, encodingId = 1, nameId = 4, value = "Charis SIL Regular")
        )
        assertEquals("Charis SIL Regular", FontNameReader.readDisplayName(bytes))
    }

    @Test
    fun fallsBackToFamily_nameId1_whenFullNameAbsent() {
        val bytes = sfntWithNames(
            NameRecord(platformId = 3, encodingId = 1, nameId = 1, value = "Charis SIL")
        )
        assertEquals("Charis SIL", FontNameReader.readDisplayName(bytes))
    }

    @Test
    fun prefersFullName_nameId4_overFamily_nameId1() {
        val bytes = sfntWithNames(
            NameRecord(platformId = 3, encodingId = 1, nameId = 1, value = "Charis SIL"),
            NameRecord(platformId = 3, encodingId = 1, nameId = 4, value = "Charis SIL Bold")
        )
        assertEquals("Charis SIL Bold", FontNameReader.readDisplayName(bytes))
    }

    @Test
    fun readsMacRoman_platform1_ascii() {
        val bytes = sfntWithNames(
            NameRecord(platformId = 1, encodingId = 0, nameId = 4, value = "Padauk")
        )
        assertEquals("Padauk", FontNameReader.readDisplayName(bytes))
    }

    @Test
    fun returnsNull_forGarbageBytes() {
        assertNull(FontNameReader.readDisplayName(byteArrayOf(0, 1, 2, 3, 4)))
    }

    // --- minimal sfnt builder for tests ---

    private data class NameRecord(
        val platformId: Int,
        val encodingId: Int,
        val nameId: Int,
        val value: String,
        val languageId: Int = if (platformId == 3) 0x0409 else 0
    )

    private fun u16(v: Int) = byteArrayOf((v ushr 8).toByte(), v.toByte())
    private fun u32(v: Int) = byteArrayOf(
        (v ushr 24).toByte(), (v ushr 16).toByte(), (v ushr 8).toByte(), v.toByte()
    )

    private fun encode(r: NameRecord): ByteArray =
        if (r.platformId == 1) r.value.encodeToByteArray()
        else r.value.flatMap { listOf((it.code ushr 8).toByte(), it.code.toByte()) }.toByteArray()

    private fun sfntWithNames(vararg records: NameRecord): ByteArray {
        val encoded = records.map { encode(it) }
        val count = records.size
        val storageStart = 6 + 12 * count

        val nameTable = ArrayList<Byte>()
        nameTable += u16(0).toList()       // format
        nameTable += u16(count).toList()   // count
        nameTable += u16(storageStart).toList() // stringOffset

        var strOffset = 0
        records.forEachIndexed { i, r ->
            nameTable += u16(r.platformId).toList()
            nameTable += u16(r.encodingId).toList()
            nameTable += u16(r.languageId).toList()
            nameTable += u16(r.nameId).toList()
            nameTable += u16(encoded[i].size).toList()
            nameTable += u16(strOffset).toList()
            strOffset += encoded[i].size
        }
        encoded.forEach { nameTable += it.toList() }
        val nameBytes = nameTable.toByteArray()

        val sfnt = ArrayList<Byte>()
        sfnt += u32(0x00010000).toList() // sfnt version
        sfnt += u16(1).toList()          // numTables
        sfnt += u16(16).toList()         // searchRange
        sfnt += u16(0).toList()          // entrySelector
        sfnt += u16(0).toList()          // rangeShift
        // table record for "name"
        sfnt += "name".encodeToByteArray().toList()
        sfnt += u32(0).toList()          // checksum
        sfnt += u32(28).toList()         // offset (12 + 16)
        sfnt += u32(nameBytes.size).toList()
        sfnt += nameBytes.toList()
        return sfnt.toByteArray()
    }
}
