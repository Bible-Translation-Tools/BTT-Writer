package org.bibletranslationtools.writer.integration.core

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.bibletranslationtools.writer.BaseIntegrationTest
import org.bibletranslationtools.writer.TestUtils
import org.bibletranslationtools.writer.core.ArchiveMigrator
import org.bibletranslationtools.writer.core.manifest.Manifest
import org.bibletranslationtools.writer.utils.Zip
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.koin.test.inject
import java.io.File

class ArchiveMigratorTest : BaseIntegrationTest() {

    private val migrator: ArchiveMigrator by inject()

    private var workDir: File? = null

    @After
    fun cleanup() {
        workDir?.deleteRecursively()
        workDir = null
    }

    @Test
    fun migrateV1Archive() = runTest {
        val (archiveDir, manifestFile) = extractArchive("exports/aa_jud_text_reg.tstudio")

        val before = Json.parseToJsonElement(manifestFile.readText()).jsonObject
        assertEquals(
            "Fixture should start as v1",
            1,
            before["package_version"]!!.jsonPrimitive.content.toInt()
        )
        assertNotNull("v1 fixture should use 'projects' key", before["projects"])

        val result = migrator.migrate(archiveDir)

        assertNotNull("Migration should succeed", result)

        val after = Json.parseToJsonElement(manifestFile.readText()).jsonObject
        assertEquals(3, after["package_version"]!!.jsonPrimitive.content.toInt())

        val translations = after["target_translations"]!!.jsonArray
        assertEquals(1, translations.size)
        val first = translations[0].jsonObject
        assertEquals("aa_jud_text_reg", first["id"]!!.jsonPrimitive.content)
        assertEquals("ltr", first["direction"]!!.jsonPrimitive.content)
        assertEquals(
            "ea8aac626a2234deedba200d76acb625efe631f3",
            first["commit_hash"]!!.jsonPrimitive.content
        )
    }

    @Test
    fun migrateV2Archive() = runTest {
        val (archiveDir, manifestFile) = extractArchive("exports/aaa_mrk_text_reg.tstudio")

        val before = Json.parseToJsonElement(manifestFile.readText()).jsonObject
        assertEquals(
            "Fixture should start as v2",
            2,
            before["package_version"]!!.jsonPrimitive.content.toInt()
        )

        val result = migrator.migrate(archiveDir)

        assertNotNull("Migration should succeed", result)

        val after = Json.parseToJsonElement(manifestFile.readText()).jsonObject
        assertEquals(3, after["package_version"]!!.jsonPrimitive.content.toInt())

        val translations = after["target_translations"]!!.jsonArray
        assertEquals(1, translations.size)
        val first = translations[0].jsonObject
        assertEquals("aaa_mrk_text_reg", first["id"]!!.jsonPrimitive.content)
        assertEquals(
            "24c20db18889d801a20731a945607cb1d8b09125",
            first["commit_hash"]!!.jsonPrimitive.content
        )
    }

    private suspend fun extractArchive(resourcePath: String): Pair<File, File> {
        val dir = directoryProvider.createTempDir("archive-migrator-it")
        workDir = dir
        TestUtils.getResourceStream(resourcePath).use {
            Zip.unzipFromStream(it, dir)
        }
        val manifestFile = File(dir, Manifest.MANIFEST_FILE)
        assertTrue("Archive should contain top-level manifest.json", manifestFile.exists())
        return dir to manifestFile
    }
}
